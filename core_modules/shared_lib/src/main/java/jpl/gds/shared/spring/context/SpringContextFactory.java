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

package jpl.gds.shared.spring.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import jpl.gds.shared.config.GdsSystemProperties;


/**
 * Basic factory for Spring ApplicationContext objects.
 * 
 *
 * @since R8
 *
 */
@EnableWebMvc
public class SpringContextFactory {
	/**
	 * Wildcarded package name for default Spring configuration classes. These go first onto
	 * the scan path.
	 */
	public static final String BOOTSTRAP_SCAN_PATH = "jpl.gds.**.spring.bootstrap";
	/**
	 * Wildcarded package name for default Spring configuration override classes.
	 */
	public static final String BOOTSTRAP_OVERRIDE_SCAN_PATH = "jpl.gds.**.spring.bootstrap.override";

	/**
	 * Package prefix for mission-specific Spring configuration classes.
	 */
    public static final String  MISSION_PACKAGE_ROOT         = "jpl.gds.";

	/**
	 * Root package suffux for mission-specific Spring configuration classes.  The mission 
	 * package needs to be unique so that it does not get picked up as part of the general 
	 * sring bootstrap wild card search.
	 */
    public static final String MISSION_PACKAGE_SUFFIX       = ".spring.override";
    
    private SpringContextFactory() {}

	/**
	 * Creates a Spring context using the scan paths provided.
	 * 
	 * @param scanPaths array of scanPaths to append to the basic list of scan paths.
	 * @return spring context
	 */
    public static final AnnotationConfigApplicationContext getSpringContext(final String[] scanPaths) {
        return new AnnotationConfigApplicationContext(scanPaths);
	}

	/**
	 * Creates context using the basic bootstrap scan paths.
	 * 
	 * @return spring context
	 */
    public static final AnnotationConfigApplicationContext getSpringContext() {
		return getSpringContext(new String[] {BOOTSTRAP_SCAN_PATH, BOOTSTRAP_OVERRIDE_SCAN_PATH});
	}
	

    /**
     * Creates context using the basic bootstrap scan paths, but with the option to add mission override
     * path.
     * 
     * @param includeMissionOverrides true to include mission override path
     * 
     * @return spring context
     */
    public static final ApplicationContext getSpringContext(final boolean includeMissionOverrides) {
        return internalGetSpringContext(Arrays.asList(BOOTSTRAP_SCAN_PATH, BOOTSTRAP_OVERRIDE_SCAN_PATH), includeMissionOverrides);
    }
    
	
    /**
	 * Creates a Spring context using the scan paths provided.  This will only add the mission scan paths if 
	 * includeMissionOverrides is true.
	 * 
	 * @param scanPaths array of scanPaths to append to the basic list of scan paths.
     * @param includeMissionOverrides true to include mission override path
	 * @return spring context
	 */
	public static final ApplicationContext getSpringContext(final List<String> scanPaths, final boolean includeMissionOverrides) {
        final ArrayList<String> mergedPaths = new ArrayList<>();
		mergedPaths.add(BOOTSTRAP_SCAN_PATH);
		mergedPaths.add(BOOTSTRAP_OVERRIDE_SCAN_PATH);
		
		// Add the scan paths passed in scan paths after the bootstrap and before the mission.
		mergedPaths.addAll(scanPaths);
		
		return internalGetSpringContext(mergedPaths, includeMissionOverrides);
		
	}
	
	/**
	 * Creates a Spring context using the scan paths provided.  This will only add the mission scan paths if 
	 * includeMissionOverrides is true.
	 * 
	 * @param scanPaths array of scanPaths to append to the basic list of scan paths.
	 * @param includeMissionOverrides true to include mission override path
	 * @return spring context
	 */
	private static final ApplicationContext internalGetSpringContext(final List<String> scanPaths, final boolean includeMissionOverrides) {
        final ArrayList<String> mergedPaths = new ArrayList<>();

		// Add the scan paths passed in scan paths after the bootstrap and before the mission.
		mergedPaths.addAll(scanPaths);

		if (includeMissionOverrides) {
			mergedPaths.add(MISSION_PACKAGE_ROOT + GdsSystemProperties.getSystemMission() + MISSION_PACKAGE_SUFFIX);
            // Remove mission+SSE package scan, it doesn't look like we have any?
            // With SSE no longer being a system property, how would we know whether or not to scan an sse package
            // - the context is not created yet!
		}

		return getSpringContext(mergedPaths.toArray(new String[0]));
	}

    /**
     * Gets a list of spring bootstrap scan paths
     * 
     * @param includeMission
     *            true if the mission be included
     * @return ArrayList of bootstrap scan paths
     */
    public static final List<String> getSpringBootstrapScanPath(final boolean includeMission) {
        final List<String> scanPath = new ArrayList<>();
        scanPath.add(SpringContextFactory.BOOTSTRAP_SCAN_PATH);
        scanPath.add(SpringContextFactory.BOOTSTRAP_OVERRIDE_SCAN_PATH);

        if (includeMission) {
            scanPath.add(SpringContextFactory.MISSION_PACKAGE_ROOT + GdsSystemProperties.getSystemMission()
                    + SpringContextFactory.MISSION_PACKAGE_SUFFIX);
            // Remove mission+SSE package scan, it doesn't look like we have any?
            // With SSE no longer being a system property, how would we know whether or not to scan an sse package
            // - the context is not created yet!
        }

        return scanPath;
    }
}
