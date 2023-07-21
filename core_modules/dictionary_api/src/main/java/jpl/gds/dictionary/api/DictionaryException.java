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
package jpl.gds.dictionary.api;

/**
 * DictionaryException is thrown by Dictionary classes when an error occurs
 * accessing or parsing the dictionary file. No other exceptions should be
 * thrown by Dictionary parsing methods. <br>
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 */
@SuppressWarnings("serial")
public class DictionaryException extends Exception
{
    /**
	 * Basic constructor.
	 */
	public DictionaryException()
	{
		super();
	}

	/**
	 * Creates a DictionaryException with the given triggering Throwable.
	 * 
	 * @param cause the Throwable that triggered this exception
	 */
	public DictionaryException(final Throwable cause)
	{
		super(cause);
	}

	/**
     * Creates an instance of DictionaryException with the given detail message.
     * 
     * @param message the detailed error message
     */
     public DictionaryException(final String message) {
    	 super(message);
     }
     
     /**
     * Creates an instance of DictionaryException with the given detail message and
     * triggering Throwable.
     * 
     * @param message the detailed error message
     * @param cause the Throwable that triggered this exception
     */
    public DictionaryException(final String message, final Throwable cause) {
    	 super(message, cause);
     }
}
