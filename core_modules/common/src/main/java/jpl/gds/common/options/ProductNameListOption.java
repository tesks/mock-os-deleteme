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
package jpl.gds.common.options;

import jpl.gds.shared.cli.options.CsvStringOption;

/**
 * A command line option class that accepts a list of product names.
 * 
 *
 * @since R8
 *
 */
public class ProductNameListOption extends CsvStringOption {
    
    /**
     * Names of complete products to process (e.g. SamMessageLog) - short
     * option.
     */
    public static final String PRODUCT_NAMES_SHORT    = "z";
    /**
     * Names of complete products to process (e.g. SamMessageLog) - long
     * option.
     */
    public static final String PRODUCT_NAMES_LONG     = "productNames";

    
    /**
     * Constructor
     * 
     * @param required true if the option is required on the command line
     */
    public ProductNameListOption(final boolean required) {
        super(PRODUCT_NAMES_SHORT, PRODUCT_NAMES_LONG, "names",
            "names of complete products to process", true, true, required);
    }

}
