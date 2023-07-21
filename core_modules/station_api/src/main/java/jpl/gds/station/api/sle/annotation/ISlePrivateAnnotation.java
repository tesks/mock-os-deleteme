/*
 * Copyright 2006-2021. California Institute of Technology.
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
package jpl.gds.station.api.sle.annotation;

import java.util.Map;

/**
 * Interface defining the methods available for interacting with an SLE Private Annotation
 *
 */
public interface ISlePrivateAnnotation {

    /**
     * Size of a private annotation may vary by mission, so implementations need to specify the expected length
     */
    int getPrivateAnnotationSizeBytes();

    /**
     * Load data in from the buffer, starting at the given index
     */
    void load(byte[] buffer, int start);

    /**
     * Return raw bytes
     *
     * @return byte array
     */
    byte[] getBytes();

    /**
     * Return bytes from start, to length
     *
     * @param start  start index
     * @param length length
     * @return byte array
     */
    byte[] getBytes(int start, int length);

    /**
     * Indicates validity of private annotations
     *
     * @return
     */
    boolean isValid();

    /**
     * Sets validity of private annotations
     *
     * @param valid
     */
    void setValid(boolean valid);

    /**
     * Sets presence of private annotations
     *
     * @param present
     */
    void setPresent(boolean present);

    /**
     * Indicates presence of private annotations
     */
    boolean isPresent();

    /**
     * Get metadata formatted as key value in readable format
     * @return KeyValueAttributes SLE metadata
     */
    public Map<String, String> getMetadata();
}