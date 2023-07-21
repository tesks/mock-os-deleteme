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
package jpl.gds.shared.xml;

import java.io.IOException;
import java.io.StringReader;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import jpl.gds.shared.gdr.GDR;

/**
 * 
 * A set of static utility functions for manipulating XML and XML-related
 * objects. Note that these methods span the DOM, SAX, and STAX realms.
 * 
 */
public final class XmlUtility {

    /**
     * Private constructor to enforce static nature.
     */
    private XmlUtility() { 
        // not used
    }

    /**
     * A piece of whitespace (includes space, newline, carriage return, tab,
     * etc.)
     */
    public static final String WHITESPACE_REGEXP = "[ \r\n\t\b\f\0]{1,}";

    /**
     * Parses a string to determine whether it is a valid XML document and
     * returns the parsed XML Document. Does not perform validation against a
     * schema.
     * 
     * @param xmlString
     *            the XML text to parse
     * @return the XML Document object
     * @throws IOException
     *             I/O error
     * @throws SAXException
     *             XML parsing error
     * @throws ParserConfigurationException
     *             Parser config error
     */
    public synchronized static Document getDocumentFromString(
            final String xmlString) throws SAXException, IOException,
            ParserConfigurationException {
        if (xmlString == null) {
            throw new IllegalArgumentException("Null input string");
        }

        StringReader reader = new StringReader(xmlString);
        InputSource is = new InputSource(reader);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
        Document doc = documentBuilder.parse(is);

        reader.close();
        return (doc);
    }

    /**
     * Removes all the whitespace from the String representation of an XML
     * document. This is literally done by removing any whitespace that appears
     * between the two characters &gt; and &lt;.
     * 
     * @param s
     *            The String containing an XML document that needs modifying
     * @return The new String with whitespace nodes removed
     */
    public static String removeWhitespaceNodesFromXMLString(final String s) {
        if (s == null) {
            throw new IllegalArgumentException("Null input string");
        }

        return (s.replaceAll(">" + WHITESPACE_REGEXP + "<", "><"));
    }

    /**
     * Parses and returns an integer value from the given text
     * 
     * @param text
     *            the String representation of an integer
     * @return the integer value
     */
    public static int getIntFromText(final String text) {
        int i = 0;
        if (text != null) {
            i = GDR.parse_int(text);
        }
        return i;
    }

    /**
     * Parses and returns a long value from the given text.
     * 
     * @param text
     *            the String representation of a long
     * @return the long value
     */
    public static long getLongFromText(final String text) {
        long i = 0;
        if (text != null) {
            i = GDR.parse_long(text);
        }
        return i;
    }

    /**
     * Parses and returns a float value from the given text.
     * 
     * @param text
     *            the String representation of a float
     * @return the float value
     */
    public static float getFloatFromText(final String text) {
        float i = 0;
        if (text != null) {
            i = Float.parseFloat(text);
        }
        return i;
    }

    /**
     * Parses and returns a double value from the given text.
     * 
     * @param text
     *            the String representation of a double
     * @return the double value
     */
    public static double getDoubleFromText(final String text) {
        double i = 0;
        if (text != null) {
            i = Double.parseDouble(text);
        }
        return i;
    }

    /**
     * Parses and returns a character value from the given text. If the input
     * contains more than one character, the first character will be returned.
     * 
     * @param text
     *            the String representation of a character
     * @return the character value, or 0 if the input does not contain a value
     *         or is null
     */
    public static char getCharFromText(final String text) {
        if (text == null || text.length() < 1) {
            return 0;
        }
        return text.charAt(0);
    }


    /**
     * Converts the current contents of the text buffer into a boolean.
     * 
     * @param boolStr
     *            Boolean as text
     * 
     * @return the boolean value, or false if the text buffer is null
     */
    public static boolean getBooleanFromText(final String boolStr) {
        boolean b = false;
        if (boolStr != null) {
            b = Boolean.valueOf(boolStr);
        }
        return b;
    }

    /**
     * Converts the current contents of an XML attribute into a boolean.
     * 
     * @param attr
     *            the SAX Attributes object as passed to the startElement()
     *            method by the parser
     * @param attrName
     *            the name of the attribute to be converted
     * @return the boolean value, or false if the attribute does not exist
     */
    public static boolean getBooleanFromAttr(final Attributes attr,
            final String attrName) {
        String boolStr = attr.getValue(attrName);
        boolean b = false;
        if (boolStr != null) {
            b = GDR.parse_boolean(boolStr);
        }
        return b;
    }

    /**
     * Converts the contents of an XML attribute to an integer.
     * 
     * @param attr
     *            the SAX Attributes object
     * @param name
     *            the name of the attribute
     * @return the integer value, or 0 if the attribute was not found
     */
    public static int getIntFromAttr(final Attributes attr, final String name) {
        int value = 0;
        String val = attr.getValue(name);
        if (val != null) {
            value = GDR.parse_int(val);
        }
        return value;
    }

    /**
     * Converts the contents of an XML attribute to an unsigned integer.
     * 
     * @param attr
     *            the SAX Attributes object
     * @param name
     *            the name of the attribute
     * @return the unsigned integer value, or 0 if the attribute was not found; 
     * note this value is a long toavoid the sign bits
     */
    public static long getUnsignedIntFromAttr(final Attributes attr, final String name) {
        long value = 0;
        final String val = attr.getValue(name);
        if (val != null) {
            value = GDR.parse_unsigned(val);
        }
        return value;
    }

