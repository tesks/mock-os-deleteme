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
package jpl.gds.common.error;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Central location for storing AMPCS error codes.  Each enumeration item has 
 * an associated number and message.
 * 
 */
public enum ErrorCode {
	/** No Error */
	NO_ERROR_CODE(0, "No Error"),
	
	/** Unknown Error */
	UNKNOWN_ERROR_CODE(-1, "Unknown Error"),
	
	/** Generic environment error */
	ENVIRONMENT_ERROR_CODE(10, "Generic Environment Error"),
	
	/** DISPLAY variable is not set for using X-windows */
	DISPLAY_VAR_ERROR_CODE(11, "Display variable is not set, unable to " +
			"initialize X-Windows"),
	
	//***********************************************************************//
	/** Generic parse error */
	PARSE_ERROR_CODE(30, "There was an error parsing the command line"),
	
	/** Missing option error */
	MISSING_OPTION_ERROR_CODE(31, "A required option has not been provided " +
			"on the command line"),
	
	/** Missing argument error */
	MISSING_ARGUMENT_ERROR_CODE(32, "An option on the command line is " +
			"missing a required value"),
	
	/** Unrecognized option error */
	UNRECOGNIZED_OPTION_ERROR_CODE(33, "An unrecognized option was seen on " +
			"the command line"),
	
	/** DSS ID parse error */
	PARSE_DSS_ERROR_CODE(34, "There was an error parsing the DSS ID on the " +
			"command line"),

	//***********************************************************************//
	/** Generic request error */
    BAD_REQUEST_ERROR_CODE(40, "The client made a bad request"),
    
    /** Authentication error */
    AUTHENTICATION_ERROR_CODE(41, "Authentication failed"),
    
    /** Authorization error */
    AUTHORIZATION_ERROR_CODE(43, "Authenticated user is not authorized to " +
    		"perform the intended action"),
    
    //***********************************************************************//
    /** Generic ICmd error */
    GENERIC_ICMD_ERROR_CODE(50, ""),
    
    /** CPD error */
    CPD_ERROR_CODE(52, "All AMPCS processing succeeded, but CPD reports an " +
    		"error"),
    
    /** CPD server error */
    CPD_CONNECTION_ERROR_CODE(54, "Unable to communicate with the CPD server"),
	
	//***********************************************************************//
	/** Generic session error */
    BAD_SESSION_ERROR_CODE(60, "The session could not be started"),
	
	/** Global LAD error */
	GLOBAL_LAD_ERROR_CODE(61, "The Global LAD could not be started"),
	
	/** Database error */
	DATABASE_ERROR_CODE(62, "The Life Of Mission database could not be " +
			"started"),
	
	/** Dictionary error */
	DICTIONARY_ERROR_CODE(63, "Error parsing dictionaries"),
	
	/** Session directory error */
	SESSION_OUTPUT_DIRECTORY_ERROR(64, "Unable to create session output " +
			"directory");
	
	
	private static Map<Integer, ErrorCode> lookup = 
			new HashMap<Integer, ErrorCode>();
    
	static {
    	for (ErrorCode code : EnumSet.allOf(ErrorCode.class)) {
    		lookup.put(code.getNumber(), code);
    	}
    }
	
    private final int code;
    private final String message;
    
    private ErrorCode(int code, String message) {
    	this.code = code;
    	this.message = message;
    }
    
    /**
     * Gets the associated number with this error
     * 
     * @return error code number
     */
    public int getNumber() {
    	return this.code;
    }
    
    /**
     * Gets the associated error message with this error
     * 
     * @return error message
     */
    public String getMessage() {
    	return this.message;
    }
    
    /**
     * Gets the ErrorCode enumeration item for the given code
     * 
     * @param code error code number
     * @return ErrorCode enumeration item. This method can never return null
     */
    public static ErrorCode get(int code) {
    	ErrorCode errorCode = lookup.get(code);
    	if (null == errorCode) {
    		errorCode = UNKNOWN_ERROR_CODE;
    	}
    	return errorCode;
    }
    
    @Override
    public String toString() {
    	return code + ": " + message;
    }
}
