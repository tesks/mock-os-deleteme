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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.time.SclkFmt.DvtFormatter;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
/**
 * TimeProperties manages the configuration information for time-related
 * parameters, such as spacecraft clock representation that MPCS should expect
 * and proper time format for output purposes.
 * 
 * Although there are setter methods, they are not intended to be used in non-test
 * or non-configuration utilities.  
 * 
 * <p>
 * This class follows the usual hierarchical loading scheme.
 * </p>
 * 
 * <p>
 * This class utilizes a singleton pattern, so that the configuration can be
 * available everywhere and is only read once, the first time getInstance() is
 * invoked. Users should be aware of this and be wary of making changes to the
 * configuration object. 
 * 
 * </p>
 * 
 *
 * @since R8
 */
public class TimeProperties extends GdsHierarchicalProperties {
    
    private static final String PROPERTY_PREFIX = "time.";

    /** The ID associated with the data validity time tag */
	private static final String DVT_ID = "dvt";
	/** The ID associated with the canonical time tag */
	public static final String CANONICAL_SCLK_ID = "canonical_sclk";
	
	private static final String TIMETAG_TEMPLATE  = PROPERTY_PREFIX + "timeTags.";
	
	private static final String ID_LIST_SUBKEY = "idList";
 
	private static final String CF_TIME_KEY = TIMETAG_TEMPLATE + "coarseFineTime.";
	private static final String CF_ID_LIST_KEY = CF_TIME_KEY + ID_LIST_SUBKEY;
    private static final String CF_COARSE_BITS_TEMPLATE = CF_TIME_KEY + "coarseBits.%s";
    private static final String CF_FINE_BITS_TEMPLATE = CF_TIME_KEY + "fineBits.%s";
    private static final String CF_FINE_MODULUS_TEMPLATE = CF_TIME_KEY + "fineModulus.%s";
	
	private static final String CF_FRAC_SEP_TEMPLATE = TIMETAG_TEMPLATE + "displayFormat.fractionalSeparator.%s";
	private static final String CF_TICKS_SEP_TEMPLATE = TIMETAG_TEMPLATE + "displayFormat.ticksSeparator.%s";
	private static final String CF_USE_FRACTIONAL_TEMPLATE = TIMETAG_TEMPLATE + "displayFormat.useFractionalFormat.%s";
	private static final String TICKS_SEP_DEFAULT = "-";
	private static final String FRAC_SEP_DEFAULT = ".";

	private static final String GPS_TIME_KEY = TIMETAG_TEMPLATE + "gpsTime.";
	private static final String GPS_ID_LIST_KEY = GPS_TIME_KEY + ID_LIST_SUBKEY;
	private static final String GPS_SUBSECOND_BITS_TEMPLATE = GPS_TIME_KEY + "subsecondBits.%s";
	private static final String GPS_SECOND_BITS_TEMPLATE = GPS_TIME_KEY + "secondBits.%s";
	private static final String GPS_WEEK_BITS_TEMPLATE = GPS_TIME_KEY + "weekBits.%s";
	private static final String GPS_SUBSEC_MODULUS_TEMPLATE = GPS_TIME_KEY + "subsecondsModulus.%s";
	
	private static final String FINE_TIME_KEY = TIMETAG_TEMPLATE + "fineTime.";
	private static final String FINE_ID_LIST_KEY = FINE_TIME_KEY + ID_LIST_SUBKEY;
	private static final String FINE_BITS_TEMPLATE = FINE_TIME_KEY + "bits.%s";
	private static final String FINE_MODULUS_TEMPLATE = FINE_TIME_KEY + "fineModulus.%s";

	private static final String FLOAT_TIME_KEY = TIMETAG_TEMPLATE + "floatingPointTime.";
	private static final String FLOAT_ID_LIST_KEY = FLOAT_TIME_KEY + ID_LIST_SUBKEY;
	private static final String FLOAT_BYTES_TEMPLATE = FLOAT_TIME_KEY + "bytes.%s";

