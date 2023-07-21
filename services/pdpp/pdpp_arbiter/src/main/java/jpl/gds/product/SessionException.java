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
package jpl.gds.product;

public class SessionException extends Exception {
	private static final long serialVersionUID = 4061192370090541722L;

	public SessionException() {
        // TODO Auto-generated constructor stub
    }

    public SessionException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public SessionException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public SessionException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public SessionException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        // TODO Auto-generated constructor stub
    }
}
