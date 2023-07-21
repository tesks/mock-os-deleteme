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
package jpl.gds.shared.holders;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.serialization.holder.Proto3PacketIdHolder;
import jpl.gds.serialization.primitives.holder.Proto3AbstractHolder;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.types.UnsignedLong;


/**
 * Packet id holder class.
 *
 * Note immutable.
 *
 * The surrogate value is used when we convert an unsupported (NULL) to a GDR
 * byte array. There is no way to represent a NULL otherwise.
 *
 */
public final class PacketIdHolder
    extends AbstractUnsignedLongHolder<PacketIdHolder>
    implements Comparable<PacketIdHolder>
{
    /** Zero is not allowed as an id */
    private static final long BASE_ID = 1L;

    /** Minimum PacketIdHolder value */
    public static final UnsignedLong MIN_VALUE =
                            UnsignedLong.valueOfLongAsUnsigned(BASE_ID);

    /** Maximum PacketIdHolder value */
    public static final UnsignedLong MAX_VALUE = UnsignedLong.MAX_VALUE;

    /** Must be last of the statics */

    /** PacketIdHolder for unsupported */
    public static final PacketIdHolder UNSUPPORTED;

    /** Surrogate for unsupported */
    private static final UnsignedLong SURROGATE_VALUE = UnsignedLong.MIN_VALUE;

    /** PacketIdHolder used as an "any" */
    private static final PacketIdHolder ANY;

    /** Packet id sources. Interpreted as unsigned */
    private static final AtomicLong FSW_KEY_COUNTER = new AtomicLong(BASE_ID);
    private static final AtomicLong SSE_KEY_COUNTER = new AtomicLong(BASE_ID);

    static
    {
        try
        {
            UNSUPPORTED = new PacketIdHolder(null, false, true);
            ANY         = UNSUPPORTED;
        }
        catch (final HolderException he)
        {
            // Won't happen
            throw new HolderRuntimeException(he);
        }
    }


    /**
     * Constructor.
     *
     * @param value Value
     *
     * @throws HolderException If invalid
     *
     * @deprecated Use valueOf
     */
    @Deprecated
    public PacketIdHolder(final UnsignedLong value)
        throws HolderException
    {
        this(value, false, false);
    }


    /**
     * Private constructor that can handle special values.
     *
     * @param value         Value
     * @param isUnspecified True if creating unspecified
     * @param isUnsupported True if creating unsupported
     *
     * @throws HolderException If invalid
     */
    private PacketIdHolder(final UnsignedLong value,
                           final boolean      isUnspecified,
                           final boolean      isUnsupported)
        throws HolderException
    {
        super(PacketIdHolder.class,
              value,
              isUnspecified,
              isUnsupported,
              null);
    }
    
    /**
     * Constructor used for recreation from a message
     * 
     * @param packetId
     *            the Protobuf message of a packet ID
     * @throws HolderException
     *             If invalid
     */
    public PacketIdHolder(final Proto3PacketIdHolder packetId) throws HolderException {
    	super(PacketIdHolder.class,
    			UnsignedLong.valueOfLongAsUnsigned(packetId.getPacketId().getLongValue()),
    			packetId.getPacketId().getIsUnspecified(),
    			packetId.getPacketId().getIsUnsupported(),
    			null);
    }


    /**
     * Method to validate cleaned value.
     *
     * @param value Value to be validated
     *
     * @throws HolderException If value not valid
     */
    @Override
    protected void validate(final UnsignedLong value)
        throws HolderException
    {
        if (value.compareTo(MIN_VALUE) < 0)
        {
            throw new HolderException(outOfRangeMessage(PacketIdHolder.class));
        }
    }


    /**
     * Non-static utility to call standard constructor.
     *
     * @param value Value
     *
     * @return PacketIdHolder
     *
     * @throws HolderException If invalid
     */
    @Override
    protected PacketIdHolder getNewInstance(final UnsignedLong value)
        throws HolderException
    {
        return new PacketIdHolder(value);
    }


    /**
     * Get PacketIdHolder from string.
     *
     * @param value Value
     *
     * @return PacketIdHolder
     *
     * @throws HolderException If not a valid string
     */
    public static PacketIdHolder valueOfString(final String value)
        throws HolderException
    {
        // Can use any instance to call
        return ANY.valueOfString(value, null, UNSUPPORTED, null);
    }


    /**
     * Get PacketIdHolder from UnsignedLong.
     *
     * @param value Value
     *
     * @return PacketIdHolder
     *
     * @throws HolderException If not a valid value
     */
    public static PacketIdHolder valueOf(final UnsignedLong value)
        throws HolderException
    {
        // Can use any instance to call
        return ANY.valueOf(value, null, UNSUPPORTED, null);
    }


    /**
     * Get from SQL result set.
     *
     * @param rs       Result set
     * @param column   Column name
     * @param warnings List to stash warnings
     *
     * @return New instance
     *
     * @throws HolderException If value out of range
     * @throws SQLException    SQL error
     */
    public static PacketIdHolder getFromDb(final ResultSet        rs,
                                           final String           column,
                                           final List<SQLWarning> warnings)
        throws HolderException, SQLException
    {
        // Can use any instance to call
        return ANY.getFromDb(rs, column, null, UNSUPPORTED, null, warnings);
    }


    /**
     * Get from SQL result set.
     *
     * @param rs       Result set
     * @param column   Column index
     * @param warnings List to stash warnings
     *
     * @return New instance
     *
     * @throws HolderException If value out of range
     * @throws SQLException    SQL error
     */
    public static PacketIdHolder getFromDb(final ResultSet        rs,
                                           final int              column,
                                           final List<SQLWarning> warnings)
        throws HolderException, SQLException
    {
        // Can use any instance to call
        return ANY.getFromDb(rs, column, null, UNSUPPORTED, null, warnings);
    }


    /**
     * Return an instance with the value incremented by one. It is
     * extremely unlikely that the exception will be thrown.
     *
     * @return New instance
     *
     * @throws HolderException If value becomes out of range
     */
    public PacketIdHolder incrementValue()
        throws HolderException
    {
        try
        {
            return valueOf(getValue().increment());
        }
        catch (final NumberFormatException nfe)
        {
            throw new HolderException(outOfRangeMessage(PacketIdHolder.class),
                                      nfe);
        }
    }


    /**
     * Reserve a new FSW packet id.
     *
     * @return Next packet id
     */
    public static PacketIdHolder getNextFswPacketId()
    {
        final long rawKey = FSW_KEY_COUNTER.getAndIncrement();

        try
        {
            return PacketIdHolder.valueOf(
                       UnsignedLong.valueOfLongAsUnsigned(rawKey));
        }
        catch (final HolderException he)
        {
            // Should not happen
            throw new HolderRuntimeException(he);
        }
    }


    /**
     * Reserve a new SSE packet id.
     *
     * @return Next packet id
     */
    public static PacketIdHolder getNextSsePacketId()
    {
        final long rawKey = SSE_KEY_COUNTER.getAndIncrement();

        try
        {
            return PacketIdHolder.valueOf(
                       UnsignedLong.valueOfLongAsUnsigned(rawKey));
        }
        catch (final HolderException he)
        {
            // Should not happen
            throw new HolderRuntimeException(he);
        }
    }


    /**
     * Extract as a protobuf byte array.
     *
     * @return protobuf in byte array
     */
    public final byte[] toProtoByteArray()
    {
        return build().toByteArray();
    }


    /**
     * Construct from a protobuf byte array.
     *
     * @param source Byte array as protobuf message
     *
     * @return PacketIdHolder
     *
     * @throws HolderException If bad byte array
     * 
     */
    public static final PacketIdHolder fromProtoByteArray(final byte[] source)
        throws HolderException
    {
        try {
            return new PacketIdHolder(Proto3PacketIdHolder.parseFrom(source));
        } catch (final InvalidProtocolBufferException e) {
            throw new HolderException(ExceptionTools.getMessage(e));
        }
    }
    
    /**
     * Convert this PacketIdHolder into a Protobuf object
     * 
     * @return the Protobuf representing this object
     */
    public Proto3PacketIdHolder build(){
    	final Proto3PacketIdHolder.Builder retVal = Proto3PacketIdHolder.newBuilder();
    	final Proto3AbstractHolder.Builder pktId = Proto3AbstractHolder.newBuilder();

    	if(this.isUnspecified()){
    		pktId.setIsUnspecified(this.isUnspecified());
    	}
    	if(this.isUnsupported()){
    		pktId.setIsUnsupported(this.isUnsupported());
    	}
    	if(this.getValue() != null){
    		pktId.setLongValue(this.getValue().longValue());
    	}

    	retVal.setPacketId(pktId);

    	return retVal.build();
    }
}
