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

/**
 * This generic class is to be used for creating CoarseFineTimes and their
 * subclasses from strings, as well as for writing those subclasses to
 * strings.
 * @param <T> type extending CoarseFineTime
 */
public abstract class SclkFmt<T extends ICoarseFineTime> {

	private final String ticksSep;
	private final String fracSep;
	private final boolean useFractional;
	private final CoarseFineEncoding cfConfig;
	private final String sclkRegex;

	private final int maxCoarseDigits;
	private final int maxFineDigits;

	/**
	 * Constructor.
	 * 
	 * @param ticksSep fine ticks separate string
	 * @param fracSep fine fraction separator string
	 * @param useFractional indicates whether to use fractional format
	 * @param cfConfig the CoarseFineEncoding object to use for formatting
	 */
	protected SclkFmt(final String ticksSep, final String fracSep, final boolean useFractional, final CoarseFineEncoding cfConfig) {
		this.ticksSep = ticksSep;
		this.fracSep = fracSep;
		this.useFractional = useFractional;
		this.cfConfig = cfConfig;
		this.sclkRegex = "\\d{1,}([\\" + fracSep + ticksSep + "]{1}\\d{1,}){0,1}";

		maxCoarseDigits = Long.toString(cfConfig.getMaxCoarse()).length();
		maxFineDigits = Long.toString(cfConfig.getMaxFine()).length();
	}

	/**
	 * Constructor.
	 * 
	 * @param cfConfig the CoarseFineEncoding object to use for formatting
	 */
	protected SclkFmt(final CoarseFineEncoding cfConfig) {
		this("-", ".", false, cfConfig);
	}

	/**
	 * @param coarseFineTime
	 *            the time object to format
	 * @return the formatted string representation of the input time
	 */
	public String fmt(final ICoarseFineTime coarseFineTime) {
		if (useFractional) {
			return toDecimalString(coarseFineTime);
		} else {
			return toTicksString(coarseFineTime);
		}
	}

	/**
	 * Parses a string and returns the value as an instance of type T.
	 * 
	 * @param timeString string containing a valid time value. Can be checked before
	 *            calling this method using the matches(String) method.
	 * @return object holding time data
	 */
	public T valueOf(final String timeString) {
		if (timeString == null) {
			throw new IllegalArgumentException("Null input SCLK string");
		} else if (!timeString.matches(sclkRegex)) {
			throw new IllegalArgumentException("Input SCLK string " + timeString + " does not match the SCLK"
					+ " regular expression: " + sclkRegex);
		}

		final int dash = timeString.indexOf(ticksSep);
		final int dot = timeString.indexOf(fracSep);
		long coarse = 0;
		long fine = 0;

		if (dash == -1 && dot == -1) {
			coarse = Long.parseLong(timeString);
		}
		// fine value is a decimal fraction
		else if (dot != -1) {
			coarse = Long.parseLong(timeString.substring(0, dot));
			final float fineOnly = Float.parseFloat("0." + timeString.substring(dot + 1));

			// (fineVal/1.0) = (X/fineUpperLimit)
			fine = Math.round(fineOnly * (cfConfig.getMaxFine() + 1));
		}
		// fine value is already in sub-ticks
		else if (dash != -1) {
			coarse = Long.parseLong(timeString.substring(0, dash));
			fine = Long.parseLong(timeString.substring(dash + 1));
		}
		return newInstance(coarse, fine, cfConfig);
	}

	/**
	 * Create a new instance of type T.
	 * 
	 * @param coarse the coarse value
	 * @param fine the fine value
	 * @param cfConfig the CoarseFineEncoding object to use for formattinge
	 * @return new instance
	 */
    protected abstract T newInstance(long coarse, long fine, CoarseFineEncoding cfConfig);

	/**
	 * Gets the flag indicating whether fractional fine formatting is in use.
	 * 
	 * @return true if using fractional format, false if not
	 */
	public boolean getUseFractional() {
		return useFractional;
	}

	/**
	 * Formats a coarse-fine time as a ticks string, i.e., coarse-fine where
	 * fine is sub-ticks.
	 * 
	 * @param sclk the time object to format
	 *
	 * @return the formatted string
	 */
	public String toTicksString(final ICoarseFineTime sclk) {
		if (sclk.isDummy()) {
			return "";
		}

		final StringBuilder buf = new StringBuilder(20);

		final String secs = String.valueOf(sclk.getCoarse());
		for (int i = 0; i < maxCoarseDigits - secs.length(); i++) {
			buf.append('0');
		}
		buf.append(secs + ticksSep);

		final String fine = String.valueOf(sclk.getFine());
		for (int i = 0; i < maxFineDigits - fine.length(); i++) {
			buf.append('0');
		}
		buf.append(fine);

		return (buf.toString());
	}