	private static final String CUSTOM_TIME_KEY = TIMETAG_TEMPLATE + "customTime.";
	private static final String CUSTOM_ID_LIST_KEY = CUSTOM_TIME_KEY + ID_LIST_SUBKEY;
	private static final String CUSTOM_EXTRACTOR_TEMPLATE = CUSTOM_TIME_KEY + "extractor.%s";
	
	private static final String DATE_BLOCK = PROPERTY_PREFIX + "date.";
	private static final String USE_DOY_FMT_KEY = DATE_BLOCK + "useDoyOutputFormat";
	private static final String USE_DOY_DIR_KEY = DATE_BLOCK + "useDoyOutputDirectory";
	private static final String ERT_PRECISION_KEY = DATE_BLOCK + "ertPrecision";
	private static final String SCET_PRECISION_KEY = DATE_BLOCK + "scetPrecision";
	private static final String USE_EXTENDED_SCET_KEY = DATE_BLOCK + "useExtendedScetPrecision";
	
	private static final String LST_BLOCK = DATE_BLOCK + "localSolarTime.";
	private static final String LST_ENABLE_KEY = LST_BLOCK + "enable";
	private static final String LST_PREFIX_KEY = LST_BLOCK + "lstPrefix";
	private static final String LST_PRECISION_KEY = LST_BLOCK + "lstPrecision";
	private static final String LST_SCET0_KEY = LST_BLOCK + "epochScet";
	private static final String LST_CONVERSION_FACTOR_KEY = LST_BLOCK + "earthSecondConversionFactor";	

	private final Map<String, ISclkExtractor> sclkExtractorMap = new HashMap<>();
	// Keep the custom algorithms separate so that they can be instantiated lazily
	// by client services
	private final Map<String, String> customSclkExtractorMap = new HashMap<>();

	private CoarseFineEncoding sclkEncoding;
	private CoarseFineEncoding dvtEncoding;

	private DvtFormatter dvtFmt;

	private SclkFormatter sclkFmt;
	
    private IAccurateDateTime lstScet0;

	private static final String FILE_NAME = "time.properties";
	    
    private boolean useDoyOutputFormat;
    private final int ertPrecision;
    private final int scetPrecision;
    
	/**
	 * Create a TimeProperties instance, which will load and process
	 * time configuration files hierarchically.
	 * 
	 * Requires at least one time.xml to be hierarchically discovered and successfully
	 * loaded, and requires time_config.rnc to be discoverable.
	 */
	public TimeProperties() {
        super(FILE_NAME, true);
		initialize();
		
	    // EHA Aggregation Integration
	    // Adding a property cache for performance reasons
		useDoyOutputFormat = getBooleanProperty(USE_DOY_FMT_KEY, true);
		ertPrecision = getIntProperty(ERT_PRECISION_KEY, 3);
		scetPrecision = getIntProperty(SCET_PRECISION_KEY, 3);
	}
	
	private void processGpsTimes() {
	    
	    final List<String> ids = getListProperty(GPS_ID_LIST_KEY, null, ",");
	    
	    if (ids == null) {
	        return;
	    }
	    
	    for (final String id: ids) {
	        final int weekBits = getIntProperty(String.format(GPS_WEEK_BITS_TEMPLATE, id), 0);
            final int secondBits = getIntProperty(String.format(GPS_SECOND_BITS_TEMPLATE, id), 0);
            final int subsecondBits = getIntProperty(String.format(GPS_SUBSECOND_BITS_TEMPLATE, id), 0);

			GpsTimeExtractor gpsExtractor;
			if (properties.containsKey(String.format(GPS_SUBSEC_MODULUS_TEMPLATE, id))) {
                final long modulus = getLongProperty(String.format(GPS_SUBSEC_MODULUS_TEMPLATE, id), 0);
				gpsExtractor =
						new GpsTimeExtractor(
								sclkEncoding, weekBits,
								secondBits, subsecondBits,
								modulus);
			} else {
				gpsExtractor = new GpsTimeExtractor(sclkEncoding, weekBits, secondBits, subsecondBits);
			}
			sclkExtractorMap.put(id, gpsExtractor);
		}
	}
	
