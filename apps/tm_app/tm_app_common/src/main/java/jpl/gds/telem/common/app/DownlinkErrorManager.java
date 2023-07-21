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
package jpl.gds.telem.common.app;

import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.eclipse.swt.SWTError;

import jpl.gds.common.error.ErrorCode;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.globallad.GlobalLadException;
import jpl.gds.shared.exceptions.ApplicationException;
import jpl.gds.shared.exceptions.ParseDssException;
import jpl.gds.shared.metadata.InvalidMetadataException;

/**
 * General class for mapping exceptions to error codes.
 * 
 *
 */
public class DownlinkErrorManager {
	 
	/**
     * Get the error code corresponding to the given Error.
     * 
     * @param e the Error to map to an error code
     * @return an error code corresponding to the specified Error
     */
    public static int getErrorCode(Error e) {
        int errorCode = 1;

        if (e instanceof SWTError) {
            errorCode = ErrorCode.DISPLAY_VAR_ERROR_CODE.getNumber();
        }

        return errorCode;
    }
    
    /**
     * Get the error code corresponding to the Exception
     * 
     * @param e the Exception to map to an error code
     * @return a session error code corresponding to the given exception
     */
	public static int getSessionErrorCode(Exception e) {
        int errorCode = 1;

        if (e instanceof DictionaryException) {
        	errorCode = ErrorCode.DICTIONARY_ERROR_CODE.getNumber();
        }
        else if (e instanceof GlobalLadException) {
        	errorCode = ErrorCode.GLOBAL_LAD_ERROR_CODE.getNumber();
        }
        else if (e instanceof DatabaseException) {
        	errorCode = ErrorCode.DATABASE_ERROR_CODE.getNumber();
        }
        else if (e instanceof IllegalStateException) {
        	errorCode = ErrorCode.SESSION_OUTPUT_DIRECTORY_ERROR.getNumber();
        }
        else if (e instanceof ApplicationException) {
        	errorCode = ErrorCode.BAD_SESSION_ERROR_CODE.getNumber();
        	
        } else if (e instanceof InvalidMetadataException) {
            errorCode = ErrorCode.BAD_SESSION_ERROR_CODE.getNumber();
        }

        return errorCode;
    }
	
	/**
	 * Get the error code corresponding to the ParseException
	 * 
	 * @param e the ParseException to map to an error code
	 * @return a parse error code corresponding to the given exception
	 */
	public static int getDownlinkParseErrorCode(ParseException e) {
		int errorCode = 1;
		
		if (e instanceof MissingOptionException) {
			errorCode = ErrorCode.MISSING_OPTION_ERROR_CODE.getNumber();
		}
		else if (e instanceof MissingArgumentException) {
			errorCode = ErrorCode.MISSING_ARGUMENT_ERROR_CODE.getNumber();
		}
		else if (e instanceof UnrecognizedOptionException) {
			errorCode = ErrorCode.UNRECOGNIZED_OPTION_ERROR_CODE.getNumber();
		}
		else if (e instanceof ParseDssException) {
			errorCode = ErrorCode.PARSE_DSS_ERROR_CODE.getNumber();
		}
		else {
			errorCode = ErrorCode.PARSE_ERROR_CODE.getNumber();
		}
		
		return errorCode;
	}
}
