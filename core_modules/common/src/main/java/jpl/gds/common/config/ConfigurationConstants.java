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

/**
 * Common constants used in configuration.
 * 
 */
public class ConfigurationConstants {

    /** Name length */
    public static final int NAME_LENGTH = 64;

    /** Description length */
    public static final int DESC_LENGTH     =  255;

	/** File length*/
	public static final int FILE_LENGTH = 1024;

    /** Testbed length */
    public static final int TESTBED_LENGTH = 32;

    /** Topic length */
    public static final int TOPIC_LENGTH = 128;

    /**
     * No need to instantiate.
     */
    private ConfigurationConstants() {
        // do nothing
    }

}
