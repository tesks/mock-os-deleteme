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
package jpl.gds.station.api.dsn.message;

import jpl.gds.serialization.station.Proto3DsnMonitorMessage;
import jpl.gds.shared.message.IMessage;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;

/**
 * An interface to be implemented by message classes meant to carry DSN MON-0158 
 * (station monitor) SFDUs.
 * 
 *
 * @since R8
 */
public interface IDsnMonitorMessage extends IMessage {

    /**
     * Gets the IChdoSfdu containing the MON data.
     * Will be transmitted via the message service.
     * 
     * @return IChdoSfdu object
     */
    IChdoSfdu getMonitorSfdu();

    /**
     * Transforms the content of the ITelemetryFrameMessage object to a
     * Protobuf message
     *
     * @return the Protobuf message representing this object
     */
    @Override
    Proto3DsnMonitorMessage build();

    /**
     * Sets the IChdoSfdu object associated with this message, if any.
     *
     * @param chdo IChdoSfdu object that arrived with the packet
     */
    void setMonitorSfdu(IChdoSfdu chdo);

}