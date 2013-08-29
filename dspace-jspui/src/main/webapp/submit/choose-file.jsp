<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.submit.AbstractProcessingStep" %>
<%@ page import="org.dspace.submit.step.UploadStep" %>
<%@ page import="org.dspace.app.util.DCInputSet" %>
<%@ page import="org.dspace.app.util.DCInputsReader" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>


<%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);    

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);
   
 	// Determine whether a file is REQUIRED to be uploaded (default to true)
 	boolean fileRequired = ConfigurationManager.getBooleanProperty("webui.submit.upload.required", true);
    boolean ajaxProgress = ConfigurationManager.getBooleanProperty("webui.submit.upload.ajax", true);

    if (ajaxProgress)
    {
 %>
<c:set var="dspace.layout.head.last" scope="request">
	<link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/jquery.fileupload-ui.css">
	<!-- CSS adjustments for browsers with JavaScript disabled -->
	<noscript><link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/jquery.fileupload-ui-noscript.css"></noscript>
	<script type="text/javascript" src="<%= request.getContextPath() %>/static/js/jquery/jquery.iframe-transport.js"></script>
	<script type="text/javascript" src="<%= request.getContextPath() %>/static/js/jquery/jquery.fileupload.js"></script>
    <script type="text/javascript">
    function setupAjaxUpload($, data){
    	var progressbarArea = $("#progressBarArea");
		progressbarArea.find('p.progressBarInitMsg').show();
   		progressbarArea.find('p.progressBarProgressMsg').hide();
   		progressbarArea.find('p.progressBarCompleteMsg').hide();
   		progressbarArea.hide();
   		
   		var inputToRestore = $('#uploadForm').data('inputToRestore');
   		if (inputToRestore == null) {
			inputToRestore = $('#uploadForm').find('input[type="submit"]').not(":disabled");   			
   		}
		inputToRestore.removeAttr('disabled');
		inputToRestore.off('click.uploadProgressDS');
   		if (data != null) {
   			data.files = [];
			$('#uploadForm').find('input[name="ajaxUpload"]').remove();
			$('#selectedFile').remove();
   		}
    }
    
    function updateProgressBar($, data){
    	var percent = parseInt(data.loaded / data.total * 100, 10);
		var progressbarArea = $("#progressBarArea");
		var progressbar = $("#progressBar");
		progressbar.progressbar({ value: data.loaded, max: data.total});
        progressbarArea.find('p.progressBarInitMsg').hide();
       	progressbarArea.find('p.progressBarProgressMsg').show();
   		progressbarArea.find('p.progressBarCompleteMsg').hide();
       	progressbarArea.find('span.bytesRead').html(data.loaded);
       	progressbarArea.find('span.bytesTotal').html(data.total);
       	progressbarArea.find('span.percent').html(percent);
    }
    
    var progresseventwork = false;
    
	jQuery(document).ready(function($){		
		$('#tfile').wrap('<span id="spanFile" class="fileinput-button"><span><fmt:message key="jsp.submit.choose-file.upload-ajax.button.select-file"/></span>');
		$('#spanFile').button({icons: {primary: "ui-icon ui-icon-folder-open"}});
		$('#uploadForm').fileupload({
				sequentialUploads: true,
				autoUpload: false,
				maxNumberOfFiles: 1,
				<% if (false && ConfigurationManager.getLongProperty("upload.max") != -1) { %>
				maxFileSize: <%=ConfigurationManager.getLongProperty("upload.max") %>,
				<% } %>
		        dataType: 'json'})
        .on('fileuploaddone', function (e, data) {
        	var resultFile = data.result.files[0];
    		if (resultFile.status == <%= UploadStep.STATUS_COMPLETE %> || 
    				resultFile.status == <%= UploadStep.STATUS_UNKNOWN_FORMAT %>)
    		{
    			var progressbarArea = $("#progressBarArea");
	    		var progressbar = $("#progressBar");
           		progressbarArea.find('p.progressBarProgressMsg').hide();
           		progressbarArea.find('p.progressBarCompleteMsg').show();
           		progressbar.progressbar({ value: data.loaded, max: data.total});
	           	progressbarArea.find('span.bytesTotal').html(data.total);
	           	if (resultFile.status == <%= UploadStep.STATUS_COMPLETE %>)
           		{
	           		$('#uploadFormPostAjax').removeAttr('enctype')
	           			.append('<input type="hidden" name="<%= UploadStep.SUBMIT_UPLOAD_BUTTON %>" value="1">');
           		}
	           	else
           		{
	           		$('#uploadFormPostAjax')
           				.append('<input type="hidden" name="submit_format_'+resultFile.bitstreamID+'" value="1">')
       					.append('<input type="hidden" name="bitstream_id" value="'+resultFile.bitstreamID+'">');
           		}
	           	
	           	$('#uploadFormPostAjax').submit();	
    		}
    		else {
    			//FIXME: trying to reset the form to allow a new upload
                // let to strange behaviour where old XHR will be resumed producing
                // fake upload and/or invalidating the progress bar count
    			if (resultFile.status == <%= UploadStep.STATUS_NO_FILES_ERROR %>) {
		        	//setupAjaxUpload($, data);
               		$('#uploadFormPostAjax')
    	       			.append('<input type="hidden" name="<%= UploadStep.SUBMIT_MORE_BUTTON %>" value="1">');
	           		$('#uploadFormPostAjax').submit();
    			}
    			else if (resultFile.status == <%= UploadStep.STATUS_VIRUS_CHECKER_UNAVAILABLE %>) {
		        	//setupAjaxUpload($, data);
					$('#virusCheckNA').dialog("open");
    			}
				else if (resultFile.status == <%= UploadStep.STATUS_CONTAINS_VIRUS %>) {
		        	//setupAjaxUpload($, data);
					$('#virusFound').dialog("open");				
    			}
				else {
		        	//setupAjaxUpload($, data);
					$('#uploadError').dialog("open");
				}
    		}    		
            })
        .on('fileuploadadd', function (e, data) {
	            if ($('#selectedFile').length > 0)
	            {
					$('#selectedFile').html(data.files[0].name);			
	            }
				else
				{
	        		$('<p id="selectedFile">'+data.files[0].name+'</p>').insertAfter($('#spanFile')).append('&nbsp;');		        		
	        		var span = $('<span id="spanSelectedFile"><fmt:message key="jsp.submit.choose-file.upload-ajax.button.cancel"/></span>');
	        		span.appendTo($('#selectedFile'));
	        		$('#uploadForm').append('<input type="hidden" name="ajaxUpload" value="true">');
	        		span.button({icons: {primary: "ui-icon ui-icon-cancel"}})
	        			.click(function(e){
	        				setupAjaxUpload($, data);
	        		});
        		
	        		var inputToBind = $('#uploadForm').find('input[type="submit"]').not(":disabled");
	        		var $this = $(this);
	        		inputToBind.on('click.uploadProgressDS', function(e){
	        			e.preventDefault();
	        			data.submit();
						$('#uploadCancel').button().click(function (e) {
						    e.preventDefault();
						    if (!data.jqXHR) {
				                data.context = data.context || template;
				                data.errorThrown = 'abort';
				                $this._trigger('fail', e, data);
				            } else {
				                data.jqXHR.abort();
				            }
						});
						var inputToDisable = $('#uploadForm').find('input[type="submit"]').not(":disabled");
						$('#uploadForm').data('inputToRestore', inputToDisable);
						inputToDisable.attr('disabled','disabled');
						$('#spanSelectedFile').remove();
		            	return false;
	    			});
       			}
       	})
        .on('fileuploadfail', function (e, data) {
	        	e.preventDefault();    
                //FIXME: trying to reset the form to allow a new upload
                // let to strange behaviour where old XHR will be resumed producing
                // fake upload and/or invalidating the progress bar count
	        	//setupAjaxUpload($, data);
	        	$('#uploadError').dialog("open");
    	})
		.on('fileuploadsubmit', function (e, data) {
			var progressbarArea = $("#progressBarArea");
    		var progressbar = $("#progressBar");
			progressbarArea.show();
			progressbar.progressbar({value: false });
			// workaround to track progress on old browser using server side listner
			var monitorProgressJSON = function(){
				if (!progresseventwork)
				{
					$.ajax({
						cache: false,
				        url: '<%= request.getContextPath() %>/json/uploadProgress'})
				    .done(function(progress) {
				    	var data = {loaded: progress.readBytes, total: progress.totalBytes};
				    	updateProgressBar($, data);
				    	setTimeout(function() {										
							monitorProgressJSON();					
						}, 250);
				    });					
				}
			};
			setTimeout(function() {
					monitorProgressJSON();					
			}, 100);
		})
		// commenting this event listner the progress will be tracked using the server side json
		.on('fileuploadprogress', function (e, data) {
			progresseventwork = true;
			updateProgressBar($, data);
    	});		
		setupAjaxUpload($, null);
		
		$('#uploadError').dialog({modal: true, autoOpen: false, buttons: {
			'<fmt:message key="jsp.submit.choose-file.upload-ajax.dialog.close"/>': function() {
				$(this).dialog("close");
				$('#uploadFormPostAjax')
       				.append('<input type="hidden" name="<%= UploadStep.SUBMIT_MORE_BUTTON %>" value="1">');
       			$('#uploadFormPostAjax').submit();
		}
		}});
		
		$('#virusFound').dialog({modal: true, autoOpen: false, buttons: {
			'<fmt:message key="jsp.submit.choose-file.upload-ajax.dialog.close"/>': function() {
				$('#uploadFormPostAjax')
       				.append('<input type="hidden" name="<%= UploadStep.SUBMIT_MORE_BUTTON %>" value="1">');
       			$('#uploadFormPostAjax').submit();
				$(this).dialog("close");
		}
		}});
		
		$('#virusCheckNA').dialog({modal: true, autoOpen:false, buttons: {
			'<fmt:message key="jsp.submit.choose-file.upload-ajax.dialog.close"/>': function() {
				$('#uploadFormPostAjax')
       				.append('<input type="hidden" name="<%= UploadStep.SUBMIT_MORE_BUTTON %>" value="1">');
       			$('#uploadFormPostAjax').submit();
				$(this).dialog("close");
			}
		}});
	});
    </script>
</c:set>
<%  } %>


