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
package jpl.gds.tc.api.icmd.exception;

import java.io.IOException;

import org.restlet.data.Status;

import jpl.gds.common.error.ErrorCode;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.tc.api.UplinkFailureReason;
import jpl.gds.tc.api.exception.RawOutputException;
import jpl.gds.tc.api.exception.ScmfWrapUnwrapException;
import jpl.gds.tc.api.exception.ScmfParseException;
import jpl.gds.tc.api.exception.UplinkException;

/**
 * This class manages the mapping between Exception/Failure reasons to error
 * status codes for Integrated Commanding.
 * 
 * @since AMPCS R5
 * 
 * 12/2/13  - MPCS-4483: ICmdErrorManager extends AmpcsErrorManager
 */
public class ICmdErrorManager {

	/* 12/2/13 - MPCS-4483: Move ICmd error codes to new ErrorCode
	 * enum. */
	
    /**
     * Get the error code corresponding to the ICmdException
     * @param e the ICmdException to map to an error code
     * @return an error code corresponding to the specified ICmdException
     * 
     * 12/2/13 - MPCS-4483: Get error codes from enum
     */
    public static int getErrorCode(ICmdException e) {
        int errorCode = 0;

        if (e instanceof AuthenticationException) {
            errorCode = ErrorCode.AUTHENTICATION_ERROR_CODE.getNumber();
        } else if (e instanceof AuthorizationException) {
            errorCode = ErrorCode.AUTHORIZATION_ERROR_CODE.getNumber();
        } else if (e instanceof CpdConnectionException) {
            errorCode = ErrorCode.CPD_CONNECTION_ERROR_CODE.getNumber();
        } else if (e instanceof CpdException) {
            errorCode = ErrorCode.CPD_ERROR_CODE.getNumber();
        } else {
            errorCode = ErrorCode.GENERIC_ICMD_ERROR_CODE.getNumber();
        }

        return errorCode;
    }

    /**
     * Get the error code corresponding to the UplinkFailureReason
     * @param e the UplinkFailureReason to map to an error code
     * @return an error code corresponding to the specified UplinkFailureReason
     * 
     * 12/2/13  - MPCS-4483: Get error codes from enum
     */
    public static int getErrorCode(UplinkFailureReason e) {
        int errorCode = 0;

        if (e.equals(UplinkFailureReason.AUTHENTICATION_ERROR)) {
            errorCode = ErrorCode.AUTHENTICATION_ERROR_CODE.getNumber();
        } else if (e.equals(UplinkFailureReason.AUTHORIZATION_ERROR)) {
            errorCode = ErrorCode.AUTHORIZATION_ERROR_CODE.getNumber();
        } else if (e.equals(UplinkFailureReason.AMPCS_SEND_FAILURE)) {
            errorCode = ErrorCode.CPD_CONNECTION_ERROR_CODE.getNumber();
        } else if (e.equals(UplinkFailureReason.COMMAND_SERVICE_REJECTION)) {
            errorCode = ErrorCode.CPD_ERROR_CODE.getNumber();
        } else if (e.equals(UplinkFailureReason.UNKNOWN)
                || e.equals(UplinkFailureReason.AMPCS_SEND_FAILURE)) {
            errorCode = ErrorCode.GENERIC_ICMD_ERROR_CODE.getNumber();
        }

        return errorCode;
    }

