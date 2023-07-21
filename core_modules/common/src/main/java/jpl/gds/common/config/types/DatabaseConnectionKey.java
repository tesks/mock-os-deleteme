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
package jpl.gds.common.config.types;

import java.util.ArrayList;
import java.util.List;

/**
 * A class containing session search/key information needed to configure a
 * DATABASE downlink connection. At this time, this consists of a list of
 * session keys and a list of host patterns.
 * 
 *
 */
public class DatabaseConnectionKey {
    /**
     * The list of patterns for the session hosts. (pattern implies this value
     * is used in the SQL "LIKE" statement and so it may contain wildcards such
     * as "%")
     */
    private List<String> hostPatternList;

    /**
     * The list of unique session IDs (numeric key)
     */
    private List<Long> keyList;

    /**
     * Creates an instance of DatabaseConnectionKey.
     */
    public DatabaseConnectionKey() {
        this.hostPatternList = new ArrayList<String>(1);
        this.keyList = new ArrayList<Long>(1);
    }

    /**
     * Gets the session key list.
     *
     * @return Returns list of Long session keys, never empty
     */
    public List<Long> getSessionKeyList() {
        return this.keyList;
    }

    /**
     * Adds a session key to the key list.
     *
     * @param key
     *            the session key to add
     */
    public void addSessionKey(final Long key) {
        if (key == null) {
            throw new IllegalArgumentException(
                    "The input session key cannot be null");
        }

        if (!this.keyList.contains(key)) {
            this.keyList.add(key);
        }
    }

    /**
     * Sets a single session key, clearing any current keys on the key list.
     *
     * @param key
     *            the session key to set
     */
    public void setSessionKey(final Long key) {
        if (key == null) {
            throw new IllegalArgumentException(
                    "The input session key cannot be null");
        }

        keyList.clear();
        keyList.add(key);
    }

    /**
     * Adds a range of session keys to the key list. Every key between keyStart
     * and keyEnd, inclusive, will be added.
     *
     * @param keyStart
     *            session key start
     * @param keyEnd
     *            session key end; must be >= keyStart
     */
    public void addSessionKeyRange(final Long keyStart, final Long keyEnd) {
        if (keyStart == null || keyEnd == null) {
            throw new IllegalArgumentException(
                    "The input session key cannot be null");
        }
        if (keyEnd < keyStart) {
            throw new IllegalArgumentException(
                    "The end session key must be >= the startKey");
        }
        for (long l = keyStart; l <= keyEnd; l++) {
            addSessionKey(Long.valueOf(l));
        }
    }

    /**
     * Sets the entire key list.
     *
     * @param keyList
     *            The List of keys to set.
     */
    public void setSessionKeyList(final List<Long> keyList) {
        if (keyList == null) {
            throw new IllegalArgumentException(
                    "The list of session keys is not allowed to be null.");
        }

        this.keyList = new ArrayList<Long>(keyList);
    }

    /**
     * Try to get the first key in the supplied DatabaseConnectionKey object,
     * and return a default if not possible (the key list is empty).
     *
     * @param connectionKey
     *            object to get the key from
     * @param defalt
     *            Default session key
     *
     * @return session key
     */
    public static long getSessionKey(final DatabaseConnectionKey connectionKey,
            final long defalt) {
        if ((connectionKey == null) || connectionKey.keyList.isEmpty()) {
            return defalt;
        }

        final Long key = connectionKey.keyList.get(0);

        if ((key == null) || (key < 0L)) {
            return defalt;
        }

        return key;
    }

    /**
     * Gets the session host pattern list.
     *
     * @return List of host pattern Strings
     */
    public List<String> getHostPatternList() {
        return this.hostPatternList;
    }

    /**
     * Try to get the first session host pattern in the supplied
     * DatabaseConnectionKey object, and return a default if not possible (the
     * host pattern list is empty).
     *
     * @param connectionKey
     *            object to get the key from
     * @param defalt
     *            Default value
     *
     * @return Returns the best guess at the host
     */
    public static String getHostPattern(
            final DatabaseConnectionKey connectionKey, final String defalt) {
        if ((connectionKey == null) || connectionKey.hostPatternList.isEmpty()) {
            return defalt;
        }

        final String host = connectionKey.hostPatternList.get(0);

        if ((host == null) || (host.trim().length() == 0)) {
            return defalt;
        }

        return host;
    }

    /**
     * Adds a session host pattern to the host pattern list. The pattern may
     * contain SQL wildcards.
     *
     * @param hostPattern
     *            Host pattern
     */
    public void addHostPattern(final String hostPattern) {
        if (hostPattern == null) {
            throw new IllegalArgumentException(
                    "The input host pattern cannot be null");
        }

        if (!this.hostPatternList.contains(hostPattern)) {
            this.hostPatternList.add(hostPattern);
        }
    }

    /**
     * Sets a single session host pattern, clearing any others currently on the
     * host pattern list. The pattern may contain SQL wildcards.
     *
     * @param hostPattern
     *            Host pattern
     */
    public void setHostPattern(final String hostPattern) {
        if (hostPattern == null) {
            throw new IllegalArgumentException(
                    "The input host pattern cannot be null");
        }

        hostPatternList.clear();

        hostPatternList.add(hostPattern);
    }

    /**
     * Sets the entire host pattern list
     *
     * @param hostPatternList
     *            The List of patterns to set.
     */
    public void setHostPatternList(final List<String> hostPatternList) {
        if (hostPatternList == null) {
            throw new IllegalArgumentException(
                    "The list of host patterns cannot be null.");
        }

        this.hostPatternList = hostPatternList;
    }

}
