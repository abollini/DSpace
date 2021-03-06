--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- SQL code to update the ID (primary key) generating sequences, if some
-- import operation has set explicit IDs.
--
-- Sequences are used to generate IDs for new rows in the database.  If a
-- bulk import operation, such as an SQL dump, specifies primary keys for
-- imported data explicitly, the sequences are out of sync and need updating.
-- This SQL code does just that.
--
-- This should rarely be needed; any bulk import should be performed using the
-- org.dspace.content API which is safe to use concurrently and in multiple
-- JVMs.  The SQL code below will typically only be required after a direct
-- SQL data dump from a backup or somesuch.


SELECT setval('bitstreamformatregistry_seq', max(bitstream_format_id)) FROM bitstreamformatregistry;
SELECT setval('fileextension_seq', max(file_extension_id)) FROM fileextension;
SELECT setval('resourcepolicy_seq', max(policy_id)) FROM resourcepolicy;
SELECT setval('workspaceitem_seq', max(workspace_item_id)) FROM workspaceitem;
SELECT setval('workflowitem_seq', max(workflow_id)) FROM workflowitem;
SELECT setval('tasklistitem_seq', max(tasklist_id)) FROM tasklistitem;
SELECT setval('registrationdata_seq', max(registrationdata_id)) FROM registrationdata;
SELECT setval('subscription_seq', max(subscription_id)) FROM subscription;
SELECT setval('metadatafieldregistry_seq', max(metadata_field_id)) FROM metadatafieldregistry;
SELECT setval('metadatavalue_seq', max(metadata_value_id)) FROM metadatavalue;
SELECT setval('metadataschemaregistry_seq', max(metadata_schema_id)) FROM metadataschemaregistry;
SELECT setval('harvested_collection_seq', max(id)) FROM harvested_collection;
SELECT setval('harvested_item_seq', max(id)) FROM harvested_item;
SELECT setval('webapp_seq', max(webapp_id)) FROM webapp;
SELECT setval('requestitem_seq', max(requestitem_id)) FROM requestitem;
SELECT setval('handle_id_seq', max(handle_id)) FROM handle;

-- Handle Sequence is a special case.  Since Handles minted by DSpace use the 'handle_seq',
-- we need to ensure the next assigned handle will *always* be unique.  So, 'handle_seq'
-- always needs to be set to the value of the *largest* handle suffix.  That way when the
-- next handle is assigned, it will use the next largest number. This query does the following:
--  For all 'handle' values which have a number in their suffix (after '/'), find the maximum
--  suffix value, convert it to a 'bigint' type, and set the 'handle_seq' to that max value.
SELECT setval('handle_seq',
              CAST (
                    max(
                        to_number(regexp_replace(handle, '.*/', ''), '999999999999')
                       )
                    AS BIGINT)
             )
    FROM handle
    WHERE handle SIMILAR TO '%/[0123456789]*';