    /**
     * Converts the contents of an XML attribute to a long integer.
     * 
     * @param attr
     *            the SAX Attributes object
     * @param name
     *            the name of the attribute
     * @return the long integer value, or 0 if the attribute was not found
     */
    public static long getLongFromAttr(final Attributes attr, final String name) {
        long value = 0;
        String val = attr.getValue(name);
        if (val != null) {
            value = GDR.parse_long(val);
        }
        return value;
    }

    /**
     * Converts the contents of an XML attribute to a double.
     * 
     * @param attr
     *            the SAX Attributes object
     * @param name
     *            the name of the attribute
     * @return the double value, or 0 if the attribute was not found
     */
    public static double getDoubleFromAttr(final Attributes attr,
            final String name) {
        double value = 0;
        String val = attr.getValue(name);
        if (val != null) {
            value = Double.parseDouble(val);
        }
        return value;
    }

    /**
     * getStringFromAttr -- This can also be done with an attr.getValue()
     * 
     * @param attr
     *            Attribute from which to extract the String.
     * 
     * @param name
     *            Name of the attribute to extract as a String value.
     * 
     * @return String value of name. Or "" if attribute is null or name is null
     *         or String value was not found.
     * 
     */
    public static String getStringFromAttr(final Attributes attr,
            final String name) {
        String value = "";
        if (null == attr) {
            return value;
        }
        if (null == name) {
            return value;
        }
        value = attr.getValue(name);
        return value;
    } // end member function getStringFromAttr

    /**
     * Replaces white space tokens in the given string with a single space.
     * 
     * @param original
     *            the original string
     * @return the normalized string, or null if there was nothing in the
     *         original string
     */
    public static String normalizeWhitespace(final StringBuilder original) {
        StringBuilder buf = null;
        StringTokenizer t = new StringTokenizer(original.toString());
        while (t.hasMoreTokens()) {
            if (buf == null) {
                buf = new StringBuilder();
            } else {
                buf.append(" ");
            }
            buf.append(t.nextToken());
        }
        if (buf == null) {
            buf = original;
        }
        return buf == null ? null : buf.toString().trim();
    }

    /**
     * Replaces white space tokens in the given string with a single space.
     * 
     * @param original
     *            the original string
     * @return the normalized string, or null if there was nothing in the
     *         original string
     */
    public static String normalizeWhitespace(final String original) {
        StringBuffer buf = null;
        StringTokenizer t = new StringTokenizer(original);
        while (t.hasMoreTokens()) {
            if (buf == null) {
                buf = new StringBuffer();
            } else {
                buf.append(" ");
            }
            buf.append(t.nextToken());
        }
        if (buf == null) {
            buf = new StringBuffer(original);
        }
        return buf == null ? null : buf.toString().trim();
    }

    /**
     * Write a simple XML text element to an XMlStreamWriter. Will write nothing if
     * the given object value is null or results in an empty string when a
     * toString().trim() is invoked on it.
     * 
     * @param writer
     *            XML stream writer
     * @param element
     *            Name of element
     * @param value
     *            Object whose string value is to be written
     * 
     * @throws XMLStreamException
     *             If error writing stream
     *             
     */
    public static void writeSimpleElement(final XMLStreamWriter writer,
            final String element, final Object value) throws XMLStreamException {

        if (value == null) {
            return;
        }

        final String trimValue = value.toString().trim();

        if (trimValue.isEmpty()) {
            return;
        }
        writer.writeStartElement(element);
        writer.writeCharacters(trimValue);
        writer.writeEndElement();
    }

    /**
     * Write a simple XML attribute to an XMlStreamWriter. Will write nothing
     * if the given object value is null or results in an empty string when a
     * toString().trim() is invoked on it.
     * 
     * @param writer
     *            XML stream writer
     * @param attribute
     *            Name of attribute
     * @param value
     *            Object whose string value is to be written
     * 
     * @throws XMLStreamException
     *             If error writing stream
     * 
     */
    public static void writeSimpleAttribute(final XMLStreamWriter writer,
            final String attribute, final String value) throws XMLStreamException {

        if (value == null) {
            return;
        }

        final String trimValue = value.trim();

        if (trimValue.isEmpty()) {
            return;
        }
        writer.writeAttribute(attribute, value);
    }

    /**
     * Write a simple XML CDATA element to an XMLStreamWriter. Will write nothing if
     * the given object value is null or results in an empty string when a
     * toString().trim() is invoked on it.
     * 
     * @param writer
     *            XML stream writer
     * @param element
     *            Name of element
     * @param value
     *            Object whose string value is to be written
     * 
     * @throws XMLStreamException
     *             If error writing stream
     *             
     */
    public static void writeSimpleCDataElement(final XMLStreamWriter writer,
            final String element, final Object value) throws XMLStreamException {

        if (value == null) {
            return;
        }

        final String trimValue = value.toString().trim();

        if (trimValue.isEmpty()) {
            return;
        }
        writer.writeStartElement(element);
        writer.writeCData(trimValue);
        writer.writeEndElement();
    }
}
