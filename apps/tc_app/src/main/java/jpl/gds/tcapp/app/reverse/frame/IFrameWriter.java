/*
 * Copyright 2006-2020. California Institute of Technology.
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
package jpl.gds.tcapp.app.reverse.frame;

import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tcapp.app.reverse.IDataWriter;

import java.util.List;

/**
 * Interface for Frame Writer. Like other writers, it has one entry point used to pass down objects
 * to be written.
 *
 */
public interface IFrameWriter extends IDataWriter {

    /**
     * Entry point to the write utility. Intended to write out contents of frames and PDUs.
     *
     * @param frames
     *          List of Tc Transfer Frames to write
     * @return
     *       byte array containing the frames' PDU data
     */
    List<byte[]> doReverseFrames(final List<ITcTransferFrame> frames);
}
