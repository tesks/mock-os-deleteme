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
package jpl.gds.station.impl.dsn.chdo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.xml.parse.SAXParserPool;
import jpl.gds.station.api.dsn.chdo.ChdoConfigurationException;
import jpl.gds.station.api.dsn.chdo.ChdoFieldFormatEnum;
import jpl.gds.station.api.dsn.chdo.IChdoConfiguration;
import jpl.gds.station.api.dsn.chdo.IChdoDefinition;
import jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition;
import jpl.gds.station.api.dsn.chdo.IChdoProperty;
import jpl.gds.station.api.dsn.chdo.UnknownChdoException;

/**
 * This class is both the container and the parser for the CHDO dictionary file (chdo.xml).
 * This class utilizes a singleton design pattern/
 * 
 *
 */
public class ChdoConfiguration extends AbstractChdoConfiguration
{
    
    private static String VERSION = "1.1";
    
    private String filepath;
    private final boolean sseFlag;

    /**
     * Constructor. Parses the config file.
     * 
     * @param sseFlag
     *            The SSE context flag
     * @throws ChdoConfigurationException
     *             if the CHDO File cannot be parsed.
     */
    public ChdoConfiguration(final SseContextFlag sseFlag) throws ChdoConfigurationException
	{
        this.sseFlag = sseFlag.isApplicationSse();
        final String file = GdsSystemProperties.getMostLocalPath(IChdoConfiguration.getDefaultConfigFilename(),
                                                                 this.sseFlag);
	    parse(file);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getVersion() {
		return (VERSION);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String returnXmlString() throws ChdoConfigurationException {
		BufferedReader buf 	   = null;
		String line		   	   = null;
		final File chdoXmlFile   	   = new File(this.filepath);
		final StringBuffer xmlString = new StringBuffer();

		try { 
			buf = new BufferedReader(new FileReader(chdoXmlFile));
		} catch(final IOException e) { 
			throw new ChdoConfigurationException("Error opening BufferedReader from " + chdoXmlFile.getAbsolutePath(), e.getCause());
		} finally { 
			try {
				while ((line = buf.readLine()) != null) { 
					xmlString.append(line + System.lineSeparator());
				}
			} catch (final IOException e) {
				throw new ChdoConfigurationException("Error using BufferedReader on " + this.filepath, e.getCause());
			}
		}
		if (buf != null) {
			try {
				buf.close();
			} catch (final IOException e) { //should never reach here...
				throw new ChdoConfigurationException("Error closing BufferedReader on " + this.filepath, e.getCause());
			}
		}
		return xmlString.toString();
		
	}
	

	/**
	 * Parses the given CHDO dictionary file. Will clear all existing
	 * CHDO definitions.
	 * 
	 * @param uri path to dictionary file to parse
	 * @throws ChdoConfigurationException if there is a parsing error or the
	 * file is not found
	 */
	@Override
    public void parse(final String uri) throws ChdoConfigurationException
	{
		this.typeToDefinitionMap.clear();

		if(uri == null)
		{
            if (!sseFlag)
			{
				log.error("CHDO dictionary path is undefined. Not reading CHDO dictionary");
			}
			throw new ChdoConfigurationException("CHDO dictionary path is undefined. Not reading CHDO dictionary");
		}

		final File path = new File(uri);

        log.debug("Parsing CHDO definitions from " +
                        FileUtility.createFilePathLogMessage(path));

		SAXParser sp = null;
		try
		{
			sp = SAXParserPool.getInstance().getNonPooledParser();

			final ChdoSaxHandler handler = new ChdoSaxHandler();
			sp.parse(uri, handler);
		}
		catch(final FileNotFoundException e)
		{
			log.error("CHDO definition file " + path.getAbsolutePath() + " not found");
			throw new ChdoConfigurationException("CHDO definition file " + path.getAbsolutePath() + " not found", e);
		}
		catch(final IOException e)
		{
			log.error("IO Error reading CHDO definition file " + path.getAbsolutePath());
			throw new ChdoConfigurationException("IO Error reading CHDO definition file " + path.getAbsolutePath(), e);
		}
		catch(final ParserConfigurationException e)
		{
			log.error("Error configuring SAX parser for CHDO definition file " + path.getAbsolutePath());
			throw new ChdoConfigurationException("Error configuring SAX parser for CHDO definition file " + path.getAbsolutePath(), e);
		}
		catch(final SAXException e)
		{
			log.error("Error parsing CHDO definition file " + path.getAbsolutePath() + ": " + e.getMessage());
			throw new ChdoConfigurationException(e.getMessage(), e);
		}
		
		  this.filepath = uri;
	}

	/**
	 * Parse handler for the chdo.xml file.
	 */
	private class ChdoSaxHandler extends DefaultHandler
	{
		private StringBuffer text;
		private IChdoDefinition currentDefinition;
		private IChdoFieldDefinition currentField;
		private IChdoProperty currentProperty;
		private ChdoCondition currentCondition;
		private static final String CA_ID_REGEXP = "[A-Z0-9]{4}";


		/**
		 * Basic constructor
		 */
		public ChdoSaxHandler()
		{
			this.text = new StringBuffer();
			this.currentDefinition = null;
			this.currentField = null;
			this.currentProperty = null;
			this.currentCondition = null;
		}


		/**
		 * {@inheritDoc}
		 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(final String uri, final String localName, final String qname, final Attributes attr) throws SAXException
		{
			this.text = new StringBuffer();

			if(qname.equals("Chdo") == true)
			{
				this.currentDefinition = new ChdoDefinition();
				this.currentDefinition.setType(getIntAttributeValue(attr, "type"));
				this.currentDefinition.setClassification(attr.getValue("classification"));
				this.currentDefinition.setName(attr.getValue("name"));

                if ((attr.getValue("byteSize") != null) &&
                    ! attr.getValue("byteSize").equals("undefined"))
                {
                    this.currentDefinition.setByteSize(getIntAttributeValue(attr, "byteSize"));
                }
			}
			else if(qname.equals("Field") == true)
			{
                currentField = new ChdoFieldDefinition();

				currentField.setFieldId(attr.getValue("fieldId"));

				final String bitLengthStr = attr.getValue("bitLength");
				if(bitLengthStr.equals("undefined") == false)
				{
					currentField.setBitLength(getIntAttributeValue(attr, "bitLength"));
				}

				currentField.setByteOffset(getIntAttributeValue(attr, "byteOffset"));
				currentField.setBitOffset(getIntAttributeValue(attr, "bitOffset"));
				currentField.setFieldFormat(ChdoFieldFormatEnum.valueOf(attr.getValue("format")));

				if(this.currentDefinition != null)
				{
					this.currentDefinition.addFieldMapping(currentField.getFieldId(), currentField);
				}
			}
			else if(qname.equals("Property"))
			{
				this.currentProperty = new ChdoProperty(attr.getValue("name"));
			}
			else if(qname.equals("Condition"))
			{
				final Integer chdo_type = Integer.valueOf(getIntAttributeValue(attr,"chdo_type"));
				final IChdoDefinition chdoDef = ChdoConfiguration.this.typeToDefinitionMap.get(chdo_type);
				if(chdoDef == null)
				{
					throw new SAXException(new UnknownChdoException("Unknown CHDO type " + chdo_type + " found in a property condition in CHDO XML",chdo_type));
				}
				this.currentCondition = new ChdoCondition(chdoDef);
			}
			else if(qname.equals("Equals"))
			{
				this.currentCondition.addEqualityCondition(attr.getValue("name"),attr.getValue("value"),true);
			}
			else if(qname.equals("NotEquals"))
			{
				this.currentCondition.addEqualityCondition(attr.getValue("name"),attr.getValue("value"),false);
			}
			else if (qname.equals("Range"))
			{
				currentField.setMinValue(Long.valueOf(attr.getValue("min")));
				currentField.setMaxValue(Long.valueOf(attr.getValue("max")));

                if ((attr.getValue("default") != null) &&
                    ! attr.getValue("default").equals("undefined"))
                {
                    currentField.setDefaultValue(getLongAttributeValue(attr, "default"));
                }
			}
			else if (qname.equals("Value"))
			{
                if ((attr.getValue("value") != null) &&
                    ! attr.getValue("value").equals("undefined"))
                {
                    currentField.setFixedValue(getLongAttributeValue(attr, "value"));
                }
			}
		}

		/**
		 * {@inheritDoc}
		 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(final String uri, final String localName, final String qname) throws SAXException
		{
			if(qname.equals("Chdo") == true)
			{
				ChdoConfiguration.this.typeToDefinitionMap.put(Integer.valueOf(this.currentDefinition.getType()),this.currentDefinition);
			}
			else if(qname.equals("ControlAuthorityId"))
			{
				final String caId = this.text.toString().trim().toUpperCase();
				if(caId.matches(CA_ID_REGEXP) == false)
				{
					log.warn("Control Authority ID value \"" + caId + "\" in CHDO XML is invalid.  It will be ignored.");
				}
				ChdoConfiguration.this.controlAuthorityIds.add(caId);
			}
			else if(qname.equals("Property"))
			{
				ChdoConfiguration.this.nameToPropertyMap.put(this.currentProperty.getName(),this.currentProperty);
			}
			else if(qname.equals("Condition"))
			{
				this.currentProperty.addCondition(this.currentCondition);
			}
			else if(qname.equals("Field"))
			{
				currentField = null;
			}
		}

		/**
		 * {@inheritDoc}
		 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
		 */
		@Override
		public void characters(final char[] chars, final int start, final int length) throws SAXException
		{
			final String newText = new String(chars, start, length);
			if(newText.equals("\n") == false)
			{
				this.text.append(newText);
			}
		}

		private int getIntAttributeValue(final Attributes attr, final String name) throws SAXException
		{
			int value = 0;
			try
			{
				final String val = attr.getValue(name);
				if(val.equals(""))
				{
					System.err.println("Expected '" + name + "' to have a value. Will set it to 0.");

					return 0;
				}
				value = GDR.parse_int(val);
			}
			catch(final NumberFormatException e)
			{
				throw new SAXException("Expected '" + name + "' to be an integer, got " + attr.getValue(name));
			}

			return (value);
		}

		private long getLongAttributeValue(final Attributes attr, final String name) throws SAXException
		{
			long value = 0L;
			try
			{
				final String val = attr.getValue(name);
				if(val.equals(""))
				{
					System.err.println("Expected '" + name + "' to have a value. Will set it to 0.");

					return 0L;
				}
				value = GDR.parse_long(val);
			}
			catch(final NumberFormatException e)
			{
				throw new SAXException("Expected '" + name + "' to be a long, got " + attr.getValue(name));
			}

			return (value);
		}

		/**
		 * {@inheritDoc}
		 * @see org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
		 */
		@Override
		public void error(final SAXParseException e) throws SAXException
		{
			throw new SAXException("Parse error in CHDO definition file line " + e.getLineNumber() + " col " + e.getColumnNumber() + ": " + e.getMessage());
		}

		/**
		 * {@inheritDoc}
		 * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.SAXParseException)
		 */
		@Override
		public void fatalError(final SAXParseException e) throws SAXException
		{
			throw new SAXException("Fatal parse error in CHDO definition file line " + e.getLineNumber() + " col " + e.getColumnNumber() + ": " + e.getMessage());
		}

		/**
		 * {@inheritDoc}
		 * @see org.xml.sax.helpers.DefaultHandler#warning(org.xml.sax.SAXParseException)
		 */
		@Override
		public void warning(final SAXParseException e)
		{
			log.warn("Parse warning in CHDO definition file line " + e.getLineNumber() + " col " + e.getColumnNumber() + ": " + e.getMessage());
		}
	}

}
