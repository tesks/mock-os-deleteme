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

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;

/**
 * Goal which runs velocity on template files.
 *
 * 
 */
@Mojo ( name = "velocity" )
@Execute( goal = "velocity" )

public class VelocityMojo
    extends AbstractMojo
{

	/** 
	 * If set to false, skip veloicty task
	 */
	@Parameter ( defaultValue = "false", property= "velocity.skip")
	private boolean skip;
	
    /**
     * Location to write the files resulting template files.  if defined, targetPath is relative to this directory.
     */
    @Parameter( defaultValue = "${project.build.directory}", property = "outputDir", required = true )
    private File outputDirectory;

    /**
     * List of velocity resources (i.e. templates) to process.
     */
    @Parameter( defaultValue = "${new ArrayList<VelocityResource>()}", property = "velocityResources", required = false )
    private List<VelocityResource> velocityResources;
    
	/**
	 * Whether to strip the base directory from the first directory from the include path when translating to an output file path.
	 * I.e.: dir1/dir2/example.vm is output to ${outputDirectory}/dir2/example if set to true.
	 */
	@Parameter ( defaultValue = "${false}", required = false)
	private boolean stripTopLevel;
	
    public void execute()
        throws MojoExecutionException
    {
    	if (skip) {
    		return;
    	}
    	Log log = this.getLog();
    	log.info("----velocity-maven-plugin----");
        File f = outputDirectory;
        log.debug("Creating directories necessary for " + f.getAbsolutePath());
        if ( !f.exists() )
        {
            boolean success = f.mkdirs();
            if (!success) {
            	throw new MojoExecutionException( "Error creating directory " + f.getAbsolutePath());
            }
        }
        
        if ( velocityResources.isEmpty() ) {
        	log.warn( "No velocityResources provided, doing nothing");
        }
        
        for ( VelocityResource res : velocityResources )
        {
        	this.processResource(res, log);
        }

    }
    
    /**
     * Encapsulation for dealing with a single templateResource element
     * @param resource The resource in question
     * @param log The log the plugin publishes to
     */
    public void processResource ( VelocityResource resource, Log log ) 
    {
    	File resourceOutputDir = new File(this.outputDirectory + File.separator + resource.getTargetPath());
    	if ( !resourceOutputDir.exists() ) {
    		resourceOutputDir.mkdirs();
    	}
    	String[] templateFileNames = resource.getFileNames(log);
    	if ( templateFileNames.length == 0 ) 
    	{
    		log.warn("No includes in template resource: " + resource.getDirectory());
    	}
    	VelocityContext context = null;
    	VelocityEngine engine = new VelocityEngine();
    	try {
	        context = resource.getVelocityContext(engine, log);
    	} catch (Exception e) {
    		log.warn("Error getting VelocityContext", e);
    	}

    	for ( String filename : templateFileNames )
    	{
    		try {
	    		Template t = engine.getTemplate(filename);
    			StringWriter writer = new StringWriter();
    			t.merge(context, writer);
			
				String outputFile = setUpOutputFile(resource, filename);
    			log.debug("Output File = " + outputFile);
    			BufferedWriter scriptWriter = new BufferedWriter (new FileWriter( new File(outputFile)));
    			scriptWriter.write(writer.getBuffer().toString());
    			scriptWriter.flush();
    			scriptWriter.close();
    			
    			log.debug("Wrote out generated file " + outputFile);
    			
    		} catch (Exception e) {
    			log.warn("Error processing template: " + filename, e);
    		}
    	}
    }
    
	/** Take a filename, prepends the output directory, and replaces the file extension as necessary
	 * Create necessary directories
	 * @param filename The filename to be manipulated
	 * @return path relative to the project directory, with appropriate file extension
	 */
	public String setUpOutputFile(VelocityResource resource, String filename) {
		Log log = this.getLog();
		StringBuilder directoryBuilder = new StringBuilder(outputDirectory.getPath());
		StringBuilder fileBuilder = new StringBuilder(filename);
		String targetPath = resource.getTargetPath();
		if (targetPath.length() > 0) {
			/** If targetPath has a value, then the input directory structure will be ignored
			 *  All included files will appear immediately under the directory pointed to by targetPath
			 */
			directoryBuilder.append(File.separator + targetPath);
			/** Truncate any directories from the filename */
			fileBuilder = fileBuilder.delete(0, filename.lastIndexOf(File.separator) + 1);
		} else if (stripTopLevel) {
			/** Remove the top directory level from the input path */
			log.debug("Stripping top level directory from input file.");
			log.debug("File name before: " + fileBuilder.toString());
			int index = fileBuilder.indexOf(File.separator);
			if (index > 0) {
				fileBuilder = fileBuilder.delete(0,  index + 1);
			}
			log.debug("File name after: " + fileBuilder.toString());
		}
		/** Now grab the directories out of fileBuilder so directories can be initialized 
		 *	
		 */
		int index = fileBuilder.lastIndexOf(File.separator);
		if (index > 0) {
			directoryBuilder.append(File.separator + fileBuilder.substring(0, index));
			fileBuilder.delete(0, index + 1);
		}
		
		/** Now, initialize directories */
		File file = new File(directoryBuilder.toString());
		log.debug("Attempting to make directories for " + directoryBuilder.toString());
		file.mkdirs();
		
		/** Deal with file extension replacement and target path.*/
		String outputExtension = resource.getOutputExtension();
		/*if (targetPath.length() != 0) {
			fileBuilder.insert(0, targetPath + File.separator);			
		} */
		int extension_start = fileBuilder.lastIndexOf(".");
		if (extension_start != -1) 
		{
			fileBuilder.delete(extension_start, fileBuilder.length());
		}

		fileBuilder.append(outputExtension);

		return (directoryBuilder + File.separator + fileBuilder).toString();
	}
	
	public boolean isStripTopLevel()
	{
		return stripTopLevel;
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}
}
