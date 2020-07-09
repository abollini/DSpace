/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.authority;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Consumer to store item related entities when an item submission/modification
 * occurs.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisConsumer implements Consumer {

    private final static String NO_RELATIONSHIP_TYPE_FOUND_MSG = "No relationship.type found for field {}";

    private final static String ITEM_CREATION_MSG = "Creation of item with relationship.type = {} related to item {}";

    private final static String NO_COLLECTION_FOUND_MSG = "No collection found with relationship.type = {} "
            + "for item = {}. No related item will be created.";

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(CrisConsumer.class);

    private Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    private ChoiceAuthorityService choiceAuthorityService;

    private ItemService itemService;

    private WorkspaceItemService workspaceItemService;

    private WorkflowService<XmlWorkflowItem> workflowService;

    private InstallItemService installItemService;

    private ConfigurationService configurationService;

    @Override
    @SuppressWarnings("unchecked")
    public void initialize() throws Exception {
        choiceAuthorityService = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
        installItemService = ContentServiceFactory.getInstance().getInstallItemService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
    }

    @Override
    public void finish(Context context) throws Exception {

    }

    @Override
    public void consume(Context context, Event event) throws Exception {

        Item item = (Item) event.getSubject(context);
        if (item == null || itemsAlreadyProcessed.contains(item) || !item.isArchived()) {
            return;
        }

        itemsAlreadyProcessed.add(item);

        context.turnOffAuthorisationSystem();
        try {
            consumeItem(context, item);
        } finally {
            context.restoreAuthSystemState();
        }

    }

    private void consumeItem(Context context, Item item) throws Exception {

        List<MetadataValue> metadataValues = item.getMetadata();

        for (MetadataValue metadata : metadataValues) {

            String authority = metadata.getAuthority();
            if (StringUtils.isNotBlank(authority) && !authority.startsWith(AuthorityValueService.GENERATE)) {
                continue;
            }

            String fieldKey = getFieldKey(metadata);

            if (!choiceAuthorityService.isChoicesConfigured(fieldKey)) {
                continue;
            }

            String relationshipType = choiceAuthorityService.getRelationshipType(fieldKey);
            if (relationshipType == null) {
                log.warn(NO_RELATIONSHIP_TYPE_FOUND_MSG, fieldKey);
                continue;
            }

            String crisSourceId = generateCrisSourceId(metadata);

            Item relatedItem = findRelatedItemByCrisSourceId(context, crisSourceId, relationshipType);
            if (relatedItem == null) {
                Collection collection = retrieveCollectionByRelationshipType(item, relationshipType);
                if (collection == null) {
                    log.warn(NO_COLLECTION_FOUND_MSG, relationshipType, item.getID());
                    continue;
                }
                collection = context.reloadEntity(collection);
                log.debug(ITEM_CREATION_MSG, relationshipType, item.getID());
                relatedItem = buildRelatedItem(context, item, collection, metadata, relationshipType, crisSourceId);
            }

            metadata.setAuthority(relatedItem.getID().toString());
            metadata.setConfidence(Choices.CF_ACCEPTED);
        }

    }

    @Override
    public void end(Context context) throws Exception {
        itemsAlreadyProcessed.clear();
    }

    private Item findRelatedItemByCrisSourceId(Context context, String crisSourceId,
            String relationshipType) throws Exception {

        Iterator<Item> iterator = itemService.findByMetadataField(context, "cris", "sourceId", null, crisSourceId);

        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (hasRelationshipTypeMetadataEqualsTo(item, relationshipType)) {
                return item;
            }
        }

        return null;

    }

    private String getFieldKey(MetadataValue metadata) {
        return metadata.getMetadataField().toString('_');
    }

    private boolean hasRelationshipTypeMetadataEqualsTo(DSpaceObject dsObject, String relationshipType) {
        return dsObject.getMetadata().stream().anyMatch(metadataValue -> {
            return "relationship.type".equals(metadataValue.getMetadataField().toString('.')) &&
                    relationshipType.equals(metadataValue.getValue());
        });
    }

    private Item buildRelatedItem(Context context, Item item, Collection collection, MetadataValue metadata,
            String relationshipType, String crisSourceId) throws Exception {

        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item relatedItem = workspaceItem.getItem();
        relatedItem.setOwningCollection(collection);
        relatedItem.setSubmitter(item.getSubmitter());
        itemService.addMetadata(context, relatedItem, "cris", "sourceId", null, null, crisSourceId);
        if (!hasRelationshipTypeMetadataEqualsTo(relatedItem, relationshipType)) {
            itemService.addMetadata(context, relatedItem, "relationship", "type", null, null, relationshipType);
        }

        if (isSubmissionEnabled(metadata)) {
            return installItemService.installItem(context, workspaceItem);
        } else {
            return workflowService.start(context, workspaceItem).getItem();
        }
    }

    private Collection retrieveCollectionByRelationshipType(Item item, String relationshipType) throws SQLException {
        Collection ownCollection = item.getOwningCollection();
        Collection collection = retrieveCollectionByRelationshipType(ownCollection.getCommunities(), relationshipType);
        if (collection == null) {
            log.warn("No collection found with relationship.type = {} for item = {}", relationshipType, item.getID());
        }
        return collection;
    }

    private Collection retrieveCollectionByRelationshipType(List<Community> communities, String relationshipType) {

        for (Community community : communities) {
            Collection collection = retriveCollectionByRelationshipType(community, relationshipType);
            if (collection != null) {
                return collection;
            }
        }

        for (Community community : communities) {
            List<Community> parentCommunities = community.getParentCommunities();
            Collection collection = retrieveCollectionByRelationshipType(parentCommunities, relationshipType);
            if (collection != null) {
                return collection;
            }
        }

        return null;
    }

    private Collection retriveCollectionByRelationshipType(Community community, String relationshipType) {

        for (Collection collection : community.getCollections()) {
            if (hasRelationshipTypeMetadataEqualsTo(collection, relationshipType)) {
                return collection;
            }
        }

        for (Community subCommunity : community.getSubcommunities()) {
            Collection collection = retriveCollectionByRelationshipType(subCommunity, relationshipType);
            if (collection != null) {
                return collection;
            }
        }

        return null;
    }

    private String generateCrisSourceId(MetadataValue metadata) {
        if (isUuidStrategyEnabled(metadata)) {
            return UUID.randomUUID().toString();
        } else {
            return DigestUtils.md5Hex(metadata.getValue().toUpperCase());
        }
    }

    private boolean isUuidStrategyEnabled(MetadataValue value) {
        String property = "cris.import.submission.strategy.uuid." + getFieldKey(value);
        return configurationService.getBooleanProperty(property, false);
    }

    private boolean isSubmissionEnabled(MetadataValue value) {
        String property = "cris.import.submission.enabled.entity";
        String propertyWithMetadataField = property + "." + getFieldKey(value);
        if (configurationService.hasProperty(propertyWithMetadataField)) {
            return configurationService.getBooleanProperty(propertyWithMetadataField);
        } else {
            return configurationService.getBooleanProperty(property, true);
        }
    }

}
