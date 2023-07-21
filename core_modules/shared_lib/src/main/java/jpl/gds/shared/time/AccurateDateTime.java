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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;

import jpl.gds.serialization.primitives.time.Proto3Adt;
import jpl.gds.shared.annotation.AssumesAccurateDateTimeIsDateObject;
import jpl.gds.shared.types.Pair;


/**
 * IAccurateDateTime is a date/time class that currently supports up to
 * nanosecond accuracy.
 *
 * dummyValue is used in cases like MonitorChannelValue where you need a SCET
 * but don't have a value for it. Setting it true gives you a SCET that has a
 * zero value but prints as an empty string.
 *
 * This thing should be rethought and rewritten.
 *
 */
public class AccurateDateTime extends Date implements IAccurateDateTime
{
    /** True means that this is not a real value and prints as "" */
    private boolean dummyValue = false;
	private static final String GMT_TIME_ZONE = "GMT";
	private static final long serialVersionUID = 1L;
	private static final int ISO_LEN = 23;
    private static final int DOY_LEN = 21;

    /**  Increased to 9 to support extended SCET */
    private static final int MAX_PRECISION = 9;

    private static final String DOY_TEMPLATE = "1970-001T00:00:00.000";
    private static final String YMD_TEMPLATE = "1970-01-01T00:00:00.000";

    /** Local string builder */
    protected static final StringBuilder sb  = new StringBuilder();

    /** Standard calendar */
    protected static final Calendar      cal = FastDateFormat.getStandardCalendar();

    /*
     * Object synchronization lock for static sb and cal instances
     */
    protected static final Object fastLock = new Object();

    // private static final int ertPrecision =
    // TimeProperties.getInstance().getErtPrecision();
    // private static final int scetPrecision =
    // TimeProperties.getInstance().getScetPrecision();
    private long nanoseconds;
    private static ConcurrentLinkedQueue<SimpleDateFormat> customFormatters = 
        new ConcurrentLinkedQueue<SimpleDateFormat>();

    private static final BigInteger MILLION_I  = BigInteger.valueOf(1_000_000L);
    private static final BigDecimal THOUSAND_D = BigDecimal.valueOf(1_000L);
    private static final BigDecimal BILLION_D  = BigDecimal.valueOf(1_000_000_000L);
    
   
    /**
     * Creates an instance of IAccurateDateTime representing the current time.
     */
    public AccurateDateTime()
    {
        super();

        nanoseconds = 0L;
    }


    /**
     * Creates an instance of IAccurateDateTime as a zero time, possibly set
     * as a dummy.
     *
     * @param dummy True if a dummy
     */
    public AccurateDateTime(final boolean dummy)
    {
        super(0L);

        nanoseconds = 0L;
        dummyValue  = dummy;
    }


    /**
     * Creates an instance of IAccurateDateTime by parsing the given
     * date/time string.  The format must be either YYYY-MM-DDYHH:mm:ss.ttt[mmm[n]]
     * or YYYY-DOYTHH:mm:ss.ttt[mmm[n]]
     * 
     * @param timeStr the date/time string to parse
     * @throws ParseException if the string cannot be parsed
     */
    public AccurateDateTime(final String timeStr) throws ParseException {
        super();
        if (timeStr == null) {
        	return;
        }

        parseFromString(fillOutTimeString(timeStr));
    }

    /**
     * 
     * Creates an instance of IAccurateDateTime.
     * 
     * @param baseDate
     *            the date up to millisecond accuracy
     * @param micros
     *            microseconds or tenths of microseconds, up to 9999
     * @param isTenths
     *            true if the microseconds argument is actually tenths of microseconds
     */
    public AccurateDateTime(final Date baseDate, final long micros, final boolean isTenths) {
        this(baseDate.getTime(), micros, isTenths);
    }

    /**
     * 
     * Creates an instance of IAccurateDateTime.
     * 
     * @param baseDate
     *            the date up to millisecond accuracy
     * @param nanos
     *            nanoseconds, from 0 to 999999
     *
     */
    public AccurateDateTime(final long baseDate, final long nanos) {
        super(baseDate);
        this.nanoseconds = nanos;
        if (this.nanoseconds > 999999 || this.nanoseconds < 0) {
            throw new IllegalArgumentException("nanoseconds must be greater than 0 and not greater than 999999");
        }
    }

