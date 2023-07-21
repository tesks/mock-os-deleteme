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
package jpl.gds.eha.channel.api;

import jpl.gds.shared.annotation.CustomerAccessible;

/**
 * InvalidChannelValueException is thrown when attempting to create a channel
 * sample with a value whose data type does not match the declared data type
 * of the channel in the telemetry dictionary.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 *  
 *
 */
@CustomerAccessible(immutable = true)
public final class InvalidChannelValueException extends RuntimeException {
  
    private static final long serialVersionUID = 5435487980850028651L;

    /**
     * Creates an instance of InvalidChannelValueException.
     * @param message the detailed error message
     */
    public InvalidChannelValueException(final String message) {
        super(message);
    }

    /**
     * Creates an instance of InvalidChannelValueException.
     * @param message the detailed error message
     * @param cause the exception that triggered this one
     */
    public InvalidChannelValueException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
