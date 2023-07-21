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
package ammos.datagen.evr.config;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ammos.datagen.config.AbstractRunConfiguration;
import ammos.datagen.config.IRunConfiguration;
import ammos.datagen.config.TraversalType;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.xml.XmlUtility;

/**
 * This is the EvrRunConfiguration class for the AMPCS data generators. It
 * parses an XML file that contains configuration information may change from
 * run to run of the EVR generator. The XML file containing the configuration is
 * verified against its schema before loading it. After the XML file is loaded,
 * the configuration values it contained are available using various accessor
 * methods.
 * 
 *
 */
public class EvrRunConfiguration extends AbstractRunConfiguration implements
        IRunConfiguration {

    /**
     * Configuration property name for the EVR level pattern (string).
     */
    public static final String LEVEL_PATTERN = "LevelPattern";

    /**
     * Configuration property name for the EVR name pattern (string).
     */
    public static final String NAME_PATTERN = "NamePattern";

    /**
     * Configuration property name for the EVR module pattern (string).
     */
    public static final String MODULE_PATTERN = "ModulePattern";

    /**
     * Configuration property name for the EVR operational category pattern
     * (string).
     */
    public static final String OPSCAT_PATTERN = "OpsCatPattern";

    /**
     * Configuration property name for the EVR subsystem pattern (string).
     */
    public static final String SUBSYSTEM_PATTERN = "SubsystemPattern";

    /**
     * Configuration property name for the flag indicating whether to include
     * invalid opcodes in the EVR arguments (boolean).
     */
    public static final String INCLUDE_INVALID_OPCODES = "IncludeInvalidOpcodes";

    /**
     * Configuration property name for the approximate percentage of invalid
     * opcode arguments to generate (float).
     */
    public static final String INVALID_OPCODE_PERCENT = "InvalidOpcodePercent";

    /**
     * Configuration property name for the opcode traversal type, indicating
     * whether to generate ocpode values sequentially or randomly
     * (TraversalType).
     */
    public static final String OPCODE_TRAVERSAL_TYPE = "OpcodeTraversalType";

    /**
     * Configuration property name for the flag indicating whether to include
     * invalid EVR identifiers in the EVR binary data (boolean).
     */
    public static final String INCLUDE_INVALID_IDS = "IncludeInvalidIds";

    /**
     * Configuration property name for the approximate percentage of invalid EVR
     * IDs to generate (float).
     */
    public static final String INVALID_ID_PERCENT = "InvalidIdPercent";

    /**
     * Configuration property name for the flag indicating whether to include
     * invalid sequence IDs for sequence ID arguments in the EVR binary data
     * (boolean).
     */
    public static final String INCLUDE_INVALID_SEQIDS = "IncludeInvalidSeqIds";

    /**
     * Configuration property name for the approximate percentage of invalid
     * SEQIDs to generate (float).
     */
    public static final String INVALID_SEQID_PERCENT = "InvalidSeqIdPercent";

    /**
     * Configuration property name for the flag indicating whether to include
     * invalid EVR binary data (boolean).
     */
    public static final String INCLUDE_INVALID_EVRS = "IncludeInvalidEvrs";

    /**
     * Configuration property name for the property indicating whether to
     * traverse the dictionary randomly or sequentially. (TraversalType).
     */
    public static final String EVR_TRAVERSAL_TYPE = "EvrTraversalType";

    /**
     * Configuration property name for the EVR task name to include in EVR
     * packets (string).
     */
    public static final String EVR_TASK_NAME = "EvrTaskName";

    /**
     * Configuration property name for the EVR stack trace depth (integer).
     */
    public static final String EVR_STACK_DEPTH = "EvrStackDepth";

    private static final String VALID_OPCODE = "ValidOpcode";
    private static final String INVALID_OPCODE = "InvalidOpcode";
    private static final String INVALID_ID = "InvalidId";
    private static final String VALID_SEQID = "ValidSeqId";
    private static final String INVALID_SEQID = "InvalidSeqId";
    private static final String STEM = "stem";
    private static final String NUMBER = "number";
    private static final String ID = "id";

    private static final String SCHEMA_RELATIVE_PATH = "schema/EvrRunConfig.rnc";

    private final SortedSet<Opcode> validOpcodes = new TreeSet<Opcode>();
    private final SortedSet<Opcode> invalidOpcodes = new TreeSet<Opcode>();
    private final SortedSet<Integer> validSeqIds = new TreeSet<Integer>();
    private final SortedSet<Integer> invalidSeqIds = new TreeSet<Integer>();
    private final SortedSet<Integer> invalidIds = new TreeSet<Integer>();

    /**
     * Constructor.
     */
    public EvrRunConfiguration() {

        super("EVR Run Configuration", SCHEMA_RELATIVE_PATH);
    }

    /**
     * Loads the configuration file.
     * 
     * @param uri
     *            the path to the configuration file.
     * 
     * @return true if the file was successfully loaded, false if not.
     */
    public boolean load(final String uri) {

        final boolean ok = super.load(uri, new RunConfigurationParseHandler());
        if (ok) {
            if (getBooleanProperty(INCLUDE_INVALID_OPCODES, false)
                    && this.invalidOpcodes.isEmpty()) {
                TraceManager

                        .getDefaultTracer()
                        .error("Invalid opcode generation specified in the run configuration, but no invalid opcodes supplied");
                return false;

            }
            if (getBooleanProperty(INCLUDE_INVALID_IDS, false)
                    && this.invalidIds.isEmpty()) {
                TraceManager

                        .getDefaultTracer()
                        .error("Invalid ID generation specified in the run configuration, but no invalid IDs supplied");
                return false;

            }
            if (getBooleanProperty(INCLUDE_INVALID_SEQIDS, false)
                    && this.invalidSeqIds.isEmpty()) {
                TraceManager

                        .getDefaultTracer()
                        .error("Invalid SEQID generation specified in the run configuration, but no invalid SEQIDs supplied");
                return false;

            }
        }
        return ok;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.config.AbstractXmlConfiguration#clear()
     */
    @Override
    public void clear() {

        super.clear();
        this.validOpcodes.clear();
        this.invalidOpcodes.clear();
        this.invalidIds.clear();
        this.validSeqIds.clear();
        this.invalidSeqIds.clear();
    }

    /**
     * Gets the list of invalid EVR IDs created from the configuration.
     * 
     * @return List of sorted Integer objects
     */
    public List<Integer> getInvalidIds() {

        final List<Integer> l = new LinkedList<Integer>(this.invalidIds);
        return Collections.unmodifiableList(l);
    }

    /**
     * Gets the list of invalid EVR SEQIDs created from the configuration.
     * 
     * @return List of sorted Integer objects
     */
    public List<Integer> getInvalidSeqIds() {

        final List<Integer> l = new LinkedList<Integer>(this.invalidSeqIds);
        return Collections.unmodifiableList(l);
    }

    /**
     * Gets the list of valid EVR SEQIDs created from the configuration.
     * 
     * @return List of sorted Integer objects
     */
    public List<Integer> getValidSeqIds() {

        final List<Integer> l = new LinkedList<Integer>(this.validSeqIds);
        return Collections.unmodifiableList(l);
    }

    /**
     * Gets the list of valid opcodes created from the configuration.
     * 
     * @return List of Opcode objects, sorted by opcode numeric value
     */
    public List<Opcode> getValidOpcodes() {

        final List<Opcode> l = new LinkedList<Opcode>(this.validOpcodes);
        return Collections.unmodifiableList(l);
    }

    /**
     * Gets the list of invalid opcodes created from the configuration.
     * 
     * @return List of Opcode objects, sorted by opcode value
     */
    public List<Opcode> getInvalidOpcodes() {

        final List<Opcode> l = new LinkedList<Opcode>(this.invalidOpcodes);
        return Collections.unmodifiableList(l);
    }

    /**
     * Establish defaults for properties that require them (have non-zero or
     * non-null default values).
     */
    @Override
    protected void setDefaultProperties() {

        super.setDefaultProperties();

        this.configProperties.put(INCLUDE_INVALID_OPCODES, false);
        this.configProperties.put(OPCODE_TRAVERSAL_TYPE, TraversalType.RANDOM);
        this.configProperties.put(INCLUDE_INVALID_IDS, false);
        this.configProperties.put(INCLUDE_INVALID_SEQIDS, false);
        this.configProperties.put(INCLUDE_INVALID_EVRS, false);
        this.configProperties.put(EVR_TRAVERSAL_TYPE, TraversalType.SEQUENTIAL);
        this.configProperties.put(EVR_TASK_NAME, "TASK");
        this.configProperties.put(EVR_STACK_DEPTH, 0);
        this.configProperties.put(STRING_MAX_LEN, Integer.valueOf(80));
    }

    /**
     * This is the SAX parse handler class for the EVR run configuration file.
     * 
     *
     */
    class RunConfigurationParseHandler extends
            AbstractRunConfigurationParseHandler {


        /**
         * {@inheritDoc}
         */
        @Override
        public void startElement(final String namespaceURI,
                final String localname, final String rawName,
                final Attributes atts) throws SAXException {

            super.startElement(namespaceURI, localname, rawName, atts);

            if (localname.equalsIgnoreCase(VALID_OPCODE)) {
                final String stem = atts.getValue(STEM).trim();
                final long num = XmlUtility
                        .getUnsignedIntFromAttr(atts, NUMBER);
                EvrRunConfiguration.this.validOpcodes.add(new Opcode(num, stem,
                        true));

            } else if (localname.equalsIgnoreCase(INVALID_OPCODE)) {

                final long num = XmlUtility
                        .getUnsignedIntFromAttr(atts, NUMBER);
                EvrRunConfiguration.this.invalidOpcodes.add(new Opcode(num,
                        null, false));
            } else if (localname.equalsIgnoreCase(INVALID_ID)) {
                final long id = XmlUtility.getUnsignedIntFromAttr(atts, ID);
                EvrRunConfiguration.this.invalidIds.add(Integer
                        .valueOf((int) id));
            } else if (localname.equalsIgnoreCase(VALID_SEQID)) {
                final long num = XmlUtility.getUnsignedIntFromAttr(atts, ID);
                EvrRunConfiguration.this.validSeqIds.add((int) num);

            } else if (localname.equalsIgnoreCase(INVALID_SEQID)) {

                final long num = XmlUtility.getUnsignedIntFromAttr(atts, ID);
                EvrRunConfiguration.this.invalidSeqIds.add((int) num);
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void endElement(final String namespaceURI,
                final String localname, final String rawName)
                throws SAXException {

            boolean found = storeBooleanElement(localname,
                    INCLUDE_INVALID_OPCODES);
            found = found
                    || storeBooleanElement(localname, INCLUDE_INVALID_EVRS);
            found = found
                    || storeBooleanElement(localname, INCLUDE_INVALID_IDS);
            found = found
                    || storeBooleanElement(localname, INCLUDE_INVALID_SEQIDS);
            found = found
                    || storeFloatElement(localname, INVALID_OPCODE_PERCENT);
            found = found || storeFloatElement(localname, INVALID_ID_PERCENT);
            found = found
                    || storeFloatElement(localname, INVALID_SEQID_PERCENT);
            found = found
                    || storeTraversalTypeElement(localname,
                            OPCODE_TRAVERSAL_TYPE);
            found = found
                    || storeTraversalTypeElement(localname, EVR_TRAVERSAL_TYPE);
            found = found || storeIntegerElement(localname, EVR_STACK_DEPTH);

            // If we got to here and the element is not found, let the
            // super class handle it.

            if (!found) {
                super.endElement(namespaceURI, localname, rawName);
            }
        }
    }
}
