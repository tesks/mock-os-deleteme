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
package jpl.gds.shared.time;

import jpl.gds.serialization.primitives.time.Proto3Adt;
import jpl.gds.shared.annotation.AssumesAccurateDateTimeIsDateObject;

import java.text.ParseException;

/**
 * Interface IAccurateDateTime
 */
public interface IAccurateDateTime extends IImmutableAccurateDateTime {
 
    /**
     * Sets this <code>date</code> object to represent a point in time that is
     * <code>time</code> milliseconds after January 1, 1970 00:00:00 GMT.
     *
     * @param time
     *            the number of milliseconds.
     */
    @AssumesAccurateDateTimeIsDateObject
    void setTime(long time);
    
    /**
     * Populate the time from a protobuf message.
     * 
     * @param msg
     *            the IAccurateDateTime in protobuf format
     */
    public void loadAccurateDateTime(Proto3Adt msg);

    void setTime(String timeStr) throws ParseException;
}