	private CoarseFineEncoding processCoarseFineEncoding(final String id) {
		final int coarseBits = getIntProperty(String.format(CF_COARSE_BITS_TEMPLATE, id), 0);
		final int fineBits = getIntProperty(String.format(CF_FINE_BITS_TEMPLATE, id), 0);
		
		final CoarseFineEncoding encoding;
		if (properties.containsKey(String.format(CF_FINE_MODULUS_TEMPLATE, id))) {
			encoding = new CoarseFineEncoding(coarseBits, fineBits, getIntProperty(String.format(CF_FINE_MODULUS_TEMPLATE, id), 0));
		} else {
			encoding = new CoarseFineEncoding(coarseBits, fineBits);
		}
		return encoding;
	}

	private void processCoarseFineTimes() {

	    final List<String> ids = getListProperty(CF_ID_LIST_KEY, null, ",");

	    if (ids == null) {
	        return;
	    }
        
	    for (final String id: ids) {
	        if (id.equalsIgnoreCase(CANONICAL_SCLK_ID) || id.equalsIgnoreCase(DVT_ID)) {
	            continue;
	        }
			final CoarseFineEncoding encoding = processCoarseFineEncoding(id);
			sclkExtractorMap.put(id, new CoarseFineExtractor(sclkEncoding, encoding));
		}
	}

	private void loadTimeTagsNoCanonical() {
		processCoarseFineTimes();
		processGpsTimes();
		processCustomTimes();
		processFineTimes();
		processFloatTimes();
	}
	
	private void loadTimeTags() {
		sclkExtractorMap.clear();
		processCanonicalSclk();
		processDvt();
        processLstEpoch();
		loadTimeTagsNoCanonical();
	}

	private void initialize() {
		loadTimeTags();
	}
	
	private void processFineTimes() {
	    final List<String> ids = getListProperty(FINE_ID_LIST_KEY, null, ",");

        if (ids == null) {
            return;
        }
		for (final String id: ids) {
		    final int bits = getIntProperty(String.format(FINE_BITS_TEMPLATE, id), 0);
	        final int modulus = getIntProperty(String.format(FINE_MODULUS_TEMPLATE, id), 0);
			sclkExtractorMap.put(id, new FineTimeExtractor(bits, modulus, sclkEncoding));
		}
	}

	private void processFloatTimes() {
	    final List<String> ids = getListProperty(FLOAT_ID_LIST_KEY, null, ",");

        if (ids == null) {
            return;
        }
        for (final String id: ids) {
			sclkExtractorMap.put(id, new FloatTimeExtractor(getIntProperty(String.format(FLOAT_BYTES_TEMPLATE,id), 4), sclkEncoding));
		}
	}

	private void processCustomTimes() {
	    final List<String> ids = getListProperty(CUSTOM_ID_LIST_KEY, null, ",");

        if (ids == null) {
            return;
        }
        for (final String id: ids) {
			customSclkExtractorMap.put(id, getProperty(String.format(CUSTOM_EXTRACTOR_TEMPLATE, id)));
		}
	}

	/**
	 *	Returns unmodifiable mapping of time tag IDs to custom sclk extractor algorithm IDs. 
	 *	These are not validated; this class does no loading or validating of custom algorithms.
	 *  
	 * 	@return unmodifiable map
	 */
	public Map<String, String> getCustomTimeExtractors() {
		return Collections.unmodifiableMap(customSclkExtractorMap);
	}

