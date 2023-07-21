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

package jpl.gds.tc.api.frame;

import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.exception.FrameWrapUnwrapException;

/**
 * Interface for telecommand frame parsing and extraction in MPS
 *
 */
public interface ITcTransferFrameParser {

    /**
     * Parse a telecommand frame from bytes.
     *
     * @param tcBytes telecommand frame bytes
     * @return a telecommand frame
     */
    ITcTransferFrame parse(byte[] tcBytes) throws FrameWrapUnwrapException;

    /**
     * Parse a telecommand frame from bytes, optionally validating FECF
     *
     * @param tcBytes      telecommand frame bytes
     * @param validateFecf true to validate FECF, false to skip validation
     * @return a telecommand frame
     * @throws FrameWrapUnwrapException
     */
    ITcTransferFrame parse(byte[] tcBytes, boolean validateFecf) throws FrameWrapUnwrapException;

    /**
     * Parse a telecommand frame from bytes, starting from the given offset.
     *
     * @param tcBytes telecommand frame bytes
     * @param offset  start index of frame
     * @return a telecommand frame
     * @throws FrameWrapUnwrapException
     */
    ITcTransferFrame parse(final byte[] tcBytes, final int offset) throws FrameWrapUnwrapException;

    /**
     * Parse a telecommand frame from bytes, starting from the given offset.
     *
     * @param tcBytes      telecommand frame bytes
     * @param offset       start index of frame
     * @param validateFecf true to validate FECF, false to skip validation
     * @return a telecommand frame
     * @throws FrameWrapUnwrapException
     */
    ITcTransferFrame parse(final byte[] tcBytes, final int offset, final boolean validateFecf) throws
                                                                                               FrameWrapUnwrapException;

}
