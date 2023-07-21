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
package jpl.gds.shared.jar;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Class to dynamically add a JAR file to the classpath
 * after the JVM has already launched. Used to add user libraries for
 * DPO and derived EHA handling to the classpath.  We expect user libraries to be in
 * !GdsUserConfigDir!/lib and the problem is that we won't know the GdsUserConfigDir value
 * until after the JVM has already launched (it's passed in as a -D java system property). 
 * 
 * Code taken from http://forum.java.sun.com/thread.jspa?threadID=300557&range=15&start=0&q=&hilite=false&forumID=32
 * 
 * For the record, this class is a complete hack around the Java class loading mechanism, but this
 * is the only way we could come up with to solve the problem I described above.  Making our own classloader wouldn't
 * even necessarily work because it's a chicken/egg problem the classloader will need to be called before we can actually
 * get a hold of the GdsUserConfigDir.
 * 
 *
 */
public class RuntimeJarFileLoader
{
	/**
	 * Static constant for the name of the class loader method we invoke to stick something
	 * onto the classpath.
	 */
	protected static final String CLASSPATH_ADD_METHOD = "addURL";
	private final ClassLoader loader;
	
	/**
	 * Constructor. Uses the system class loader.
	 */
	public RuntimeJarFileLoader() {
		this.loader = null;
	}

	/**
	 * Constructor that takes a class loader.
	 */
	public RuntimeJarFileLoader(final ClassLoader loader)
    {
		this.loader = loader;
	}

	/**
	 * Add a file/directory to the current classpath
	 * 
	 * @param s A full or relative path to put on the actual classpath
	 */
	public void addFile(final String s)
    {
    	final File f = new File(s);
    	addFile(f);
    }
     
	/**
	 * Add a file/directory to the current classpath
	 * 
	 * @param f The file object to be added to the classpath.  The result of f.toURL() is actually
	 * what will get added to the classpath.
	 */
    public void addFile(final File f)
    {
    	try
    	{
			addURL(f.toURI().toURL());
		}
    	catch (final MalformedURLException e)
    	{
			throw new JarLoadException("Could not load input file " + f.getAbsolutePath() + " onto the classpath due to a path format error.",e);
		}
    }
     
    /**
     * Add a URL to the classpath.  Although this is a URL, we only ever really add
     * files/directories on the local system, but since the system class loader is usually an
     * instance of URLClassLoader, we have to add things to it as URLs.
     * 
     * @param u The URL of the file/directory to add to the classpath
     */
    public void addURL(final URL u)
    {
    	URLClassLoader sysloader = null;
    	final ClassLoader classLoader = loader == null ? ClassLoader.getSystemClassLoader() : loader;
    	if((classLoader instanceof URLClassLoader) == false)
    	{
    		throw new JarLoadException("Could not add  the value " + u + " to the system classpath because" +
    				" the system class loader was expected to be an instance of " + URLClassLoader.class.getName() + ", but" +
    				" it's instead an instance of " + classLoader.getClass().getName());
    	}
    	
    	//currently we only know how to pull this trick off with a URLClassLoader
    	sysloader = (URLClassLoader)classLoader;
    	
    	//grab the method we need to access in the system class loader via reflection
    	Method method = null;
    	try
    	{
			method = URLClassLoader.class.getDeclaredMethod(CLASSPATH_ADD_METHOD,new Class<?>[] { URL.class });
		}
    	catch(final Exception e1)
    	{
    		throw new JarLoadException("Could not instantiate classpath appending method " + CLASSPATH_ADD_METHOD + " through reflection " +
    				"for user library entry path " + u + ": " + e1.getMessage(), e1);
    	} 
    	
    	//HACK: this is the complete hack in the process. The method that we grabbed above & need to use is protected
    	//so we set it to accessible before we call it.
    	method.setAccessible(true);
    	
    	//invoke the method to add a URL to the classpath
    	try
    	{
			method.invoke(sysloader,new Object[]{ u });
		}
    	catch(final Exception e2)
    	{
    		throw new JarLoadException("Could not invoke classpath append method " + CLASSPATH_ADD_METHOD + " through reflection " +
    				"for user library entry path " + u + ": " + e2.getMessage(),e2);
    	} 
    	
    	//put it back to the way it was
    	method.setAccessible(false);
    	
    }
}