	private void processDvt() {
		dvtEncoding = processCoarseFineEncoding(DVT_ID);
		sclkExtractorMap.put(DVT_ID, new CoarseFineExtractor(dvtEncoding));
		
		final String ticksSep = getProperty(String.format(CF_TICKS_SEP_TEMPLATE, DVT_ID), TICKS_SEP_DEFAULT);
        final String fracSep = getProperty(String.format(CF_FRAC_SEP_TEMPLATE, DVT_ID), FRAC_SEP_DEFAULT);
        final boolean useFracFormat = getBooleanProperty(String.format(CF_USE_FRACTIONAL_TEMPLATE, DVT_ID), false);
		dvtFmt = new DvtFormatter(ticksSep, fracSep, useFracFormat, dvtEncoding);
	}

	private void processCanonicalSclk() {
		sclkEncoding = processCoarseFineEncoding(CANONICAL_SCLK_ID);
		sclkExtractorMap.put(CANONICAL_SCLK_ID, new CoarseFineExtractor(sclkEncoding));

		final String ticksSep = getProperty(String.format(CF_TICKS_SEP_TEMPLATE, CANONICAL_SCLK_ID), TICKS_SEP_DEFAULT);
		final String fracSep = getProperty(String.format(CF_FRAC_SEP_TEMPLATE, CANONICAL_SCLK_ID), FRAC_SEP_DEFAULT);
		final boolean useFracFormat = getBooleanProperty(String.format(CF_USE_FRACTIONAL_TEMPLATE, CANONICAL_SCLK_ID), false);
		sclkFmt = new SclkFormatter(ticksSep, fracSep, useFracFormat, sclkEncoding);
	}
	
    private void processLstEpoch() {
        try {
            lstScet0 = new AccurateDateTime(getLstEpoch());
        } catch (final ParseException e) {
            TraceManager.getDefaultTracer().error("Invalid SCET0 supplied.", e.getCause());
        }
    }

    /**
     * Get the configured Scet 0 since LST epoch
     * 
     * @return Scet0 from LST epoch
     */
    public IAccurateDateTime getLstScet0() {
        return lstScet0;
    }

	/**
	 * Get the number of bits used to represent
	 * the SCLK coarse ticks in a binary encoding
	 * @return number of bits
	 */
	public int getSclkCoarseBitLength() {
		return sclkEncoding.getCoarseBits();
	}

	/**
	 * Get the number of bits used to represent
	 * the SCLK fine ticks in a binary encoding
	 * @return number of bits
	 */
	public int getSclkFineBitLength() {
		return sclkEncoding.getFineBits();
	}

	/**
	 * Get the separator used when SCLKs are displayed with
	 * a fractional fine value.
	 * @return the separator String
	 */
	public String getSclkFractionalSeparator() {
		return sclkFmt.getFracSep();
	}
 
	/**
	 * Get the separator used when SCLKs are displayed with
	 * a fine ticks value.
	 * @return the separator String
	 */
	public String getSclkTicksSeparator() {
		return sclkFmt.getTicksSep();
	}

	/** 
	 * Get whether the default SCLK format
	 * should display a fractional fine value
	 * @return true if the fractional value should be used
	 */
	public boolean useFractionalSclkFormat() {
		return sclkFmt.usesFractional();
	}

	/**
	 * 
	 * Gets the upper limit on the SCLK fine time.
	 * 
	 * @return upper limit
	 * 
	 */
	public long getSclkFineUpperLimit() {
		return sclkEncoding.getMaxFine();
	}

	/**
	 * Get the number of bits used to represent
	 * the DVT coarse ticks in a binary encoding
	 * @return number of bits
	 */
	public int getDvtCoarseBitLength() {
		return dvtEncoding.getCoarseBits();
	}

	/**
	 * Get the number of bits used to represent
	 * the DVT fine ticks in a binary encoding
	 * @return number of bits
	 */
	public int getDvtFineBitLength() {
		return dvtEncoding.getFineBits();
	}

	/**
	 * Get the separator used when DVTs are displayed with
	 * a fractional fine value.
	 * @return the separator String
	 */
	public String getDvtFractionalSeparator() {
		return dvtFmt.getFracSep();
	}

