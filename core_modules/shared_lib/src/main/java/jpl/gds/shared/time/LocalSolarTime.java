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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import jpl.gds.serialization.primitives.time.Proto3Adt;
import jpl.gds.serialization.primitives.time.Proto3Lst;

/**
 * Represents a Local Solar Time object. Contains formatting and conversion
 * methods.
 * 
 * For the constructors that require spacecraft ID in its signature, a
 * convenience factory class is provided (LocalSolarTimeFactory) in MPCS. This
 * is not available in the shared library, as it depends on GDS configuration.
 * 
 */
public class LocalSolarTime extends AccurateDateTime implements ILocalSolarTime {
	private static final long serialVersionUID = 1L;
	private static final long MS_IN_SECOND = 1000;
	private static final long MS_IN_MINUTE = MS_IN_SECOND * 60;
	private static final long MS_IN_HOUR = MS_IN_MINUTE * 60;
	private static final long MS_IN_DAY = MS_IN_HOUR * 24;
	private static final int SOL_MAX = 9999;

    // (8) + sol number (2)
    private IAccurateDateTime    scet0                       = TimeProperties.getInstance().getLstScet0();
    private static final String SOL_PREFIX                  = TimeProperties.getInstance().getLstPrefix() + "-";
    private static final int    solPrecision                = TimeProperties.getInstance().getLstPrecision();
    private static final double earthSecondConversionFactor = TimeProperties.getInstance()
            .getEarthSecondConversionFactor();

    private static final String TIME_TEMPLATE               = "00:00:00.000";
    private static final String SOL_TEMPLATE                = SOL_PREFIX + "0000";

    private SclkScetConverter   converter;

    private int                 sol;
    private double              leapSeconds;
    private double              decimalPortion;

	/**
	 * Constructor: Creates an instance of ILocalSolarTime representing the
	 * current time.
	 *
	 * @param scid S/C id
	 */
    public LocalSolarTime(final int scid) {
		super();
		/*  Catch case where the needed config values are not set. */
        if (earthSecondConversionFactor == 0.0) {
            throw new IllegalStateException(
                    "Attempting to create Local Solar Time object, but LST-related configuration values (earthSecondConversionFactor) are not defined in the time configuration file");
		}
		initializeConverter(scid);
	}
	
	/**
	 * Constructor. Creates an instance from the given millisecond time and 
	 * sol number.
	 * 
	 * @param time time in milliseconds
	 * @param solNum sol day number
	 */
    public LocalSolarTime(final long time, final int solNum) {
	    super(time);
	    sol = solNum;
	}

	/**
	 * Constructor: Creates a dummy instance of ILocalSolarTime
	 *
	 * @param dummy True if a dummy value
	 */
    public LocalSolarTime(final boolean dummy) {
		super(dummy);
	}

	/**
	 * Constructor: Creates an instance of ILocalSolarTime with a given scet.
	 * Creates a SCLK/SCET converter based on the spacecraft ID.
	 * 
	 * @param scid S/C id
	 * @param scet
	 *            will be converted to sol
	 */
    public LocalSolarTime(final int scid, final IAccurateDateTime scet) {
		super();
		initializeConverter(scid);
		scetToSol(scet);
	}

	/**
	 * Constructor: Creates an instance of ILocalSolarTime with a given sclk.
	 * Creates a SCLK/SCET converter based on the spacecraft ID.
	 * 
	 * @param scid S/C id
	 * @param sclk
	 *            will be converted to sol
	 */
    public LocalSolarTime(final int scid, final ISclk sclk) {
		super();
		initializeConverter(scid);
		sclkToSol(sclk);
	}

//	/**
//	 * Constructor: Creates an instance of ILocalSolarTime based on the given
//	 * length of time in milliseconds, which is assumed to represent local solar
//	 * hours, minutes, seconds, and milliseconds. Sol number is assumed to be 0.
//	 * 
//	 * @param scid S/C id
//	 * @param time Time in milliseconds
//	 */
    // public LocalSolarTime(final int scid, final long time) {
//		super(time);
//		initializeConverter(scid);
//	}

	/**
	 * Constructor: Creates an instance of ILocalSolarTime based on the given
	 * length of time in milliseconds, which is assumed to represent local solar
	 * hours, minutes, seconds, and milliseconds, and solar day number.
	 * 
	 * @param scid S/C id
	 * @param sol
	 *            solar day number
	 * @param time
	 *            local solar hours, minutes, seconds, and milliseconds as one
	 *            millisecond value
	 */
    public LocalSolarTime(final int scid, final int sol, final long time) {
		super(time, 0);
		this.sol = sol;
		initializeConverter(scid);
	}

