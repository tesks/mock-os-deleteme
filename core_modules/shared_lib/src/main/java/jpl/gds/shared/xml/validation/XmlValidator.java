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


/**
 * Interface for all XML validators.
 * 
 */
public interface XmlValidator
{
    /**
     * Validate XML.
     *
     * @param schemaPath Schema
     * @param xmlPath    XML to validate
     *
     * @return Success or failure
     *
     * @throws XmlValidationException XML validation error
     */
	boolean validateXml(final String schemaPath,
                        final String xmlPath)
        throws XmlValidationException;


    /**
     * Validate XML.
     *
     * @param schemaFile Schema
     * @param xmlFile    XML to validate
     *
     * @return Success or failure
     *
     * @throws XmlValidationException XML validation error
     */
	boolean validateXml(final File schemaFile,
                        final File xmlFile) throws XmlValidationException;
	

    /**
     * Validate XML.
     *
     * @param schemaPath Schema
     * @param xmlText    XML to validate
     *
     * @return Success or failure
     *
     * @throws XmlValidationException XML validation error
     */
	boolean validateXmlString(final String schemaPath,
                              final String xmlText)
        throws XmlValidationException;
	

    /**
     * Validate XML.
     *
     * @param schemaFile Schema
     * @param xmlText    XML to validate
     *
     * @return Success or failure
     *
     * @throws XmlValidationException XML validation error
     */
	boolean validateXmlString(final File   schemaFile,
                              final String xmlText)
        throws XmlValidationException;
}
