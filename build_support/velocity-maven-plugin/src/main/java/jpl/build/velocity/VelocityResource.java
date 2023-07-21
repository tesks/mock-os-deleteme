/*
 * Copyright 2006-2017. California Institute of Technology.
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
package jpl.build.velocity;

import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeConstants;

@Mojo (name = "velocityResource")
public class VelocityResource {
	
    /**
     * The Maven project
     *
     */
	@Parameter ( defaultValue="${project}", property = "project")
    private MavenProject project;
    
	/**
	 * Directory containing the VM_global_library.vm file
	 */
	@Parameter
	private File velocityLibrary;
	
	/**
	 * Root directory from which Velocity Templates should will be enumerated
	 */
	@Parameter ( defaultValue= "${new FileSet()}")
	private FileSet fileset;
	
	/**
	 * A set of file patterns to feed to Velocity
	 */
	@Parameter 
	private String[] includes;
	
	/**
	 * A set of file patterns to exclude from those fed to Velocity
	 */
	@Parameter
	private String[] excludes;
	
	/**
	 * The relative path to the desired output directory
	 */
	@Parameter
	private String targetPath;
	
	/** 
	 * The extension desired for the file post-Velocity process.  By default, no extension is kept.
	 * E.g.: extension template.vm could be configured to be output as template.sh
	 */
	@Parameter ( defaultValue = "", required = false)
	private String outputExtension;
	
	
	/**
	 * List of properties to be substituted into templates
	 */
	@Parameter ( defaultValue = "${new HashMap<String, String>()}" )
	private Map<String, String> templateProperties = new HashMap<String, String>();
	
	public VelocityResource()
	{
		if (targetPath == null) {
			targetPath = "";
		}
		if (outputExtension == null) {
			outputExtension = "";
		}
		if (includes == null) {
			includes = new String[1];
			includes[0] = "**/*";
		}
		if (excludes == null) {
			excludes = new String[1];
			excludes[0] = "";
		}
	}
	/**
	 *  Returns the list of template files to be processed
	 */
	public String[] getFileNames (Log log)
	{
		String dir = fileset.getDirectory();
		if (dir == null) {
			log.warn("Directory not set");
		} else {
			File f = new File(fileset.getDirectory());
			if (f.exists()) {
				fileset.setFollowSymlinks(true);
				FileSetManager manager = new FileSetManager();
				String files[] = manager.getIncludedFiles(fileset);
				return files;
			}
		}
		return new String[0];
	}
	
	/**
	 * Returns the base directory of the underlying FileSet.
	 */
	public String getDirectory()
	{
		return fileset.getDirectory();
	}
	
	/**
	 * Returns VelocityResource will construct a VelocityContext relating to the details of this
	 * resource
	 * @throws Exception 
	 */
	public VelocityContext getVelocityContext(VelocityEngine engine, Log log) throws Exception
	{
    	String dir = fileset.getDirectory();
		Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER,"file");
		if (velocityLibrary != null) {
			if (!velocityLibrary.exists()) {
				log.warn("velocityLibrary " + velocityLibrary.getAbsolutePath() + " does not exist");
			} else {
		        engine.addProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, velocityLibrary.getParent());

				engine.setProperty(RuntimeConstants.VM_LIBRARY, velocityLibrary.getName());
			}
		}
		log.debug("Setting Velocity resource loader path to: " + dir);
        engine.addProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, dir);
        Velocity.init();
        VelocityContext context = new VelocityContext();
        if (templateProperties.size() == 0) {
        	log.info("templateProperties map is empty for velocityResource " + this.getDirectory());
        }
        for ( Entry<String, String> e : templateProperties.entrySet() )
        {
        	context.put(e.getKey(), e.getValue());
        }
        return context;
	}

	public void setTargetPath(String targetPath)
	{
		this.targetPath = targetPath;
	}
	
	public String getTargetPath()
	{
		return targetPath;
	}
	
	public void setOutputExtension(String outputExtension)
	{
		this.outputExtension = outputExtension;
	}
	
	public String getOutputExtension()
	{
		return outputExtension;
	}
	 
}