<dspace:layout locbar="off"
               navbar="off"
               titlekey="jsp.submit.choose-file.title"
               nocache="true">
<% if (ajaxProgress) { %>
	<div style="display:none;" id="uploadError" title="<fmt:message key="jsp.submit.choose-file.upload-ajax.upload-error.title" />">
		<p><fmt:message key="jsp.submit.upload-error.info" /></p>
	</div>
	<div style="display:none;" id="virusFound" title="<fmt:message key="jsp.submit.choose-file.upload-ajax.virus-found.title" />">
		<p><fmt:message key="jsp.submit.virus-error.info" /></p>
	</div>
	<div style="display:none;" id="virusCheckNA" title="jsp.submit.choose-file.upload-ajax.virus-checkna.title">
		<p><fmt:message key="jsp.submit.virus-checker-error.info" /></p>
	</div>
    <form style="display:none;" id="uploadFormPostAjax" method="post" action="<%= request.getContextPath() %>/submit" 
    	enctype="multipart/form-data" onkeydown="return disableEnterKey(event);">
    <%= SubmissionController.getSubmissionParameters(context, request) %>    
    </form>
<% } %>
    <form id="uploadForm" method="post" action="<%= request.getContextPath() %>/submit" enctype="multipart/form-data" onkeydown="return disableEnterKey(event);">
		
		<jsp:include page="/submit/progressbar.jsp"/>
		<%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>

        <%-- <h1>Submit: Upload a File</h1> --%>
		<h1><fmt:message key="jsp.submit.choose-file.heading"/></h1>
    
        <%-- <p>Please enter the name of
        <%= (si.submission.hasMultipleFiles() ? "one of the files" : "the file" ) %> on your
        local hard drive corresponding to your item.  If you click "Browse...", a
        new window will appear in which you can locate and select the file on your
        local hard drive. <object><dspace:popup page="/help/index.html#upload">(More Help...)</dspace:popup></object></p> --%>

		<p><fmt:message key="jsp.submit.choose-file.info1"/>
			<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#upload\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup></p>
        
        <%-- FIXME: Collection-specific stuff should go here? --%>
        <%-- <p class="submitFormHelp">Please also note that the DSpace system is
        able to preserve the content of certain types of files better than other
        types.
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.formats\")%>">Information about file types</dspace:popup> and levels of
        support for each are available.</p> --%>
        
		<div class="submitFormHelp"><fmt:message key="jsp.submit.choose-file.info6"/>
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.formats\")%>"><fmt:message key="jsp.submit.choose-file.info7"/></dspace:popup>
        </div>

