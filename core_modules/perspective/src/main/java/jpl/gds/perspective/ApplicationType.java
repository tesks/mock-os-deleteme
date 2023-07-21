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
package jpl.gds.perspective;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.shared.types.EnumeratedType;

/**
 * An enumeration of application types in a user perspective.
 */
public class ApplicationType extends EnumeratedType {
	/**
	 * Application type is not known
	 */
	static public final int UNKNOWN_APP = 0;
	
	/**
	 * Telemetry downlink application
	 */
	static public final int DOWNLINK_APP = 1;

	/**
	 * Telemetry visualization application
	 */
	static public final int MONITOR_APP = 2;
	
	/**
	 * Command uplink application
	 */
	static public final int UPLINK_APP = 3;

	/**
	 * static string values for the enumerated values
	 */
	@SuppressWarnings({"MALICIOUS_CODE","MS_PKGPROTECT"}) 
	static public final String[] displayTypes = { 
		"UNKNOWN",
		"Downlink", 
		"Monitor", 
		"Uplink"
	};

	/**
	 * Static convenience instance for an application whose type is unknown
	 */
	public static final ApplicationType UNKNOWN = new ApplicationType(UNKNOWN_APP);
	
	/**
	 * Static convenience instance for a downlink app
	 */
	public static final ApplicationType DOWNLINK = new ApplicationType(DOWNLINK_APP);
	
	/**
	 * Static convenience instance for a monitor app
	 */
	public static final ApplicationType MONITOR = new ApplicationType(MONITOR_APP);
	
	/**
	 * Static convenience instance for an uplink app
	 */
	public static final ApplicationType UPLINK = new ApplicationType(UPLINK_APP);

	/**
	 * Creates an instance of ApplicationType.
	 * @param intVal the integer value constant from this class
	 * @throws IllegalArgumentException if the supplied argument does 
	 * not map to a known application type 
	 */
	public ApplicationType(final int intVal) throws IllegalArgumentException {
		super(intVal);
	}

	/**
	 * Creates an instance of ApplicationType.
	 * @param stringVal the string value constant from this class
	 * @throws IllegalArgumentException if the supplied argument does 
	 * not map to a known application type 
	 */
	public ApplicationType(final String stringVal) throws IllegalArgumentException {
		super(stringVal);
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.types.EnumeratedType#getStringValue(int)
	 */
	@Override
	protected String getStringValue(final int index) {
		if (index < 0 || index > getMaxIndex()) {
			throw new ArrayIndexOutOfBoundsException();
		}
		return (displayTypes[index]);
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.types.EnumeratedType#getMaxIndex()
	 */
	@Override
	protected int getMaxIndex() {
		return (displayTypes.length - 1);
	}

	/**
	 * Generates an XML representation of the ApplicationType
	 * @return the XML string
	 */
	public String toXML() {
		final StringBuffer buf = new StringBuffer();
		buf.append("<applicationType> name=" + this.getValueAsString()
				+ " </applicationType>");
		return buf.toString();
	}

	/**
	 * Indicates whether this application type utilizes the database.
	 * @return true if application needs access to the database, false if not
	 */
	public boolean usesDatabase() {
	    return (valIndex == DOWNLINK_APP || valIndex == UPLINK_APP);
	}
	
	/**
	 * Indicates whether this application type utilizes the message service.
	 * @return true if application needs access to the message service, false if not
	 */
	public boolean usesMessageService() {
	    return valIndex != UNKNOWN_APP;
	}
}
