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
package jpl.gds.product.api.builder;

import jpl.gds.shared.time.IAccurateDateTime;

/**
 * {@code IProductOutputDirectoryUtil} is an interface to be implemented by classes that provide utility functions
 * related to product output directories.
 *
 * @since 8.1
 */
public interface IProductOutputDirectoryUtil {

    /**
     * Get the Product Output Directory
     *
     * @param productDirOverrideConfig Override to product output directory
     * @param scet                     The product SCET to use as part of the output path
     * @param apidName                 The Product APID
     * @return String representing the current product output directory
     */
    String getProductOutputDir(String productDirOverrideConfig, IAccurateDateTime scet, String apidName);

}
