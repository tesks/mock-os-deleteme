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

package jpl.gds.shared.xml.validation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.shared.config.GdsSystemProperties;

/**
 * Class to validate XML against a schema.
 * 
 */
public class XmlSchemaValidator implements XmlValidator
{
	private static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	private static final String XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
	private static final String SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    /**
     * Constructor.
     */	
	public XmlSchemaValidator()
	{
	}
	

    /**
     * {@inheritDoc}
     */
    @Override
	public boolean validateXml(final String schemaPath, final String xmlPath) throws XmlValidationException
	{
		if(schemaPath == null)
		{
			throw new IllegalArgumentException("Null input schema path");
		}

		if(xmlPath == null)
		{
			throw new IllegalArgumentException("Null input XML path");
		}
		
		final File schemaFile = new File(schemaPath);
		if(schemaFile.exists() == false)
		{
			throw new XmlValidationException(new FileNotFoundException("Could not find the XML schema file " + schemaPath + ".  Make sure this file exists."));
		}
		
		final File xmlFile = new File(xmlPath);
		if(xmlFile.exists() == false)
		{
			throw new XmlValidationException(new FileNotFoundException("Could not find the XML file " + xmlPath + ".  Make sure this file exists."));
		}
		
		return(validateXml(schemaFile,xmlFile));
	}
	
    /**
     * {@inheritDoc}
     */
    @Override
	public boolean validateXml(final File schemaFile,final File xmlFile) throws XmlValidationException
	{
		if(schemaFile == null)
		{
			throw new IllegalArgumentException("Null input schema file");
		}

		if(xmlFile == null)
		{
			throw new IllegalArgumentException("Null input XML file");
		}
		
		boolean result = false;
		try
		{
			result = validateXmlFromXsd(schemaFile,xmlFile);
		}
		catch(final SAXException e)
		{
			throw new XmlValidationException("Validation failed due to an exception.",e);
		}
		catch(final IOException e)
		{
			throw new XmlValidationException("Validation failed due to an exception.",e);
		}
		catch(final ParserConfigurationException e)
		{
			throw new XmlValidationException("Validation failed due to an exception.",e);
		}
		
		return(result);
	}
	
	private boolean validateXmlFromXsd(final File schemaFile, final File xmlFile) throws IOException, ParserConfigurationException, SAXException
	{
		/*
		 * Must override our command line property value which calls
		 * out the piccolo SAX parser.  That parser will not validate. Must use the xerces parser.
		 */
        final String parserName = GdsSystemProperties.getSystemProperty("javax.xml.parsers.SAXParserFactory");
        GdsSystemProperties.setSystemProperty("javax.xml.parsers.SAXParserFactory",
                                              "org.apache.xerces.jaxp.SAXParserFactoryImpl");
		final SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(true);

		final SAXParser parser = factory.newSAXParser();
		parser.setProperty(SCHEMA_LANGUAGE,XML_SCHEMA);
		parser.setProperty(SCHEMA_SOURCE,schemaFile.getAbsolutePath());
		
		boolean result = true;
		try
		{
			parser.parse(xmlFile,new ParseErrorHandler());
		}
		catch(final SAXException e)
		{
			e.printStackTrace();
			result = false;
		}
		/*
		 * Must restore the default SAX parser.
		 */
        GdsSystemProperties.setSystemProperty("javax.xml.parsers.SAXParserFactory", parserName);
		
		return(result);
	}
	

    /** Err handling class */
	private class ParseErrorHandler extends DefaultHandler
	{
		public ParseErrorHandler()
		{
			super();
		}
		
		/** {@inheritDoc}
		 * @see org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
		 */
		@Override
		public void error(final SAXParseException arg0) throws SAXException
		{
			throw new SAXException("Parsing/Validation error encountered: " + getErrorString(arg0));
		}

		/** {@inheritDoc}
		 * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.SAXParseException)
		 */
		@Override
		public void fatalError(final SAXParseException arg0) throws SAXException
		{
			throw new SAXException("Parsing/Validation error encountered: " + getErrorString(arg0));
		}

		private String getErrorString(final SAXParseException e)
		{
			final MessageFormat message = new MessageFormat( "({0}: {1}, {2}): {3}" );
			
			final String msg = message.format( 
					new Object[]
					{
							e.getSystemId(),
							Integer.valueOf(e.getLineNumber()),
							Integer.valueOf(e.getColumnNumber()),
						    e.getMessage()
			        }
			);
			
			return(msg);
		}
	}


    /**
     * {@inheritDoc}
     */
    @Override
	public boolean validateXmlString(final String schemaPath, final String xmlText) throws XmlValidationException
	{
		if(schemaPath == null)
		{
			throw new IllegalArgumentException("Null input schema path");
		}

		if(xmlText == null)
		{
			throw new IllegalArgumentException("Null input XML string");
		}

		final File schemaFile = new File(schemaPath);
		if(schemaFile.exists() == false)
		{
			throw new XmlValidationException(new FileNotFoundException("Could not find the XML schema file " + schemaPath + ".  Make sure this file exists."));
		}
		
		return(validateXmlString(schemaFile,xmlText));
	}


    /**
     * {@inheritDoc}
     */
    @Override
	public boolean validateXmlString(final File schemaFile, final String xmlText) throws XmlValidationException
	{
		if(schemaFile == null)
		{
			throw new IllegalArgumentException("Null input schema file");
		}

		if(xmlText == null)
		{
			throw new IllegalArgumentException("Null input XML string");
		}
		
		boolean result = false;
		try
		{
			result = validateXmlStringFromXsd(schemaFile,xmlText);
		}
		catch(final SAXException e)
		{
			throw new XmlValidationException("Validation failed due to an exception.",e);
		}
		catch(final IOException e)
		{
			throw new XmlValidationException("Validation failed due to an exception.",e);
		}
		catch(final ParserConfigurationException e)
		{
			throw new XmlValidationException("Validation failed due to an exception.",e);
		}
		
		return(result);
	}

	private boolean validateXmlStringFromXsd(final File schemaFile, final String xmlText) throws IOException, ParserConfigurationException, SAXException
	{
		final SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(true);

		final SAXParser parser = factory.newSAXParser();
		parser.setProperty(SCHEMA_LANGUAGE,XML_SCHEMA);
		parser.setProperty(SCHEMA_SOURCE,schemaFile.getAbsolutePath());
		
		boolean result = true;

		final StringReader sr = new StringReader(xmlText);
        final InputSource is = new InputSource(sr);

        try
		{
			parser.parse(is,new ParseErrorHandler());
		}
		catch(final SAXException e)
		{
			e.printStackTrace();
			result = false;
		}
		
		return(result);
	}
}
