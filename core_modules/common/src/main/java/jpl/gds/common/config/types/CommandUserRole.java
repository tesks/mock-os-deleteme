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
package jpl.gds.common.config.types;

/**
 * This enumeration defines valid roles for users or applications that may perform 
 * commanding. The role may used used in request authentication.
 * 
 * @since AMPCS R3
 * 
 */
public enum CommandUserRole {
	/** User is the command ACE or test conductor */
    ACE, 
    
    /** User is associated with command sequencing */
	SEQUENCE, 
	
	/** User is a scientist */
	SCIENTIST, 
	
	/** User is a generic command viewer */
	VIEWER;

	/**
	 * Get the least powerful user role
	 * 
	 * @return the least powerful user role
	 */
	public static CommandUserRole getLeastPowerful() {
		return VIEWER;
	}
}
