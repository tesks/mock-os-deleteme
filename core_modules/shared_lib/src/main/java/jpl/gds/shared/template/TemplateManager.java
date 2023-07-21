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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;

import jpl.gds.shared.config.GdsSystemProperties;

/**
 * TemplateManager manages the formatting of text content using Velocity templates.
 * Templates are cached once loaded.
 *
 */
public class TemplateManager {
    
    /** Template subdirectory */
    protected static final String COMMON_TEMPLATE_SUBDIR = "common";
    
    /** Tables of loaded Velocity templates **/
    protected static final Hashtable<String,Template> templateTable = new Hashtable<String,Template>();
    
    /** Velocity template engine instance **/
    protected VelocityEngine engine;
    
    /** Full template directory path **/
    protected String templateDir;
    
    /** Current mission mnemonic **/
    protected String mission;

    /** List of template directories */
    protected List<String> templateDirectories;
    
    /**
     * Extension for template files. Templates should be named <message-type>/<style-name>.<extension>
     */
    public static final String EXTENSION = ".vm";
    
    /** Name of the GDS template sub-directory, relative to the root **/
    private static final String DEFAULT_TEMPLATE_SUBDIR = "templates";
    
    /**
     * Constructs a new TemplateManager by appending the default template
     * sub-directory name to the value of the GdsDirectory property and
     * then adding the mission specifier.
     *
     * @param mission Mission name
     */
    public TemplateManager(final String mission)
    {
        this(mission, null);
    }
    
    /**
     * Constructs a TemplateManager using the given template directory. If the
     * baseDir is null, then the template directory will be read out of the
     * configuration file. If the configuration variable isn't set, a default incantation
     * will construct a likely directory name.
     *
     * @param mission Mission name
     * @param baseDir the base directory to use for locating templates.
     */
    public TemplateManager(final String mission, final String baseDir)
    {
        final String missionTemplateDir = GdsSystemProperties.getSystemProperty("GdsProjectTemplateDir");
        try {
            this.mission = mission;
            this.templateDir = GdsSystemProperties.getSystemProperty("GdsSystemTemplateDir");
            if (this.templateDir == null) {
            if (baseDir != null) {
                this.templateDir = baseDir;
            } else {
                this.templateDir = DEFAULT_TEMPLATE_SUBDIR;
                    final String gdsDir = GdsSystemProperties.getSystemProperty(GdsSystemProperties.DIRECTORY_PROPERTY);
                 
                if (this.templateDir == null && gdsDir != null) {
                    this.templateDir = gdsDir + File.separator + DEFAULT_TEMPLATE_SUBDIR;
                } else {
                    final File f = new File(this.templateDir);
                    if (!f.isAbsolute() && gdsDir != null) {
                        this.templateDir = gdsDir + File.separator + this.templateDir;
                    }
                }
            }
            }
            if (this.templateDir == null) {
                this.templateDir = DEFAULT_TEMPLATE_SUBDIR;
            }

        } catch (final Exception e) {
            e.printStackTrace();
            System.err.println("Unable to load configuration in TemplateManager: " + e.getMessage());
        }
        
        engine = new VelocityEngine();
        engine.setProperty(RuntimeConstants.CHECK_EMPTY_OBJECTS, false);
        engine.setProperty(RuntimeConstants.DEFAULT_RUNTIME_LOG_NAME, "/dev/null");
        engine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "true");
        
        
        // First look in the templates folder in the user's home directory
        // Second look in the mission specific template sub-directory in chill
        // Third look in the common template sub-directory in chill
        // Fourth look in the templates folder itself
        // finally, look in the default directory

        this.templateDirectories = new ArrayList<String>();
        this.templateDirectories.add(getUserTemplateDir());
        if (missionTemplateDir == null) {
            this.templateDirectories.add(new File(this.templateDir + File.separator + this.mission).getAbsolutePath());
        } else {
        	this.templateDirectories.add(new File(missionTemplateDir).getAbsolutePath());
        }
        this.templateDirectories.add(new File(this.templateDir + File.separator + COMMON_TEMPLATE_SUBDIR).getAbsolutePath());
        this.templateDirectories.add(new File(this.templateDir).getAbsolutePath());
        this.templateDirectories.add(".");

