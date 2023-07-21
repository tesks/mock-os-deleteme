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
package jpl.gds.product.api;

/**
 * A class containing constants for use by data product viewing applications. 
 * This is to avoid defining these in product app classes and then having other modules
 * dependent upon the app classes.
 * 
 *
 * @since R8
 * 
 *
 */
public class DpViewAppConstants {
    /**
     * Short command line option for the directory output option.
     */
    public static final String DIRECTORY_OPTION_SHORT = "d";
    /**
     * Long command line option for the directory output option.
     */
    public static final String DIRECTORY_OPTION_LONG = "directory";
    /**
     * Short command line option for the filename output option.
     */
    public static final String FILENAME_OPTION_SHORT = "f";
    /**
     * Long command line option for the filename output option.
     */
    public static final String FILENAME_OPTION_LONG = "filename";
    /**
     * Short command line option for the XML output option.
     */
    public static final String XML_OPTION_SHORT = "x";
    /**
     * Long command line option for the XML output option.
     */
    public static final String XML_OPTION_LONG = "xml";
    /**
     * Short command line option for the CSV output option.
     */
    public static final String CSV_OPTION_SHORT = "c";
    /**
     * Long command line option for the CSV output option.
     */
    public static final String CSV_OPTION_LONG = "csv";
    /**
     * Short command line option for the no text output option.
     */
    public static final String NOTEXT_OPTION_SHORT = "t";
    /**
     * Long command line option for the no text output option.
     */
    public static final String NOTEXT_OPTION_LONG = "noText";
    /**
     * Short command line option to ignore invalid checksums.
     */
    public static final String IGNORE_CHECKSUM_SHORT = "i";
    /**
     * Long command line option to ignore invalid checksums.
     */
    public static final String IGNORE_CHECKSUM_LONG = "ignoreChecksum";
    /**
     * Short command line option to launch product viewers.
     */
    public static final String LAUNCH_PRODUCT_VIEWER_SHORT = "p";
    /**
     * Long command line option to launch product viewers.
     */
    public static final String LAUNCH_PRODUCT_VIEWER_LONG = "productViewer";
    /**
     * Short command line option to launch DPO viewers.
     */
    public static final String LAUNCH_DPO_VIEWER_SHORT = "l";
    /**
     * Long command line option to launch DPO viewers.
     */
    public static final String LAUNCH_DPO_VIEWER_LONG = "dpoViewers";
    /**
     * Short command line option to display launch command lines.
     */
    public static final String SHOW_LAUNCH_SHORT = "s";
    /**
     * Long command line option to display launch command lines.
     */
    public static final String SHOW_LAUNCH_LONG = "showLaunch";
    /**
     * Short command line option for DPO list.
     */
    public static final String DPO_LIST_SHORT = "n";
    /**
     * Long command line option for DPO list.
     */
    public static final String DPO_LIST_LONG = "dpoList";

}
