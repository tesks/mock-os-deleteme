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
package jpl.gds.dictionary.impl;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.IBaseDictionary;
import jpl.gds.dictionary.api.OpcodeUtil;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.log.*;
import jpl.gds.shared.xml.parse.SAXParserPool;
import jpl.gds.shared.xml.validation.XmlValidationException;
import jpl.gds.shared.xml.validation.XmlValidator;
import jpl.gds.shared.xml.validation.XmlValidatorFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class from which to extend XML dictionary parser classes. Basic parsing,
 * support methods, and shared members are defined here.
 * 
 *
 */
public abstract class AbstractBaseDictionary extends DefaultHandler implements IBaseDictionary {

    /** 
     * XML element name for the multimission XML header.
     */
    protected static final String MM_HEADER_ELEMENT_NAME = "header";
        
    /**
     * Trace logger to be shared with subclasses.
     */
    protected static final Tracer          tracer                 = TraceManager.getTracer(Loggers.DICTIONARY);

    /**
     * The StringBuilder in which element text is written by the parser.
     */
    protected StringBuilder text = new StringBuilder();

    /** RNC schema validator for XML dictionaries */
    private static final XmlValidator schemaValidator =
            XmlValidatorFactory.createValidator(XmlValidatorFactory.SchemaType.RNC);

    /** Whether or not we have warned about schema validation */
    private static boolean warnedSchemaValidationNotEnabled = false;
    
    /**
     * Tracker for required schema elements.
     */
    private StartingRequiredElementTracker requiredElementTracker;

    /** GDS Dictionary version */
    private String gdsDictVersion = UNKNOWN;
    
    /** FSW or SSE build version */
    private String buildVersionId = null;
    
    /** FSW or SSE dictionary release version */
    private String releaseVersionId = null;
    
    /** Mission found in the parsed XML.*/
    private String mission = UNKNOWN;
    
    /** Latest schema supported by the parser */
    private final String implementedSchemaVersion;
    
    /** Actual schema version in the parsed XML */
    private String actualSchemaVersion = UNKNOWN;
    
    /** Spacecraft IDs in the parsed XML */
    private final List<Integer>                  spacecraftIds          = new ArrayList<>();
    
    /** Current dictionary configuration */
    private DictionaryProperties dictConfig = new DictionaryProperties(true);

    /**
     * Document locator provides a reference to current parse location for error
     * messages.
     */
    protected Locator locator;
    
    /** Type of dictionary, for log messages and exceptions */
    private DictionaryType dictionaryType;
    
    /** Shared opcode utility. */
    protected OpcodeUtil opcodeUtil;


    // For now, do not warn if the application name (system property) is
    // a "chill_get" application, this would be a huge interface change
    // Extended chill_get suppression to other applications that interface
    // with MCWS/VISTA. They expect clean data and should not have to filter out logs.
    // TODO - This should be a system property lookup rather than application name
    // Added case for frame gaps. Next time we run into an issue with this behavior we
    // should bite the bullet and make a system property. I'm not doing it this time because of time constraints.
    private static final boolean isDataInterface = ApplicationConfiguration.getApplicationName().startsWith("chill_get")
            || ApplicationConfiguration.getApplicationName().startsWith("chill_parse_")
            || ApplicationConfiguration.getApplicationName().startsWith("chill_dp_view")
            || ApplicationConfiguration.getApplicationName().endsWith("_gaps");

    /**
     * Constructor.
     * 
     * @param dictType the type of this dictionary (e.g., channel, alarm, etc)
     * @param maxSchemaVersion the currently implemented max schema version
     */
    protected AbstractBaseDictionary(final DictionaryType dictType, final String maxSchemaVersion) {
        
        if (dictType == null) {
            throw new IllegalArgumentException("dictionary type cannot be null or empty");
        }
        if (maxSchemaVersion == null) {
            throw new IllegalArgumentException("schema version cannot be null");
        }
        this.dictionaryType = dictType;
        this.implementedSchemaVersion = maxSchemaVersion;
        this.opcodeUtil = new OpcodeUtil(getDictionaryConfiguration());
    }