    /**
     * 
     * Creates an instance of IAccurateDateTime.
     * 
     * @param baseDate
     *            the date up to millisecond accuracy
     * @param micros
     *            microseconds or tenths of microseconds, up to 9999
     * @param isTenths
     *            true if the microseconds argument is actually tenths of microseconds
     */
    public AccurateDateTime(final long baseDate, final long micros, final boolean isTenths) {
        super(baseDate);
        if (isTenths) {
            this.nanoseconds = micros * 100;
        }
        else {
            this.nanoseconds = micros * 1000;
        }
        if (this.nanoseconds < 0 || this.nanoseconds > 999900) {
            throw new IllegalArgumentException("Microseconds must be greater than 0 and not greater than 9999");
        }
    }

    /**
     * Creates an instance of IAccurateDateTime.
     * 
     * @param baseDate
     *            the long representation of the base date, up to millisecond accuracy
     */
    public AccurateDateTime(final long baseDate) {
        this(baseDate, 0, false);
    }

    /**
     * Creates an instance of IAccurateDateTime.
     * 
     * @param baseDate
     *            the long representation of the base date, up to millisecond accuracy
     */
    public AccurateDateTime(final Date baseDate) {
        this(baseDate, 0, false);
    }

    /**
     * Copy Constructor for IAccurateDateTime.
     * Creates an instance of IAccurateDateTime from another IAccurateDateTime object.
     * 
     * @param baseDate
     *            the IAccurateDateTime from which to copy
     */
    public AccurateDateTime(final IAccurateDateTime baseDate) {
        this(baseDate.getTime(), baseDate.getNanoseconds());
    }

    /**
     * Copy Constructor for IAccurateDateTime.
     * Creates an instance of IAccurateDateTime from another IAccurateDateTime object.
     * 
     * @param baseDate
     *            the IAccurateDateTime from which to copy
     * @param micros
     *            microseconds or tenths of microseconds, up to 9999
     * @param isTenths
     *            true if the microseconds argument is actually tenths of microseconds
     */
    public AccurateDateTime(final IAccurateDateTime baseDate, final long micros, final boolean isTenths) {
        this(baseDate.getTime(), micros, isTenths);
    }
    
    /**
     * Creates an instance of IAccurateDateTime
     * 
     * @param msg
     *            the protobuf representation of the base date, up to nanosecond
     *            accuracy
     */
    public AccurateDateTime(final Proto3Adt msg) {
        this(msg.getMilliseconds(), msg.getNanoseconds());
    }
    
