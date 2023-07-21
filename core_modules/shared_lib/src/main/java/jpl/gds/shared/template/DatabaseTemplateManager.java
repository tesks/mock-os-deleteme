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
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.Template;


/**
 * 
 * DatabaseTemplateManager is a velocity template manager meant specifically
 * for formatting database query results in various ways.
 *
 */
public class DatabaseTemplateManager extends TemplateManager
{

	/**
	 * The subdirectory under the base directory where database
	 * templates can be found
	 */
	public static final String DIRECTORY = "db";
	
	/**
     * Creates an instance of MessageTemplateManager.
     *
     * @param mission Mission name
     *
     * @throws TemplateException Could not load template
     */
    public DatabaseTemplateManager(final String mission) throws TemplateException {
        super(mission);
    }
    
    /**
     * Returns the Velocity template object for the given database table and
     * formatting style.
     * 
     * @param dbTableName The name of the database table where the query is being done
     * @param style the string indicating the format style to use, as defined in the
     * configuration file
     * @return the Velocity Template object to format the database record in the
     * given style, or null if no such template is found
     * @throws TemplateException Could not load template
     */
    public Template getTemplateForStyle(String dbTableName, String style) throws TemplateException {

        String name = DIRECTORY + File.separator + dbTableName + File.separator + style.toLowerCase();
        if(name.endsWith(EXTENSION) == false)
    	{
    		name += EXTENSION;
    	}
        
        return getTemplate(name);
    }


    /**
     * Returns a list of available style names for the given database table.
     * 
     * @param dbTableName The name of the database table where the query is being done
     * @return an array of valid style names, or null if none found
     * @throws TemplateException Could not load template
     */
    public String[] getStyleNames(String dbTableName) throws TemplateException
    {
    	boolean foundDirectories = false;
    	FilenameFilter filter = new VelocityFilter(EXTENSION);
    	ArrayList<String> styles = new ArrayList<String>();
    	
    	List<String> directories = getTemplateDirectories();
    	for(int i=0; i < directories.size(); i++)
    	{
    		File dir = new File(directories.get(i) + File.separator + DIRECTORY + File.separator + dbTableName);
    		if(dir.exists() == false)
    		{
    			continue;
    		}
    		
    		foundDirectories = true;
    		
    		File[] files = dir.listFiles(filter);

            for (int j = 0; j < files.length; j++)
            {
                final String name  = files[j].getName();
                final String style = name.substring(0, name.length() - EXTENSION.length());

                // Ignore if not in lowercase

            	if (style.equals(style.toLowerCase()))
                {
                    if (! styles.contains(style))
                    {
                        styles.add(style);
                    }
                }
            }
    	}
        
    	if (foundDirectories == false)
        {
            throw new TemplateException("Unable to locate style templates for database output");            
        }
        
    	String[] stringStyles = new String[styles.size()];
    	styles.toArray(stringStyles);
    	
        return(stringStyles);
    }
    
    /**
     * Gets the list of template directories for a specific database table.
     * @param dbTableName database table name
     * @return list of directory names
     */
    public List<String> getTemplateDirectories(String dbTableName) {
    	List<String> baseDirs = super.getTemplateDirectories();
    	List<String> localDirs = new ArrayList<String>(baseDirs.size());
    	for (String s: baseDirs) {
    		localDirs.add(s + File.separator + DIRECTORY + File.separator + dbTableName);
    	}
    	return localDirs;
    }
    
 
}
