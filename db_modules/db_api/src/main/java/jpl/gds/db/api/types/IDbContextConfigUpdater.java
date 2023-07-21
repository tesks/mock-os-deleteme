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

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.dictionary.api.config.DictionaryProperties;

/**
 * Interface for context configuration updater
 */
public interface IDbContextConfigUpdater extends IDbContextConfigProvider {

    //we have setSessionId() from ancestor IDbQueryable

    /**
     * Sets the type
     *
     * @param type The type to set.
     */
    void setType(String type);

    /**
     * Sets the user
     *
     * @param user The user to set.
     */
    void setUser(String user);

    /**
     * Sets the name
     *
     * @param name The context name to set.
     */
    void setName(String name);

    /**
     * Sets the parent context ID
     * @param parentId Parent ID
     */
    void setParentId(long parentId);

    /**
     * Sets the mpcsVersion
     *
     * @param mpcsVersion The mpcsVersion to set.
     */
    void setMpcsVersion(String mpcsVersion);

    /**
     * Sets database context configuration information into a ContextIdentification object.
     *
     * @param si SessionIdentification to set values into
     *
     */
    void setIntoContextIdentification(IContextIdentification si);

    /**
     * Sets the members of this object into the given context configuration object.
     *
     * @param cc ContextConfiguration to set values into
     *
     */
    void setIntoContextConfiguration(IContextConfiguration cc);

    /**
     * Sets the members of this object into the given dictionary configuration object.
     *
     * @param dc DictionaryConfiguration to set values into context configuration
     */
    void setIntoDictionaryConfiguration(DictionaryProperties dc);

    /**
     * Set value based on key
     * @param key The Key
     * @param val The Value
     */
    void setValue(String key, String val);
}
