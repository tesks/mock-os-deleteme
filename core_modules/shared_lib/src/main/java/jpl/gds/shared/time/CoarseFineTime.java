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

import org.apache.commons.lang.builder.HashCodeBuilder;

import jpl.gds.shared.gdr.GDR;

/**
 * CoarseFine time class to hold many of the "SCLK"-natured
 * operations and characteristics, while allowing separation of
 * different SCLK formats (namely, coarse/fine bit lengths) such as
 * that of SCLK and DVT.
 * 
 * This class was created after the ISclk class has been in place for
 * quite some time. For more details not found here, refer to the ISclk
 * class.
 * 
 *
 *
 */
public class CoarseFineTime implements ICoarseFineTime {

    /** True means that this is not a real value and prints as "" */
    protected boolean dummyValue = false;

	/** The SCLK coarse count.  This does not have to refer to an epoch. */
	private final long coarse;

	/** The SCLK fine count */
	private final long fine;
	
	private final long maxFine;
	
	/** The coarse fine encoding for this coarse fine time.
	 * This encoding may differ from the source encoding of this time.
	 * The originalBitLength tracks the length of the source encoding for this reason.
	 * 
	 */
	protected final CoarseFineEncoding cfEncoding;

	/**
	 * The original length of the binary data this time originated from
	 * may differ from the coarse fine encoding length.
	 */
	private final int originalBitLength;

	/**
	 * Creates a coarse-fine time instance with a zero value
     *
     * @param dummy   True if a dummy
     * @param cfEncoding the target encoding this time should match 
	 */
	public CoarseFineTime(final boolean dummy,
								   final CoarseFineEncoding cfEncoding) {
        this(0, 0, cfEncoding);
        dummyValue = dummy; 
    }
	
	/**
	 * Creates zero coarse fine time
	 * that may be a dummy time.
	 * @param dummy whether the coarse fine time is a dummy and printed as empty quotes
	 */
	public CoarseFineTime(final boolean dummy) {
		this(0, 0, new CoarseFineEncoding(0, 0, 0));
		dummyValue = dummy;
	}

	/**
	 * Creates a SC CLOCK value with the given coarse and fine times, using the maximum lengths
	 * given for coarse and fine bit lengths. Normally, SC CLOCKs are create with the misison default 
	 * coarse and fine bit lengths.  The maximum for both values is 32.
	 * 
	 * @param coarse the coarse seconds to set
	 * @param fine the fine "ticks" to set
	 * @param coarseLenBits the length of the coarse field in bits
	 * @param fineLenBits the length of the fine field in bits
	 * @param fineLimit Fine value upper limit
     * 
	 */
	public CoarseFineTime(final long coarse, final long fine, final int coarseLenBits, final int fineLenBits, final long fineLimit) {
		cfEncoding = new CoarseFineEncoding(coarseLenBits, fineLenBits, fineLimit);
		if(cfEncoding.getCoarseByteLength() > 4)
		{
			throw new IllegalStateException("The system cannot currently handle a SC CLOCK coarse byte length greater than 4 bytes.");
		}

		if(cfEncoding.getFineByteLength() > 4)
		{
			throw new IllegalStateException("The system cannot currently handle a SC CLOCK fine byte length greater than 4 bytes.");
		}

		this.maxFine = fineLimit;
		validateCoarse(coarse);
		validateFine(fine);
		this.coarse = coarse;
		this.fine = fine;
		this.originalBitLength = cfEncoding.getBitLength();
	}
	
	/**
	 * Create a coarse fine time with the given number of coarse and fine ticks,
	 * with the provided encoding.
	 * @param coarse the number of coarse ticks to set
	 * @param fine the number of fine ticks to set
	 * @param cfEncoding the encoding rules this coarse fine time should match
	 */
    public CoarseFineTime(final long coarse, final long fine,
			final CoarseFineEncoding cfEncoding) {
    	this(coarse, fine, cfEncoding, cfEncoding.getBitLength());
	}

