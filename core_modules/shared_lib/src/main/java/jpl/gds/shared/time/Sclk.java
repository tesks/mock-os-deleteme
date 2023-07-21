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
package jpl.gds.shared.time;

import jpl.gds.serialization.primitives.time.Proto3Sclk;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;


/**
 * The spacecraft clock class. Events occurring on the spacecraft are labeled
 * with the value of the spacecraft clock counter when they are transmitted.
 * This is known as SCLK, or spacecraft clock time.
 * 
 * SCLK is really only approximately proportional to actual time since the rate
 * at which spacecraft clocks count varies slightly due to a host of causes
 * including the spacecraft temperature distribution. Spacecraft clock rates and
 * formats are usually spacecraft dependent.
 * 
 * According to the 820-013 0173-Telecomm-TIME Standard Time Formats document,
 * MER uses a short fine time that is interpreted to have leading zeroes. MSAP
 * and MSL will use a fine time (short/long is TBD) that is interpreted to have
 * trailing zeroes. SCLK seconds must always have leading zeroes.
 * 
 * dummyValue is used in cases like MonitorChannelValue where you need a SCLK
 * but don't have a value for it. Setting it true gives you a SCLK that has a
 * zero value but prints as an empty string.
 * 
 *
 */
public class Sclk extends CoarseFineTime implements ISclk {

	/**
	 * UNDEFINED SCLK value.
	 */
	public static final ISclk undefinedSclk = new Sclk(0, 0, 32, 16, 0);

    /** Coarse bit length */
	private static final int COARSE_BIT_LENGTH;

    /** Coarse byte length */
	private static final int COARSE_BYTE_LENGTH;

    /** Fine bit length */
	private static final int FINE_BIT_LENGTH;

    /** Fine byte length */
	private static final int FINE_BYTE_LENGTH;

	/** Fine time upper limit */
    public static final long FINE_UPPER_LIMIT;

	static {
		COARSE_BIT_LENGTH = TimeProperties.getInstance()
				.getSclkCoarseBitLength();
		if (COARSE_BIT_LENGTH > 32) {
			throw new IllegalStateException(
					"The system cannot currently handle a SCLK coarse length greater than 32 bits long.");
		}
		COARSE_BYTE_LENGTH = ((COARSE_BIT_LENGTH / Byte.SIZE) + (((COARSE_BIT_LENGTH % 8) > 0) ? 1
				: 0));
		if (COARSE_BYTE_LENGTH > 4) {
			throw new IllegalStateException(
					"The system cannot currently handle a SCLK coarse byte length greater than 4 bytes.");
		}
		FINE_BIT_LENGTH = TimeProperties.getInstance().getSclkFineBitLength();
		if (FINE_BIT_LENGTH > 32) {
			throw new IllegalStateException(
					"The system cannot currently handle a SCLK fine length greater than 32 bits long.");
		}
		FINE_BYTE_LENGTH = ((FINE_BIT_LENGTH / Byte.SIZE) + (((FINE_BIT_LENGTH % 8) > 0) ? 1
				: 0));
		if (FINE_BYTE_LENGTH > 4) {
			throw new IllegalStateException(
					"The system cannot currently handle a SCLK fine byte length greater than 4 bytes.");
		}
		  
		FINE_UPPER_LIMIT = TimeProperties.getInstance().getSclkFineUpperLimit();
	}

    /** Minimum SCLK value */
	public static final ISclk MIN_SCLK = new Sclk(0, 0);

	private static class CanonicalConfigHelper {
		private static final CoarseFineEncoding INSTANCE = new CoarseFineEncoding(
				TimeProperties.getInstance().getSclkCoarseBitLength(),
				TimeProperties.getInstance().getSclkFineBitLength(),
				TimeProperties.getInstance().getSclkFineUpperLimit()
		);
	}
	
	private static class CanonicalFmtHelper {
		private static final SclkFormatter INSTANCE =
				TimeProperties.getInstance().getSclkFormatter();
	}


	/**
	 * Creates an instance of SCLK. Length of SCLK is loaded from the GDS
	 * configuration.
     *
     * @param dummy True if a dummy value
	 */
	public Sclk(final boolean dummy) {
		super(dummy);
		//super(dummy, COARSE_BIT_LENGTH, COARSE_BYTE_LENGTH, FINE_BIT_LENGTH,
		//		FINE_BYTE_LENGTH, FINE_UPPER_LIMIT);
	}

	/**
	 * Create a 0 SCLK according to canonical encoding.
	 */
	public Sclk() {
		super(0, 0, CanonicalConfigHelper.INSTANCE);
	}

	/**
	 * 
	 * Creates an instance of SCLK. Length of SCLK is loaded from the GDS
	 * configuration.
	 * 
	 * @param secs
	 *            The initial SCLK secs count
	 */
	public Sclk(final long secs, final CoarseFineEncoding cfEncoding) {
		super(secs, cfEncoding);
	}
	
	public Sclk(final long secs) {
		super(secs, CanonicalConfigHelper.INSTANCE);
	}
	

	/**
	 * 
	 * Creates an instance of SCLK. Length of SCLK is loaded from the GDS
	 * configuration.
	 * 
	 * @param secs
	 *            The initial SCLK secs count
	 * @param fine
	 *            The initial SCLK fine count
	 */
	public Sclk(final long secs, final long fine, final CoarseFineEncoding cfEncoding) {
		super(secs, fine, cfEncoding);
	}
	
