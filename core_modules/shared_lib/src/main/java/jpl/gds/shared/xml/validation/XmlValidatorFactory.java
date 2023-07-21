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

/**
 * Class to create XML validators of various types.
 * 
 */
public class XmlValidatorFactory
{
    /** Types of validators */
	public enum SchemaType
	{
        /** XSD validator */
		XSD,

        /** RNC validator */
		RNC
	}
	

    /**
     * Create validator.
     *
     * @param type Type of validator
     *
     * @return Validator
     */	
	public static XmlValidator createValidator(final SchemaType type)
	{
		if(type == null)
		{
			throw new IllegalArgumentException("Null input schema type");
		}
		
		switch(type)
		{
			case XSD:
				
				return(new XmlSchemaValidator());
			
			case RNC:
				
				return(new RelaxNgCompactValidator());
				
			default:
				
				return(null);
		}
	}


    /**
     * Create validator.
     *
     * @param strType Type of validator
     *
     * @return Validator
     */	
	public static XmlValidator createValidator(final String strType)
	{
		if(strType == null)
		{
			throw new IllegalArgumentException("Null input schema type");
		}
		
		SchemaType type = null;
		try
		{
			type = SchemaType.valueOf(strType.trim().toUpperCase());
		}
		catch(IllegalArgumentException iae)
		{
			return(null);
		}
		
		return(createValidator(type));
	}
}
