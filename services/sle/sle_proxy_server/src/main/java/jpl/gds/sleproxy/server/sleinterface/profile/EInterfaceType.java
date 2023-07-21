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
package jpl.gds.sleproxy.server.sleinterface.profile;

/**
 * Enumerates the different types of SLE interfaces definable.
 * 
 */
public enum EInterfaceType {

	/**
	 * Type for the RETURN ALL FRAMES (RAF) SLE service. 
	 */
	RETURN_ALL,
	
	/**
	 * Type for the RETURN CHANNEL FRAMES (RCF) SLE service.
	 */
	RETURN_CHANNEL,
	
	/**
	 * Type for the FORWARD CLTU (FCLTU) SLE service.
	 */
	FORWARD;

}