	/**
	 * Constructor: Creates an instance of ILocalSolarTime by parsing given LST
	 * string
	 * 
	 * @param scid S/C id
	 * @param timeStr
	 *            must be in SOL-NNNNMHH:MM:SS:ttt.mmmmmm format
	 * 
	 * @throws ParseException If unable to parse
	 */
    public LocalSolarTime(final int scid, final String timeStr) throws ParseException {
		super();

		initializeConverter(scid);

		if (timeStr == null) {
			return;
		} else {
			parseSolString(timeStr);
		}
	}
	
	/**
     * Constructor: Creates an instance of ILocalSolarTime by parsing given LST
     * string
     * 
     * @param timeStr
     *            must be in SOL-NNNNMHH:MM:SS:ttt.mmmmmm format
     * 
     * @throws ParseException If unable to parse
     */
    public LocalSolarTime(final String timeStr) throws ParseException {
        super();

        if (timeStr == null) {
            return;
        } else {
            parseSolString(timeStr);
        }
    }


	/**
     * {@inheritDoc}
     */
	@Override
    public void parseSolString(String timeStr) throws ParseException {
		timeStr = fillOutTimeString(timeStr);
		final String[] pieces = timeStr.split("M");

		parseFromString("1970-000T" + pieces[1]);
		if (pieces[0].startsWith(SOL_PREFIX)) {
			pieces[0] = pieces[0].substring(SOL_PREFIX.length());
		}
		if (pieces[0].length() == 0 || !Character.isDigit(pieces[0].charAt(0))) {
			throw new ParseException(timeStr
					+ " cannot be parsed as an LST time", timeStr.length());
		}
		this.sol = Integer.valueOf(pieces[0]);
		if (sol > 9999) {
			throw new ParseException(timeStr
					+ " cannot be parsed as an LST time", timeStr.length());
		}

		// should be in form HH:MM:SS:mmm (mmm is optional)
		final double hours = Integer.valueOf(pieces[1].substring(0,
				pieces[1].indexOf(":")));
		final double minutes = Integer.valueOf(pieces[1].substring(
				pieces[1].indexOf(":") + 1, pieces[1].indexOf(":", 4)));
		final double seconds = Integer.valueOf(pieces[1].substring(
				pieces[1].indexOf(":", 4) + 1, pieces[1].indexOf(":", 4) + 3));
		double milliseconds = 0;

		if (pieces[1].contains(".")) {
			milliseconds = Integer.valueOf(pieces[1].substring(
					pieces[1].indexOf(".") + 1, 12));
		}

		final long lstWithoutDays = (long) (hours * MS_IN_HOUR + minutes
				* MS_IN_MINUTE + seconds * MS_IN_SECOND + milliseconds);

		// set date with year and month 0
		this.setTime(lstWithoutDays);

		// create scet from sol
		final IAccurateDateTime scet = this.toScet();

		// scet0 is greater than scet...this is bad news.
		if (scet0 == null || scet0.after(scet)) {
			this.leapSeconds = 0;
			this.sol = 0;
			this.setTime(0);
			return;
		}

		// set the leap seconds for this scet
		leapSeconds = getLeapSeconds(scet);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String getFormattedSol(final boolean suppressTrailingZeros) {
		return formatSafe(null, null, suppressTrailingZeros, solPrecision);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String getFormattedSolFast(final boolean suppressTrailingZeros) {
		synchronized (fastLock) {
			return formatSafe(cal, sb, suppressTrailingZeros, solPrecision);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.time.IAccurateDateTime#doFastFormat(java.util.Calendar,
	 *      java.lang.StringBuilder)
	 */
	@Override
	protected String doFastFormat(final Calendar localCal, final StringBuilder useSb) {
		return FastDateFormat.formatSol(this.sol, this, localCal, useSb);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public int getSolNumber() {
		return this.sol;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public double getLeapSeconds() {
		return this.leapSeconds;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setConverter(final SclkScetConverter converter) {
		this.converter = converter;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public SclkScetConverter getConverter() {
		return this.converter;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setScet0(final IAccurateDateTime scet0) {
		this.scet0 = scet0;
	}

	/**
     * {@inheritDoc}
     */
    @Override
    public ILocalSolarTime sclkToSol(final ISclk iSclk) {
		// map sclk to scet
        final IAccurateDateTime scet = converter.to_scet(iSclk, null);

		return scetToSol(scet);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public ILocalSolarTime scetToSol(final IAccurateDateTime scet) {

        // setScet0();

		// scet0 is greater than scet...this is bad news: return dummy LST
		if (scet0 == null || scet0.after(scet)) {
			this.leapSeconds = 0;
			this.sol = 0;
			this.setTime(0);
			return this;
		}

		// translate scet and scet0 into units of "milliseconds since the epoch"
		final long scetInMilliseconds = scet.getRoundedTimeAsMillis();
		final long scetZeroInMilliseconds = scet0.getRoundedTimeAsMillis();

		// subtract integer portion of DUTs to obtain leap seconds
		leapSeconds = getLeapSeconds(scet);

		// create date objects set hours, minutes, seconds (leave year and month
		// to be 0 and store SOL separately)

		// calculate LST = ((SCET - SCET0) + LeapSeconds) * conversionFactor
		final double lstInMilliseconds = ((scetInMilliseconds - scetZeroInMilliseconds) + (leapSeconds * 1000.0))
				* earthSecondConversionFactor;

		// store sol
		sol = (int) (Math.floor(lstInMilliseconds / MS_IN_DAY));

		// get milliseconds without the days included
		final long lstWithoutDays = (long) (lstInMilliseconds - (sol * MS_IN_DAY));

		// set date with year and month 0
		this.setTime(lstWithoutDays);

		if (sol > SOL_MAX) {
			// Log4jTracer.getDefaultTracer().warn("SOL number too large to compute LST time from SCET "

			// + scet.getFormattedScet(true) + "; SOL number set to 9999");
			sol = 9999;
		}

		return this;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public ISclk toSclk() {
		if (converter == null) {
			throw new IllegalStateException(
					"ILocalSolarTime object's converter is null--should not have occured");
		}

        // setScet0();

		final double lstInMilliseconds = this.getRoundedTimeAsMillis() + (sol * MS_IN_DAY);
		final long scetInMilliseconds = (long) Math
				.ceil((lstInMilliseconds / earthSecondConversionFactor)
						- (leapSeconds * 1000.0) + scet0.getRoundedTimeAsMillis());
		final IAccurateDateTime scet = new AccurateDateTime(scetInMilliseconds);

		return converter.to_sclk(scet, null);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public IAccurateDateTime toScet() {
        // setScet0();

		final double lstInMilliseconds = this.getRoundedTimeAsMillis() + (sol * MS_IN_DAY);
		final long scetInMilliseconds = (long) Math
				.floor(((lstInMilliseconds / earthSecondConversionFactor)
						- (leapSeconds * 1000.0) + scet0.getRoundedTimeAsMillis()));
		final IAccurateDateTime scet = new AccurateDateTime(scetInMilliseconds);

		return scet;
	}

	/**
	 * Helper method: calculates the leap seconds for a particular scet by
	 * finding the DUT values for scet and scet0.
	 * 
	 * @param scet
	 *            is the date for which leap seconds are needed
	 * @return number of leap seconds that have transpired since scet0 up until
	 *         scet
	 */
	private double getLeapSeconds(final IAccurateDateTime scet) {
		if (converter == null) {
			return 0.0;
		}
		final double dutOnScet =
            converter.getDut(converter.to_sclk(scet, null));
		final double dutOnScetZero =
            converter.getDut(converter.to_sclk(scet0, null));

		// subtract integer portion of DUTs to obtain leap seconds
		return Math.floor(dutOnScet) - Math.floor(dutOnScetZero);
	}

	/**
	 * Creates a sclk/scet converter based on the spacecraft id for the session
	 * configuration
	 */
	private void initializeConverter(final int scid) {
		this.converter = SclkScetUtility.getConverterFromSpacecraftId(scid);
	}


	/**
     * {@inheritDoc}
     */
	@Override
    public boolean equals(final IImmutableLocalSolarTime anotherDate) {
		if (anotherDate instanceof IImmutableLocalSolarTime) {
			return this.getSolNumber() == anotherDate.getSolNumber()
					&& this.getRoundedTimeAsMillis() == anotherDate.getRoundedTimeAsMillis();
		} else {
			throw new ClassCastException("ILocalSolarTime object required");
		}
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public int compareTo(final IImmutableLocalSolarTime anotherDate) throws ClassCastException {
		// TODO possibly need more error checking in case the sol or time
		// haven't been set
		if (anotherDate instanceof IImmutableLocalSolarTime) {
			final int solCompare = this.getSolNumber() < anotherDate.getSolNumber() ? -1
					: (this.getSolNumber() == anotherDate.getSolNumber() ? 0
							: 1);

			// if sol is not 0 then they are different so we already know which
			// one is before/after
			if (solCompare != 0) {
				return solCompare;
			}
			// else, we need to compare the hours, minutes, seconds which are
			// stored in time
			else {
				return this.getRoundedTimeAsMillis() < anotherDate.getRoundedTimeAsMillis() ? -1 : (this
						.getRoundedTimeAsMillis() == anotherDate.getRoundedTimeAsMillis() ? 0 : 1);
			}

		} else {
			throw new ClassCastException("ILocalSolarTime object required");
		}
	}


	/**
	 * {@inheritDoc}
	 *
	 * @see jpl.gds.shared.time.IAccurateDateTime#fillOutTimeString(java.lang.String)
	 */
	@Override
	protected String fillOutTimeString(String origTime) {
		if (origTime == null) {
			return null;
		}

		origTime = origTime.toUpperCase();

		if (origTime.indexOf('M') == -1) {
			if (origTime.startsWith(SOL_PREFIX)) {
				return origTime + 'M' + TIME_TEMPLATE;
			} else {
				if (origTime.length() < TIME_TEMPLATE.length()) {
					return SOL_TEMPLATE + 'M' + origTime
							+ TIME_TEMPLATE.substring(origTime.length());
				} else {
					return SOL_TEMPLATE + 'M' + origTime;
				}
			}
		} else if (origTime.indexOf('M') == 0) {
			if (origTime.length() < TIME_TEMPLATE.length()) {
				return SOL_TEMPLATE + origTime
						+ TIME_TEMPLATE.substring(origTime.length());
			} else {
				return SOL_TEMPLATE + origTime;
			}
		} else {
			final String pieces[] = origTime.split("M");
			if (pieces[1].length() < TIME_TEMPLATE.length()) {
				return pieces[0] + 'M' + pieces[1]
						+ TIME_TEMPLATE.substring(pieces[1].length());
			} else {
				return origTime;
			}
		}
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public long getSolExact() {
		return getRoundedTimeAsMillis() + sol * MS_IN_DAY;
	}


	/**
     * {@inheritDoc}
     */
	@Override
    public double getDecimalPortion() {
		return decimalPortion;
	}

	/**
	 * Formats this ILocalSolarTime using a custom format string. This method
	 * allows a 'SOL-' and 'xxxx' to be prepended to the basic java date/time
	 * format string. The former indicates an optional prefix while the latter
	 * indicates digits for the solar number.
	 * 
	 * assumptions: nanoseconds are NOT supported SOL number must be 4 digits
	 *
	 * @param formatString
	 *            custom format pattern ALA SimpleDateFormat
	 * @return formatted date/time
	 */
	@Override
	public String formatCustom(final String formatString) {

		SimpleDateFormat format = null;
		StringBuilder start = null;
		int solNumberLength = 0;

		try {
			format = getDateFormatFromPool();

			if (formatString.contains("xxxx")) {
				solNumberLength = 4;
			}

			// Store the "SOL-" prefix, which may be the empty string
			String prefix = "";

			if (formatString.indexOf('x') != -1) {
				prefix = formatString.substring(0, formatString.indexOf('x'));
			} else if (formatString.indexOf('M') != -1) {
				prefix = formatString.substring(0, formatString.indexOf("\'M"));
			} else if (formatString.indexOf('H') != -1) {
				prefix = formatString.substring(0, formatString.indexOf('H'));
			}

			// apply base pattern to format (sans prefix and sol number stuff)
			String basePattern = formatString;
			if (solNumberLength + prefix.length() > 0) {
				basePattern = formatString
						.substring(solNumberLength + prefix.length(),
								basePattern.length());
			}
			format.applyPattern(basePattern);

			// 1. begin constructing format. first add the prefix
			// replace any lingering single quotes in the prefix with empty
			// string
			start = new StringBuilder(prefix);

			// 2. add padding for sol number
			// add in zeros as necessary to pad sol number since it must be 4
			// digits
			if (solNumberLength != 0) {
				switch (solNumberLength - String.valueOf(this.sol).length()) {
				case 3:
					start.append("000");
					break;
				case 2:
					start.append("00");
					break;
				case 1:
					start.append("0");
					break;
				default:
					break;
				}

				// 3. append sol number
				start.append(this.sol);
			}

			// 4. append the baseformat
			start.append(format.format(this));

		}
		// catch (Exception e){
		// //TODO catch incorrect format patterns here?
		// }
		finally {
			releaseDateFormatToPool(format);
		}
		return start.toString();
	}

    @Override
    public Proto3Lst buildLocalSolarTime() {
    	final Proto3Lst.Builder retVal = Proto3Lst.newBuilder();
    	retVal.setSol(this.getSolNumber())
    			.setMilliseconds(this.getTime());
    	
    	return retVal.build();
    }
    
    @Override
    public void loadLocalSolarTime(final Proto3Lst msg) {
    	this.sol = msg.getSol();
    	this.setTime(msg.getMilliseconds());
    }

    @Override
    public void loadAccurateDateTime(final Proto3Adt msg) {
        final IAccurateDateTime scet = new AccurateDateTime(msg);
        scetToSol(scet);
    }

    @Override
    public Proto3Adt buildAccurateDateTime() {
        return toScet().buildAccurateDateTime();
    }
}