    @Override
    public void setDictionaryType(final DictionaryType dictType) {
        this.dictionaryType = dictType;
    }
    
    @Override
    public synchronized void setDocumentLocator(final Locator arg0) {
    	super.setDocumentLocator(arg0);
    	locator = arg0;
    }

    @Override
    public String getGdsVersionId() {
    	if (this.gdsDictVersion == null) {
    		return UNKNOWN;
    	}
    	return this.gdsDictVersion;
    }

    @Override
    public String getBuildVersionId() {
        return buildVersionId;
    }

    @Override
    public String getReleaseVersionId() {
        return releaseVersionId;
    }

    @Override
    public String getMission() {
        if (mission == null) {
            return UNKNOWN;
        }
        return mission;
    }

    @Override
    public String getActualSchemaVersion() {
        if (actualSchemaVersion == null) {
            return UNKNOWN;
        }
        return actualSchemaVersion;
    }

    @Override
    public String getImplementedSchemaVersion() {
        if (implementedSchemaVersion == null) {
            return UNKNOWN;
        }
        return implementedSchemaVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> getSpacecraftIds() {
        return spacecraftIds;
    }
    
    /**
     * {@inheritDoc}
     * 
     */
    @Override
    public synchronized void clear() {
      
        locator = null;
        text = new StringBuilder();
        gdsDictVersion = UNKNOWN;
        buildVersionId = null;
        releaseVersionId = UNKNOWN;
        spacecraftIds.clear();
        actualSchemaVersion = UNKNOWN;
        mission = UNKNOWN;
        dictConfig = new DictionaryProperties(true);
               
        if (requiredElementTracker != null) {
            requiredElementTracker.clearState();
        }       
    }
       
    @Override
    public synchronized void parse(final String uri) throws DictionaryException {
        parse(uri, this.dictConfig);
    }

    @Override
    public synchronized void parse(final String uri, final DictionaryProperties config) throws DictionaryException {
        parse(uri, config, tracer);
    }

    @Override
    public void parse(final String uri, final DictionaryProperties config, final Tracer tracer)
            throws DictionaryException {
        // set app context into static tracer if it's null so header info gets logged to db
        if (AbstractBaseDictionary.tracer.getAppContext() == null) {
            AbstractBaseDictionary.tracer.setAppContext(tracer.getAppContext());
        }
        if (uri == null) {
            final String message = dictionaryType + " dictionary path is undefined. Not reading " + dictionaryType + " dictionary";
            tracer.error(message);
            throw new DictionaryException(message);
        }
        
        if (config == null) {
            throw new IllegalArgumentException("Dictionary configuration may not be null");
        }
        /* Save dictionary config */
        setDictionaryConfiguration(config);

        
        final String defFile = dictionaryType + " dictionary file ";
        final File path = new File(uri);

        /*
         * Added schema validation before parsing dictionary
         * Added logic to not validate sequence dictionary (we don't own the schema)
         */
        if (dictConfig.isSchemaValidationEnabled() && dictionaryType != DictionaryType.SEQUENCE) {
            checkSchema(path);
        } else {
            if (!warnedSchemaValidationNotEnabled && dictionaryType != DictionaryType.SEQUENCE) {
                warnedSchemaValidationNotEnabled = true;

                tracer.log(isDataInterface ? Markers.SUPPRESS : Markers.DICT,
                           TraceSeverity.WARN,
                           " Dictionary schema validation property [" +
                                   DictionaryProperties.SCHEMA_VALIDATION_PROPERTY + "] is NOT enabled!");
            }
        }

        tracer.info(isDataInterface ? Markers.SUPPRESS : Markers.DICT,
                "Parsing " + dictionaryType + " definitions from "
                + FileUtility.createFilePathLogMessage(path));

        SAXParser sp = null;
        try {
            sp = SAXParserPool.getInstance().getNonPooledParser();
            sp.parse(uri, this);
        } catch (final FileNotFoundException e) {
            tracer.error(defFile + path.getAbsolutePath()
                    + " not found");
            throw new DictionaryException(defFile
                    + path.getAbsolutePath() + " not found", e);
        } catch (final IOException e) {
            tracer.error("IO Error reading " + defFile
                    + path.getAbsolutePath());
            throw new DictionaryException(
                    "IO Error reading " + defFile
                            + path.getAbsolutePath());
        } catch (final ParserConfigurationException e) {
            tracer.error("Error configuring SAX parser for " + defFile
                    + path.getAbsolutePath());
            throw new DictionaryException(
                    "Error configuring SAX parser for " + defFile 
                            + path.getAbsolutePath(), e);
        } catch (final SAXException e) {
            tracer.error("Error parsing " + defFile
                    + path.getAbsolutePath());
            tracer.error(e.getMessage());
            tracer.error("Verify that the configured " + dictionaryType
                    + " parser matches the XML schema to which the dictionary file conforms");
            throw new DictionaryException(e.getMessage(), e);
        } catch (final Exception e) {
            tracer.error("Unexpected error parsing or reading " + defFile);
            throw new DictionaryException(String.format("Unexpected error parsing or reading " + defFile
                    + " at line %d", locator.getLineNumber() - 1), e);
        }

    }

    @Override
    public DictionaryProperties getDictionaryConfiguration() {
        return this.dictConfig;
    }
    
    @Override
    public DictionaryType getDictionaryType() {
        return this.dictionaryType;
    }
    
    @Override
	public void setDictionaryConfiguration(final DictionaryProperties config) {
        if (config == null) {
            throw new IllegalArgumentException("Dictionary configuration may not be null");
        }
        this.dictConfig = config;
        this.opcodeUtil = new OpcodeUtil(getDictionaryConfiguration());
    }

    /**
     * Extract a required XML attribute or throw an exception.
     * 
     * @param name
     *            required attribute name
     * @param ename
     *            element name (for error message only)
     * @param attr
     *            attribute list
     * @return value of found attribute
     * @throws SAXParseException if the attribute is not present
     */
    protected String getRequiredAttribute(final String name, final String ename, final Attributes attr)
            throws SAXParseException {
            	final String val = attr.getValue(name);
            	if (null == val) {
            		error(String.format("Missing required attribute %s in element %s",
            				name, ename));
            	}
            	return val;
            }

    /**
     * Sets the FSW or SSE build version ID. There is no standard
     * format.
     * 
     * @param buildVersion build version to set; may be null
     */
    protected void setBuildVersionId(final String buildVersion) {
        this.buildVersionId = buildVersion;
    }

    /**
     * Sets the FSW or SSE dictionary release version ID. There is 
     * no standard format.
     * 
     * @param releaseVersion version to set; may be null
     */
    protected void setReleaseVersionId(final String releaseVersion) {
        this.releaseVersionId = releaseVersion;
    }

    /**
     * Set the required starting elements for the current schema.  If set,
     * the startElement() method in this class will invoke a check for 
     * every XML element seen. The elements on the required list are those that MUST 
     * be seen prior to any other element in the XML for there to be confirmed match
     * to the schema the parser corresponds to. This is a sanity check for schema only,
     * because at this time, we do not validate the XML against the schema.
     * 
     * @param elementNames List of element names required at the start of the XML
     * @param schemaName the name of the schema this parser instance supports
     * 
     */
    protected void setRequiredElements(final String schemaName, final List<String> elementNames) {
    	requiredElementTracker = new StartingRequiredElementTracker(schemaName, dictionaryType, elementNames);
    }

    /**
     * Warn if actual schema version seems to be later than implemented schema version.
     *
     *
     * @param dictionaryFile The dictionary file to validate against a schema
     */
    protected void checkSchema(final File dictionaryFile) {
        boolean valid = false;
        final String schemaLocation = dictConfig.getSchemaLocationFromDictionaryType(dictionaryType);

        File schemaFile = new File(schemaLocation);
        if (!schemaLocation.isEmpty() && schemaFile.exists()) {


            // TODO: Figure out a better way to suppress these errors
            // Picollo xml parser prints validations errors to stderr
            // For now, hijack stderr for xml validation and set it back to the original afterwards
            PrintStream stderr = System.err;
            PrintStream tempErrorStream = new PrintStream(new ByteArrayOutputStream());
            try {
                tracer.debug("Validating ", dictionaryFile.getAbsoluteFile() , " against ", schemaFile.getAbsolutePath());

                System.setErr(tempErrorStream);

                valid = schemaValidator.validateXml(schemaFile, dictionaryFile);
                tracer.debug("SUCCESSFULLY ran schema validation on ", schemaFile, " against ", dictionaryFile);

                System.setErr(stderr);
            }
            catch (XmlValidationException | IllegalStateException e) {
                System.setErr(stderr);

                tracer.warn(isDataInterface ? Markers.SUPPRESS : Markers.DICT, ExceptionTools.getMessage(e), ": "
                                    + "Exception occurred "
                                    + "validating ",
                             dictionaryFile.getAbsoluteFile(), " against the schema ", schemaFile.getAbsolutePath());
            }
            tempErrorStream.close();
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(dictionaryType.toString())
          .append(" schema validation ")
          .append(valid ? "PASSED!" :"FAILED!")
          .append(" against ")
          .append(dictConfig.getDictionarySchemaName(dictionaryType));
        if (schemaLocation.isEmpty() || !schemaFile.exists()) {
            sb.append(" Unable to locate schema ")
              .append(schemaFile)
              .append(" at the configured location ")
              .append(schemaLocation);
        }

        tracer.log(isDataInterface ? Markers.SUPPRESS : Markers.DICT, (valid ? TraceSeverity.INFO : TraceSeverity.WARN), sb.toString());
    }
    
    /**
     * Reports XML header information (spacecraft ID, mission, version) to the external logger using
     * the info logging level.
     */
    protected void reportHeaderInfo() {
        if (spacecraftIds.isEmpty()) {
            spacecraftIds.add(IBaseDictionary.UNKNOWN_SCID);
        }
        reportHeaderInfo(TraceSeverity.INFO);
    }
       
    /**
     * Reports XML header information (spacecraft ID, mission, version) to the external logger 
     * using the goven loggin severity.
     * 
     * @param severity TraceSeverity for the logger 
     */
    protected void reportHeaderInfo(final TraceSeverity severity) {
        final StringBuilder builder = new StringBuilder("Parsing " + dictionaryType + " dictionary for mission " + mission);

        builder.append(", spacecraft ID(s) [");
        for (final Integer scid : spacecraftIds) {
            builder.append(scid).append(",");
        }
        builder.append("]");

        builder.append(", GDS dictionary version " + gdsDictVersion);
        if (this.buildVersionId != null) {
            builder.append(", build version " + buildVersionId);
        }
        if (this.releaseVersionId != null) {
            builder.append(", release version " + releaseVersionId);
        }
        tracer.log(isDataInterface ? Markers.SUPPRESS : Markers.DICT, severity, builder.toString());
    }
    
    /**
     * Sets the GDS version ID.
     * 
     * @param version version to set; if null, will be set to UNKNOWN
     */
    protected void setGdsVersionId(final String version) {
        if (version == null) {
            this.gdsDictVersion = UNKNOWN;
        } else {
            this.gdsDictVersion = version.trim();
        }
    }

    /**
     * Sets the mission/project name.
     * 
     * @param mission mission to set; if null, will be set to UNKNOWN 
     */
    protected void setMission(final String mission) {
        if (mission == null) {
            this.mission = UNKNOWN;
        } else {
            this.mission = mission.trim();
        }
    }

    /**
     * Sets the actual schema version as found in the dictionary file. Schema
     * version must be of the form "V.M", where V is major version number and M
     * is minor version number,UNKNOWN
     * 
     * @param actualSchemaVersion
     *            schema version as V.M string; may also be UNKNOWN
     */
    protected void setActualSchemaVersion(final String actualSchemaVersion) {
        if (actualSchemaVersion == null) {
            this.actualSchemaVersion = UNKNOWN;
        } else {
            this.actualSchemaVersion = actualSchemaVersion.trim();
        }
    }

    /**
     * Adds a spacecraft ID, to the List found in the dictionary file.
     * 
     * @param spacecraftId
     *            the numeric spacecraft ID to add
     */
    protected void addSpacecraftId(final int spacecraftId) {
        this.spacecraftIds.add(spacecraftId);
    }
    
    /**
     * If the supplied XML element name matches the multimission header element,
     * parses the multimission XML header and reports the header information
     * to the log.
     * 
     * @param elementName XML element being parsed
     * @param atts SAX attributes object
     * @throws SAXParseException if a required header attribute is missing
     */
    protected void parseMultimissionHeader(final String elementName, final Attributes atts) throws SAXParseException {
        if (elementName.equalsIgnoreCase(MM_HEADER_ELEMENT_NAME)) {

            /* Updates to parsing of mission/scid/schema_version.*/

            setMission(getRequiredAttribute("mission_name", elementName,
                    atts).trim());

            final String scId = atts.getValue("spacecraft_id");
                       
            if (scId != null) {
                try {
                    addSpacecraftId(Integer.valueOf(scId.trim()));
                } catch (final NumberFormatException e) {
                    tracer.warn("Spacecraft ID in XML header is not an integer and will be considered undefined");
                }
            }

            buildVersionId = atts.getValue("dictionary_build_id");

            setActualSchemaVersion(getRequiredAttribute("schema_version", elementName, atts).trim().toUpperCase());
            if (getActualSchemaVersion().startsWith("V")) {
                setActualSchemaVersion(getActualSchemaVersion().substring(1));
            }
            setGdsVersionId(getRequiredAttribute("version", elementName, atts));
        }
        else if (elementName.equalsIgnoreCase("spacecraft_ids")) {
            // Nothing required; this is a list
        }
        else if (elementName.equalsIgnoreCase("spacecraft_id")) {
            final String scId = atts.getValue("value");
            if (scId != null) {
                try {
                    addSpacecraftId(Integer.valueOf(scId));
                }
                catch (final NumberFormatException e) {
                    tracer.warn("Spacecraft ID entry [" + scId
                            + "] in XML header is not an integer and will be considered undefined");
                }
            }
        }
            
    }

    /**
     * Gets a trimmed string from the buffer of SAx-parsed characters.
     * 
     * @return trimmed string; may be empty but never null
     */
    protected String getTrimmedText() {
        return text.toString().trim();
        
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(final String uri, final String localName, final String qname,
            final Attributes attr) throws SAXException {
            	text = new StringBuilder();
            	/* Added check for required starting elements. */
            	if (this.requiredElementTracker != null) {
            		requiredElementTracker.checkState(qname);
            	}
            }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName, final String qname) throws SAXException {
        if (localName.equalsIgnoreCase(MM_HEADER_ELEMENT_NAME)) {
            reportHeaderInfo();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters(final char[] chars, final int start, final int length)
            throws SAXException {
            	final String newText = new String(chars, start, length);
            	if (!newText.equals("\n")) {
            		text.append(newText);
            	}
            }

    /**
     * Local shorthand for throwing SAXParseException
     * 
     * @param message the error message for the exception
     * @throws SAXParseException always
     */
    public void error(final String message) throws SAXParseException {
    	throw new SAXParseException(message, locator);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
     */
    @Override
    public void error(final SAXParseException e) throws SAXException {
    	throw new SAXException("Parse error in " + dictionaryType + " dictionary file line "
    			+ e.getLineNumber() + " col " + e.getColumnNumber() + ": "
    			+ e.getMessage());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.SAXParseException)
     */
    @Override
    public void fatalError(final SAXParseException e) throws SAXException {
    	throw new SAXException(
    			"Fatal parse error in " + dictionaryType + " dictionary file line "
    					+ e.getLineNumber() + " col " + e.getColumnNumber()
    					+ ": " + e.getMessage());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#warning(org.xml.sax.SAXParseException)
     */
    @Override
    public void warning(final SAXParseException e) {
        tracer.warn("Parse warning in" + dictionaryType + " dictionary file line "
    			+ e.getLineNumber() + " col " + e.getColumnNumber() + ": "
    			+ e.getMessage());
    }

}