	/**
	 * Get the separator used when DVTs are displayed with
	 * a fine ticks value.
	 * @return the separator String
	 */
	public String getDvtTicksSeparator() {
		return dvtFmt.getTicksSep();
	}

	/** 
	 * Get whether the default DVT format
	 * should display a fractional fine value
	 * @return true if the fractional value should be used
	 */
	public boolean useFractionalDvtFormat() {
		return dvtFmt.getUseFractional();
	}

	/**
	 * 
	 * Gets the upper limit on the DVT fine time.
	 * 
	 * @return upper limit
	 * 
	 */
	public long getDvtFineUpperLimit() {
		return dvtEncoding.getMaxFine();
	}


    /**
     * Use extended SCET precision.
     *
     * @return Tue if extended SCET required
     *
     */
	public boolean useExtendedScet()
    {
		return getBooleanProperty(USE_EXTENDED_SCET_KEY, false);
	}

    /**
     * Get SCET precision.
     *
     * @return number of digits to use for SCET subseconds
     */	
	public int getScetPrecision() {
	    return scetPrecision;
	}
	

    /**
     * Get ERT precision.
     *
     * @return number of digits to use for ERT subseconds
     */	
	public int getErtPrecision() {
		return ertPrecision;
	}

    /**
     * Get whether DOY format should be used for dates.
     *
     * @return true if DOY format shoud be used.
     */	
	public boolean useDoyOutputFormat() {
	    return useDoyOutputFormat;
	}

    /**
     * Get whether DOY format should be used to
     * construct output folders.
     *
     * @return true if DOY format should be used for output
     * directory.
     */		
	public boolean useDoyOutputDirectory() {
		return getBooleanProperty(USE_DOY_DIR_KEY, useDoyOutputFormat());
	}
	

	/**
	 * This is a configuration accessor that simply states whether or not the
	 * current mission is capable of using any LST times whatsoever. It has
	 * nothing to do with the current venue.
	 * 
	 * @return True if the current mission support LST, false otherwise.
	 */
	public boolean usesLst() {
		return getBooleanProperty(LST_ENABLE_KEY, false);
	}

	/**
	 * Get the prefix to display at the beginning of LST times.
	 * @return Prefix string
	 */
	public String getLstPrefix() {
		return getProperty(LST_PREFIX_KEY, "SOL");
	}

	/**
	 * Get the LST precision.
	 * @return the number of digits to use for LST subseconds
	 */
	public int getLstPrecision() {
		return getIntProperty(LST_PRECISION_KEY, 3);
	}

	/** 
	 * Get the string representing the SCET that should be
	 * considered the earliest LST time.
	 * @return String containing LST epoch
	 */
	public String getLstEpoch() {
		return getProperty(LST_SCET0_KEY);
	}

    /** 
     *
     * Get the conversion factor between earth seconds and local solar seconds
     *
     * @return the conversion factor to use
     */	
	public double getEarthSecondConversionFactor() {
		return getDoubleProperty(LST_CONVERSION_FACTOR_KEY, 1.0);
	}

	/**
	 * Get the ISclk formatter configured and usable for output
	 * or string parsing needs.
	 * @return the configured SclkFormatter
	 */
	public SclkFormatter getSclkFormatter() {
		return sclkFmt;
	}
	
	/**
	 * Get the canonical ISclk encoding
	 * @return the canonical ISclk encoding
	 */
	public CoarseFineEncoding getCanonicalEncoding() {
		return sclkEncoding;
	}

	/**
	 * Get the encoding for DVT
	 * @return the DVT encoding
	 */
	public CoarseFineEncoding getDvtEncoding() {
		return dvtEncoding;
	}

	/**
	 * Get the formatter that should be used for formatting DVTs
	 * and parsing DVTs from strings
	 * @return the DVT formatter
	 */
	public DvtFormatter getDvtFormatter() {
		return dvtFmt;
	}
	