    /* Added to support MPCS-4974 */
    /**
     * Get a corresponding Restlet status object that matches the Exception
     * object
     * 
     * @param e the exception object to match to a Restlet status object
     * @return the Restlet status object that matches the exception
     */
    public static Status getRestletStatus(Exception e) {
        Status restletStatus = Status.SERVER_ERROR_INTERNAL;
        if (e instanceof ICmdException) {
            ICmdException iCmdEx = (ICmdException) e;
            restletStatus = getRestletStatus(iCmdEx);
        } else if (e instanceof RawOutputException) {
            restletStatus = Status.SERVER_ERROR_INTERNAL;
        } else if (e instanceof ScmfParseException) {
            restletStatus = Status.CLIENT_ERROR_BAD_REQUEST;
        } else if (e instanceof ScmfWrapUnwrapException) {
            restletStatus = Status.CLIENT_ERROR_BAD_REQUEST;
        } else if (e instanceof UplinkException) {
            UplinkFailureReason failReason =
                    ((UplinkException) e).getUplinkResponse()
                            .getFailureReason();
            restletStatus = getRestletStatus(failReason);
        } else if (e instanceof DictionaryException) {
            restletStatus = Status.SERVER_ERROR_INTERNAL;
        } else if (e instanceof IOException) {
            restletStatus = Status.SERVER_ERROR_INTERNAL;
        }

        return restletStatus;
    }

    /* 12/2/13  - MPCS-4483: Get error codes from enum */
    private static Status getRestletStatus(ICmdException iCmdEx) {
        Status restletStatus = Status.SERVER_ERROR_INTERNAL;

        if (getErrorCode(iCmdEx) == ErrorCode.BAD_REQUEST_ERROR_CODE.getNumber()) {
            restletStatus = Status.CLIENT_ERROR_BAD_REQUEST;
        } else if (getErrorCode(iCmdEx) == ErrorCode.AUTHENTICATION_ERROR_CODE.getNumber()) {
            restletStatus = Status.CLIENT_ERROR_UNAUTHORIZED;
        } else if (getErrorCode(iCmdEx) == ErrorCode.AUTHORIZATION_ERROR_CODE.getNumber()) {
            restletStatus = Status.CLIENT_ERROR_FORBIDDEN;
        } else if (getErrorCode(iCmdEx) == ErrorCode.GENERIC_ICMD_ERROR_CODE.getNumber()) {
            restletStatus = Status.SERVER_ERROR_INTERNAL;
        } else if (getErrorCode(iCmdEx) == ErrorCode.CPD_ERROR_CODE.getNumber()) {
            restletStatus = Status.SERVER_ERROR_BAD_GATEWAY;
        } else if (getErrorCode(iCmdEx) == ErrorCode.CPD_CONNECTION_ERROR_CODE.getNumber()) {
            restletStatus = Status.SERVER_ERROR_GATEWAY_TIMEOUT;
        }

        return restletStatus;
    }

    /* 12/2/13  - MPCS-4483: Get error codes from enum */
    private static Status getRestletStatus(UplinkFailureReason failReason) {
        Status restletStatus = Status.SERVER_ERROR_INTERNAL;

        if (getErrorCode(failReason) == ErrorCode.BAD_REQUEST_ERROR_CODE.getNumber()) {
            restletStatus = Status.CLIENT_ERROR_BAD_REQUEST;
        } else if (getErrorCode(failReason) == ErrorCode.AUTHENTICATION_ERROR_CODE.getNumber()) {
            restletStatus = Status.CLIENT_ERROR_UNAUTHORIZED;
        } else if (getErrorCode(failReason) == ErrorCode.AUTHORIZATION_ERROR_CODE.getNumber()) {
            restletStatus = Status.CLIENT_ERROR_FORBIDDEN;
        } else if (getErrorCode(failReason) == ErrorCode.GENERIC_ICMD_ERROR_CODE.getNumber()) {
            restletStatus = Status.SERVER_ERROR_INTERNAL;
        } else if (getErrorCode(failReason) == ErrorCode.CPD_ERROR_CODE.getNumber()) {
            restletStatus = Status.SERVER_ERROR_BAD_GATEWAY;
        } else if (getErrorCode(failReason) == ErrorCode.CPD_CONNECTION_ERROR_CODE.getNumber()) {
            restletStatus = Status.SERVER_ERROR_GATEWAY_TIMEOUT;
        }

        return restletStatus;
    }
}
