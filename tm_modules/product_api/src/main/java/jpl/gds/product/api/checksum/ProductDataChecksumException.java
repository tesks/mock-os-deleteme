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
package jpl.gds.product.api.checksum;

/**
 * Common exception for all product data checksum problems.
 * 
 */
public class ProductDataChecksumException extends Exception
{
    private static final long serialVersionUID = 0L;


    /**
     * Creates an instance of ProductDataChecksumException.
     */
    public ProductDataChecksumException()
    {
        super();
    }


    /**
     * Creates an instance of ProductDataChecksumException.
     * 
     * @param message The associated error message
     */
    public ProductDataChecksumException(final String message)
    {
        super(message);
    }


    /**
     * Creates an instance of ProductDataChecksumException.
     * 
     * @param message The associated error message
     * @param cause The exception that caused this one
     */
    public ProductDataChecksumException(final String    message,
                                        final Throwable cause)
    {
        super(message, cause);
    }


    /**
     * Creates an instance of ProductDataChecksumException.
     * 
     * @param cause The exception that caused this one.
     */
    public ProductDataChecksumException(final Throwable cause)
    {
        super(cause);
    }
}
