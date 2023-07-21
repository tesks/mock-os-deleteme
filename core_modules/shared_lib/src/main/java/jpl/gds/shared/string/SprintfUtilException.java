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
package jpl.gds.shared.string;

/**
 * This exception is throw to indicate errors found by methods in
 * SprintfUtil.
 * 
 * @since AMPCS 6.1
 */
public class SprintfUtilException extends Exception {
	
	   private static final long serialVersionUID = 0L;

	    /**
	     * Constructor.
	     */
	    public SprintfUtilException() {
	        super();
	    }

	    /**
	     * Constructor.
	     *
	     * @param message Message text
	     */
	    public SprintfUtilException(String message) {
	        super(message);
	    }

	    /**
	     * Constructor.
	     *
	     * @param message   Message text
	     * @param rootCause Underlying cause
	     */
	    public SprintfUtilException(String message, Throwable rootCause) {
	        super(message, rootCause);
	    }

}
