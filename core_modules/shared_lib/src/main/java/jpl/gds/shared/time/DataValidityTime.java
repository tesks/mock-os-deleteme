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


import jpl.gds.shared.time.SclkFmt.DvtFormatter;

/**
 * The DVT class for MSAP products. In MSAP, DVT does not necessarily
 * have the same clock format as the packet SCLK, so we create a
 * different subclass to treat DVT as a separate breed.
 *
 */
public class DataValidityTime extends CoarseFineTime
{
	/** The regular expression format of a DVT */
	private static final String DVT_REGEXP;

    /** Coarse bit length */
	private static final int COARSE_BIT_LENGTH;

    /** Coarse byte length */
	private static final int COARSE_BYTE_LENGTH;

    /** Fine bit length */
	private static final int FINE_BIT_LENGTH;

    /** Fine byte length */
	private static final int FINE_BYTE_LENGTH;

    /** DVT bit length */
	private static final int DVT_BIT_LENGTH;

    /** DVT byte length */
	private static final int DVT_BYTE_LENGTH;

    /** Fractional separator string */
	private static final String FRACTIONAL_SEPARATOR;

    /** Ticks separator string */
	private static final String TICKS_SEPARATOR;

    /** Use-fractional-format flag */
	private static final boolean USE_FRACTIONAL_FORMAT;
	
	/** Fine time upper limit */
	private static final long FINE_UPPER_LIMIT;

    /** Minimum data validity time */
	public static final DataValidityTime MIN_DVT;

	static
	{
		COARSE_BIT_LENGTH = TimeProperties.getInstance().getDvtCoarseBitLength();
		if(COARSE_BIT_LENGTH > 32)
		{
			throw new IllegalStateException("The system cannot currently handle a DVT coarse length greater than 32 bits long.");
		}
		COARSE_BYTE_LENGTH = ((COARSE_BIT_LENGTH / Byte.SIZE) + (((COARSE_BIT_LENGTH % 8) > 0) ? 1 : 0));
		if(COARSE_BYTE_LENGTH > 4)
		{
			throw new IllegalStateException("The system cannot currently handle a DVT coarse byte length greater than 4 bytes.");
		}
		FINE_BIT_LENGTH = TimeProperties.getInstance().getDvtFineBitLength();
		if(FINE_BIT_LENGTH > 32)
		{
			throw new IllegalStateException("The system cannot currently handle a DVT fine length greater than 32 bits long.");
		}
		FINE_BYTE_LENGTH = ((FINE_BIT_LENGTH / Byte.SIZE) + (((FINE_BIT_LENGTH % 8) > 0) ? 1 : 0));
		if(FINE_BYTE_LENGTH > 4)
		{
			throw new IllegalStateException("The system cannot currently handle a DVT fine byte length greater than 4 bytes.");
		}
		DVT_BIT_LENGTH = COARSE_BIT_LENGTH + FINE_BIT_LENGTH;
		DVT_BYTE_LENGTH = COARSE_BYTE_LENGTH + FINE_BYTE_LENGTH;
		FRACTIONAL_SEPARATOR = TimeProperties.getInstance().getDvtFractionalSeparator();
		TICKS_SEPARATOR = TimeProperties.getInstance().getDvtTicksSeparator();
		USE_FRACTIONAL_FORMAT = TimeProperties.getInstance().useFractionalDvtFormat();
		DVT_REGEXP = "\\d{1,}([\\" + FRACTIONAL_SEPARATOR + TICKS_SEPARATOR + "]{1}\\d{1,}){0,1}";

		MIN_DVT = new DataValidityTime(0,0);


		FINE_UPPER_LIMIT = TimeProperties.getInstance().getDvtFineUpperLimit();
	}
	
	private static class DvtConfigHelper {
		private static final CoarseFineEncoding INSTANCE = TimeProperties.getInstance().getDvtEncoding();
	}

	private static class DvtFmtHelper {
		private static final DvtFormatter INSTANCE = TimeProperties.getInstance().getDvtFormatter();
	}

	/**
	 * Creates an instance of DVT. Length of DVT is loaded from the
	 * GDS configuration.
	 */
	public DataValidityTime() {
		super(0, 0, DvtConfigHelper.INSTANCE);
	}

	/**
	 * Creates an instance of DVT. Length of DVT is loaded from the
	 * GDS configuration.
     *
     * @param dummy True if a dummy value
	 */
	public DataValidityTime(final boolean dummy)
	{
        super(dummy, DvtConfigHelper.INSTANCE);
	}

	/**
	 *
	 * Creates an instance of DVT.  Length of DVT is loaded from the
	 * GDS configuration.
	 *
	 * @param secs The initial DVT secs count
	 */
	public DataValidityTime(final long secs)
	{
		super(secs, DvtConfigHelper.INSTANCE);
	}

	/**
	 *
	 * Creates an instance of DVT. Length of DVT is loaded from the
	 * GDS configuration.
	 * @param secs The initial DVT secs count
	 * @param fine The initial DVT fine count
	 */
	public DataValidityTime(final long secs, final long fine)
	{
		super(secs, fine, DvtConfigHelper.INSTANCE);
	}
	
	public DataValidityTime(long coarse, long fine, CoarseFineEncoding cfConfig) {
		super(coarse, fine, cfConfig);
	}

	public DataValidityTime(DataValidityTime dvt) {
		super(dvt.getCoarse(), dvt.getFine(), dvt.getEncoding());
	}

	/**
     * {@inheritDoc}
     *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		//TODO hunt down references and make them use sclkfmt
		return DvtFmtHelper.INSTANCE.fmt(this);
		//return String.format("coarse ticks: %d, fine ticks: %d");
	}

}
