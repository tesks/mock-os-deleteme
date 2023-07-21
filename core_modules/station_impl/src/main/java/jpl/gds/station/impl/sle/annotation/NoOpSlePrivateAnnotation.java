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
package jpl.gds.station.impl.sle.annotation;

import jpl.gds.station.api.sle.annotation.ISlePrivateAnnotation;

import java.util.Collections;
import java.util.Map;

/**
 * SLE spec defines an optional space for additional metadata, called a "private annotation." Many missions choose not
 * to use a private annotation, so this implementation does not load or parse anything out of the buffer.
 *
 */
public class NoOpSlePrivateAnnotation implements ISlePrivateAnnotation {

    /**
     * Size of a private annotation may vary by mission, so implementations need to specify the expected length
     */
    @Override
    public int getPrivateAnnotationSizeBytes() {
        return 0;
    }

    /**
     * Load a buffer of the appropriate size
     *
     * @param buffer
     * @param start
     */
    @Override
    public void load(final byte[] buffer, final int start) {

    }

    @Override
    public byte[] getBytes() {
        return new byte[0];
    }

    @Override
    public byte[] getBytes(int start, int length) {
        return new byte[0];
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void setValid(boolean valid) {
        // no op
    }

    @Override
    public void setPresent(boolean present) {
        // no op
    }

    @Override
    public boolean isPresent() {
        return false;
    }

    @Override
    public Map<String, String> getMetadata(){
        return Collections.EMPTY_MAP;
    }
}