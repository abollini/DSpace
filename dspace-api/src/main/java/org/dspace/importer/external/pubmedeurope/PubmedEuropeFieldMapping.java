/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.pubmedeurope;

import java.util.Map;
import javax.annotation.Resource;

import org.dspace.importer.external.metadatamapping.AbstractMetadataFieldMapping;

/**
 * An implementation of {@link AbstractMetadataFieldMapping}
 * Responsible for defining the mapping of the ArXiv metadatum fields on the DSpace metadatum fields
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 */
public class PubmedEuropeFieldMapping extends AbstractMetadataFieldMapping {

    /**
     * Defines which metadatum is mapped on which metadatum. Note that while the key must be unique it
     * only matters here for postprocessing of the value. The mapped MetadatumContributor has full control over
     * what metadatafield is generated.
     *
     * @param metadataFieldMap The map containing the link between retrieve metadata and metadata that will be set to
     *                         the item.
     */
    @Override
    @Resource(name = "pubmedEuropeMetadataFieldMap")
    public void setMetadataFieldMap(Map metadataFieldMap) {
        super.setMetadataFieldMap(metadataFieldMap);
    }

}