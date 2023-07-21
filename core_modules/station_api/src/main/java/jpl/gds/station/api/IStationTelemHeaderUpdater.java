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

package jpl.gds.station.api;

import jpl.gds.shared.io.BitOutputStream;

import java.io.IOException;

/**
 * Updater interface for IStationTelemHeader
 *
 * We don't want customers to be forced to implement the write() method in the regular interface
 * which is used by adaptations
 *
 */
public interface IStationTelemHeaderUpdater extends IStationTelemHeader{
    /**
     * Writes the header and attached data to the given output stream.
     *
     * @param out the bit output stream to write to
     * @return number of bytes written
     * @throws IOException if there is a problem with writing
     */
    int write(final BitOutputStream out) throws IOException;
}
