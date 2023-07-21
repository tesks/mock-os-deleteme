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
import java.io.FilenameFilter;

/**
 * A file filter for only accepting 
 * JAR files (files that end in ".jar").
 * 
 *
 */
public class JarFileFilter implements FilenameFilter
{
	/**
	 * Accept all files that end in ".jar"
	 * 
	 * {@inheritDoc}
	 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
	 */
    public boolean accept(File dir, String name)
    {
        return name.endsWith(".jar");
    }
}

