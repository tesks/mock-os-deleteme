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

import java.util.Map;

import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.interfaces.ICsvSupport;

public interface IDbAccessItem extends IDbRecord, ICsvSupport {

    /**
     * @return the Session ID as a Long
     */
	Long getSessionId();

    /**
     * @return the Context ID as a Long
     */
    Long getContextId();

	/**
     * @return the Session Host Name
     */
	String getSessionHost();

    /**
     * @return the Context Host Name
     */
    String getContextHost();

    /**
     * Get session host id.
     *
     * @return Session host id
     */
    Integer getSessionHostId();

    /**
     * Get context host id.
     *
     * @return Context host id
     */
    Integer getContextHostId();

    /**
     * @return the Session Fragment for this Item
     */
	SessionFragmentHolder getSessionFragment();

	/**
     * Return DSS id from Session.
     *
     * @return DSS id from Session or NULL if none exists
     */
    Integer getSessionDssId();

    /**
     * Return VCID from Session.
     *
     * @return VCID from Session
     */
    Integer getSessionVcid();

    /**
     * Getter for header holder.
     *
     * @return Header holder
     */
    HeaderHolder getRawHeader();

    /**
     * Getter for trailer holder.
     *
     * @return Trailer holder
     */
    TrailerHolder getRawTrailer();

    /**
     * Returns a comma-separated representation of the final fields.
     * (Used to return session DSS and session VCID>)
     *
     * @return Final fields
     */
    String getPartialSessionCsvHeader();

    /**
     * Convert Session.vcid int to proper output form.
     *
     * @return Session.vcid in proper output form.
     */
    String getTransformedVcid();

    /**
     * Convert Session.vcid string to its int
     *
     * @param str String id to convert
     *
     * @return Session.vcid in proper int form.
     */
    int getTransformedStringId(String str);

    /**
     * Returns a map of data to be displayed to various output files.
     *
     * @param NO_DATA is the string to be used to represent no data
     *
     * @return Populated map
     */
    public abstract Map<String, String> getFileData(final String NO_DATA);
}