	/**
	 * Formats a coarse-fine time as a decimal string, i.e., coarse.fine where
	 * fine is fractional seconds, not ticks.
	 * @param sclk the time object to format
	 *
	 * @return the formatted string
	 */
	public String toDecimalString(final ICoarseFineTime sclk) {
		if (sclk.isDummy()) {
			return "";
		}

		final StringBuilder buf = new StringBuilder(20);

		final String secs = String.valueOf(sclk.getCoarse());
		final int coarseLength = maxCoarseDigits;
		final int secsLen = secs.length();
		for (int i = 0; i < coarseLength - secsLen; i++) {
			buf.append('0');
		}
		buf.append(secs);

		buf.append(fracSep);

		/** Use doubles in the math to calculate
		 *  the fraction of coarse ticks. Otherwise, 32-bit fine SCLK have grossly incorrect fractions.
		 */
		final double decimalFine = (double) sclk.getFine() / (double) (sclk.getFineUpperLimit() + 1);
		final long intFine = Math.round(decimalFine * (Math.pow(10, maxFineDigits)));

		String fine = String.valueOf(intFine);

		if (fine.length() > maxFineDigits) {
			fine = fine.substring(0, maxFineDigits);
		}
		for (int i = 0; i < maxFineDigits - fine.length(); i++) {
			buf.append('0');
		}
		buf.append(fine);

		return (buf.toString());
	}

	/**
	 * Checks if a given string is a valid coarse-fine clock string.
	 * 
	 * @param inputVal
	 *            the string to check
	 * @return true if the input contains a valid coarse-fine clock
	 */
	public boolean matches(final String inputVal) {
		return inputVal.matches(sclkRegex);
	}

	/**
	 * @return the configured separator for a tick-style string
	 */
	public String getTicksSep() {
		return ticksSep;
	}

	/**
	 * @return the configurated separator for a fractional-style string
	 */
	public String getFracSep() {
		return fracSep;
	}

	/**
	 * @return true if the formatter is configured to print coarse-fine times
	 *         using the fractional format by default
	 */
	public boolean usesFractional() {
		return useFractional;
	}
	
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("fractional separator: ")
			.append(fracSep)
			.append(" ")
			.append("tick separator: ")
			.append(ticksSep)
			.append(" ")
			.append("using fractional format: ")
			.append(useFractional);
		return builder.toString();
	}

	/**
	 * A specific SclkFmt class for ISclk objects.
	 *
	 */
    public static class SclkFormatter extends SclkFmt<ISclk> {
		/**
		 * Configurable class that should be used for writing ISclk objects to
		 * strings, as well as parsing them from string.
		 */

		/**
		 * Construct a SclkFormatter instance, using the default formatting.
		 * 
		 * @param cfConfig
		 *            the configuration to use with all created
		 *            DataValidityTimes
		 */
		public SclkFormatter(final CoarseFineEncoding cfConfig) {
			super(cfConfig);
		}

		/**
		 * Construct a SclkFormatter instance, using the given format
		 * parameters.
		 * 
		 * @param ticksSep
		 *            a string that should separate coarse and fine fields when
		 *            tick formatting is used
		 * @param fracSep
		 *            a string that should separate coarse and the fraction of
		 *            coarse when fractional formatting is used
		 * @param useFractional
		 *            indicates whether the fmt(CoarseFineTime) method will
		 *            default to fractional format or not.
		 * @param cfConfig
		 *            the configuration to use with all created
		 *            DataValidityTimes
		 */
		public SclkFormatter(final String ticksSep, final String fracSep, final boolean useFractional, final CoarseFineEncoding cfConfig) {
			super(ticksSep, fracSep, useFractional, cfConfig);
		}

		@Override
		protected ISclk newInstance(final long coarse, final long fine, final CoarseFineEncoding cfConfig) {
			return new Sclk(coarse, fine, cfConfig);
		}

	}

	/**
	 * A specific SclkFmt class for DataValidityTime objects.
	 *
	 */
	public static class DvtFormatter extends SclkFmt<DataValidityTime> {

		/**
		 * Configurable class that should be used for writing DataValidityTime
		 * objects to strings, as well as parsing them from string.
		 */

		/**
		 * Construct a DvtFormatter instance, using the default formatting.
		 * 
		 * @param cfConfig
		 *            the configuration to use with all created
		 *            DataValidityTimes
		 */
		public DvtFormatter(final CoarseFineEncoding cfConfig) {
			super(cfConfig);
		}

		/**
		 * Construct a DvtFormatter instance, using the given format parameters.
		 * 
		 * @param ticksSep
		 *            a string that should separate coarse and fine fields when
		 *            tick formatting is used
		 * @param fracSep
		 *            a string that should separate coarse and the fraction of
		 *            coarse when fractional formatting is used
		 * @param useFractional
		 *            indicates whether the fmt(DataValidityTime) method will
		 *            default to fractional format or not.
		 * @param cfConfig
		 *            the configuration to use with all created
		 *            DataValidityTimes
		 */
		public DvtFormatter(final String ticksSep, final String fracSep, final boolean useFractional, final CoarseFineEncoding cfConfig) {
			super(ticksSep, fracSep, useFractional, cfConfig);
		}

		@Override
		protected DataValidityTime newInstance(final long coarse, final long fine, final CoarseFineEncoding cfConfig) {
			return new DataValidityTime(coarse, fine, cfConfig);
		}
	}

}
