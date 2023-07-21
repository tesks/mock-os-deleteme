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
package jpl.gds.eha.channel.api;

import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IImmutableChannelDefinition;
import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.shared.time.IImmutableAccurateDateTime;
import jpl.gds.shared.time.IImmutableLocalSolarTime;
import jpl.gds.shared.time.ISclk;

/**
 * The IChannelValue interface is used for access to channel samples in customer
 * derivations and EU calculations. It is a read-only interface so that samples
 * cannot be inadvertently changed by customer classes. New IChannelValues must
 * be created using the convenience functions supplied by the derivation
 * interfaces.
 * 
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 *
 * 
 *
 * @see IImmutableChannelDefinition
 */
@CustomerAccessible(immutable = true)
public interface IChannelValue {

    /**
     * Retrieves the spacecraft event time (SCET) for this channel value.
     * 
     * @return the scet; may be null
     */
    public abstract IImmutableAccurateDateTime getScet();

    /**
     * Gets the record creation time (RCT) of this channel value.
     * 
     * @return the record creation time; may be null
     */
    public abstract IImmutableAccurateDateTime getRct();

    /**
     * Gets the DSS (receiving station) ID for this channel value.
     * 
     * @return the DSS ID; 0 for no station
     */
    public abstract int getDssId();

    /**
     * Gets the VCID (virtual channel id) for this channel value.
     * 
     * @return the VCID; null for unknown
     */
    public abstract Integer getVcid();

    /**
     * Gets the local solar time (LST) for this channel value.
     * 
     * @return the lst value; may be null
     */
    public abstract IImmutableLocalSolarTime getLst();
    
    /**
     * Indicates whether an EU value has been set on this channel value.
     * 
     * @return true if an EU value has ever been set, false otherwise
     */
    public abstract boolean hasEu();

    /**
     * Gets the engineering units value for this channel value. Only returns a
     * meaningful value if the channel definition for this channel value has an
     * EU calculation defined.
     * 
     * @return the EU value; 0.0 if none defined
     */
    public abstract double getEu();

    /**
     * Gets the spacecraft clock (SCLK) value associated with this channel
     * value.
     * 
     * @return the SCLK value; may be null
     */
    public abstract ISclk getSclk();

    /**
     * Gets the earth receive time (ERT) associated with this channel value.
     * 
     * @return the ERT value; may be null.
     */
    public abstract IImmutableAccurateDateTime getErt();

    /**
     * Convenience method to get the channel ID for this channel value, which is
     * obtained from the channel definition.
     * 
     * @return the Channel Id string, or null if no channel definition is
     *         defined.
     */
    public abstract String getChanId();

    /**
     * Getter for byte value of DN. Returns 0 if type is not numeric or DN is
     * null.
     * 
     * @return byte value of DN
     */
    public abstract byte byteValue();

    /**
     * Getter for short value of DN. Returns 0 if type is not numeric or DN is
     * null.
     * 
     * @return short value of DN
     */
    public abstract short shortValue();

    /**
     * Getter for integer value of DN. Returns 0 if type is not numeric or DN is
     * null.
     * 
     * @return integer value of DN
     */
    public abstract int intValue();

    /**
     * Getter for boolean value of DN. Returns false if type is not numeric or
     * DN is null.
     * 
     * @return boolean value of DN
     */
    public abstract boolean booleanValue();

    /**
     * Getter for long value of DN. Returns 0 if type is not numeric or DN is
     * null.
     * 
     * @return long value of DN
     */
    public abstract long longValue();

    /**
     * Getter for float value of DN. Returns 0.0 if type is not numeric or DN is
     * null.
     * 
     * @return float value of DN
     */
    public abstract float floatValue();

    /**
     * Getter for double value of DN. Returns 0.0d if type is not numeric or DN
     * is null.
     * 
     * @return double value
     */
    public abstract double doubleValue();

    /**
     * Getter for string value of DN. Returns DN.toString() if the channel type
     * is not string. Returns the empty string if DN is null.
     *
     * @return String value of the DN
     */
    public abstract String stringValue();

    /**
     * Gets the raw data number object.
     * 
     * @return the DN as an Object may be null
     */
    public abstract Object getDn();

    /**
     * Return the status (enumeration) value of the channel if it has one, or
     * null otherwise.
     * 
     * @return the status value, or null if none defined
     */
    public abstract String getStatus();

    /**
     * Gets the DN of this channel value into a GDR-ordered (big-endian) byte
     * array.
     * 
     * @return array of bytes containing the DN. Length is defined by the
     *         associated channel definition
     */
    public abstract byte[] getDnAsBytes();

    /**
     * Gets the current DN alarm level.
     * 
     * @return AlarmLevel
     */
    public abstract AlarmLevel getDnAlarmLevel();

    /**
     * Gets the current EU alarm level.
     * 
     * @return AlarmLevel
     */
    public abstract AlarmLevel getEuAlarmLevel();

    /**
     * Gets a string representation of the DN alarm state.
     * 
     * @return alarm text; may be null
     */
    public abstract String getDnAlarmState();

    /**
     * Gets a string representation of the EU alarm state.
     * 
     * @return alarm text; may be null
     */
    public abstract String getEuAlarmState();

    /**
     * Retrieves the realtime flag, indicating this is a realtime versus
     * recorded channel value.
     * 
     * @return true if realtime, false if recorded
     */
    public abstract boolean isRealtime();

    /**
     * Gets the definition type of this channel, which indicates whether it is a
     * flight, SSE, header, or monitor channel.
     * 
     * @return the definition type of this channel
     */
    public ChannelDefinitionType getDefinitionType();

    /**
     * Gets a metadata category value for this channel, which is fetched from
     * the channel's dictionary definition. Categories currently include module,
     * subsystem, and operational category. Not all missions utilize all
     * categories in the telemetry dictionary.
     * 
     * @param categoryName
     *            the name of the category to retrieve; must be one of "module",
     *            "subsystem", or "ops category"
     * @return the corresponding category name; may be null
     */
    public String getCategory(String categoryName);

    /**
     * Gets the data type of this channel per the dictionary definition.
     * 
     * @return the Channel Type of this channel
     */
    public ChannelType getChannelType();
}
