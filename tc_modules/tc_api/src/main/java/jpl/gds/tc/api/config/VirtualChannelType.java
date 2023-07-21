/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.tc.api.config;

/**
 * This class is an enumeration to keep track of the different types of virtual
 * channels used in telecommand.
 *
 *
 * MPCS-8822  - 06/01/17 - Completely redesigned - no longer dependent on EnumeratedType
 * MPCs-10928  - 07/17/19 - Added CFDP
 */
public enum VirtualChannelType {
	/** Enumeration for the hardware command value */
	HARDWARE_COMMAND("HardwareCommand", "HW CMD"),
	/** Enumeration for the flight software command value */
	FLIGHT_SOFTWARE_COMMAND("FlightSoftwareCommand", "FSW CMD"),
	/** Enumeration for the file load value */
	FILE_LOAD("FileLoad", "FILE"),
	/** Enumeration for the CFDP value */
	CFDP("Cfdp", "CFDP"),
	/** Enumeration for the sequence directive value */
	SEQUENCE_DIRECTIVE("SequenceDirective", "SEQ DIR"),
	/** Enumeration for the delimiter value */
	DELIMITER("Delimiter", "DELIM"),
	/** Enumeration for the unknown value */
	UNKNOWN("Unknown", "N/A");

	private String type;
	private String shortString;

	private VirtualChannelType(String type, String shortString) {
		this.type = type;
		this.shortString = shortString;
	}

	@Override
	public String toString(){
		return this.type;
	}
	
	/**
	 * Get the short name version of the enum (used for display purposes).
	 * 
	 * @return A shortened string name of the current value of the enum
	 */
	public String toShortString() {
		return this.shortString;
	}

	/**
	 * Gets an enumeration of VirtualChannelType
	 * 
	 * @param intVal
	 *            The value for this enumeration. Should be a valid index
	 * @return the VirtualChannelType at the supplied index
	 */
	public static VirtualChannelType getByIndex(final int intVal) {
		for (VirtualChannelType vct : VirtualChannelType.values()) {
			if (vct.ordinal() == intVal) {

				return vct;
			}
		}
		throw new IllegalArgumentException("Invalid enumeration index " + intVal);
	}
	
	/**
	 * Get a type of this enumeration based on a virtual channel number from a
	 * telecommand frame header
	 *
	 * @param frameConfig
	 *            The current Frame Configuration being used
	 * @param vcNumber
	 *            The virtual channel number to get an associated virtual
	 *            channel type
	 *
	 * @return The virtual channel type enumeration corresponding to the input
	 *         virtual channel number or null if a mapping does not exist
	 */
	public static VirtualChannelType getTypeFromNumber(final CommandFrameProperties frameConfig, final long vcNumber) {
		for (VirtualChannelType vct : VirtualChannelType.values()) {

			if (frameConfig.getVirtualChannelNumber(vct) == vcNumber) {
				return vct;
			}
		}

		return VirtualChannelType.UNKNOWN;
	}
}