<% if (ajaxProgress)
{
%>
       <div id="progressBarArea" style="display: none;  width: 50%; float: right;">
               <div id="progressBar"></div>
               <p class="progressBarInitMsg">
               			<fmt:message key="jsp.submit.choose-file.upload-ajax.uploadInit"/>
               	</p>
               <p class="progressBarProgressMsg" style="display: none;">
                       <fmt:message key="jsp.submit.choose-file.upload-ajax.uploadInProgress">
                               <fmt:param><span class="percent">&nbsp;</span></fmt:param>
                               <fmt:param><span class="bytesRead">&nbsp;</span></fmt:param>
                               <fmt:param><span class="bytesTotal">&nbsp;</span></fmt:param>
                       </fmt:message></p>
               <p class="progressBarCompleteMsg" style="display: none;">
                       <fmt:message key="jsp.submit.choose-file.upload-ajax.uploadCompleted">
                               <fmt:param><span class="bytesTotal">&nbsp;</span></fmt:param>
                       </fmt:message></p>
               <button id="uploadCancel"><fmt:message key="jsp.submit.choose-file.upload-ajax.button.cancel" /></button>        
       </div>
<% } %>
    
        <table border="0" align="center">
            <tr>
                <td class="submitFormLabel">
                    <%-- Document File: --%>
					<label for="tfile"><fmt:message key="jsp.submit.choose-file.document"/></label>
                </td>
                <td>
                    <input type="file" size="40" name="file" id="tfile" />
                </td>
            </tr>
