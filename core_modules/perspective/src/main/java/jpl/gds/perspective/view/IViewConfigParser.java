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
package jpl.gds.perspective.view;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * An interface to be implemented by custom view configuration parsers.
 * 
 *
 */
public interface IViewConfigParser {

    /**
     * Initializes the parser.
     */
    public void init();

    /**
     * Invoked upon XML element start.
     * 
     * @param uri
     *            URI of the element
     * @param localName
     *            non-namespace-qualified element name
     * @param qname
     *            namespace-qualified element name
     * @param attr
     *            XML attributes (from SAX)
     * @return true if the element was handled by this method, false if not
     *         recognized
     * @throws SAXException
     *             if there is any issue with the parsing.
     */
    public boolean startElement(final String uri, final String localName,
            final String qname, final Attributes attr) throws SAXException;

    /**
     * Invoked upon XML element end.
     * 
     * @param uri
     *            URI of the element
     * @param localName
     *            non-namespace-qualified element name
     * @param qname
     *            namespace-qualified element name
     * @param text
     *            the element text
     * @return true if the element was handled by this method, false if not
     *         recognized
     * @throws SAXException
     *             if there is any issue with the parsing.
     */
    public boolean endElement(final String uri, final String localName,
            final String qname, StringBuilder text) throws SAXException;

}
