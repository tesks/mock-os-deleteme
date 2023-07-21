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
package jpl.gds.eha.api.channel;

import jpl.gds.common.types.IRealtimeRecordedSupport;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.eha.api.channel.serialization.Proto3ChannelValue;
import jpl.gds.eha.channel.api.IChannelValue;
import jpl.gds.shared.interfaces.EscapedCsvSupport;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;

/**
 * An interface to be implemented by client (read-only) channel values. This is
 * the type of channel value contained in channel messages that go to message
 * clients.
 * 
 * @since R8
 * 
 */
public interface IClientChannelValue extends IChannelValue, Templatable,
        EscapedCsvSupport, IRealtimeRecordedSupport {

    /**
     * Gets the title of the channel.
     * 
     * @return title string
     */
    public String getTitle();

    /**
     * Gets the worst alarm state found in this channel value.
     * 
     * @return AlarmLevel; will be AlarmLevel.NONE if there is no alarm on the
     *         channel
     */
    public AlarmLevel getWorstAlarmLevel();
    
    /**
     * Gets the earth receive time (ERT) associated with this channel value.
     * 
     * @return the ERT value; may be null.
     */
    @Override
    public abstract IAccurateDateTime getErt();
    
    /**
     * Retrieves the spacecraft event time (SCET) for this channel value.
     * 
     * @return the scet; may be null
     */
    @Override
    public abstract IAccurateDateTime getScet();

    /**
     * Gets the record creation time (RCT) of this channel value.
     * 
     * @return the record creation time; may be null
     */
    @Override
    public abstract IAccurateDateTime getRct();
    
    /**
     * Gets the local solar time (LST) for this channel value.
     * 
     * @return the lst value; may be null
     */
    @Override
    public abstract ILocalSolarTime getLst();

    /**
     * Writes the channel value to a protobuf message
     * 
     * @return the protobuf message containing this channel value
     */
    public Proto3ChannelValue build();
    
    /**
     * Returns the set of alarm states on this channel value.
     * 
     * @return the alarms.
     */
    
    public abstract IAlarmValueSet getAlarms();
      
    /**
     * Gets the units specifier for DN, from the dictionary definition of the channel.
     * @return DN units string; may be null
     * 
     */
    public String getDnUnits();
    
    /**
     * Gets the units specifier for EU, from the dictionary definition of the channel.
     * @return EU units string; may be null
     *
     */
    public String getEuUnits();
}