<%
    if (subInfo.getSubmissionItem().hasMultipleFiles())
    {
%>
            <tr>
                <td colspan="2">&nbsp;</td>
            </tr>
            <tr>
                <td class="submitFormHelp" colspan="2">
                    <%-- Please give a brief description of the contents of this file, for
                    example "Main article", or "Experiment data readings." --%>
					<fmt:message key="jsp.submit.choose-file.info9"/>
                </td>
            </tr>
            <tr>
                <%-- <td class="submitFormLabel">File Description:</td> --%>
				<td class="submitFormLabel"><label for="tdescription"><fmt:message key="jsp.submit.choose-file.filedescr"/></label></td>
                <td><input type="text" name="description" id="tdescription" size="40"/></td>
            </tr>
<%
    }
%>
        </table>
        
		<%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>
    
        <p>&nbsp;</p>

        <center>
            <table border="0" width="80%">
                <tr>
                    <td width="100%">&nbsp;</td>
               	<%  //if not first step, show "Previous" button
					if(!SubmissionController.isFirstStep(request, subInfo))
					{ %>
                    <td>
                        <input type="submit" name="<%=AbstractProcessingStep.PREVIOUS_BUTTON%>" value="<fmt:message key="jsp.submit.general.previous"/>" />
                    </td>
				<%  } %>
                    <td>
                        <input type="submit" name="<%=UploadStep.SUBMIT_UPLOAD_BUTTON%>" value="<fmt:message key="jsp.submit.general.next"/>" />
                    </td> 
                    <%
                        //if upload is set to optional, or user returned to this page after pressing "Add Another File" button
                    	if (!fileRequired || UIUtil.getSubmitButton(request, "").equals(UploadStep.SUBMIT_MORE_BUTTON))
                        {
                    %>
                        	<td>
                                <input type="submit" name="<%=UploadStep.SUBMIT_SKIP_BUTTON%>" value="<fmt:message key="jsp.submit.choose-file.skip"/>" />
                            </td>
                    <%
                        }
                    %>   
                              
                    <td>&nbsp;&nbsp;&nbsp;</td>
                    <td align="right">
                        <input type="submit" name="<%=AbstractProcessingStep.CANCEL_BUTTON%>" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>" />
                    </td>
                </tr>
            </table>
        </center>  
    </form>

</dspace:layout>
