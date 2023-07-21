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
 * Defines the beans available in the Spring bootstrap for the product modules.
 * 
 *
 * @since R8
 */
public final class ProductApiBeans {

    /**
     * restricting instantiation
     */
    private ProductApiBeans() {
    }

    /** Product builder table bean */
    public static final String PRODUCT_BUILDER_TABLE = "PRODUCT_BUILDER_TABLE";
    /** Product builder service bean */
    public static final String PRODUCT_BUILDER = "PRODUCT_BUILDER";
    /** Product builder manager service bean */
    public static final String PRODUCT_BUILDER_MANAGER = "PRODUCT_BUILDER_MANAGER";
    /** Product tracking service bean */
    public static final String PRODUCT_TRACKING_SERVICE = "PRODUCT_TRACKING_SERVICE";
    /** Disk product storage bean for product builder */
    public static final String DISK_PRODUCT_STORAGE = "DISK_PRODUCT_STORAGE";
    /** Disk product storage table bean */
    public static final String DISK_PRODUCT_STORAGE_TABLE = "DISK_PRODUCT_STORAGE_TABLE";
    /** Transaction log storage bean for product builder */
    public static final String TRANSACTION_LOG_STORAGE = "TRANSACTION_LOG_STORAGE";
    /** Product configuration properties bean */
    public static final String PRODUCT_PROPERTIES = "PRODUCT_PROPERTIES";
    /** Product template manager bean */
    public static final String PRODUCT_TEMPLATE_MANAGER = "PRODUCT_TEMPLATE_MANAGER";
    /** Received parts tracker bean */
    public static final String PARTS_TRACKER = "PARTS_TRACKER";
    /** Product dictionary bean */
    public static final String PRODUCT_DICTIONARY = "PRODUCT_DICTIONARY";
    /** Product automation adder bean */
    public static final String PRODUCT_AUTOMATION_ADDER = "PRODUCT_AUTOMATION_ADDER";
    /** Product decom field factory bean */
    public static final String PRODUCT_DECOM_FIELD_FACTORY = "PRODUCT_DECOM_FIELD_FACTORY";
    /** Product builder object instance factory bean */
    public static final String PRODUCT_INSTANCE_FACTORY = "PRODUCT_INSTANCE_FACTORY";
    /** Product definition object instance factory bean */
    public static final String PRODUCT_DEFINITION_FACTORY = "PRODUCT_DEFINITION_FACTORY";
    /** Product mission adapter bean */
    public static final String PRODUCT_MISSION_ADAPTOR = "PRODUCT_MISSION_ADAPTOR";
    /** Product filename builder factory bean */
    public static final String PRODUCT_FILENAME_BUILDER_FACTORY = "PRODUCT_FILENAME_BUILDER_FACTORY";
    /** Product message factory bean */
    public static final String PRODUCT_MESSAGE_FACTORY = "PRODUCT_MESSAGE_FACTORY";
    /** Product file filter bean */
    public static final String PRODUCT_FILTER = "PRODUCT_FILTER";
    /** Product definition dumper bean  */
    public static final String PRODUCT_DEFINITION_DUMPER = "PRODUCT_DEFINITION_DUMPER";
    /** Stored product input bean */
    public static final String STORED_PRODUCT_INPUT = "STORED_PRODUCT_INPUT";
    /** Product decom output formatter bean */
    public static final String PRODUCT_DECOM_OUTPUT_FORMATTER = "PRODUCT_DECOM_OUTPUT_FORMATTER";
    /** Product decom bean */
    public static final String PRODUCT_DECOM = "PRODUCT_DECOM";
    /** Product data checksum bean */
    public static final String PRODUCT_CHECKSUM = "PRODUCT_CHECKSUM";
    /** Recorded product properties bean. */
    public static final String RECORDED_PRODUCT_PROPERTIES = "RECORDED_PRODUCT_PROPERTIES";
    /** Recorded engineering product decom bean */
    public static final String RECORDED_ENG_PRODUCT_DECOM = "RECORDED_ENG_PRODUCT_DECOM";
    /** Product output directory utility bean */
    public static final String PRODUCT_OUTPUT_DIRECTORY_UTIL = "PRODUCT_OUTPUT_DIRECTORY_UTIL";
    /** Product automation container creator bean */
    public static final String PDPP_CONTEXT_CONTAINER_CREATOR = "PDPP_CONTEXT_CONTAINER_CREATOR";
}
