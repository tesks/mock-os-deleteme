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
package jpl.gds.common.config.connection;

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * A general interface to be implemented by all connection configurations.
 * 
 * @since R8
 */
public interface IConnection {
    /**
     * Copies members from the given connection object to this connection
     * object. The implementation must ensure that the connection types
     * are compatible for copying.
     * 
     * @param toCopy the object to copy data from
     */
    public void copyValuesFrom(IConnection toCopy);
    
    /**
     * Sets template variables into the given map for this connection.
     * 
     * @param map map to set template values into
     * @param varPrefix prefix for variable names
     */
    public void setTemplateContext(final Map<String, Object> map, String varPrefix); 
    
    /**
     * Generates the XML for this connection.
     * 
     * @param writer XML stream to write to
     * @param elementName top-level element name for this connection
     * @throws XMLStreamException if there is a problem generating the XML
     */
    public void generateStaxXml(final XMLStreamWriter writer, String elementName) throws XMLStreamException;
 }
