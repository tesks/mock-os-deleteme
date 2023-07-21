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
import java.net.MalformedURLException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.thaiopensource.validate.ValidationDriver;
import com.thaiopensource.validate.rng.CompactSchemaReader;

/**
 * Class to validate XML against a RelaxNG schema.
 * 
 */
public class RelaxNgCompactValidator implements XmlValidator
{
    /**
     * Constructor.
     */
	public RelaxNgCompactValidator()
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
		
		File schemaFile = new File(schemaPath);
		if(schemaFile.exists() == false)
		{
			throw new XmlValidationException(new FileNotFoundException("Could not find the RNC schema file " + schemaPath + ".  Make sure this file exists."));
		}
		
		File xmlFile = new File(xmlPath);
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
		
		if(xmlFile== null)
		{
			throw new IllegalArgumentException("Null input XML file");
		}
		
		boolean result = false;
		try
		{
			result = validateXmlFromRnc(schemaFile,xmlFile);
		}
		catch(MalformedURLException e)
		{
			throw new XmlValidationException("Validation failed due to an exception.",e);
		}
		catch(SAXException e)
		{
			throw new XmlValidationException("Validation failed due to an exception.",e);
		}
		catch(IOException e)
		{
			throw new XmlValidationException("Validation failed due to an exception.",e);
		}
		
		return(result);
	}
	
	private boolean validateXmlFromRnc(final File schemaFile, final File xmlFile) throws MalformedURLException, SAXException, IOException
	{	
		ValidationDriver vd = new ValidationDriver(CompactSchemaReader.getInstance());
		vd.loadSchema(ValidationDriver.fileInputSource(schemaFile));
		boolean result = vd.validate(ValidationDriver.fileInputSource(xmlFile));
		
		return(result);
	}

    /**
     * {@inheritDoc}
     */
    @Override
	public boolean validateXmlString(String schemaPath, String xmlText) throws XmlValidationException
	{
		if(schemaPath == null)
		{
			throw new IllegalArgumentException("Null input schema path");
		}

		if(xmlText == null)
		{
			throw new IllegalArgumentException("Null input XML path");
		}
		
		File schemaFile = new File(schemaPath);
		if(schemaFile.exists() == false)
		{
			throw new XmlValidationException(new FileNotFoundException("Could not find the RNC schema file " + schemaPath + ".  Make sure this file exists."));
		}
		
		return(validateXmlString(schemaFile,xmlText));
	}


    /**
     * {@inheritDoc}
     */
    @Override
	public boolean validateXmlString(File schemaFile, String xmlText) throws XmlValidationException
	{
		if(schemaFile == null)
		{
			throw new IllegalArgumentException("Null input schema file");
		}
		
		if(xmlText== null)
		{
			throw new IllegalArgumentException("Null input XML string");
		}

		boolean result = false;
		try
		{
			result = validateXmlStringFromRnc(schemaFile,xmlText);
		}
		catch(MalformedURLException e)
		{
			throw new XmlValidationException("Validation failed due to an exception.",e);
		}
		catch(SAXException e)
		{
			throw new XmlValidationException("Validation failed due to an exception.",e);
		}
		catch(IOException e)
		{
			throw new XmlValidationException("Validation failed due to an exception.",e);
		}

		return(result);
	}

	private boolean validateXmlStringFromRnc(File schemaFile, String xmlText) throws MalformedURLException, SAXException, IOException
	{
		ValidationDriver vd = new ValidationDriver(CompactSchemaReader.getInstance());
		vd.loadSchema(ValidationDriver.fileInputSource(schemaFile));
		StringReader sr = new StringReader(xmlText);
        InputSource is = new InputSource(sr);
		boolean result = vd.validate(is);
		
		return(result);
	}
}
