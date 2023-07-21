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
package jpl.gds.shared.formatting;

import java.util.Date;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.Sclk;

/**
 * A data type enumeration used by the SprintFormat class.
 * 
 * @see SprintfFormat
 *
 * DO NOT change the order of enums without direction from the CogE
 *
 */
public enum SprintfDataType {
    	// Do not change the bracketing values of UNKNOWN and INVALID.

        /** Unknown data type */
		UNKNOWN, 

        /** Enum data type */
		ENUM, 

        /** String data type */
		STRING, 

        /** Character data type */
		CHAR, 

        /** Byte data type */
		BYTE, 

        /** Short data type */
		SHORT, 

        /** Hexadecimal data type */
		HEXADECIMAL, 

        /** Octal data type */
		OCTAL, 

        /** Integer data type */
		INT, 

        /** Long data type */
		LONG, 

        /** Float data type */
		FLOAT, 

        /** Double data type */
		DOUBLE, 

        /** Pointer data type */
		POINTER, 

        /** Time data type */
		TIME, 

        /** Invalid data type */
		INVALID;

		
		/**
		 * Returns the SprintDataType that maps to the given object.
		 * @param value Value
		 * @return Data type
		 */
		public static SprintfDataType getTypeForObject(Object value) {
            SprintfDataType type = SprintfDataType.UNKNOWN;
            if (value instanceof Long) {
                type = SprintfDataType.LONG;
            } else if (value instanceof Integer) {
                type = SprintfDataType.INT;
            } else if (value instanceof Float) {
                type = SprintfDataType.FLOAT;
            } else if (value instanceof Double) {
                type = SprintfDataType.DOUBLE;
            } else if (value instanceof Byte) {
                type = SprintfDataType.BYTE;
            } else if (value instanceof Short) {
                type = SprintfDataType.SHORT;
            } else if (value instanceof String) {
                type = SprintfDataType.STRING;
            } else if (value instanceof Character) {
            	type = SprintfDataType.CHAR;
            } else if (value instanceof Sclk) { 
                type = SprintfDataType.TIME;
            } else if (value instanceof IAccurateDateTime) { 
                type = SprintfDataType.TIME;
            } else if (value instanceof ILocalSolarTime) { 
                type = SprintfDataType.TIME;
            } else if (value instanceof Date) { 
                type = SprintfDataType.TIME;
            }
            
            return type;
		}


        /**
         * Get desired type for format character and value type.
         *
         * @param formatChar Format character
         * @param valueType  Type of value
         *
         * @return Data type
         */		
		public static SprintfDataType getDesiredType(char formatChar, SprintfDataType valueType) {
			
			SprintfDataType theDesiredType = SprintfDataType.UNKNOWN;
			
			switch(formatChar) {
			case 'b': // desire an enum value
				theDesiredType = SprintfDataType.ENUM;
				break;
			case 'c': //desire a character value
				switch(valueType) {
				case BYTE: 
				case INT:
				case SHORT:
				case LONG:
					theDesiredType = valueType;
					break;
				default: 
					theDesiredType = SprintfDataType.STRING;
				}          
				break;
			case 'U':
			case 'u': // desire an unsigned value
			case 'd': // desire int value
            case 'i':
				theDesiredType = SprintfDataType.INT;
				if (SprintfDataType.LONG == valueType) {
					theDesiredType = valueType;
				}
				break;
			case 'E':
			case 'e': // desire floating point value
				theDesiredType = SprintfDataType.INT; // This will force an error
				// if a good type is not
				// supplied.
				if ((SprintfDataType.FLOAT == valueType) || (SprintfDataType.DOUBLE == valueType)) {
					theDesiredType = valueType;
				}
				break;
			case 'f':  
			case 'g' :
			case 'G' : // desire floating point value
				if ((SprintfDataType.FLOAT == valueType) || (SprintfDataType.DOUBLE == valueType)) {
					theDesiredType = valueType;
				}
				break;
			case 'n': // desire number of characters output. Not implemented  
				break;
			case 'o': // desire octal value
				theDesiredType = SprintfDataType.OCTAL;
				break;
			case 'p': // processing pointer values
				theDesiredType = SprintfDataType.POINTER;
				break;
			case 's': // desire string value
				theDesiredType = SprintfDataType.STRING;
				break;
			case 't': // desire time value
				theDesiredType = SprintfDataType.TIME;
				break;
			case 'X':
			case 'x': // desire hexadecimal value
				theDesiredType = SprintfDataType.HEXADECIMAL;
				break;
            default:
			    TraceManager.getDefaultTracer().error(
			            "Invalid format character of: " + formatChar);
			} // end switch
			
			return theDesiredType;
		}


        /**
         * Get is-floating status.
         *
         * @return Is-floating status
         */
		public boolean isFloatingPoint() {
			return this == DOUBLE || this == FLOAT;
		}
}
