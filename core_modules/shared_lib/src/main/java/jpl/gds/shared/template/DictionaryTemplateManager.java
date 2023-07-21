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
package jpl.gds.shared.template;

import java.io.File;

import org.apache.velocity.Template;

/**
 * 
 * DictionaryTemplateManager is a velocity template manager meant specifically
 * for formatting database query results in various ways.
 *
 */
public class DictionaryTemplateManager extends TemplateManager
{
	/**
	 * The sub-directory under the base directory where dictionary
	 * templates can be found
	 */
	public static final String DIRECTORY = "dictionary";
    
    /**
     * Creates an instance of DictionaryTemplateManager.
     *
     * @param mission Mission name
     *
     * @throws TemplateException Could not load template
     */
    public DictionaryTemplateManager(final String mission) throws TemplateException {
        super(mission);
    }
    
    /**
     * Returns the Velocity template object for the given message type and
     * formatting style.
     * 
     * @param dictionaryName The type of dictionary element being formatted
     * @param style the string indicating the format style to use
     * @return the Velocity Template object to format the message in the
     * given style, or null if no such template is found
     * @throws TemplateException Could not load template
     */
    public Template getTemplateForStyle(String dictionaryName, String style) throws TemplateException {
    	String name = DIRECTORY + File.separator + dictionaryName + File.separator + style.toLowerCase();
    	if(name.endsWith(EXTENSION) == false)
    	{
    		name += EXTENSION;
    	}
    	
        return getTemplate(name);
    }
}