	/**
	 * Get the extractor to be used on bytes that are known to
	 * contain a canonically encoded SCLK
	 * @return the extractor for the canonical SCLK
	 */
	public ISclkExtractor getCanonicalExtractor() {
		return sclkExtractorMap.get(CANONICAL_SCLK_ID);
	}

	/**
	 * Get the maximum canonical SCLK (max coarse, max fine)
	 * @return ISclk
	 */
	public ISclk getMaxCanonicalSclk() {
		return new Sclk(sclkEncoding.getMaxCoarse(), sclkEncoding.getMaxFine(), sclkEncoding);
	}
	
	/** 
	 * Static inner class to ensure threadsafe instantiation of the global
	 * TimeProperties instance
	 */
	private static class TimePropertiesInstanceHelper {
		private static final TimeProperties INSTANCE = new TimeProperties();
	}

	/**
	 * Gets the master static instance of this class.
	 * 
	 * @return TimeProperties
	 */
	public static TimeProperties getInstance() {
		return TimePropertiesInstanceHelper.INSTANCE;
	}
	
	/** 
	 * Set the canonical ISclk Encoding.  Use for test only. 
	 * @param encoding the new encoding to be used for the canonical ISclk
	 */
	public void setCanonicalSclkEncoding(final CoarseFineEncoding encoding) {
		sclkEncoding = encoding;
		sclkExtractorMap.put(CANONICAL_SCLK_ID, new CoarseFineExtractor(sclkEncoding));
		loadTimeTagsNoCanonical();
	}
	
	/**
	 * Set the canonical ISclk formatter.  Use for test only.
	 * @param fmt the new formatter to use for the canonical ISclk
	 */
	public void setSclkFormatter(final SclkFormatter fmt) {
		sclkFmt = fmt;
	}
	
	/**
	 * Set the LST epoch.  Use for test only.
	 * @param time SCET formatted string to be used as the 0 time for LST
	 */
	public void setLstEpoch(final String time) {
		properties.put(LST_SCET0_KEY, time);
	}

	/**
	 * Sets the flag indicating to use day-of-year format for dates.
	 * 
	 * @param flag true for DOY formats, false for YYYY-MM-DD.
	 */
	public void setUseDoyOutputFormat(final boolean flag) {
        // EHA Aggregation Integration
        // Adding a property cache for performance reasons
        useDoyOutputFormat = flag;
        properties.put(USE_DOY_FMT_KEY, String.valueOf(flag));
	}

	/**
	 * Set where SCLKs should be displayed with a fractional fine value.
	 * Only use in test.
	 * @param b true if fractional fine ticks should be displayed
	 */
	public void setSclkUseFractionalFormat(final boolean b) {
		properties.put(String.format(CF_USE_FRACTIONAL_TEMPLATE, CANONICAL_SCLK_ID), String.valueOf(b));
		processCanonicalSclk();
	}

	/**
	 * Set whether LST should used
	 * @param enable true if LST should be used, false if not
	 */
	public void setLstEnabled(final boolean enable) {
		properties.put(LST_ENABLE_KEY, String.valueOf(enable));
	}

	/** 
	 * Get whether LST should be used.
	 * @return true if LST should be used
	 */
	public boolean getLstEnabled() {
		return getBooleanProperty(LST_ENABLE_KEY, false);
	}

	/**
	 * Get the map of time IDs to ISclk Extractors.  Note that TimeProperties does
	 * not instantiate user configured algorithm instances, so extractors for custom
	 * times will not be included. Information pertaining to custom times can
	 * be retrieved with {@link #getCustomTimeExtractors()}
	 * @return Map of String (time ID) to SclkExtractor
	 */
	public Map<String, ISclkExtractor> getSclkExtractorMap() {
		return sclkExtractorMap;
	}
	
	@Override
    public String getPropertyPrefix() {
	    return PROPERTY_PREFIX;
	}
	
}
