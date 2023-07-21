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
package jpl.gds.product.api.builder;

/**
 * An exception thrown for problems with storing and retrieving product builder 
 * transaction information.
 * 
 */
@SuppressWarnings("serial")
public class ProductStorageException extends Exception {
    
    /**
     * Creates an instance of ProductStorageException.
     * @param message the detailed error message
     */
    public ProductStorageException(String message) {
        super(message);
    }
    
    /**
     * Creates an instance of ProductStorageException with a known
     * root cause.
     * @param message the detailed error message
     * @param cause the triggering exception
     */
    public ProductStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