    /**
     * Parses the given date/time string.The format must be either YYYY-MM-DDYHH:mm:ss.ttt[mmm[n]]
     * or YYYY-DOYTHH:mm:ss.ttt[mmm[n]]
     * 
     * @param timeStr the date/time string to parse
     * @throws ParseException if the string cannot be parsed
     */
    protected void parseFromString(final String timeStr) throws ParseException {
        final String inputStr = timeStr;
         final DateFormat dIso = TimeUtility.getISOFormatterFromPool();
         final DateFormat dDoy = TimeUtility.getDoyFormatterFromPool();
         Date baseDate = null;
         int baseLen = 0;
         try {
             try {
                 baseDate = dIso.parse(inputStr.substring(0, Math.min(inputStr.length(), ISO_LEN)));
                 baseLen = Math.min(ISO_LEN, inputStr.length());
             } catch (final ParseException e) {
                 baseDate = dDoy.parse(inputStr.substring(0, Math.min(inputStr.length(), DOY_LEN)));
                 baseLen = Math.min(DOY_LEN, inputStr.length());
             }
             setTime(baseDate.getTime());
  
             final StringBuilder nanoStr = new StringBuilder(inputStr.substring(baseLen));
             if (nanoStr.length() == 0)
             {
                 this.nanoseconds = 0L;

                 return;
             }

             /** Allow full nanoseconds to support extended SCET */

             if (nanoStr.length() > 6) {
                 throw new ParseException("Date/time string " + timeStr + 
                         " exceeds supported resolution of 999999 microseconds", timeStr.length());         
             }

             while (nanoStr.length() < 6) {
                 nanoStr.append('0');
             }
             
             this.nanoseconds = Long.parseLong(nanoStr.toString());

             if (this.nanoseconds > 999999 || this.nanoseconds < 0) {
                 throw new ParseException("Date/time string " + timeStr + 
                         " exceeds supported resolution of 999999 microseconds", timeStr.length());               
             }
         } catch (final ParseException  e) {
             throw e;
         } catch (final NumberFormatException e) {
            throw new ParseException("Unable to parse date time string " + timeStr + ", " + e.toString(),
                                     timeStr.length());
         } catch (final Exception e) {
             e.printStackTrace();
             throw new ParseException("Unable to parse date time string " + timeStr + ", " + 
                     e.toString(), timeStr.length());
         } finally {
             if (dIso != null) {
                 TimeUtility.releaseISOFormatterToPool(dIso);
             }
             if (dDoy != null) {
                 TimeUtility.releaseDoyFormatterToPool(dDoy);
             }
         }
    }
    
    /**
     * Gets the extended portion of the date, beyond the milliseconds, as nanoseconds. Note
     * this includes the microseconds.
     * @return the nanoseconds
     */
    @Override
    public long getNanoseconds() {
        return this.nanoseconds;
    }
    
    /**
     * Gets the extended portion of the date as microseconds, truncating any nanoseconds. 
     * @return the microseconds
     */
    @Override
    public long getMicros() {
        return this.nanoseconds / 1000;
    }
    
    /**
     * Gets the extended portion of the date as tenths of microseconds, truncating any nanoseconds 
     * beyond that. 
     * @return tenths of microseconds
     */
    @Override
    public long getMicroTenths() {
        return this.nanoseconds / 100;
    }
    

    /**
     * Formats the accurate date time as YYYY-MM-DDTHH:mm:ss.ttt[mmm[n]]
     *
     * The code was very buggy and only worked because the
     * precision was only ever 7 or 3. This is now fixed for but needs to be
     * rethought WRT rounding. It does not round now (and never did properly).
     *
     * It is possible to round when dividing down jnano; but in that case you
     * must check for the value rounding up to a value too large! If so you
     * must increment the milliseconds (which may increment the seconds, and so on),
     * but that part is already formatted!
     *
     * If the precision is three, the nanos are not looked at at all! What if the nanos
     * are 0.5 milliseconds? They should increment the milliseconds, but it is too late.
     *
     * Fixing the rounding (if that
     * is even desired anymore) is out of scope.
     *
     * NB: ERT should be OK because the two digits beyond the seven it wants by default
     * will always be zero anyway. SCET will be OK because it wants all the digits.
     * If the defaults are altered, that's when the lack of rounding can show up.
     *
     * @param localCal              Calendar to use
     * @param localSb               String builder to use (or null)
     * @param suppressTrailingZeros True if trailing 0s beyond milliseconds
     *                                  should be stripped
     * @param precision             Precision desired
     *
     * @return the formatted string
     *
     */
    protected String formatSafe(final Calendar      localCal,
                                final StringBuilder localSb,
                                final boolean       suppressTrailingZeros,
                                final int           precision)
    {
        if (dummyValue)
        {
            return "";
        }

        final StringBuilder useSb = (localSb != null) ? localSb
                                                      : new StringBuilder();

        final String baseDate = doFastFormat(localCal, useSb);

        if (precision == 3)
        {
            // We don't want nano
            return baseDate;
        }

        long jnano = this.nanoseconds;

        for (int i = MAX_PRECISION; i > precision; i--)
        {
        	jnano = jnano / 10L;
        }

        if ((jnano == 0L) && suppressTrailingZeros)
        {
            // We don't need nano at all
            // because it cannot contribute anything but zeroes
            return baseDate;
        }

        useSb.setLength(0);

        useSb.append(jnano);

        for (int i = (precision - useSb.length() - 3); i > 0; --i)
        {
            useSb.insert(0, "0");
        }

        if (suppressTrailingZeros)
        {
            int length = useSb.length();

            for (int i = length - 1; i >= 0; --i)
            {
                if (useSb.charAt(i) != '0')
                {
                    break;
                }

                --length;
            }

            useSb.setLength(length);
        }

        useSb.insert(0, baseDate);

        return useSb.toString();
    }


