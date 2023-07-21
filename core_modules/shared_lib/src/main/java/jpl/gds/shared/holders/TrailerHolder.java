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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.List;

import com.google.protobuf.ByteString;

import jpl.gds.serialization.holder.Proto3TrailerHolder;
import jpl.gds.serialization.primitives.holder.Proto3AbstractHolder;
import jpl.gds.shared.database.BytesBuilder;


/**
 * Trailer holder class. Uses ComparableByteArray internally instead of byte[]
 * in order to make the array Comparable.
 *
 * Note immutable.
 *
 *
 */
public final class TrailerHolder extends AbstractBytesHolder<TrailerHolder>
    implements Comparable<TrailerHolder>
{
    /** Minimum TrailerHolder length */
    public static final int MIN_LENGTH = 0;

    /** Maximum TrailerHolder length */
    public static final int MAX_LENGTH = 255;

    /** Must be last of the statics */

    /** TrailerHolder for unspecified. */
    private static final TrailerHolder UNSPECIFIED;

    /** TrailerHolder for unsupported. Also used as an "any". */
    private static final TrailerHolder UNSUPPORTED;

    /** Synonym for unsupported. */
    public static final TrailerHolder NULL_HOLDER;

    /** Synonym for unspecified. */
    public static final TrailerHolder ZERO_HOLDER;

    /** Special for common case of length one */
    public static final TrailerHolder ONE_HOLDER;

    /** Value for common case of length one */
    private static final byte ZERO_BYTE = (byte) 0;

    static
    {
        try
        {
            UNSUPPORTED = new TrailerHolder(null, false, true);
            UNSPECIFIED =
                new TrailerHolder(
                        ComparableByteArray.valueOf(new byte[MIN_LENGTH]),
                        true,
                        false);
            NULL_HOLDER = UNSUPPORTED;
            ZERO_HOLDER = UNSPECIFIED;
            ONE_HOLDER  =
                new TrailerHolder(
                        ComparableByteArray.valueOf(new byte[] {ZERO_BYTE}));
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
    public TrailerHolder(final byte[] value)
        throws HolderException
    {
        this(ComparableByteArray.valueOf(value));
    }


    /**
     * Constructor.
     *
     * @param value Value
     *
     * @throws HolderException If invalid
     */
    private TrailerHolder(final ComparableByteArray value)
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
    private TrailerHolder(final ComparableByteArray value,
                          final boolean             isUnspecified,
                          final boolean             isUnsupported)
        throws HolderException
    {
        super(TrailerHolder.class,
              value,
              isUnspecified,
              isUnsupported,
              null);
    }
    
    /**
     * Constructor used for recreation from a message
     * 
     * @param trailer
     *            the Protobuf message of a TrailerHolder
     * @throws HolderException
     *             If invalid
     */
    public TrailerHolder(final Proto3TrailerHolder trailer) throws HolderException {
    	super(TrailerHolder.class,
    			ComparableByteArray.valueOf(trailer.getTrailer().getBytesValue().toByteArray()),
    			trailer.getTrailer().getIsUnspecified(),
    			trailer.getTrailer().getIsUnsupported(),
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
    protected void validate(final ComparableByteArray value)
        throws HolderException
    {
        final int length = value.getLength();

        if ((length < MIN_LENGTH) || (length > MAX_LENGTH))
        {
            throw new HolderException(outOfRangeMessage(TrailerHolder.class));
        }
    }


    /**
     * Non-static utility to call standard constructor.
     *
     * @param value Value
     *
     * @return TrailerHolder
     *
     * @throws HolderException If invalid
     */
    @Override
    protected TrailerHolder getNewInstance(final ComparableByteArray value)
        throws HolderException
    {
        return new TrailerHolder(value);
    }


    /**
     * Get TrailerHolder.
     *
     * @param value Value
     *
     * @return TrailerHolder
     *
     * @throws HolderException If not a valid value
     */
    public static TrailerHolder valueOf(final byte[] value)
        throws HolderException
    {
        if ((value != null) && (value.length == 1) && (value[0] == ZERO_BYTE))
        {
            return ONE_HOLDER;
        }

        // Can use any instance to call
        return UNSUPPORTED.valueOf(ComparableByteArray.valueOf(value),
                                   UNSPECIFIED,
                                   UNSUPPORTED,
                                   null);
    }


    /**
     * Get TrailerHolder.
     *
     * @param value  Value
     * @param offset Offset into byte array
     * @param length Number of bytes to take
     *
     * @return TrailerHolder
     *
     * @throws HolderException If not a valid value
     */
    public static TrailerHolder valueOf(final byte[] value,
                                        final int    offset,
                                        final int    length)
        throws HolderException
    {
        if ((value != null) && (length == 1) && (value[offset] == ZERO_BYTE))
        {
            return ONE_HOLDER;
        }

        // Can use any instance to call
        return UNSUPPORTED.valueOf(ComparableByteArray.valueOf(value,
                                                               offset,
                                                               length),
                                   UNSPECIFIED,
                                   UNSUPPORTED,
                                   null);
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
     * @throws SQLException Any error
     */
    public static TrailerHolder getFromDb(final ResultSet        rs,
                                          final String           column,
                                          final List<SQLWarning> warnings)
        throws SQLException
    {
        // Can use any instance to call
        return UNSUPPORTED.getFromDbRethrow(rs,
                                            column,
                                            UNSPECIFIED,
                                            UNSUPPORTED,
                                            null,
                                            warnings);
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
     * @throws SQLException Any error
     */
    public static TrailerHolder getFromDb(final ResultSet        rs,
                                          final int              column,
                                          final List<SQLWarning> warnings)
        throws SQLException
    {
        // Can use any instance to call
        return UNSUPPORTED.getFromDbRethrow(rs,
                                            column,
                                            UNSPECIFIED,
                                            UNSUPPORTED,
                                            null,
                                            warnings);
    }


    /**
     * Override so that a zero-length array goes in as a NULL.
     *
     * @param bb Bytes builder
     *
     * @throws SQLException If cannot insert
     */
    @Override
    public void insert(final BytesBuilder bb)
        throws SQLException
    {
        if (isUnspecified())
        {
            UNSUPPORTED.insert(bb);
        }
        else
        {
            super.insert(bb);
        }
    }


    /**
     * Override so that a zero-length array goes in as a NULL.
     *
     * @param ps       Prepared statement
     * @param index    Index in prepared statement
     * @param warnings List to stash warnings
     *
     * @throws SQLException SQL error
     */
    @Override
    public void insert(final PreparedStatement ps,
                       final int               index,
                       final List<SQLWarning>  warnings)
        throws SQLException
    {
        if (isUnspecified())
        {
            UNSUPPORTED.insert(ps, index, warnings);
        }
        else
        {
            super.insert(ps, index, warnings);
        }
    }


    /**
     * Make sure a TrailerHolder is not null; if it is, we return an
     * unsupported.
     *
     * @param bytes A TrailerHolder or null
     *
     * @return Non-null TrailerHolder
     */
    public static TrailerHolder getSafeHolder(final TrailerHolder bytes)
    {
    	return ((bytes != null) ? bytes : NULL_HOLDER);
    }

    /**
     * Convert this TrailerHolder into a Protobuf object
     * 
     * @return the Protobuf representing this object
     */
    public Proto3TrailerHolder build() {
    	final Proto3TrailerHolder.Builder retVal = Proto3TrailerHolder.newBuilder();
    	final Proto3AbstractHolder.Builder tr = Proto3AbstractHolder.newBuilder();

    	if(this.isUnspecified()){
    		tr.setIsUnspecified(this.isUnspecified());
    	}
    	if(this.isUnsupported()){
    		tr.setIsUnsupported(this.isUnsupported());
    	}
    	if(this.getValue() != null){
    		tr.setBytesValue(ByteString.copyFrom(this.getValue()));
    	}

    	retVal.setTrailer(tr);

    	return retVal.build();
    }
}
