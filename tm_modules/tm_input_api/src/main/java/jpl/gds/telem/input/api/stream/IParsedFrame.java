/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.telem.input.api.stream;

import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.station.api.IStationTelemHeader;

/**
 * Interface for a parsed transfer frame returned by a IFrameStreamParser,
 * with access to IStationTelemHeader, frame and trailer data
 *
 */
@CustomerAccessible(immutable = true)
public interface IParsedFrame {

    /**
     * Get station header object, containing info about header and frame
     * @return IStationTelemHeader object
     */
    IStationTelemHeader getHeader();

    /**
     * Get frame bytes only, without header info
     *
     * @return Frame bytes
     */
    byte[] getFrame();

    /**
     * Get trailer bytes only, without header info
     *
     * @return Trailer bytes
     */
    byte[] getTrailer();
}
