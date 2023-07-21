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
package jpl.gds.dictionary.api.config;

import jpl.gds.dictionary.api.DictionaryClassContainer;
import jpl.gds.dictionary.api.DictionaryClassContainer.ClassType;

/**
 * The class enumerates the possible types of dictionaries. It also keeps the default
 * file name and parser class name for each dictionary type.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br><br>
 * 
 */
public enum DictionaryType {
    /** Application Processing ID (APID) dictionary. */
    APID ("apid.xml", ClassType.MM_APID, DictionaryProperties.DICTIONARY_PATH, "ApidDictionary.rnc"),

    /** Event Record (EVR) dictionary. */
    EVR ("evr.xml", ClassType.MM_EVR, DictionaryProperties.DICTIONARY_PATH, "EvrDictionary.rnc"),

    /** Flight or SSE/GSE telemetry channel dictionary. */
    CHANNEL ("channel.xml", ClassType.MM_CHANNEL, DictionaryProperties.DICTIONARY_PATH, "ChannelDictionary.rnc"),

    /** Command dictionary */
    COMMAND ("command.xml", ClassType.MM_COMMAND, DictionaryProperties.DICTIONARY_PATH, "CommandDictionary.rnc"),

    /** Command Sequence ID dictionary */
    SEQUENCE ("sequence.xml", ClassType.MM_SEQUENCE, DictionaryProperties.CONFIG_PATH, ""),

    /** Station monitor channel dictionary */
    MONITOR ("monitor_channel.xml", ClassType.MM_CHANNEL, DictionaryProperties.CONFIG_PATH, "ChannelDictionary.rnc"),

    /** Telemetry header channel dictionary */
    HEADER ("header_channel.xml", ClassType.MM_CHANNEL, DictionaryProperties.CONFIG_PATH,"ChannelDictionary.rnc"),

    /** Data product dictionary. Note in this one case the default filename is actually a sub-directory name */
    PRODUCT ("product", ClassType.MM_PRODUCT, DictionaryProperties.DICTIONARY_PATH, "DataProductDictionary.rnc"),

    /** Transfer frame dictionary */
    FRAME ("transfer_frame.xml", ClassType.MM_FRAME, DictionaryProperties.CONFIG_PATH, "TransferFrameDictionary.rnc"),

    /** Channel alarm dictionary */
    ALARM ("alarms.xml", ClassType.MM_ALARM, DictionaryProperties.DICTIONARY_PATH, "AlarmDictionary.rnc"),
    
    /** Channel decom dictionary */
    DECOM (".", ClassType.MM_DECOM, DictionaryProperties.DICTIONARY_PATH, "GenericDecom.rnc"),

    /** FSW Dictionary mapping table */
    MAPPER("dictionary_mappings.xml", ClassType.M20_DICT_MAPPER, DictionaryProperties.CONFIG_PATH, "fsw_to_dictionary_mappings.rnc");

    private String defaultFile;
    private DictionaryClassContainer.ClassType defaultParser;
    private String defaultSearchPath;
    private String defaultSchemaName;

    /**
     * Constructor.
     *  @param defaultFileName
     *            the default base file name (no path) for the dictionary.
     * @param defaultParserClass
     *            the default parser class (with package name)
     * @param defaultSearch
 *            the default search method: CONFIG_PATH, DICTIONARY_PATH, or a
     * @param defaultSchema
     *        the default schema (rnc) name
     */
    private DictionaryType(final String defaultFileName, final ClassType defaultParserClass, final String defaultSearch,
                           String defaultSchema) {
        defaultFile = defaultFileName;
        defaultParser = defaultParserClass;
        defaultSearchPath = defaultSearch;
        defaultSchemaName = defaultSchema;
    }

    /**
     * Gets the official name of this dictionary type. Used in configuration
     * property names.
     * 
     * @return dictionary type name
     */
    public String getDictionaryName() {
        return this.toString().toLowerCase();
    }

    /**
     * Gets the default base file name (no path) for this dictionary type.
     * 
     * @return file name
     */
    public String getDefaultFileName() {
        return defaultFile;
    }

    /**
     * Gets the default parser class (with package) for this dictionary type.
     * 
     * @return parser class name
     */
    public DictionaryClassContainer.ClassType getDefaultParserClass() {
        return defaultParser;
    }

    /**
     * Gets the default search path for this dictionary type. This will be
     * DictionaryConfiguration.CONFIG_PATH,
     * DictionaryConfiguration.DICTIONARY_PATH, or a directory name.
     * 
     * @return search path designator
     */
    public String getDefaultSearchPath() {
        return defaultSearchPath;
    }

    /**
     * Gets the default schema name for this dictionary type
     * @return The dictionary type default schema (RNC) name
     */
    public String getDefaultSchemaName() {
        return defaultSchemaName;
    }
}
