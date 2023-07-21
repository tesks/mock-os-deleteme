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

import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.IProductMetadataUpdater;
import jpl.gds.product.api.IReferenceProductMetadataUpdater;
import jpl.gds.product.api.ProductException;
import jpl.gds.shared.interfaces.ICsvSupport;
import jpl.gds.shared.xml.stax.StaxSerializable;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.List;
import java.util.Map;

/**
 * Interface that defines what product metadata should be accessible for a PDPP product
 */
public interface IProductMetadata extends IProductMetadataProvider, IProductMetadataUpdater, StaxSerializable, ICsvSupport {

    /**
     * @return true if data file is COMPRESSED
     *      false if datafile is NOT COMPRESSED
     */
    boolean getIsCompressed();

    /**
     * @return sets true if datafile is COMPRESSED
     */
    void setIsCompressed(boolean compressed);

    /**
     * constructs the data file name for the data associated with this metadata
     * instance
     *
     * @param productIsPartial
     *			true if the product is not yet complete, and false if the
     *			product is complete
     *
     */
    void setDataFileName(Boolean productIsPartial);

    /**
     * returns the name of the data file associated with this metadata instance
     *
     * @return data file name
     */
    String getDataFileName();

    /**
     * Constructs the current partial product data file name.
     *
     * @return the name for the complete file path to the data file
     */
    String getCurrentPartialDataFile();

    /**
     * Constructs the file name for a complete data product with the top-level
     * directory
     *
     * @return the name for the complete file path to the data file
     */
    String getCompleteDataFile();

    /**
     * Tool allows user to get just the file name, no extension or version
     * For example, if the file is "test-1.emd" then "test" will be returned.
     *
     * @return the filename with no version or extension
     */
    String getFilenameNoVersionOrExtension();
}
