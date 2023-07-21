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
/**
 * 
 */
package jpl.gds.product.api.file;

import java.io.IOException;

/**
 * An exception thrown when there are issues managing product filenames.
 * 
 */
public class ProductFilenameException extends IOException {
	private static final long	serialVersionUID	= 4019581533253149380L;

	/**
	 * Constructor.
	 */
	public ProductFilenameException() {
		// empty
	}

	/**
	 * Constructor.
	 * 
	 * @param paramString detailed error message
	 */
	public ProductFilenameException(final String paramString) {
		super(paramString);
	}

	/**
	 * Constructor.
	 * 
	 * @param paramThrowable the throwable that triggered this exception
	 */
	public ProductFilenameException(final Throwable paramThrowable) {
		super(paramThrowable);
	}

	/**
     * Constructor.
     * 
     * @param paramString detailed error message
     * @param paramThrowable the throwable that triggered this exception
     */
	public ProductFilenameException(final String paramString, final Throwable paramThrowable) {
		super(paramString, paramThrowable);
	}
}