    /**
     * Fast formats the accurate date time as an ERT string of the format YYYY-MM-DDTHH:mm:ss.ttt[mmm[n]]
     * 
     * @param localCal Calendar to use
     * @param useSb    String builder to use (or null)
     *
     * @return the formatted string
     */
    protected String doFastFormat(final Calendar localCal, final StringBuilder useSb)
    {
        if (dummyValue)
        {
            return "";
        }

        return FastDateFormat.format(this, localCal, useSb);
    }


    /**
     * Formats the accurate date time as an ERT string of the format YYYY-MM-DDTHH:mm:ss.ttt[mmm[n]]
     * 
     * @param localCal              Calendar to use
     * @param builder               String builder to use (or null)
     * @param suppressTrailingZeros True if trailing 0s beyond milliseconds
     *                                  should be stripped
     *
     * @return the formatted string
     */
    @Override
    public String getFormattedErtSafe(final Calendar localCal, final StringBuilder builder, final boolean suppressTrailingZeros)
    {
        return formatSafe(localCal, builder, suppressTrailingZeros, TimeProperties.getInstance().getErtPrecision());
    }
    

    /**
     * Formats the accurate date time as an ERT string of the format YYYY-MM-DDTHH:mm:ss.ttt[mmm[n]]
     *
     * @param suppressTrailingZeros True if trailing 0s beyond milliseconds
     *                                  should be stripped
     *
     * @return the formatted string
     */
    @Override
    public String getFormattedErt(final boolean suppressTrailingZeros)
    {
        return formatSafe(null, null, suppressTrailingZeros, TimeProperties.getInstance().getErtPrecision());
    }
    
    /**
     * Formats the accurate date time as an ERT string of format YYYY-MM-DDTHH:mm:ss.ttt[mmm[n]] using a faster,
     * method than getDsnFormat().
     * adding synchronization. since the introduction of TI/TP, this method
     * can be used within threaded contexts unpredictably. Benchmarks have shown that adding synchronization
     * causes a minimal performance hit in exchange for a safer method call.
     * 
     * @param suppressTrailingZeros true if trailing 0s beyond milliseconds should be stripped
     *
     * @return the formatted string
     */
    @Override
    public String getFormattedErtFast(final boolean suppressTrailingZeros)
    {
        synchronized (fastLock) {
            return formatSafe(cal, sb, suppressTrailingZeros, TimeProperties.getInstance().getErtPrecision());
        }
    }

    

    /**
     * Formats the accurate date time as a SCET string of format YYYY-MM-DDTHH:mm:ss.ttt[mmm[n]]
     *
     * @param suppressTrailingZeros True if trailing 0s beyond milliseconds
     *                                  should be stripped
     *
     * @return the formatted string
     */
    @Override
    public String getFormattedScet(final boolean suppressTrailingZeros)
    {
        return formatSafe(null, null, suppressTrailingZeros, TimeProperties.getInstance().getScetPrecision());
    }
    
    
    /**
     * Formats the accurate date time as as SCET of format YYYY-MM-DDTHH:mm:ss.ttt[mmm[n]] using a faster,
     * method than getDsnFormat().
     * adding synchronization. since the introduction of TI/TP, this method
     * can be used within threaded contexts unpredictably. Benchmarks have shown that adding synchronization
     * causes a minimal performance hit in exchange for a safer method call.
     * 
     * @param suppressTrailingZeros true if trailing 0s beyond milliseconds should be stripped
     *
     * @return the formatted string
     */
    @Override
    public String getFormattedScetFast(final boolean suppressTrailingZeros)
    {
        synchronized (fastLock) {
            return formatSafe(cal, sb, suppressTrailingZeros, TimeProperties.getInstance().getScetPrecision());
        }
    }


