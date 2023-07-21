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
package jpl.gds.shared.xml.parse;

import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

/**
 * Maintains a pool of SAX XML parsers.
 * 
 */
public class SAXParserPool {
	private final SAXParserFactory spf;
	private static volatile SAXParserPool instance;
	private final Vector<SAXParser> freeParsers = new Vector<SAXParser>();
	private final Vector<SAXParser> busyParsers = new Vector<SAXParser>();

	/**
	 * Constructor.
	 */
	protected SAXParserPool() {
		// 6/18/2008 This fixes a class loader problem with Leopard and Java
		// 1.5

		ClassLoader scl = ClassLoader.getSystemClassLoader();
		Thread t = Thread.currentThread();
		ClassLoader cl = t.getContextClassLoader();
		if (cl == null) {
			t.setContextClassLoader(scl);
		}
		spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
	}

	/**
	 * Get singleton instance.
	 * 
	 * @return Instance
	 */
	public static SAXParserPool getInstance() {
		if (instance == null) {
			instance = new SAXParserPool();
		}
		return instance;
	}

	/**
	 * Gets a properly configured SAXParser that is NOT pooled.
	 * 
	 * @return SAXParser
	 * 
	 * @throws ParserConfigurationException
	 *             Error configuring parser
	 * @throws SAXException
	 *             Error in SAX processing
	 */
	public SAXParser getNonPooledParser() throws ParserConfigurationException,
			SAXException {
		return spf.newSAXParser();
	}

	/**
	 * Retrieves an available parser from the pool, or creates a new one if one
	 * is not available.
	 * 
	 * @return A parser
	 * 
	 * @throws ParserConfigurationException
	 *             Error configuring parser
	 * @throws SAXException
	 *             Error in SAX processing
	 */
	public synchronized SAXParser get() throws ParserConfigurationException,
			SAXException {
		SAXParser sp = getFreeParser();
		if (sp == null) {
			sp = spf.newSAXParser();
			busyParsers.add(sp);
		}
		return sp;
	}

	private SAXParser getFreeParser() {
		if (freeParsers.size() == 0) {
			return null;
		} else {
			SAXParser sp = freeParsers.remove(0);
			busyParsers.add(sp);
			return sp;
		}
	}

	/**
	 * Returns a parser to the pool.
	 * 
	 * @param parser
	 *            A parser
	 */
	public synchronized void release(SAXParser parser) {
		busyParsers.remove(parser);
		try {
			parser.getXMLReader().setDTDHandler(null);
			parser.getXMLReader().setContentHandler(null);
			parser.getXMLReader().setEntityResolver(null);
			parser.getXMLReader().setErrorHandler(null);
		} catch (SAXException e) {
			e.printStackTrace();
		}
		freeParsers.add(parser);
	}
}
