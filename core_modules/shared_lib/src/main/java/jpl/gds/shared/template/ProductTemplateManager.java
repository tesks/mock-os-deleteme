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
 * ProductTemplateManager is a velocity template manager meant specifically
 * for formatting product information.
 *
 */
public class ProductTemplateManager extends TemplateManager
{
	/**
	 * The sub-directory under the base directory where product
	 * templates can be found
	 */
	public static final String DIRECTORY = "product";
    
    /**
     * Creates an instance of ProductTemplateManager.
     *
     * @param mission Mission name
     *
     * @throws TemplateException Could not load template
     */
    public ProductTemplateManager(final String mission) throws TemplateException {
        super(mission);
    }
    
    /**
     * Returns the Velocity template object for the given product template type.
     * 
     * @param templateName The name/type of the template to load 
     * @param version the version of the template to load
     *
     * @return the Velocity Template object to format the database record in the
     * given style, or null if no such template is found
     *
     * @throws TemplateException Could not load template
     */
    public Template getTemplateForType(String templateName, String version) throws TemplateException {

        final String name = DIRECTORY + File.separator + templateName +  "_" + version + EXTENSION;
        return getTemplate(name);
    }
}
