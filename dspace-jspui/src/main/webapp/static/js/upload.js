function startUploadAJAX(myform, contextPath) {
	jQuery('#uploadFrameID').load(function(){
		var newhtml = jQuery('#uploadFrameID').contents().find('body').html();
		window.setTimeout(function(){
			jQuery('body').html(newhtml);
			}, 
			500);
	});
	if (jQuery('#tfile').val() != '')
	{
		jQuery('#tfile').data('contextPath',contextPath);		
		monitorUploadAJAX();
	}
}

function monitorUploadAJAX() {
	contextPath = jQuery('#tfile').data('contextPath');
	if (contextPath == null) return;
	jQuery.ajax({
        url: contextPath+'/json/uploadProgress'})
    .done(function(progress) {
    		var progressbarArea = jQuery("#progressBarArea");
    		var progressbar = jQuery("#progressBar");
    		jQuery('#tfile').parents('form').children('input').attr(
						'disabled', 'disabled');
			
			progressbarArea.show();
			// Check to see if it's even started yet
			if (progress == null)
			{
	    		progressbar.progressbar({value: false });
		    
	    		// Sleep then call the function again
	        	window.setTimeout("monitorUploadAJAX();", 200);
	      	}
	      	else 
	      	{
				progressbar.progressbar({ value: progress.readBytes, max: progress.totalBytes});
	           	progressbarArea.children('p.progressBarInitMsg').hide();
	           	//progressbarArea.children('span.bytesRead').html(progress.readBytes);
	           	//progressbarArea.children('span.bytesTotal').html(progress.totalBytes);
	           	//progressbarArea.children('span.percent').html(progress.percent);
	           	
	           	if (progress.completed)
	           	{
	           		progressbarArea.children('p.progressBarProgressMsg').hide();
	           		progressbarArea.children('p.progressBarCompleteMsg').show();
	           	}
	           	else
	           	{
     	       		progressbarArea.children('p.progressBarProgressMsg').show();
	           		progressbarArea.children('p.progressBarCompleteMsg').hide();
	           	
		           	// Sleep then call the function again
		           	window.setTimeout("monitorUploadAJAX();", 200);
	           	}
	      	}
		});
}