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
package jpl.gds.common.config;

import java.io.File;

import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.jar.JarFileFilter;
import jpl.gds.shared.jar.RuntimeJarFileLoader;

/**
 * A utility class for loading/adding user jars to the class path.
 * 
 * @TODO R8 Refactor TODO - this is currently being called nowhere. We need to
 *       decide when and where it is appropriate to do so.
 * 
 *
 * @since R8
 *
 */
public class UserJarLoader {

	/**
	 * Load all jar files from the GdsUserConfigDir/lib location. This is the
	 * location that users can use to get things on the classpath. We have to do
	 * this for users to be able to add derived EHA and DPO algorithms to the
	 * classpath. We have to add to the classpath dynamically because we don't
	 * know the GdsUserConfigDir until runtime. If the jar files exist, they
	 * will be put on the classpath
	 * 
	 * @param props
	 *            the current GeneralProperties object
	 */
	protected void loadUserJarFiles(GeneralProperties props) {
		final String jarSubDir = props.getUserJarDirectory();

		String userLibDir = jarSubDir;
		if (!jarSubDir.startsWith(File.separator)) {
			userLibDir = GdsSystemProperties.getUserConfigDir()
					+ File.separator + jarSubDir;
		}

		final RuntimeJarFileLoader loader = new RuntimeJarFileLoader();

		final File userLib = new File(userLibDir);
		if (!userLib.exists()) {
			// this is not an error, it just means the user is supplying no
			// libraries
			return;
		}

		try {
			loader.addFile(userLib);
		} catch (final Exception e1) {
			System.err.println("Could not load user lib directory "
					+ userLib.getAbsolutePath()
					+ " to the classpath due to an error: " + e1.getMessage());
		}

		final File[] userLibFiles = userLib.listFiles(new JarFileFilter());
		for (final File jarFile : userLibFiles) {
			try {
				loader.addFile(jarFile);
			} catch (final Exception e) {
				System.err.println("Could not load user JAR file "
						+ jarFile.getAbsolutePath() + " due to an error: "
						+ e.getMessage());
			}
		}
	}
}
