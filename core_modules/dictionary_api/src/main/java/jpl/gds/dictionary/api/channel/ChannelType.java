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
package jpl.gds.dictionary.api.channel;

import jpl.gds.shared.annotation.CustomerAccessible;

/**
 * ChannelType is an enumeration that defines all the valid data types for telemetry 
 * channels.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * ChannelType is an enumeration of supported telemetry channel types. Every ChannelDefinition
 * object has an associated ChannelType. ChannelDefinitions of the proper type can be created
 * using the ChannelDefinitionFactory.
 *
 *
 *
 * @see IChannelDefinition
 * @see ChannelDefinitionFactory
 */
@CustomerAccessible(immutable = true)
public enum ChannelType 
{
	/**
	 * Data type of the channel Data Number is unknown or undefined.
	 */
	UNKNOWN,
	/**
	 * Data type of the channel Data Number is signed integer.
	 */
	SIGNED_INT,
	/**
	 * Data type of the channel Data Number is unsigned integer.
	 */
	UNSIGNED_INT,
	/**
	 * Data type of the channel Data Number is an unsigned integer
	 * and channel value display should be formatted in hex.
	 */
	DIGITAL,
	/**
	 * Data type of the channel Data Number is a signed integer that
	 * can be mapped to a symbolic state. (i.e., an enum)
	 */
	STATUS,
	/**
	 * Data type of the channel Data Number is an IEEE floating point number.
	 */
	FLOAT,
	/**
	 * Data type of the channel Data Number is a string of ASCII characters.
	 */
	ASCII,

	/**
	 * Data type of the channel Data Number is unsigned integer, for which non-zero
	 * values can be considered a TRUE value, and 0 values can be considered a FALSE
	 * value. 
	 */
	BOOLEAN,
	/**
	 * Data type of the channel is a coarse SCLK time.
	 */
	TIME;
	
    /**
     * Indicates if the current channel type represents a numeric type. This means 
     * that the value of the channel is an integer or float type.  By this definition,
     * BOOLEAN and STATUS channels are also numeric channels.
     * @return true if this is a numeric type
     */
    public boolean isNumberType() {
        return !this.equals(ChannelType.UNKNOWN) && !this.equals(ChannelType.ASCII);
    }

    /**
     * Indicates if the current channel type represents a string type.
     * @return true if this is a string type
     */
    public boolean isStringType() {
        return this.equals(ChannelType.ASCII);
    }

    /**
     * Indicates if the current channel type represents a numeric integral type. This
     * means the value of the channel is an integer. By this definition, BOOLEAN and STATUS
     * channels are also integral channels.
     * 
     * @return true if this is an integral type
     */
    public boolean isIntegralType() {
        return this.equals(ChannelType.SIGNED_INT) ||
        this.equals(ChannelType.UNSIGNED_INT) ||
        this.equals(ChannelType.DIGITAL) ||
        this.equals(ChannelType.STATUS) ||
        this.equals(ChannelType.BOOLEAN) ||
        this.equals(ChannelType.TIME);
    }
    
    /**
     * Indicates if the current channel type has an associated state table
     * that can be used to map the integer data number to a symbolic string state.
     * By this definition, STATUS and BOOLEAN channels qualify.
     * 
     * @return true if this channel type has an enumeration
     */
    public boolean hasEnumeration() {
        return this.equals(ChannelType.STATUS) ||
        this.equals(ChannelType.BOOLEAN);
    }

    /**
     * Gets a unique one character identifier of the channel type for use in reports.
     * 
     * @return one character string indicating the channel type
     */
    public String getBriefChannelType() {
    	switch(this) {
    		case SIGNED_INT:
    			return "I";
    		case UNSIGNED_INT:
    			return "U";
    		case FLOAT:
    			return "F";
    		case ASCII:
    			return "A";
    		case STATUS:
    			return "S";
    		case DIGITAL:
    			return "M";
    		case BOOLEAN:
    		    return "B";
    		case TIME:
                return "T";
    		default:
    			return "X";
    	}
    }
}