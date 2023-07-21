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
package jpl.gds.tm.service.api.frame;

import jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;

/**
 * An interface to be implemented by factories that create telemetry frame info
 * objects.
 * 
 * @since R8
 */
public interface ITelemetryFrameInfoFactory {

    /**
     * Creates an empty ITelemetryFrameInfo object.
     * 
     * @return ITelemetryFrameInfo object
     */
    public ITelemetryFrameInfo create();

    /**
     * Creates an ITelemetryFrameInfo object. 
     * 
     * @param scid the spacecraft ID
     * @param vc the virtual channel ID for the frame
     * @param seq the sequence number (VCFC) of the frame
     * @return IFrameInfo object
     */
    public ITelemetryFrameInfo create(int scid, int vc, int seq);

    /**
     * Creates an ITelemetryFrameInfo object and initializes it
     * using the supplied ITelemetryFrameHeader and ITransferFrameDefinition objects. If
     * either of these objects is non-null, then the other member fields in the
     * ITelemetryFrameInfo instance will be initialized from the fields in those objects,
     * where possible. It is important to note that the header size and packet
     * store size will be initialized using the "Actual" ASM length in the
     * ITransferFrameDefinition object, as opposed to the vanilla ASM length. If the
     * vanilla ASM length is desired, then the caller must override the packet
     * store size and header size fields after this constructor completes.
     * 
     * @param header
     *            the ITelemetryFrameHeader object (populated) to be associated with the
     *            ITelemetryFrameInfo instance; may be null
     * @param format
     *            the ITransferFrameDefinition object (populated) to be associated
     *            with the ITelemetryFrameInfo instance; may be null
     * @return the new ITelemetryFrameInfo object, initialized from the header and format
     *         objects
     */
    public ITelemetryFrameInfo create(ITelemetryFrameHeader header, ITransferFrameDefinition format);

}