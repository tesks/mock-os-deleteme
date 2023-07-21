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

import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.template.Templatable;

/**
 * Interface for all datatypes representing data queried from database.
 */
public interface IDbQueryable extends IDbAccessItem, Templatable
{
    /**
     * Set session id.
     *
     * @param sessionId Session id
     */
	void setSessionId(final Long sessionId);

    /**
     * Set context id.
     *
     * @param contextId Context id
     */
    void setContextId(final Long contextId);

    /**
     * Set session fragment.
     *
     * @param fragment Session fragment
     */
	void setSessionFragment(
        final SessionFragmentHolder fragment);

    /**
     * Set context host
     *
     * @param contextHost Context host
     */
    void setContextHost(final String contextHost);

    /**
     * Set session host
     *
     * @param sessionHost Session host
     */
    void setSessionHost(final String sessionHost);

    /**
     * Set context host id.
     *
     * @param contextHostId Context host id
     */
    void setContextHostId(final Integer contextHostId);

    /**
     * Set record bytes
     *
     * @param bytes Record bytes
     */
	void setRecordBytes(final byte[] bytes);
	
    /**
     * Set record offset.
     *
     * @param offset Record offset
     */
	void setRecordOffset(final Long offset);

    /**
     * Sets the Host ID for this IDbQueryable
     * 
     * @param sessionHostId
     *            the Host ID to set
     */
    void setSessionHostId(Integer sessionHostId);

    /**
     * Set record bytes length. This is used to set the length when we do
     * not have a body.
     *
     * @param length
     *            Record bytes length
     */
    void setRecordBytesLength(int length);

    /**
     * Sets the DSS ID for this IDbQueryable
     * 
     * @param dssId
     *            the DSS ID to set
     */
    void setRecordDssId(int dssId);

    /**
     * Set the DSS ID for the current session
     * 
     * @param dssId
     *            the DSS ID to set
     */
    void setSessionDssId(int dssId);
}
