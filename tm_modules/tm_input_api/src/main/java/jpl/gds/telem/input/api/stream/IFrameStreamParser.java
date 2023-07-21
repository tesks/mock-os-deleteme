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
import jpl.gds.telem.input.api.message.RawInputMetadata;

import java.io.IOException;

/**
 * Interface for a  transfer frame parser.
 * It will be implemented by adaptations
 *
 */
@CustomerAccessible(immutable = true)
public interface IFrameStreamParser {
    /**
     * Extract an entire frame. Update the metadata from the header, but
     * do not return header.
     *
     * @param data Raw bytes
     * @param metadata  Metadata
     *
     * @return IParsedFrame object
     *
     * @throws IOException If unable to get next frame
     */
    IParsedFrame readNextFrame(byte[] data, RawInputMetadata metadata) throws IOException;

    /**
     * Length of header, assuming fixed length
     * @return number of bits for trailer
     */
    int getHeaderBits();

    /**
     * Length of frame data, assuming fixed length
     * @return number of bits for trailer
     */
    int getFrameBits();

    /**
     * Length of trailer, assuming fixed length
     * @return number of bits for trailer
     */
    int getTrailerBits();
}
