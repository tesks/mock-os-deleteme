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
package jpl.gds.shared.xml.stax;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Interface for serializing STAX XML.
 * 
 */
public interface StaxSerializable {
	/**
	 * Writes out the given XML of a message to the Writer inside the
	 * XMLStreamWriter object that is passed in.
	 * 
	 * @param writer
	 *            The XMLStreamWriter object that will be used to generate the
	 *            XML output using the STAX Stream API
	 * 
	 * @throws XMLStreamException
	 *             Writing error
	 */
	public void generateStaxXml(final XMLStreamWriter writer)
			throws XMLStreamException;

	/**
	 * Generate an XML string representation of the object. Should make use of
	 * the generateStaxXml method defined in this interface.
	 * 
	 * @return The XML string representation of this object.
	 */
	public String toXml();
}