        for(int i=0; i < this.templateDirectories.size(); i++)
        {
        	engine.addProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, this.templateDirectories.get(i));
        }
        
        try {
            engine.init();
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            e.printStackTrace();
            System.err.println("Unable to initialize velocity in TemplateManager: " + e.getMessage());
        }
    }
    
    /**
     * Loads the template with the given relative file name, which will be
     * appended to the current template base directory name.  Templates are
     * cached once loaded, and if a cached template is available, this method 
     * will return it.
     * @param templateName the name of the template file, relative to the
     * template base directory
     * @return a Velocity Template
     * @throws TemplateException Could not load template
     */
    public synchronized Template getTemplate(final String templateName) throws TemplateException {
    	return getTemplate(templateName, false);
    }
    
    public synchronized Template getTemplate(final String templateName, final boolean ignoreCache) throws TemplateException {
    	final String longName = this.templateDir + File.separator + templateName;

    	synchronized(templateTable) {
    		Template t = templateTable.get(longName);
    		if (t == null || (t != null && ignoreCache)) {
    			// 02/22/16 - Removed stack traces and verify that TemplateException does not have line breaks.
    			//                          This causes errors to be more compactly displayed/handled.
    			//				 02/26/16 - Added Runtime Exception catch/throw
    			try {
    				t = engine.getTemplate(templateName);
    				templateTable.put(longName, t);                
    			} catch (final ResourceNotFoundException e) {
    				//e.printStackTrace();

    				if(e.getMessage() != null && e.getMessage().contains(System.lineSeparator())){
    					final ResourceNotFoundException tempErr = new ResourceNotFoundException(e.getMessage().replaceAll(System.lineSeparator(), " "));
    					throw new TemplateException("Template name " + templateName + " not found in " + this.templateDir + " or " + this.templateDir + File.separator + this.mission, tempErr);
    				}

    				throw new TemplateException("Template name " + templateName + " not found in " + this.templateDir + " or " + this.templateDir + File.separator + this.mission, e);

    			} catch (final ParseErrorException e) {
    				//e.printStackTrace();

    				if(e.getMessage() != null && e.getMessage().contains(System.lineSeparator())){
    					final ParseErrorException tempErr = new ParseErrorException(e.getMessage().replaceAll(System.lineSeparator(), " "));
    					throw new TemplateException("Parse error in template " + longName, tempErr);
    				}

    				throw new TemplateException("Parse error in template " + longName, e);

    			} catch (final RuntimeException e){
    				throw e;
    				
    			} catch (final Exception e) {
    				//e.printStackTrace();

    				//don't want to lose the error type unless we have to
    				if(e.getMessage() != null && e.getMessage().contains(System.lineSeparator())){
    					final Exception tempErr = new Exception(e.getMessage().replaceAll(System.lineSeparator(), " "));
    					throw new TemplateException("Problem loading template " + longName, tempErr);
    				}

    				throw new TemplateException("Problem loading template " + longName, e);
    			}
    		}
    		return t;
    	}
    }

    /**
     * Generated formatted text using the given Template and a context object
     * containing substitution variables.
     * @param template the loaded Velocity Template object
     * @param context the VelocityContext object containing the map of 
     * variable names to values
     * @return the formatted text
     */
    public static String createText(final Template template, final VelocityContext context) {
        
        final StringWriter writer = new StringWriter();
        try {
            template.merge(context, writer);
        } catch (final ResourceNotFoundException e) {
            e.printStackTrace();
            return "";
        } catch (final ParseErrorException e) {
            e.printStackTrace();
            return "";
        } catch (final MethodInvocationException e) {
            e.printStackTrace();
            return "";
        } catch (final Exception e) {
            e.printStackTrace();
            return "";
        }
        return writer.getBuffer().toString();
    }

    /**
     * Generated formatted text using the given Template and a HashMap object
     * containing substitution variables.
     * @param template the loaded Velocity Template object
     * @param map the Map object containing the map of 
     * variable names to values
     * @return the formatted text
     */
    public static String createText(final Template template, final Map<String,Object> map) {
    	// Velocity doesn't have a built-in null checker, so using a
    	// third-party "tool".
    	map.put("nullTool", NullTool.getInstance());
        return createText(template, new VelocityContext(map));
    }
    
    /**
     * Static convenience method to return either the input object
     * or the empty string, in the cases where the object is bull.
     * @param obj the Object to return
     * @return the Object or the empty String if it is null
     */
    public static Object escapeNulls(final Object obj) {
        return (obj == null) ? "" : obj;
    }
    
    /**
     * Retrieves the list of searched template directories.
     * @return a list of Strings that are directory names
     */
    public List<String> getTemplateDirectories()
    {
    	return(this.templateDirectories);
    }
    
    private static String getUserTemplateDir(){
    	return GdsSystemProperties.getUserConfigDir() + File.separator
                + DEFAULT_TEMPLATE_SUBDIR;
    }

    /**
     * Filename filter for velocity files.
     */
    public static class VelocityFilter implements FilenameFilter {
        private final String extension;


        /**
         * Constructer.
         *
         * @param extension File extension
         */        
    	public VelocityFilter(final String extension) {
    		this.extension = extension;
    	}


        /**
         * {@inheritDoc}
         */    	
        @Override
		public boolean accept(final File arg0, final String arg1) {
            return arg1.endsWith(this.extension);
        }
    }
}
