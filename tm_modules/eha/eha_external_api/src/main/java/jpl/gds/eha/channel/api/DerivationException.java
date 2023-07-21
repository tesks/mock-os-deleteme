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
 * DerivationException is the exception to be thrown when an error occurs
 * during the execution of a channel derivation or its setup.
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
 *
 */
@SuppressWarnings("serial")
@CustomerAccessible(immutable = true)
public final class DerivationException extends Exception
{
    private final String parentId;
    private final String childId;
    private final String derivationId;

    
    /**
     * Creates an instance of DerivationException.
     *
     * @param message detailed error message
     * @param parent  the parent channel ID
     * @param child   the child channel ID
     */
    public DerivationException(final String    message,
                               final String parent,
                               final String child)
    {
    	super(message);

        parentId     = parent;
        childId      = child;
        derivationId = null;
    }


    /**
     * Creates an instance of DerivationException.
     *
     * @param message detailed error message
     * @param parent  the parent channel ID
     * @param child   the child channel ID
     * @param id      the unique ID of the derivation
     */
    public DerivationException(final String    message,
                               final String    parent,
                               final String    child,
                               final String    id)
    {
        super(message);

        parentId     = parent;
        childId      = child;
        derivationId = id;
    }


    /**
     * Creates an instance of DerivationException.
     *
     * @param message detailed error message
     */
    public DerivationException(final String message)
    {
        super(message);

        parentId     = null;
        childId      = null;
        derivationId = null;
    }


    /**
     * Creates an instance of DerivationException.
     *
     * @param message detailed error message
     * @param cause   original cause
     */
    public DerivationException(final String    message,
                               final Throwable cause)
    {
        super(message, cause);

        parentId     = null;
        childId      = null;
        derivationId = null;
    }


    /**
     * Gets the parent channel ID for the derivation. If there are multiple
     * parents, either the trigger parent or the first parent should be used.
     * 
     * @return the parent channel ID 
     */
    public String getParentId()
    {
    	return parentId;
    }


    /**
     * Gets the child channel ID (the ID of the channel being derived when the
     * error occurred).
     * 
     * @return the child channel ID
     */
    public String getChildId()
    {
    	return childId;
    }


    /**
     * Gets the unique derivation ID of the channel derivation generating this error.
     * 
     * @return the unique derivation ID
     */
    public String getDerivationId()
    {
    	return derivationId;
    }
}
