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

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Error handler for SAX XML parsing.
 *
 */
public class SaxErrorHandler implements ErrorHandler {
    /** {@inheritDoc}
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    @Override
    public void error(SAXParseException e) throws SAXException {
    	int line = e.getLineNumber();
    	int col = e.getColumnNumber();
        System.err.println("SAX error at line: " + line + " Col: " + col + ": " + e.toString());
        throw new SAXException(e.getMessage() + " at line " + line + " column " +
        		col);
    }
    
    
    /** {@inheritDoc}
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    @Override
    public void warning(SAXParseException e) throws SAXException {
       	int line = e.getLineNumber();
    	int col = e.getColumnNumber();
        System.err.println("SAX warning at line: " + line + " Col: " + col + ": " + e.toString());
        throw new SAXException(e.getMessage() + " at line " + line + " column " +
        		col);
    }
    
    /** {@inheritDoc}
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    @Override
    public void fatalError(SAXParseException e) throws SAXException {
       	int line = e.getLineNumber();
    	int col = e.getColumnNumber();
        System.err.println("SAX fatal error at line: " + line + " Col: " + col + ": " + e.toString());
        throw new SAXException(e.getMessage() + " at line " + line + " column " +
        		col);
    }     
}
