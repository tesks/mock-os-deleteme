/*
 * Copyright 2006-2020. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */
package jpl.gds.product.api.file;

/**
 * Interface used to create the appropriate PDPP product metadata object
 */
public interface IProductMetadataBuilder {
    /**
     * Builds a new metadata object. Assuming it will be used by Spring and populated with the mission-appropriate concrete class.
     * @return product metadata instance
     */
    IProductMetadata build();

}