	/**
	 * 
	 * Creates an instance of SCLK. Length of SCLK is loaded from the GDS
	 * configuration.
	 * 
	 * @param secs
	 *            The initial SCLK secs count
	 * @param fine
	 *            The initial SCLK fine count
	 */
	public Sclk(final long secs, final long fine, final CoarseFineEncoding cfEncoding, final int originalBitLength) {
		super(secs, fine, cfEncoding, originalBitLength);
	}

	public Sclk(final long secs, final long fine) {
		super(secs, fine, CanonicalConfigHelper.INSTANCE);
	}

	/**
	 * Creates a SCLK value with the given coarse and fine times, using the
	 * maximum lengths given for coarse and fine bit lengths. Normally, SCLKs
	 * are create with the mission default coarse and fine bit lengths. The
	 * maximum for both values is 32.
	 * 
	 * @param secs
	 *            the seconds to set
	 * @param fine
	 *            the fine "ticks" to set
	 * @param coarseLenBits
	 *            the length of the coarse field in bits
	 * @param fineLenBits
	 *            the length of the fine field in bits
	 */
	public Sclk(final long secs, final long fine, final int coarseLenBits,
			final int fineLenBits, final long fineLimit) {
		super(secs, fine, new CoarseFineEncoding(coarseLenBits, fineLenBits, fineLimit));
	}

	/**
	 * 
	 * Creates an instance of Sclk.
	 * 
	 * @param sclk
	 *            The SCLK to copy into this SCLK
	 */
    public Sclk(final ISclk sclk) {
		super(sclk.getCoarse(), sclk.getFine(), sclk.getEncoding());
	}


	public Sclk(final int coarse, final int fine, final CoarseFineEncoding cfEncoding) {
		super(coarse, fine, cfEncoding);

	}
	
    /**
     * Creates an instance of SCLK with the values in the provided protobuf
     * message
     * 
     * @param msg
     *            a SCLK protobuf message
     */
	public Sclk(final Proto3Sclk msg) {
        this(msg.getSeconds(), msg.getNanos());
    }


	/**
     * {@inheritDoc}
     *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			throw new IllegalArgumentException("Null input object");
		} else if (!(obj instanceof Sclk)) {
			throw new IllegalArgumentException("Input object is not a Sclk");
		}

		return (compareTo(obj) == 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public int compareTo(final Object obj) {
		if (obj == null) {
			throw new IllegalArgumentException("Null input object");
		} else if ((obj instanceof Sclk) == false) {
			throw new IllegalArgumentException("Input object is not a Sclk");
		}

		return (compare((ISclk) obj));
	}

	/**
	 * Create a SCLK object from a floating point time
	 * 
	 * @param time The floating point number representing a SCLK value
	 * @param cfEncoding  the target encoding
	 * 
	 */
	public Sclk(final double time, final CoarseFineEncoding cfEncoding) {
		super(time, cfEncoding);
	}
	
	/**
	 * Create a SCLK object from a floating point time.
	 * @param time time in seconds
	 */
	public Sclk(final double time) {
		super(time, CanonicalConfigHelper.INSTANCE);
	}

	/**
     * {@inheritDoc}
     *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
	    // This is always using the
        // canonical encoding, rather than the encoding specific to this SCLK 
        // instance. 
		return CanonicalFmtHelper.INSTANCE.fmt(this);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String toTicksString() {
	    // This is always using the
        // canonical encoding, rather than the encoding specific to this SCLK 
        // instance. 
		return CanonicalFmtHelper.INSTANCE.toTicksString(this);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String toDecimalString() {
	    // This is always using the
	    // canonical encoding, rather than the encoding specific to this SCLK 
	    // instance. 
		return CanonicalFmtHelper.INSTANCE.toDecimalString(this);
	}

	
	/**
	 * Create a SCLK object from a String that matches the
	 * configured formatting rules.  Use SclkFormatter instead.
	 * @param sclkString string containing a formatted SCLK
	 * @return a new SCLK with the canonical SCLK encoding
	 */
    public static ISclk getSclkFromString(final String sclkString) {
		return CanonicalFmtHelper.INSTANCE.valueOf(sclkString);
	}
	
	/**
	 * Create a SCLK object from the given number of milliseconds.
	 * @param millis the number of milliseconds giving the SCLK time
	 * @param cfEncoding the encoding of the resulting SCLK object
	 */
	public static ISclk sclkFromMillis(final long millis, final CoarseFineEncoding cfEncoding) {
		long secs = millis / 1000;
		final float fractionalSubseconds = (float) ((millis % 1000) / 1000.0);
		/** If the fine clock rounds to its upper limit,
		 * the coarse clock needs to be incremented.
		 */
		int fineClock = Math.round(fractionalSubseconds * (cfEncoding.getMaxFine() + 1));
		if (fineClock == cfEncoding.getMaxFine() + 1) {
			++secs;
			fineClock = 0;
		}
		return new Sclk(secs, fineClock, cfEncoding);
	}

	/**
	 * Set current SCLK object from the given number of milliseconds.
	 * @param millis the number of milliseconds giving the SCLK time
	 */
	public static ISclk sclkFromMillis(final long millis) {
		return Sclk.sclkFromMillis(millis, CanonicalConfigHelper.INSTANCE);
	}
	
	@Override
	public Proto3Sclk buildSclk(){
		final Proto3Sclk.Builder retVal = Proto3Sclk.newBuilder();
		retVal.setSeconds(this.getCoarse()).setNanos(this.getFine());
		
		return retVal.build();
	}
}
