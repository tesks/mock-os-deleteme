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
package jpl.gds.context.impl.message.parser;

import java.text.ParseException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.context.api.IContextIdentification;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.AccurateDateTime;

/**
 * This class is responsible for the XML Parsing of the ContextIdentification object.
 * 
 *
 */
public class ContextIdentificationParser extends DefaultHandler {
    private static Tracer                log            = TraceManager.getDefaultTracer();

    /** ContextId XML element name (tag) */
    public static final String           CONTEXT_ID_TAG = "ContextId";

    /** Buffer used to capture parsed text */
    private StringBuilder                text           = new StringBuilder(512);

    private boolean                      inContextId;
    private final IContextIdentification contextIdentification;

    /**
     * Creates a SessionIdentification parser that will store the data is parses
     * in the specified identification object.
     * 
     * @param target the IContextIdentification object to store data in
     */
    public ContextIdentificationParser(final IContextIdentification target) {
        if (target == null) {
            throw new IllegalArgumentException("Target session identification may not be null");
        }
        this.contextIdentification = target;
    }

    /**
     * Retrieves the target identification object.
     * 
     * @return IContextIdentification object
     */
    public IContextIdentification getTargetContextId() {
        return this.contextIdentification;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String,
     *      org.xml.sax.Attributes)
     */
    @Override
    public void startElement(final String uri, final String localName, final String elementName, final Attributes attr)
            throws SAXException {
        text = new StringBuilder(512);

        if (elementName.equals(CONTEXT_ID_TAG)) {
            inContextId = true;
        }
    }


    /**
     * Wrapper function for the standard SAX endElement event handler that
     * allows a buffer containing the text parsed from the most recent element
     * to be passed in.
     * 
     * @param uri
     *            the Namespace URI, or the empty string if the element has no
     *            Namespace URI or if Namespace processing is not being
     *            performed
     * @param localName
     *            the local name (without prefix), or the empty string if
     *            Namespace processing is not being performed
     * @param qname
     *            the qualified XML name (with prefix), or the empty string if
     *            qualified names are not available
     * @param currentText
     *            StringBuilder that contains the text content of the element
     * @throws SAXException
     *             if there is a parsing error
     */
    public void endElement(final String uri, final String localName, final String qname,
                           final StringBuilder currentText)
            throws SAXException {

        if (qname.equals(CONTEXT_ID_TAG)) {
            inContextId = false;
        }
        else if (inContextId) {
            parseElementFromContextIdSection(qname, currentText.toString().trim());
        }
    }

    private void parseElementFromContextIdSection(final String elementName, final String text) throws SAXException {
        switch(elementName) {
            case "Number": {
                try {
                    contextIdentification.setNumber(Long.valueOf(text));
                } catch (final NumberFormatException nfe) {
                    throw new SAXException("Session number " + text + " is not a valid Long value", nfe);
                }
                break;
            }
            case "SpacecraftId": {
                contextIdentification.setSpacecraftId(Integer.valueOf(text));
                break;
            }
            case "ContextId": {
                break;
            }
            case "Name": {
                contextIdentification.setName(text);
                break;
            }
            case "Type": {
                contextIdentification.setType(text);
                break;
            }
            case "Description": {
                contextIdentification.setDescription(text);
                break;
            }
            case "Host": {
                contextIdentification.setHost(text);
                break;
            }
            case "HostId": {
                try {
                    contextIdentification.setHostId(Integer.valueOf(text));
                } catch (final NumberFormatException nfe) {
                    throw new SAXException("HostId " + text + " is not a valid Integer value", nfe);
                }
                break;
            }
            case "User": {
                contextIdentification.setUser(text);
                break;
            }
            case "StartTime": {
                try {
                    contextIdentification.setStartTime(new AccurateDateTime(text));
                } catch (final ParseException pe) {
                    throw new SAXException("Unparseable time format: " + text, pe);
                }
                break;
            }
            case "EndTime": {
                try {
                    if (text.length() > 0) {
                        contextIdentification.setEndTime(new AccurateDateTime(text));
                    }
                } catch (final ParseException pe) {
                    throw new SAXException("Unparseable time format: " + text, pe);
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public void characters(final char[] chars, final int start, final int length) {
        final String newText = new String(chars, start, length);
        if (!newText.equals("\n")) {
            text.append(newText);
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qname) throws SAXException {

        endElement(uri, localName, qname, text);
    }

    @Override
    public void error(final SAXParseException e) throws SAXException {
        log.error("Line: " + e.getLineNumber() + " Col: " + e.getColumnNumber() + ": " + e.toString());
        throw new SAXException(e);
    }

    @Override
    public void fatalError(final SAXParseException e) throws SAXException {
        error(e);
    }

    @Override
    public void warning(final SAXParseException e) throws SAXException {
        log.warn("Line: " + e.getLineNumber() + " Col: " + e.getColumnNumber() + ": " + e.toString());
    }

}
