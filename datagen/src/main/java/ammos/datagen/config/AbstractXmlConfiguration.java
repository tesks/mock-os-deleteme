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
package ammos.datagen.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.shared.xml.parse.SAXParserPool;
import jpl.gds.shared.xml.validation.RelaxNgCompactValidator;
import jpl.gds.shared.xml.validation.XmlValidationException;
import jpl.gds.shared.xml.validation.XmlValidator;

/**
 * This class is the abstract representation of an XML configuration file for
 * the data generators. It provides basic methods for XML parsing and accessing
 * configuration properties once loaded and stored. The other variable XML
 * configuration classes extend this one.
 * 
 *
 */
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class AbstractXmlConfiguration implements IXmlConfiguration {

    /**
     * The map of configuration properties: name/value.
     */
    protected final Map<String, Object> configProperties = new HashMap<String, Object>(
            1024);

    private final String schemaFile;
    private final String configName;
    private boolean validating = true;

    /**
     * Constructor.
     * 
     * @param name
     *            the name of this config file, for display in error messages
     * @param schema
     *            the root-relative name of the XML schema file to validate the
     *            XML configuration file against
     */
    public AbstractXmlConfiguration(final String name, final String schema) {

        super();
        this.schemaFile = schema;
        this.configName = name + " file ";

    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.config.IXmlConfiguration#load(java.lang.String,
     *      org.xml.sax.helpers.DefaultHandler)
     */
    @Override
    public boolean load(final String uri, final DefaultHandler handler) {

        if (uri == null) {
            throw new IllegalArgumentException("File path for "
                    + this.configName + " cannot be null");
        }
        final File path = new File(uri);

        SAXParser sp = null;
        boolean success = false;

        try {
            /*
             * Validate the XML file against its schema first and bail if it
             * fails.
             */
            if (!validate(path)) {
                TraceManager.getDefaultTracer().error(

                        "XML schema validation of " + this.configName
                                + path.getAbsolutePath() + " failed");
                return false;
            }
            /*
             * Now clear out old values and parse the new file.
             */
            clear();
            sp = SAXParserPool.getInstance().getNonPooledParser();
            sp.parse(uri, handler);
            success = true;

        } catch (final FileNotFoundException e) {
            TraceManager.getDefaultTracer().error(

                    this.configName + path.getAbsolutePath() + " not found");
        } catch (final IOException e) {
            TraceManager.getDefaultTracer().error(

                    "IO Error reading " + this.configName
                            + path.getAbsolutePath());
        } catch (final ParserConfigurationException e) {
            TraceManager.getDefaultTracer().error(

                    "Error configuring SAX parser for " + this.configName
                            + path.getAbsolutePath());
        } catch (final SAXException e) {
            TraceManager.getDefaultTracer()

                    .error("Error parsing " + this.configName
                            + path.getAbsolutePath());
            /* MPCS-6333 - 7/1/14. Added additional log message */
            TraceManager.getDefaultTracer().error(e.toString());

        }
        return success;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.config.IXmlConfiguration#clear()
     */
    @Override
    public void clear() {

        this.configProperties.clear();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.config.IXmlConfiguration#validate(java.io.File)
     */
    @Override
    public boolean validate(final File configFile) {

        /*
         * If not validating just assume success.
         */
        if (!this.validating) {
            return true;
        }

        /*
         * Set up an RNC file validator.
         */
        final XmlValidator validator = new RelaxNgCompactValidator();
        final String schemaLoc = ApplicationConfiguration.getRootDir()
                + File.separatorChar + this.schemaFile;
        final File schemaFileObj = new File(schemaLoc);
        /*
         * Validate the XML. Errors from the validator go to stdout and there's
         * nothing we can really do about that.
         */
        try {
            return validator.validateXml(schemaFileObj, configFile);
        } catch (final XmlValidationException e) {
            TraceManager.getDefaultTracer().error(

                    "XML validation of " + this.configName + "failed: "
                            + e.getMessage());
            if (e.getCause() != null) {
                TraceManager.getDefaultTracer()

                        .error(e.getCause().getMessage());
            }
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.config.IXmlConfiguration#getStringProperty(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public String getStringProperty(final String propertyName,
            final String defaultVal) {

        final Object val = this.configProperties.get(propertyName);
        if (!(val instanceof String)) {
            return defaultVal;
        }
        return (String) val;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.config.IXmlConfiguration#getIntProperty(java.lang.String,
     *      int)
     */
    @Override
    public int getIntProperty(final String propertyName, final int defaultVal) {

        final Object val = this.configProperties.get(propertyName);
        if (!(val instanceof Integer)) {
            return defaultVal;
        }
        return (Integer) val;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.config.IXmlConfiguration#getLongProperty(java.lang.String,
     *      long)
     */
    @Override
    public long getLongProperty(final String propertyName, final long defaultVal) {

        final Object val = this.configProperties.get(propertyName);
        if (!(val instanceof Long)) {
            return defaultVal;
        }
        return (Long) val;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.config.IXmlConfiguration#getBooleanProperty(java.lang.String,
     *      boolean)
     */
    @Override
    public boolean getBooleanProperty(final String propertyName,
            final boolean defaultVal) {

        final Object val = this.configProperties.get(propertyName);
        if (!(val instanceof Boolean)) {
            return defaultVal;
        }
        return (Boolean) val;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.config.IXmlConfiguration#getBooleanProperty(java.lang.String,
     *      boolean)
     */
    @Override
    public float getFloatProperty(final String propertyName,
            final float defaultVal) {

        final Object val = this.configProperties.get(propertyName);
        if (!(val instanceof Float)) {
            return defaultVal;
        }
        return (Float) val;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.config.IXmlConfiguration#getTraversalTypeProperty(java.lang.String,
     *      ammos.datagen.config.TraversalType)
     */
    @Override
    public TraversalType getTraversalTypeProperty(final String propertyName,
            final TraversalType defaultVal) {

        final Object val = this.configProperties.get(propertyName);
        if (!(val instanceof TraversalType)) {
            return defaultVal;
        }
        return (TraversalType) val;
    }

    /**
     * Sets the flag indicating whether this configuration validates against the
     * schema. Must be set before load() is called.
     * 
     * @param validate
     *            true to validate, false to not
     */
    public void setValidating(final boolean validate) {

        this.validating = validate;
    }

    /**
     * This abstract class provides basic methods for implementing a SAX parse
     * handler, including error handling methods and methods for capturing
     * parsed characters. The parse handler subclasses in the XML configuration
     * subclasses extend this inner class.
     * 
     *
     */
    public class AbstractXmlParseHandler extends DefaultHandler {
        private final StringBuilder text = new StringBuilder(256);

        /**
         * {@inheritDoc}
         * 
         * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
         *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(final String namespaceURI,
                final String localname, final String rawName,
                final Attributes atts) throws SAXException {

            /*
             * This clears out the per-element text buffer, where the characters
             * inside an XML element are placed.
             */
            this.text.delete(0, this.text.length());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
         *      java.lang.String, java.lang.String)
         */
        @Override
        public void endElement(final String namespaceURI,
                final String localname, final String rawName)
                throws SAXException {

            // do nothing
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
         */
        @Override
        public void error(final SAXParseException e) throws SAXException {

            throw new SAXException(
                    "Parse error in mission configuration file line "
                            + e.getLineNumber() + " col " + e.getColumnNumber()
                            + ": " + e.getMessage());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.SAXParseException)
         */
        @Override
        public void fatalError(final SAXParseException e) throws SAXException {

            throw new SAXException(
                    "Fatal parse error in mission configuration file line "
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

            TraceManager.getDefaultTracer().error(

                    "Parse warning in mission configuration file line "
                            + e.getLineNumber() + " col " + e.getColumnNumber()
                            + ": " + e.getMessage());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
         */
        @Override
        public void characters(final char[] chars, final int start,
                final int length) throws SAXException {

            final String newText = new String(chars, start, length);
            if (!newText.equals("\n")) {
                this.text.append(newText);
            }
        }

        /**
         * Gets the text parsed out of the latest element.
         * 
         * @return trimmed text string
         */
        protected String getBufferText() {

            return this.text.toString().trim();
        }

        /**
         * Checks the given XML element name to see if it matches the given
         * configuration property name. If so, parses the current element's
         * buffer text, converts it to an integer, and stores it in the
         * configuration properties table.
         * 
         * @param localName
         *            the XML element that has just been parsed using SAX
         * @param propertyName
         *            the name of the configuration property
         * 
         * @return true if the configuration property was stored; false if not
         */
        protected boolean storeIntegerElement(final String localName,
                final String propertyName) {

            if (localName.equalsIgnoreCase(propertyName)) {
                final int val = XmlUtility.getIntFromText(getBufferText());
                AbstractXmlConfiguration.this.configProperties.put(
                        propertyName, val);
                return true;
            }
            return false;
        }

        /**
         * Checks the given XML element name to see if it matches the given
         * configuration property name. If so, parses the current element's
         * buffer text, converts it to a TraversalType, and stores it in the
         * configuration properties table.
         * 
         * @param localName
         *            the XML element that has just been parsed using SAX
         * @param propertyName
         *            the name of the configuration property
         * 
         * @return true if the configuration property was stored; false if not
         */
        protected boolean storeTraversalTypeElement(final String localName,
                final String propertyName) {

            if (localName.equalsIgnoreCase(propertyName)) {
                final TraversalType val = TraversalType
                        .valueOf(getBufferText());
                AbstractXmlConfiguration.this.configProperties.put(
                        propertyName, val);
                return true;
            }
            return false;
        }

        /**
         * Checks the given XML element name to see if it matches the given
         * configuration property name. If so, parses the current element's
         * buffer text, converts it to a long, and stores it in the
         * configuration properties table.
         * 
         * @param localName
         *            the XML element that has just been parsed using SAX
         * @param propertyName
         *            the name of the configuration property
         * 
         * @return true if the configuration property was stored; false if not
         */
        protected boolean storeLongElement(final String localName,
                final String propertyName) {

            if (localName.equalsIgnoreCase(propertyName)) {
                final long val = XmlUtility.getLongFromText(getBufferText());
                AbstractXmlConfiguration.this.configProperties.put(
                        propertyName, val);
                return true;
            }
            return false;
        }

        /**
         * Checks the given XML element name to see if it matches the first
         * configuration property name. If so, parses the current element's
         * buffer text, converts it to a long, and stores it in the
         * configuration properties table using the second property name.
         * 
         * @param localName
         *            the XML element that has just been parsed using SAX
         * @param propertyName
         *            the name of the configuration property to parse
         * @param storeAsProperty
         *            the name of the configuration property to store
         * 
         * @return true if the configuration property was stored; false if not
         */
        protected boolean storeLongElementAs(final String localName,
                final String propertyName, final String storeAsProperty) {

            if (localName.equalsIgnoreCase(propertyName)) {
                final long val = XmlUtility.getLongFromText(getBufferText());
                AbstractXmlConfiguration.this.configProperties.put(
                        storeAsProperty, val);
                return true;
            }
            return false;
        }

        /**
         * Checks the given XML element name to see if it matches the given
         * configuration property name. If so, parses the current element's
         * buffer text, converts it to a float, and stores it in the
         * configuration properties table.
         * 
         * @param localName
         *            the XML element that has just been parsed using SAX
         * @param propertyName
         *            the name of the configuration property
         * 
         * @return true if the configuration property was stored; false if not
         */
        protected boolean storeFloatElement(final String localName,
                final String propertyName) {

            if (localName.equalsIgnoreCase(propertyName)) {
                final float val = XmlUtility.getFloatFromText(getBufferText());
                AbstractXmlConfiguration.this.configProperties.put(
                        propertyName, val);
                return true;
            }
            return false;
        }

        /**
         * Checks the given XML element name to see if it matches the given
         * configuration property name. If so, parses the current element's
         * buffer text, converts it to a boolean, and stores it in the
         * configuration properties table.
         * 
         * @param localName
         *            the XML element that has just been parsed using SAX
         * @param propertyName
         *            the name of the configuration property
         * 
         * @return true if the configuration property was stored; false if not
         */
        protected boolean storeBooleanElement(final String localName,
                final String propertyName) {

            if (localName.equalsIgnoreCase(propertyName)) {
                final boolean val = XmlUtility
                        .getBooleanFromText(getBufferText());
                AbstractXmlConfiguration.this.configProperties.put(
                        propertyName, val);
                return true;
            }
            return false;
        }

        /**
         * Checks the given XML element name to see if it matches the given
         * configuration property name. If so, parses the current element's
         * buffer text as a string and stores it in the configuration properties
         * table.
         * 
         * @param localName
         *            the XML element that has just been parsed using SAX
         * @param propertyName
         *            the name of the configuration property
         * 
         * @return true if the configuration property was stored; false if not
         */
        public boolean storeStringElement(final String localName,
                final String propertyName) {

            if (localName.equalsIgnoreCase(propertyName)) {
                AbstractXmlConfiguration.this.configProperties.put(
                        propertyName, getBufferText());
                return true;
            }
            return false;
        }

        /**
         * Checks the given XML element name to see if it matches the first
         * configuration property name. If so, parses the current element's
         * buffer text as a string and stores it in the configuration properties
         * table using the second property name.
         * 
         * @param localName
         *            the XML element that has just been parsed using SAX
         * @param propertyName
         *            the name of the configuration property
         * @param storeAsProperty
         *            the name of the configuration property to store
         * 
         * @return true if the configuration property was stored; false if not
         */
        public boolean storeStringElementAs(final String localName,
                final String propertyName, final String storeAsProperty) {

            if (localName.equalsIgnoreCase(propertyName)) {
                AbstractXmlConfiguration.this.configProperties.put(
                        storeAsProperty, getBufferText());
                return true;
            }
            return false;
        }
    }
}