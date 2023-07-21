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
package jpl.gds.tc.api.config;

import jpl.gds.shared.config.DynamicEnum;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class is an enumeration of the possible execution strings on the
 * spacecraft that can be targeted by an uplink transmission.
 *
 * Note that the DB holds just a single character.
 *
 * MPCS-11865 - 7/21/20: Updated implementation so missions can define their own StringId's
 */
public class ExecutionStringType extends DynamicEnum<ExecutionStringType> {

	/** Enumeration of the A string type */
	public static final ExecutionStringType A = new ExecutionStringType("A", 0);

	/** Enumeration of the B string type */
	public static final ExecutionStringType B = new ExecutionStringType("B", 1);

	/** Enumeration of the AB string type */
	public static final ExecutionStringType AB = new ExecutionStringType("AB", 2);

	String stringId;

	/**
	 * Constructor for Execution String ID's
	 * @param stringId Execution StringID (spacecraft side)
	 * @param ordinal ordinal
	 */
	public ExecutionStringType(final String stringId, final int ordinal){
		super(stringId.toUpperCase(), ordinal);
		this.stringId = stringId;

		//overwrite with actual string id object with properties
		elements.get(getClass()).putIfAbsent(stringId, this);
	}

	@Override
	public String toString(){
		return this.stringId;
	}

	/**
	 * Get object from string
	 *
	 * @param name Name of option
	 * @return ExecutionStringType object
	 */
	public static ExecutionStringType valueOf(String name) {
		Map<String, DynamicEnum<?>> map = elements.get(ExecutionStringType.class);
		if(map != null && map.containsKey(name)) {
			return (ExecutionStringType) map.get(name);
		}

		throw new IllegalArgumentException("No enum constant " + name);
	}

	/**
	 * Explicit definition of values() is needed here to trigger static initializer.
	 * @return array of ExecutionStringType
	 */
	public static ExecutionStringType[] values() {
		return values(ExecutionStringType.class);
	}

	/**
	 * Get string representation of this enum
	 * @return Comma separated string
	 */
	public static String valuesAsString(){
		List<String> names = new ArrayList<>();
		for(ExecutionStringType type : values()){
			names.add(type.name());
		}
		return String.join(",", names);
	}

	/**
	 * Gets an enumeration of ExecutionStringType.
	 *
	 * @param intVal The value for this enumeration.  Should be a valid index
	 *
	 * @return the ExecutionStringType at the supplied index
	 */
	public static ExecutionStringType getByIndex(final int intVal){
		for(final ExecutionStringType est : ExecutionStringType.values()){
			if(est.ordinal() == intVal){
				return est;
			}
		}
		throw new IllegalArgumentException("Invalid enumeration index "	+ intVal);
	}

	/**
	 * Get the configured virtual channel number for this enumerated value
	 *
	 * @param appContext the ApplicationContext in which this object is being used
	 * @return The virtual channel number for this object
	 */
	public byte getVcidValue(final ApplicationContext appContext)
	{
		return(appContext.getBean(CommandFrameProperties.class).getStringIdVcidValue(this.toString()));
	}

	/**
	 * Get the configured virtual channel number for this enumerated value
	 *
	 * @param frameProperties command frame properties
	 * @return the virtual channel number for this object
	 */
	public byte getVcidValue(final CommandFrameProperties frameProperties) {
		return frameProperties.getStringIdVcidValue(this.toString());
	}

	/**
	 * Get a type of this enumeration based on a string ID integer value from a telecommand frame header
	 *
	 * @param appContext the ApplicationContext in which this object is being used
	 * @param value The string ID value to get an associated execution string type
	 *
	 * @return The execution string type enumeration corresponding to the input value
	 * or null if a mapping does not exist
	 */
	public static ExecutionStringType getTypeFromVcidValue(final ApplicationContext appContext, final long value)
	{
		return getTypeFromVcidValue(appContext.getBean(CommandFrameProperties.class), value);
	}

    /**
     * Get a type of this enumeration based on a string ID integer value from a telecommand frame header
     *
     * @param frameProperties command frame properties
     * @param value           The string ID value to get an associated execution string type
     * @return The execution string type enumeration corresponding to the input value or null if a mapping does not
     * exist
     */
    public static ExecutionStringType getTypeFromVcidValue(final CommandFrameProperties frameProperties,
                                                           final long value) {
        for (final ExecutionStringType est : ExecutionStringType.values()) {
            final long vcidValue = frameProperties.getStringIdVcidValue(est.toString());
            if (vcidValue == value) {
                return est;
            }
        }

        return null;
    }
}