    /**
     * Creates a coarse fine time with the given coarse, fine, and
     * encoding, but a bit length that may differ from the encoding.
     * Appropriate for times that have a different source encoding
     * from target encoding
	 * @param secs the number of coarse ticks to set
	 * @param fine the number of fine ticks to set
	 * @param cfEncoding the target encoding rules this coarse fine time should match
     * @param originalBitLength the original number of bits used to represent this time in binary
     */
    public CoarseFineTime(final long secs, final long fine,
			final CoarseFineEncoding cfEncoding, final int originalBitLength) {
    	this.cfEncoding = cfEncoding;
    	this.maxFine = cfEncoding.getMaxFine();
    	validateCoarse(secs);
    	validateFine(fine);
    	this.coarse = secs;
    	this.fine = fine;
    	this.originalBitLength = originalBitLength;
	}

    /**
     * Creates a coarse-fine time with the given number of coarse ticks
     * Assumes zero value for fine ticks.
     * @param secs the number of coarse ticks to set
     * @param cfEncoding the target encoding rules this coarse-fine time should match
     */
	public CoarseFineTime(final long secs,
			final CoarseFineEncoding cfEncoding) {
		this.cfEncoding = cfEncoding;
		validateCoarse(secs);
		this.coarse = secs;
		this.fine = 0;
    	this.maxFine = cfEncoding.getMaxFine();
    	this.originalBitLength = cfEncoding.getBitLength();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public long getCoarse()
	{
		return(coarse);
	}
	

	/**
     * {@inheritDoc}
     */
	@Override
    public long getFine()
	{
		return(fine);
	}

	
	/**
	 * Sets the secs
	 *
	 * @param coarse The secs to set.
	 */
	private void validateCoarse(final long coarse)
	{
		if(coarse > getCoarseUpperLimit())
		{
            throw new IllegalArgumentException("Input coarse time " + coarse + " is too large for this SC CLOCK."
                    + " The coarse time is configured to fit into " + cfEncoding.getCoarseBits() + " bits.");
		}
		else if(coarse < 0)
		{
            throw new IllegalArgumentException("SC CLOCK secs cannot be negative.");
		}

	}

	/**
	 * Sets the fine
	 *
	 * @param fine The fine to set.
	 */
	private void validateFine(final long fine)
	{

		if(fine > getFineUpperLimit())
		{
			throw new IllegalArgumentException("Input fine time " + fine + " is too large for this SC CLOCK." +
                    " The fine time is configured to fit into " + cfEncoding.getFineBits() + " bits.");
		}
		else if(fine < 0)
		{
            throw new IllegalArgumentException("Fine time cannot be negative.");
		}

	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public long getCoarseUpperLimit()
	{
		return cfEncoding.getMaxCoarse();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public long getFineUpperLimit()
	{

		return maxFine;
	}
	
	
	/**
     * {@inheritDoc}
     */
	@Override
    public double getFloatingPointTime()
	{
		return(getCoarse() + ((double)getFine()/(getFineUpperLimit() + 1)));
	}

	/**
	 * Creates a coarse-fine time, converting the time argument from a floating point
	 * to a integral number of coarse and fine ticks according to the encoding.
	 * @param time the floating point value of coarse ticks
	 * @param cfEncoding the target encoding
	 */
	public CoarseFineTime(final double time, final CoarseFineEncoding cfEncoding) {
		final double fractional = time % 1;
		this.cfEncoding = cfEncoding;
		this.coarse = Math.round(time-fractional);
		this.validateCoarse(this.coarse);
		this.fine = Math.round(fractional * (cfEncoding.getMaxFine() + 1));
    	this.maxFine = cfEncoding.getMaxFine();
		this.validateFine(this.fine);
    	this.originalBitLength = cfEncoding.getBitLength();
	}
	

	/**
     * {@inheritDoc}
     */
	@Override
    public ISclk increment(final long sec, final long fin)
	{
		if(sec < 0)
		{
            throw new IllegalArgumentException("Negative input secs.");
		}
		else if(fin < 0)
		{
            throw new IllegalArgumentException("Negative input fine.");
		}

		long tempSecs = coarse;
		long tempFine = fine;

		tempSecs += sec;
		tempFine += fin;

		if(tempFine > maxFine)
		{
			tempSecs += (tempFine / (cfEncoding.getMaxFine() + 1));
			tempFine = (tempFine % (cfEncoding.getMaxFine() + 1));
		}

		// Add encoding param.
        // The new SCLK needs to use the same encoding
		return new Sclk(tempSecs, tempFine, cfEncoding);
	}
	

	/**
     * {@inheritDoc}
     */
	@Override
    public ISclk decrement(final long sec, final long fin)
	{
		if(sec < 0)
		{
            throw new IllegalArgumentException("Negative input secs.");
		}
		else if(fin < 0)
		{
            throw new IllegalArgumentException("Negative input fine.");
		}

		long tempSecs = coarse;
		long tempFine = fine;
		
	    //  Computation or new coarse and fine corrected

		long localSecs = sec;
		long localFin = fin;
		while (localFin > cfEncoding.getMaxFine() + 1) {
		    localFin -= cfEncoding.getMaxFine() + 1;
		    localSecs++;
		}

		tempSecs -= localSecs;
	    tempFine -= localFin;
	        
		if(tempFine < 0)
		{
		    tempSecs--;
		    tempFine = cfEncoding.getMaxFine() + 1 - localFin;
			
		}
		
	    // Add encoding param.
		// The new SCLK needs to use the same encoding
		return new Sclk(tempSecs, tempFine, cfEncoding);
	}
	

    /**
     * {@inheritDoc}
     */
	@Override
    public byte[] getCoarseBytes()
	{
		byte[] bytes = new byte[0];
		if(cfEncoding.getCoarseBits() <= 8)
		{
			bytes = new byte[1];
			GDR.set_u8(bytes,0,(short)getCoarse());
		}
		else if(cfEncoding.getCoarseBits() <= 16)
		{
			bytes = new byte[2];
			GDR.set_u16(bytes,0,(int)getCoarse());
		}
		else if(cfEncoding.getCoarseBits() <= 24)
		{
			bytes = new byte[3];
			GDR.set_u24(bytes,0,(int)getCoarse());
		}
		else if(cfEncoding.getCoarseBits() <= 32)
		{
			bytes = new byte[4];
			GDR.set_u32(bytes,0,(int)getCoarse());
		}

		return(bytes);
	}


    /**
     * {@inheritDoc}
     */
	@Override
    public byte[] getFineBytes()
	{
		byte[] bytes = new byte[0];
		if(cfEncoding.getFineBits() <= 8)
		{
			bytes = new byte[1];
			GDR.set_u8(bytes,0,(short)getFine());
		}
		else if(cfEncoding.getFineBits() <= 16)
		{
			bytes = new byte[2];
			GDR.set_u16(bytes,0,(int)getFine());
		}
		else if(cfEncoding.getFineBits() <= 24)
		{
			bytes = new byte[3];
			GDR.set_u24(bytes,0,(int)getFine());
		}
		else if(cfEncoding.getFineBits() <= 32)
		{
			bytes = new byte[4];
			GDR.set_u32(bytes,0,(int)getFine());
		}

		return(bytes);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public int getByteLength()
	{
		return (int) Math.ceil((double) originalBitLength / Byte.SIZE);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public int toBytes(final byte[] buff, final int startingOffset)
	{
		if(buff == null)
		{
            throw new IllegalArgumentException("Null input byte buffer.");
		}
		else if(startingOffset < 0 || startingOffset >= buff.length)
		{
			throw new ArrayIndexOutOfBoundsException("Input offset falls outside the bounds of" +
                    " the input byte buffer.");
		}
		else if((startingOffset + getByteLength()) > buff.length)
		{
			throw new ArrayIndexOutOfBoundsException("SCLK must be " + getByteLength() + " bytes" +
                    " long, but this extends past the end of the input buffer.");
		}

		final byte[] coarseBytes = getCoarseBytes();
		final byte[] fineBytes = getFineBytes();

		System.arraycopy(coarseBytes,0,buff,startingOffset,coarseBytes.length);
		System.arraycopy(fineBytes,0,buff,startingOffset+coarseBytes.length,fineBytes.length);

		return(coarseBytes.length + fineBytes.length);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public byte[] getBytes()
	{
		final byte[] coarseBytes = getCoarseBytes();
		final byte[] fineBytes = getFineBytes();

		final byte[] buff = new byte[coarseBytes.length+fineBytes.length];

		System.arraycopy(coarseBytes,0,buff,0,coarseBytes.length);
		System.arraycopy(fineBytes,0,buff,coarseBytes.length,fineBytes.length);

		return(buff);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public long getBinaryGdrLong()
	{
		final byte[] longBytes = new byte[Long.SIZE/8];
		final byte[] sclkBytes = getBytes();

		final int offset = longBytes.length-sclkBytes.length;
		System.arraycopy(sclkBytes,0,longBytes,offset,sclkBytes.length);

		final long gdrLong = GDR.get_i64(longBytes,0);

		return(gdrLong);
	}
	
	/**
	 * Compare this SC CLOCK to the input SC CLOCK.
	 *
	 * @param scClock The SC CLOCK to compare against this one
	 *
	 * @return -1 if this SC CLOCK is earlier than the input SC CLOCK, 0 if the
	 * two SC CLOLKs are equal, or 1 if this SC CLOCK is later than the input SC CLOCK
	 */
	protected int compare(final ICoarseFineTime scClock)
	{
		if(scClock == null) {
            throw new IllegalArgumentException("Null input SCLK.");
		}

		if (getCoarse() < scClock.getCoarse()) {
			return(-1);
		}

		if (getCoarse() > scClock.getCoarse())
		{
			return(1);
		}

		if (getFineUpperLimit() == scClock.getFineUpperLimit()) {
			
			if (getFine() < scClock.getFine()) {
				return(-1);
			}
	
			if (getFine() > scClock.getFine()) {
				return(1);
			}
			
			return 0;
		}
		
		// The following expression is treating each CoarseFineTime as a mixed number, where coarse ticks
		// are the whole number and fine ticks / maximum fine is the fraction.  The mixed numbers are converted
		// into full fractions, and then each numerator is multiplied by the others denominator.
		return Long.compareUnsigned(
				( coarse * (this.getFineUpperLimit() + 1) + fine ) * (scClock.getFineUpperLimit() + 1),
				(scClock.getCoarse() * (scClock.getFineUpperLimit() + 1) + scClock.getFine()) * (this.getFineUpperLimit() + 1)
				);
	}
	

    /**
     * {@inheritDoc}
     */
	@Override
	public int hashCode() {
		return new HashCodeBuilder()
			.append(dummyValue)
			.append(coarse)
			.append(fine)
			.append(maxFine)
			.append(cfEncoding)
			.toHashCode();
		
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDummy()
    {
        return dummyValue;
    }

	/**
     * {@inheritDoc}
     */
	@Override
    public CoarseFineEncoding getEncoding() {
		return cfEncoding;
	}

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public long getExact() {
        long exact = coarse * (cfEncoding.getMaxFine() + 1);
        exact += fine;
        return exact;
    }

	/**
     * {@inheritDoc}
     *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
            throw new IllegalArgumentException("Null input object.");
		} else if (!(obj instanceof CoarseFineTime)) {
            throw new IllegalArgumentException("Input object is not a CoarseFineTime.");
		}

		return (compareTo(obj) == 0);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
    public int compareTo(final Object obj) {
		if (obj == null) {
            throw new IllegalArgumentException("Null input object.");
		} else if ((obj instanceof CoarseFineTime) == false) {
            throw new IllegalArgumentException("Input object is not a Sclk.");
		}

		return (compare((ICoarseFineTime) obj));
	}

}
