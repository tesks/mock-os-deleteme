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
package jpl.gds.session.config;

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
 * This class is responsible for the XML Parsing of the session
 * identification object.
 * 
 */
public class SessionIdentificationParser extends DefaultHandler {

    private static Tracer               log            = TraceManager.getDefaultTracer();

	
	/**
	 * SessionId XML element name (tag) 
	 */
	public static final String SESSION_ID_TAG = "SessionId";
	
	/**
	 * Buffer used to capture parsed text
	 */
	private StringBuilder text = new StringBuilder(512);
	
	private boolean inSessionId;
	private final IContextIdentification sessionConfig;	
	
	/**
	 * Creates a SessionIdentification parser that will store the data is parses
	 * in the specified identification object.
	 * 
	 * @param target the IContextIdentification object to store data in
	 */
	public SessionIdentificationParser(final IContextIdentification target) {
		if (target == null) {
			throw new IllegalArgumentException("Target session identification may not be null");
		}
		this.sessionConfig = target;
	}
	
	/**
	 * Retrieves the target identification object.
	 * 
	 * @return SessionIdentification object
	 */
	public IContextIdentification getTargetSessionId() {
		return this.sessionConfig;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(final String uri, final String localName,
			final String elementName, final Attributes attr) throws SAXException {
		text = new StringBuilder(512);

		if (elementName.equals(SESSION_ID_TAG)) {
			inSessionId = true;
		} 
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(final char[] chars, final int start,
			final int length) {
		final String newText = new String(chars, start, length);
		if (!newText.equals("\n")) {
			text.append(newText);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(final String uri, final String localName,
			final String qname) throws SAXException {

		endElement(uri, localName, qname, text);
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
	public void endElement(final String uri, final String localName,
			final String qname, final StringBuilder currentText) throws SAXException {

		if (qname.equals(SESSION_ID_TAG)) {
			inSessionId = false;
		} else if (inSessionId) {
			parseElementFromSessionIdSection(qname, currentText.toString().trim());
		} 
	}
	
	 /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#error(
     *      org.xml.sax.SAXParseException)
     */
    @Override
    public void error(final SAXParseException e) throws SAXException {
        log.error("Line: " + e.getLineNumber() + " Col: "
                + e.getColumnNumber() + ": " + e.toString());
        throw new SAXException(e);
    }

    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#fatalError(
     *      org.xml.sax.SAXParseException)
     */
    @Override
    public void fatalError(final SAXParseException e) throws SAXException {
        log.error("Line: " + e.getLineNumber() + " Col: "
                + e.getColumnNumber() + ": " + e.toString());
        throw new SAXException(e);
    }

    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#warning(
     *      org.xml.sax.SAXParseException)
     */
    @Override
    public void warning(final SAXParseException e) throws SAXException {
        log.warn("Line: " + e.getLineNumber() + " Col: "
                + e.getColumnNumber() + ": " + e.toString());
    }
	
    private void parseElementFromSessionIdSection(final String elementName,
    		final String text) throws SAXException {
    	if (elementName.equals("Number")) {
    		try {
    			sessionConfig.setNumber(Long.valueOf(text));
    		} catch (final NumberFormatException nfe) {
    			throw new SAXException("Session number " + text
    					+ " is not a valid Long value", nfe);
    		}
    	} else if (elementName.equals("Name")) {
    		sessionConfig.setName(text);
    	} else if (elementName.equals("Type")) {
    		sessionConfig.setType(text);
    	} else if (elementName.equals("Description")) {
    		sessionConfig.setDescription(text);
    	} else if (elementName.equals("Host")) {
    		sessionConfig.setHost(text);
    	} else if (elementName.equals("HostId")) {
    		try {
    			sessionConfig.setHostId(Integer.valueOf(text));
    		} catch (final NumberFormatException nfe) {
    			throw new SAXException("HostId " + text
    					+ " is not a valid Integer value", nfe);
    		}
    	} else if (elementName.equals("User")) {
    		sessionConfig.setUser(text);
    	} else if (elementName.equals("StartTime")) {
    		try {
    			sessionConfig.setStartTime(new AccurateDateTime(text));
    		} catch (final ParseException pe) {
    			throw new SAXException("Unparseable time format: " + text, pe);
    		}
    	} else if (elementName.equals("EndTime")) {
    		try {
    			if (text.length() > 0) {
    				sessionConfig.setEndTime(new AccurateDateTime(text));
    			}
    		} catch (final ParseException pe) {
    			throw new SAXException("Unparseable time format: " + text, pe);
    		}
    	}
    }
}
