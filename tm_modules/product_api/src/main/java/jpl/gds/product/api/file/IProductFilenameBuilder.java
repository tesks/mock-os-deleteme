/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.product.api.file;

import jpl.gds.common.spring.context.IContextContainer;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.product.api.IProductMetadataProvider;

/**
 * An interface to be implemented by product filename builder classes.
 * 
 *
 * @since R8
 *
 */
public interface IProductFilenameBuilder {

    /**
     * If using the full product path all other setter values called before or after this will
     * be ignored.
     *
     * @param fullProductPath full path of either the dat or emd file.
     * @return this
     */
    public IProductFilenameBuilder addFullProductPath(String fullProductPath);

    /**
     * If addFullProductPath was used called this will not change the path.
     *
     * @param productPath the productPath to set.
     * @return this
     */
    public IProductFilenameBuilder addProductPath(String productPath);

    /**
     * If addFullProductPath was used called this will not change the name.
     * @param productName the name of the product
     * @return this
     */
    public IProductFilenameBuilder addProductName(String productName);

    /**
     * If addFullProductPath was used called this will not change the partial flag.
     *
     * @param isPartial true if the product is partial
     * @return this
     */
    public IProductFilenameBuilder addIsPartial(boolean isPartial);

    /**
     * Sets isPartial, productPath and productName with the values from metadata.
     * @param metadata product metadata object to set the values from.
     * @return this
     */
    public IProductFilenameBuilder addProductMetadata(IProductMetadataProvider metadata);

    /**
     * Sets isPartial, productPath and productName with the values from pfn.
     *
     * @param pfn the product file name to set the internal values with.
     * @return this
     */
    public IProductFilenameBuilder addProductFilename(IProductFilename pfn);

    /**
     * Checks if all required values have been set and are valid and will build a new filename object.
     * @return product filename instance
     * @throws ProductFilenameException required values are not set or input values were invalid.
     */
    public IProductFilename build() throws ProductFilenameException;

    /**
     * Fabricate a new Cross String Context's output directory based upon PRIME context's configuration.
     *
     * @param xsQuerySession
     *            the IContextConfiguration of the PRIME context
     * @return the Output Directory path for the ONLINE context related to the provided PRIME context.
     */
    public String getContextOutputDirectory(IContextConfiguration xsQuerySession);

    /**
     * Sets the internal values for the product file name using sc and md in such a way that the data and emd file paths
     * will be set for the venue type.
     *
     * @param configuration
     *            the context configuration
     * @param metadata
     *            the metadata object for the product.
     * @return this for chaining
     */
    public IProductFilenameBuilder addVenueAppropriateFullyQualifiedDataFilename(IContextConfiguration configuration, IProductMetadata metadata);

    /**
     * If addFullProductPath was used called this will not change the compression flag.
     *
     * @param isCompressed
     * @return this
     */
    public IProductFilenameBuilder addIsCompressed(boolean isCompressed);

}