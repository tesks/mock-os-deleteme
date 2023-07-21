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
/**
 * Project:	AMMOS Mission Data Processing and Control System (MPCS)
 * Package:	jpl.gds.shared.template
 * File:	ApplicationTemplateManager.java
 *
 * Author:	Josh Choi (joshchoi)
 * Created:	Feb 7, 2012
 *
 */
package jpl.gds.shared.template;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.velocity.Template;

/**
 * A version of the Velocity template manager specific to the purpose of
 * formatting output from applications, as opposed to formatting defined for
 * data types/objects.
 * 
 * This template manager assumes a certain directory hierarchy for accessing
 * template files. For example, it looks for the "xml" style template at:
 * 
 * <templates_base_dir>/app/<application_name>/<data_type>/xml.vm
 * 
 * @since AMPCS R3
 * @see TemplateManager
 */
public class ApplicationTemplateManager extends TemplateManager {

	/**
	 * The subdirectory under the base template directory where application
	 * templates can be found.
	 */
	public static final String DIRECTORY = "app";
	
	/**
	 * The subdirectory under DIRECTORY where templates specific to the using
	 * application can be found.
	 */
	public String thisAppSubdir;
	
	
    /**
     * Creates an instance of ApplicationTemplateManager.
     *
     * @param mission Mission name
     * @param app Application object. All that will be accessed is the class name.
     *
     * @throws TemplateException Could not load template
     */
    public ApplicationTemplateManager(final String mission, final Object app) throws TemplateException {
		super(mission);

		String appName = app.getClass().getName();

		if (appName.lastIndexOf('.') > 0) {
		    appName = appName.substring(appName.lastIndexOf('.') + 1);
		}
		
		if (appName == null) {
			throw new TemplateException(
					"Cannot instantiate ApplicationTemplateManager because application name provided is null");
		}

		thisAppSubdir = appName;
	}

    
	/**
	 * Returns the Velocity template object for the given data/formatting type
	 * and formatting style.
	 * 
	 * @param type
	 *            the data/formatting type
	 * @param style
	 *            the string indicating the format style to use
	 * @return the Velocity Template object to format the data in the given
	 *         style, or null if no such template is found
	 * @throws TemplateException
	 *             Could not load template
	 */
    public Template getTemplateForStyle(final String type, String style) throws TemplateException {

        String name = DIRECTORY + File.separator + thisAppSubdir + File.separator + type + File.separator + style.toLowerCase();
        if(name.endsWith(EXTENSION) == false)
    	{
    		name += EXTENSION;
    	}
        
        return getTemplate(name);
    }

    
	/**
	 * Returns a list of available style names for the given data type.
	 * 
	 * @param type
	 *            The data type
	 * @return an array of valid style names, or null if none found
	 * @throws TemplateException
	 *             Could not load template
	 */
	public String[] getStyleNames(String type) throws TemplateException {
    	boolean foundDirectories = false;
    	FilenameFilter filter = new VelocityFilter(EXTENSION);
    	ArrayList<String> styles = new ArrayList<String>();
    	
    	List<String> directories = getTemplateDirectories();

    	for(int i=0; i < directories.size(); i++)
    	{
    		File dir = new File(directories.get(i) + File.separator + type);
    		if(dir.exists() == false)
    		{
    			continue;
    		}
    		
    		foundDirectories = true;
    		
    		File [] files = dir.listFiles(filter);
            for (int j = 0; j < files.length; j++)
            {
            	String style = files[j].getName().substring(0, files[j].getName().length() - EXTENSION.length());
            	style = style.toLowerCase();
            	if(styles.contains(style) == false)
            	{
            		styles.add(style);
            	}
            }
    	}
        
    	if (foundDirectories == false)
        {
            throw new TemplateException("Unable to locate style templates for message output");            
        }
        
    	String[] stringStyles = new String[styles.size()];
    	styles.toArray(stringStyles);
    	
        return(stringStyles);
    }
	
	
	/**
	 * Returns a list of available types used by the application.
	 * 
	 * @return an array of valid type names, or null if none found
	 * @throws TemplateException
	 *             could not find template types for this application
	 */
	public String[] getTypeNames() throws TemplateException {
	   	boolean foundDirectories = false;
    	ArrayList<String> types = new ArrayList<String>();
    	List<String> directories = getTemplateDirectories();
    	
    	for (int i=0; i < directories.size(); i++) {
    		File dir = new File(directories.get(i));

    		if(dir.exists() == false) {
    			continue;
    		}
    		
    		foundDirectories = true;
    		
    		File [] files = dir.listFiles();
            for (int j = 0; j < files.length; j++)
            {
            	String type = files[j].getName();
            	if(types.contains(type) == false)
            	{
            		types.add(type);
            	}
            }
    	}
        
    	if (foundDirectories == false)
        {
            throw new TemplateException("Unable to locate template types for this application");            
        }
        
    	String[] stringStyles = new String[types.size()];
    	types.toArray(stringStyles);
        return stringStyles;
	}
    
	
	/**
	 * Gets the list of template directories for a specific data type
	 * 
	 * @param type
	 *            the data type
	 * @return list of directory names
	 */
	public List<String> getTemplateDirectories(String type) {
    	List<String> baseDirs = getTemplateDirectories();
    	List<String> localDirs = new ArrayList<String>(baseDirs.size());
    	for (String s: baseDirs) {
    		localDirs.add(s + File.separator + type);
    	}
    	return localDirs;
    }


	/**
	 * {@inheritDoc}
	 *
	 * @see jpl.gds.shared.template.TemplateManager#getTemplateDirectories()
	 */
	@Override
	public List<String> getTemplateDirectories() {
		List<String> dirs = new ArrayList<String>(super.getTemplateDirectories());
		ListIterator<String> dirsIterator = dirs.listIterator();

		while(dirsIterator.hasNext()) {
			String newDir = dirsIterator.next() + File.separator + DIRECTORY + File.separator + thisAppSubdir;
			dirsIterator.set(newDir);
		}
		
		return dirs;
	}


}
