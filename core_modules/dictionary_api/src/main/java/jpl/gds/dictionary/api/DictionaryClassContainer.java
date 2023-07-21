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
package jpl.gds.dictionary.api;

/**
 * This class is used to represent a dictionary parser class/type. Standard
 * classes are indicated using an enumerated value. These values can be used in
 * the dictionary properties file to identify the parser to be used for each
 * dictionary type. If any other value is found in the configuration file, it is
 * assumed to be a custom dictionary parser class name. In that case, the CUSTOM
 * enum value is used, and this class contains the custom parser name.
 *
 *
 * @since R8
 *
 */
public class DictionaryClassContainer {

    /**
     * Enumeration for parser class types.
     *
     */
    public enum ClassType {

        /** Value representing the multimission alarm dictionary parser */
        MM_ALARM,
        /** Value representing the MSL alarm dictionary parser */
        MSL_ALARM,
        /** Value representing the SMAP alarm dictionary parser */
        SMAP_ALARM,
        /** Value representing the multimission APID dictionary parser */
        MM_APID,
        /** Value representing the MSL APID dictionary parser */
        MSL_APID,
        /** Value representing the SMAP APID dictionary parser */
        SMAP_APID,
        /** Value representing the SSE APID dictionary parser */
        SSE_APID,
        /** Value representing the multimission channel dictionary parser */
        MM_CHANNEL,
        /** Value representing the MSL channel dictionary parser */
        MSL_CHANNEL,
        /** Value representing the SMAP channel dictionary parser */
        SMAP_CHANNEL,
        /** Value representing the SSE channel dictionary parser */
        SSE_CHANNEL,
        /** Value representing the old-style multimission header channel dictionary parser */
        OLD_MM_HEADER_CHANNEL,
        /** Value representing the old-style multimission monitor channel dictionary parser */
        OLD_MM_MONITOR_CHANNEL,
        /** Value representing the multimission command dictionary parser */
        MM_COMMAND,
        /** Value representing the MSL command dictionary parser */
        MSL_COMMAND,
        /** Value representing the SMAP command dictionary parser */
        SMAP_COMMAND,
        /** Value representing the multimission decom dictionary parser */
        MM_DECOM,
        /** Value representing the old-style multimission decom dictionary parser */
        OLD_MM_DECOM,
        /** Value representing the multimission EVR dictionary parser */
        MM_EVR,
        /** Value representing the MSL EVR dictionary parser */
        MSL_EVR,
        /** Value representing the SMAP EVR dictionary parser */
        SMAP_EVR,
        /** Value representing the SSE EVR dictionary parser */
        SSE_EVR,
        /** Value representing the multimission frame dictionary parser */
        MM_FRAME,
        /** Value representing the old-style multimission frame dictionary parser */
        OLD_MM_FRAME,
        /** Value representing the M20 flight dictionary mapping file parser */
        M20_DICT_MAPPER,
        /** Value representing the multimission sequence dictionary parser */
        MM_SEQUENCE,
        /** Value representing the MSL sequence dictionary parser */
        MSL_SEQUENCE,
        /** Value representing the multimission product dictionary parser */
        MM_PRODUCT,
        /** Value representing the MSL product dictionary parser */
        MSL_PRODUCT,
        /** Value representing a non-standard parser. */
        CUSTOM;
    }

    private String customClassName;
    private final ClassType type;

    /**
     * Creates an instance that represents one of the standard parser classes.
     *
     * @param inType the ClassType enum value for the standard parser
     */
    public DictionaryClassContainer(final ClassType inType) {
        this.type = inType;
    }

    /**
     * Creates an instance for a custom parser class.
     *
     * @param klazz the fully-qualified Java parser classes name.
     */
    public DictionaryClassContainer(final String klazz) {
        if (klazz == null) {
            throw new IllegalArgumentException("Class name cannot be null");
        }
        this.type = ClassType.CUSTOM;
        this.customClassName = klazz;
    }

    /**
     * Gets the custom class name associated with this container.
     * Returns null if the ClassType is not CUSTOM.
     *
     * @return custom class name
     */
    public String getCustomClassName() {
        return this.customClassName;
    }

    /**
     * Gets the ClassType associated with this container.
     * @return a ClassType value
     */
    public ClassType getClassType() {
        return this.type;
    }

}
