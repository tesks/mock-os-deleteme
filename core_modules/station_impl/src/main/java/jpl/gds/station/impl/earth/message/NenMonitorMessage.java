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
package jpl.gds.station.impl.earth.message;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.station.api.IStationTelemHeader;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.station.api.StationMessageType;
import jpl.gds.station.api.earth.message.INenMonitorMessage;


/**
 * This is an internal message that contains a raw NEN status packet
 * that is to be sent directly to decommutation from the telemetry input service.
 * 
 */
public class NenMonitorMessage extends Message implements INenMonitorMessage
{
    private final byte[] data;
    private final IStationTelemInfo stationInfo;  
    private final IStationTelemHeader stationHeader;


    
    /**
     * Constructor
     * 
     * @param statInfo the associated station information object
     * @param statHeader the associated station header object
     * @param data     the payload (NEN status packet) data
     */
    public NenMonitorMessage(final IStationTelemInfo statInfo,
            final IStationTelemHeader statHeader, 
            final byte[]           data)
    {
        super(StationMessageType.NenStationMonitor, System.currentTimeMillis());
        this.stationHeader = statHeader;
        this.stationInfo = statInfo;
        
        this.data = data;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
    public void generateStaxXml(final XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return "Raw NEN Status Packet";
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return "MSG:" + getType() + " " + getEventTimeString();
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.earth.message.INenMonitorMessage#getData()
     */
    @Override
    public byte[] getData() {
        return this.data;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.earth.message.INenMonitorMessage#getStationInfo()
     */
    @Override
    public IStationTelemInfo getStationInfo() {
        return this.stationInfo;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.earth.message.INenMonitorMessage#getStationHeader()
     */
    @Override
    public IStationTelemHeader getStationHeader() {
        return this.stationHeader;
    }
}
