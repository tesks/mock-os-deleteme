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
package jpl.gds.product.api;

import jpl.gds.shared.exceptions.NestedException;

/**
 * 
 * ProductException is a general exception class thrown during product generation
 * to indicate an error in product processing.
 *
 *
 */
@SuppressWarnings("serial")
public class ProductException extends NestedException {
    /**
     * Creates an instance of ProductException.
     */
    public ProductException() {
        super();
    }

    /**
     * Creates an instance of ProductException with the given detail text.
     * @param message the exception message
     */
    public ProductException(String message) {
        super(message);
    }

    /**
     * Creates an instance of ProductException with the given detail text and triggering
     * Throwable.
     * @param message the exception message
     * @param rootCause the exception that Trigger this one
     */
    public ProductException(String message, Throwable rootCause) {
        super(message, rootCause);
    }
}
