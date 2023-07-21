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

import jpl.gds.common.types.IIdleSupport;
import jpl.gds.context.api.filtering.IScidFilterable;
import jpl.gds.context.api.filtering.IStationFilterable;
import jpl.gds.context.api.filtering.IVcidFilterable;
import jpl.gds.serialization.frame.Proto3TelemetryFrameMessage;
import jpl.gds.shared.holders.FrameIdHolder;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.message.IMessage;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;

/**
 * An interface to be implemented by message classes that represent telemetry
 * transfer frames. It carries frame metadata, the frame station header, the frame
 * station trailer, and the frame body itself.
 * 
 */
public interface ITelemetryFrameMessage extends IMessage, IIdleSupport, IScidFilterable, IStationFilterable, IVcidFilterable {

    /**
     * Gets the IChdoSfdu object associated with this frame, if any. This object
     * is available only during telemetry ingestion. If the message has been transmitted
     * via the message service, this object will be null.
     * 
     * @return IChdoSfdu 
     */
    IChdoSfdu getChdoObject();
    
    /**
     * Sets the IChdoSfdu object associated with this frame, if any.
     * 
     * @param chdo IChdoSfdu object that arrived with the frame 
     */
    void setChdoObject(IChdoSfdu chdo);
    
    /**
     * Basic function to get the number of bytes.
     * @return Returns the number of bytes.
     */
    int getNumBodyBytes();

    /**
     * Return frame header.
     *
     * @return Frame header (never null)
     */
    HeaderHolder getRawHeader();

    /**
     * Return frame trailer.
     *
     * @return Frame trailer (never null)
     */
    TrailerHolder getRawTrailer();

    /**
     * Return frame id.
     *
     * @return Frame id (never null)
     *
     */
    FrameIdHolder getFrameId();

    /**
     * Basic function to get the transfer frame.
     * @return Returns the transfer frame.
     */
    byte[] getFrame();
    
    /**
     * Gets the ITelemetryFrameInfo object containing metadata about this 
     * frame.
     * 
     * @return frame information object
     */
    ITelemetryFrameInfo getFrameInfo();

    /**
     * Gets the IStationInfo object containing metadata from the station
     * about receipt of the frame.
     * 
     * @return frame information object
     */
    IStationTelemInfo getStationInfo();

    /**
	 * Transforms the content of the ITelemetryFrameMessage object to a
	 * Protobuf message
	 * 
	 * @return the Protobuf message representing this object
	 */
    @Override
    Proto3TelemetryFrameMessage build();
}