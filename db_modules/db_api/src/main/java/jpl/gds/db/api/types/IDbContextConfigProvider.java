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

package jpl.gds.db.api.types;

/**
 * Interface for context configuration provider
 */
public interface IDbContextConfigProvider extends IDbQueryable{

    //we have getSessionId() from ancestor IDbAccessItem

    /**
     * Get type
     *
     * @return Returns the type.
     */
    String getType();

    /**
     * Get user
     *
     * @return Returns the user.
     */
    String getUser();

    /**
     * Get name
     *
     * @return Returns the context name.
     */
    String getName();

    /**
     * Parent context Id, optional
     * @return Parent ID
     */
    long getParentId();

    /**
     * Get MPCS version
     *
     * @return Returns the mpcsVersion.
     */
    String getMpcsVersion();

    /**
     * Get the value based on the key
     * @param key Key
     * @return The value
     */
    String getValue(String key);
}