    /* (non-Javadoc)
     * @see java.util.Date#hashCode()
     */
    @Override
    public int hashCode() {
        return (int)(super.hashCode() + this.nanoseconds);
    }
    
    /* (non-Javadoc)
     * @see java.util.Date#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        
        if (!(o instanceof IAccurateDateTime)) {
            if (o instanceof Date) {
                return super.equals(o) && this.nanoseconds == 0;
            } else {
                return false;
            }
        }
         
        final IAccurateDateTime compare = (IAccurateDateTime)o;
        final boolean superEquals = super.equals(o);
        final boolean nanosEqual = compare.getNanoseconds() == this.nanoseconds;
        return superEquals && nanosEqual;
    }


    /**
     * Compares two Dates for ordering.
     *
     * @param anotherAccurateDateTime
     *            the <code>IAccurateDateTime</code> to be compared.
     * @return the value <code>0</code> if the argument Date is equal to
     *         this Date; a value less than <code>0</code> if this Date
     *         is before the Date argument; and a value greater than
     *         <code>0</code> if this Date is after the Date argument.
     * @exception NullPointerException
     *                if <code>anotherDate</code> is null.
     */
    @Override
    @AssumesAccurateDateTimeIsDateObject
    public int compareTo(final IAccurateDateTime anotherAccurateDateTime) {
        if (anotherAccurateDateTime instanceof Date) {
            return compareTo((Date) anotherAccurateDateTime);
        }
        else {
            throw new ClassCastException("IAccurateDateTime object required");
        }
    }

