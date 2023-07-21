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
package jpl.gds.shared.directive;

/**
 * An enumeration indicating the status of a process control directive.
 * 
 * @since R8
 *
 */
public enum DirectiveStatus {
    /**
     * Directive was processed successfully. 
     */
    SUCCESS,
    
    /**
     * Directive argument was invalid.
     */
    INVALID_ARGUMENT_ERROR,
    
    /**
     * Directive attempted to operate on a non-existent service instance or object.
     */
    NO_SUCH_INSTANCE,
    
    /**
     * Directive could not be executed due to a processing error.
     */
    EXECUTION_ERROR,
    
    /**
     * Directive was missing a required argument.
     */
    MISSING_ARGUMENT_ERROR, 
    
    /**
     * Directive attempted to instantiate a duplicate object or service instance.
     */
    DUPLICATE_INSTANCE, 
    
    /**
     * URI entered for the directive was invalid.
     */
    INVALID_URI;
    
}
