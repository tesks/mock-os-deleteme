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

import java.io.StringWriter;
import java.io.Writer;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

/**
 * Factory for STAX stream writers.
 * 
 */
public class StaxStreamWriterFactory {
	private final XMLOutputFactory xof;
	private static volatile StaxStreamWriterFactory instance;

	/**
	 * Constructor.
	 */
	protected StaxStreamWriterFactory() {
		// BRN 6/2/2009 This fixes a class loader problem with Leopard and Java
		// 1.5

		ClassLoader scl = ClassLoader.getSystemClassLoader();
		Thread t = Thread.currentThread();
		ClassLoader cl = t.getContextClassLoader();
		if (cl == null) {
			t.setContextClassLoader(scl);
		}

		xof = XMLOutputFactory.newInstance();
		xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, "false");
	}

	/**
	 * Get singleton instance.
	 * 
	 * @return Instance
	 */
	public static StaxStreamWriterFactory getInstance() {
		if (instance == null) {
			instance = new StaxStreamWriterFactory();
		}

		return (instance);
	}

	/**
	 * Retrieves an available writer from the pool or creates a new one if one
	 * is not available.
	 * 
	 * @param w
	 *            Writer
	 * 
	 * @return Pretty-print writer
	 * 
	 * @throws XMLStreamException Error writing stream
	 */
	public synchronized XMLStreamWriter get(final Writer w)
			throws XMLStreamException {
		XMLStreamWriter writer = xof.createXMLStreamWriter(w);
		return (writer);
	}

	/**
	 * Retrieves an available pretty-print writer from the pool or creates a new
	 * one if one is not available.
	 * 
	 * @param w
	 *            Writer
	 * 
	 * @return Pretty-print writer
	 * 
	 * @throws XMLStreamException Error writing stream
	 */
	public synchronized XMLStreamWriter getPretty(final Writer w)
			throws XMLStreamException {
		XMLStreamWriter writer = new IndentingXMLStreamWriter(
				xof.createXMLStreamWriter(w));
		return (writer);
	}

	/**
	 * Convenience method to generate the XML string from a StaxSerializable
	 * object
	 * 
	 * @param source
	 *            A class that implements the StaxSerializable interface
	 * 
	 * @return The XML string representation of the input object
	 * @throws XMLStreamException Error writing stream
	 */
	public static String toXml(final StaxSerializable source)
			throws XMLStreamException {
		StringWriter sw = new StringWriter(2048);
		XMLStreamWriter writer = StaxStreamWriterFactory.getInstance().get(sw);

		source.generateStaxXml(writer);

		writer.flush();
		writer.close();

		return (sw.toString());
	}

	/**
	 * Convenience method to generate the pretty-printed XML string from a
	 * StaxSerializable object
	 * 
	 * @param source
	 *            A class that implements the StaxSerializable interface
	 * 
	 * @return The XML string representation of the input object
	 * @throws XMLStreamException Error writing stream
	 */
	public static String toPrettyXml(final StaxSerializable source)
			throws XMLStreamException {
		StringWriter sw = new StringWriter(2048);
		XMLStreamWriter writer = StaxStreamWriterFactory.getInstance()
				.getPretty(sw);

		source.generateStaxXml(writer);

		writer.flush();
		writer.close();

		return (sw.toString());
	}
}