	/* (non-Javadoc)
	 * @see java.util.Date#compareTo(java.util.Date)
	 */
    @Override
    @AssumesAccurateDateTimeIsDateObject
    public int compareTo(final Date anotherDate) throws ClassCastException {
    	if (anotherDate instanceof IAccurateDateTime) {
            final int superRes = super.compareTo(anotherDate);
    		if (superRes == 0) {
                final IAccurateDateTime anotherAccurateDateTime = (IAccurateDateTime) anotherDate;
    			final long thisNanoSec = this.nanoseconds;
    			final long anotherNanoSec = anotherAccurateDateTime.getNanoseconds();
                return (thisNanoSec < anotherNanoSec ? -1 : (thisNanoSec == anotherNanoSec ? 0 : 1));
    		} else {
    			return superRes;
    		}
    	} else {
    		throw new ClassCastException("IAccurateDateTime object required");
    	}
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean before(final IAccurateDateTime when) {
        final long millis = this.getTime() - when.getTime();
        return (millis == 0) ? this.nanoseconds < when.getNanoseconds() : millis < 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean after(final IAccurateDateTime when) {
        final long millis = this.getTime() - when.getTime();
        return (millis == 0) ? this.nanoseconds > when.getNanoseconds() : millis > 0;
    }

    /**
     * Performs addition and subtraction arithmetic of an
     * IAccurateDateTime instance. <code>forward</code> flag indicates
     * whether to add to (<code>true</code>) or subtract from
     * (<code>negative</code>) the instance. The term accepted is in
     * the form of milliseconds and nanosecond fraction.
     * 
     * Nanosecond argument will be rounded to the nearest 100,
     * consistent with the supported granularity of this class.
     * 
     * @param termInMillis milliseconds to add/subtract
     * @param nanoTerm nanoseconds to add/subtract in addition to milliseconds
     * @param isAdd add if <code>true</code>, subtract if <code>false</code>
     * @return new IAccurateDateTime object containing the result time instance
     * @throws IllegalArgumentException If time values out of range
     */
    @Override
    public IAccurateDateTime roll(final long termInMillis,
    		final long nanoTerm,
    		final boolean isAdd)
    throws IllegalArgumentException {

    	if (nanoTerm > 999999 || nanoTerm < 0) {
    		throw new IllegalArgumentException("nanoTerm " + nanoTerm + " falls outside acceptable range [0-999999]");
    	} else if (termInMillis < 0) {
    		throw new IllegalArgumentException("termInMillis " + termInMillis + " can't be negative");
    	}

    	long resultMillis = this.getTime();
    	long resultNanos = this.nanoseconds;

    	// Round the new term to the nearest 100 nanoseconds
    	final Pair<Long, Long> roundedRightTerm = roundToHundredNanos(termInMillis, nanoTerm);

    	// Now add or subtract time
    	if (isAdd) {
    		// Addition
    		resultNanos += roundedRightTerm.getTwo().longValue();

    		if (resultNanos > 999900) {
    			resultMillis++;
    			resultNanos -= 1000000;
    		}

    		resultMillis += roundedRightTerm.getOne().longValue();

    	} else {
    		// Subtraction

    		final long rightMillis = roundedRightTerm.getOne().longValue();
    		final long rightNanos = roundedRightTerm.getTwo().longValue();
    		
    		if (rightMillis > resultMillis ) {
    			throw new IllegalArgumentException("Millisec term can't be greater than original for subtraction: " +
    					resultMillis + " - " + rightMillis);
    		} else if (rightMillis == resultMillis && rightNanos > resultNanos) {
    			throw new IllegalArgumentException("Nanosec term can't be greater than original for subtraction: " +
    					resultNanos + " - " + rightNanos);
    		}

    		if (rightNanos > resultNanos) {
    			resultMillis--;
    			resultNanos += 1000000;
    		}

    		resultNanos -= rightNanos;
    		resultMillis -= rightMillis;
    	}
    	
    	return new AccurateDateTime(resultMillis, resultNanos);
    }

    /**
     * Rounds the nanoseconds to the nearest 100, and returns the
     * result in Pair of milliseconds and nanoseconds.
     * 
     * @param millis Milliseconds
     * @param nanos  Nanoseconds
     * @return Pair<Long, Long> object containing resulting
     * 		   milliseconds and nanoseconds, respectively.
     */
    protected Pair<Long, Long> roundToHundredNanos(final long millis, final long nanos) {
    	
    	if (nanos % 100 == 0)
        {
    		return new Pair<Long, Long>(millis, nanos);
    	}
    	
    	long m = millis, n = nanos;
    	final long mod = n % 100;
    	
    	if (mod < 50) {
    		n -= mod;
    	} else {
    		n += (100 - mod);
    		
    		if (n >= 1000000) {
    			n -= 1000000;
    			m++;
    		}
    	}

    	return new Pair<Long, Long>(m, n);
    }
    
    /**
     * Formats this IAccurateDateTime using a custom format string. This method allows 'e' to be appended to the 
     * basic java date/time format string to indicate how many extended digits (beyond milliseconds) are desired.
     * @param formatString custom format pattern ALA SimpleDateFormat
     * @return formatted date/time
     */
    @Override
    public String formatCustom(final String formatString) {
    	SimpleDateFormat format = null;
    	StringBuilder start = null;
    	try {
    		format = getDateFormatFromPool();
	    	int extendedLength = 0;
	    	for (int i = formatString.length() - 1 ; i > 0; i--) {
	    		final char c = formatString.charAt(i);
	    		if (c == 'e') {
	    			extendedLength++;
	    		} else {
	    			break;
	    		}
	    	}
	    	
	    	String basePattern = formatString;
	    	if (extendedLength > 0) {
	    		basePattern = formatString.substring(0, basePattern.length() - extendedLength);
	    	}
	    	int maxDigitsToRemove = 6;
	       	format.applyPattern(basePattern);
	    	extendedLength = Math.min(extendedLength, maxDigitsToRemove);
	    	start = new StringBuilder(format.format(this));
	   	    
	    	if (extendedLength != 0) {
	    		long shiftedNanos = this.nanoseconds;
	    		double numberOfDigitsToRemove = maxDigitsToRemove - extendedLength;
	    		shiftedNanos = this.nanoseconds / (long)Math.pow(10.0, numberOfDigitsToRemove);
	    		
	    		final String nanoString =  String.valueOf(shiftedNanos);
	    		int numberOfZerosToAppend = extendedLength - nanoString.length();
				if (numberOfZerosToAppend <= maxDigitsToRemove) {
					for (int k = 0; k < numberOfZerosToAppend; k++) {
						start.append("0");
					}	    			
	    		}

	    		start.append(nanoString);
	    		
	    	}
    	} finally {
    		releaseDateFormatToPool(format);
    	}
    	return start.toString();
    }
    

    /**
     * Get date formatter from pool.
     *
     * @return Formatter
     */    
    protected SimpleDateFormat getDateFormatFromPool()
    {
    	SimpleDateFormat df = customFormatters.poll();
        if (df == null) {
        	 df = new SimpleDateFormat();
        	 df.setTimeZone(TimeZone.getTimeZone(GMT_TIME_ZONE));
             return(df);
        }
        return df;
    }


    /**
     * Release date formatter to pool.
     *
     * @param df Formatter to release
     */    
    protected void releaseDateFormatToPool(final SimpleDateFormat df) {
    	customFormatters.add(df);
    }


    /**
     * Expands the given time string to fill out all digits not supplied in the input string
     * @param origTime the original time string
     * @return the expanded time string
     */
    protected String fillOutTimeString(final String origTime) {
    	if (origTime == null) {
    		return null;
    	}
    	String templateStr = DOY_TEMPLATE;
		if (origTime.lastIndexOf("-") != origTime.indexOf("-")) {
			templateStr = YMD_TEMPLATE;
		}
    
    	if (templateStr.length() <= origTime.length()) {
    		return origTime;
    	}
    	final String result = origTime + templateStr.substring(origTime.length());
    	return result;
    }


    /**
     * Get is-dummy state.
     *
     * @return True if value is a dummy
     */
    @Override
    public boolean isDummy()
    {
        return dummyValue;
    }

    /**
     * Compare with another IAccurateDateTime.
     *
     * @param anotherDate The other guy
     *
     * @return Compare result
     *
     */
    @Override
    public int fastCompareTo(final IAccurateDateTime anotherDate)
    {
        final int superRes = compareTo(anotherDate);

        if (superRes != 0)
        {
            return superRes;
        }

        final long anotherNanoSec = anotherDate.getNanoseconds();

        if (nanoseconds > anotherNanoSec)
        {
            return 1;
        }

        if (nanoseconds < anotherNanoSec)
        {
            return -1;
        }

        return 0;
    }


    /**
     * Convert to fractional seconds.
     *
     * @return BigDecimal
     *
     */
    @Override
    public BigDecimal asFractionalSeconds()
    {
        final long time = getTime();

        if (nanoseconds == 0L)
        {
            return BigDecimal.valueOf(time).divide(THOUSAND_D,
                                                   MathContext.DECIMAL128);
        }

        final BigInteger bi =
            BigInteger.valueOf(time).multiply(MILLION_I).add(
                BigInteger.valueOf(nanoseconds));

        return new BigDecimal(bi).divide(BILLION_D, MathContext.DECIMAL128);
    }


    /**
     * Return time as milliseconds, rounding up nanoseconds.
     *
     * @return Time as milliseconds, rounded
     *
     */
    @Override
    public long getRoundedTimeAsMillis()
    {
        long millis = getTime();

        if (nanoseconds >= 500000L)
        {
            ++millis;
        }

        return millis;
    }
    
    @Override
    public Proto3Adt buildAccurateDateTime() {
        final Proto3Adt.Builder retVal = Proto3Adt.newBuilder();
    	retVal.setMilliseconds(this.getTime())
				.setNanoseconds(this.getNanoseconds());
    	
    	return retVal.build();
    }
    
    @Override
    public void loadAccurateDateTime(final Proto3Adt msg) {
    	this.setTime(msg.getMilliseconds());
    	this.nanoseconds = msg.getNanoseconds();
    }

    @Override
    public void setTime(final String timeStr) throws ParseException {
        parseFromString(fillOutTimeString(timeStr));
    }

}
