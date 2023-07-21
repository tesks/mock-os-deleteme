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
package jpl.gds.station.api.dsn.chdo;

import static jpl.gds.station.api.dsn.chdo.ChdoConstants.CHARSET;
import static jpl.gds.station.api.dsn.chdo.ChdoConstants.DAYS_BYTE_LENGTH;
import static jpl.gds.station.api.dsn.chdo.ChdoConstants.DAY_OF_YEAR_BYTE_LENGTH;
import static jpl.gds.station.api.dsn.chdo.ChdoConstants.JULIAN_DATE_1958;
import static jpl.gds.station.api.dsn.chdo.ChdoConstants.JULIAN_DATE_1970;
import static jpl.gds.station.api.dsn.chdo.ChdoConstants.MILLISECONDS_PER_DAY;
import static jpl.gds.station.api.dsn.chdo.ChdoConstants.MSECS_BYTE_LENGTH;
import static jpl.gds.station.api.dsn.chdo.ChdoConstants.YEAR_BYTE_LENGTH;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import jpl.gds.context.api.filtering.IScidFilterable;
import jpl.gds.context.api.filtering.IStationFilterable;
import jpl.gds.context.api.filtering.IVcidFilterable;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.sfdu.SfduException;
import jpl.gds.shared.sfdu.SfduId;
import jpl.gds.shared.sfdu.SfduLabel;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.TimeProperties;

/**
 * An interface to be implemented by CHDO SFDU objects.
 * 
 *
 * @since R8
 */
public interface IChdoSfdu extends IScidFilterable, IVcidFilterable, IStationFilterable {

    /** Maximum CHDO SFDU size (straight out of the 0172-Telecomm) */
    public static final int MAX_SFDU_SIZE = 131096;
    
    /** Cache the coarse length of the canonical ISclk */
    public final static int coarseLen = TimeProperties.getInstance().getCanonicalEncoding().getCoarseByteLength();
    
    /**
     * Label for SFDU hearbeat SFDUs
     */
    public static final String TDS_HEARTBEAT_SFDU_LABEL = "NJPL3KS0L009STAT/ERR";

    /**
     * Clears the current CHDO definition and all CHDO/SFDU related fields.
     */
    public void clear();

    /**
     * Read a full CHDO SFDU from the data stream. If the next byte isn't
     * pointing to the first byte of the SFDU label, then this function will
     * search until it finds a proper control authority ID character set
     * (usually "NJPL" or "CCSD") that starts an SFDU label and then it will
     * read the full SFDU from there.
     * 
     * @param dis The data input stream to read from
     * 
     * @throws IOException if there is a problem reading the input stream
     * @throws EOFException if the input stream is null or at EOF
     * @throws SfduException if there is a problem with the SFDU/CHDO structure or definition
     */
    public void readSfdu(DataInputStream dis) throws IOException, EOFException,
            SfduException;
    
    /**
     * Loads the SFDU CHDO header only (not the SFDU data) from the given byte buffer.
     * 
     * @param buffer byte buffer containing the CHDO SFDU
     * @param offset starting offset into the buffer
     * @param length length of the data to load
     * @throws IOException if there is an error reading the data
     * @throws SfduException if there is an error loading the SDFU
     */
    public void loadSdfuHeaderOnly(byte[] buffer, int offset, int length) throws IOException, SfduException;

    /**
     * Gets the value of the indicated CHDO field as an array of bytes.
     * @param fieldName name of the CHDO field to fetch
     * @return byte array containing the extracted field value
     */
    public byte[] getFieldValueAsByteArray(String fieldName);

    /**
     * Gets the value of the indicated CHDO field as an unsigned integer.
     * @param fieldName name of the CHDO field to fetch
     * @return value of the field, or null if not found
     */
    public Long getFieldValueAsUnsignedInt(String fieldName);

    /**
     * Gets the value of the indicated CHDO field as an signed integer.
     * @param fieldName name of the CHDO field to fetch
     * @return value of the field, or null if not found
     */
    public Long getFieldValueAsSignedInt(String fieldName);

    /**
     * Gets the value of the indicated CHDO field as a floating point number.
     * Can only fetch values that are byte aligned.
     * 
     * @param fieldName name of the CHDO field to fetch
     * @return value of the field, or null if not found
     */
    public Double getFieldValueAsFloatingPoint(String fieldName);

    /**
     * Gets the value of the indicated CHDO field as a Date.
     * Can only fetch values that are byte aligned.
     * 
     * @param fieldName name of the CHDO field to fetch
     * @return value of the field, or null if not found
     */
    public IAccurateDateTime getFieldValueAsDate(String fieldName);

    /**
     * Gets the value of the indicated CHDO field as a Sclk.
     * Can only fetch values that are byte aligned.
     * 
     * @param fieldName name of the CHDO field to fetch
     * @return value of the field, or null if not found
     */
    public ISclk getFieldValueAsSclk(String fieldName);

    /**
     * Gets the value of the indicated CHDO field as a Boolean.
     * 
     * @param fieldName name of the CHDO field to fetch
     * @return value of the field, or FALSE if not found
     */
    public Boolean getFieldValueAsBoolean(String fieldName);

    /**
     * Gets the value of the indicated CHDO field as a String.
     * 
     * @param fieldName name of the CHDO field to fetch
     * @return value of the field, or null if not found
     * @throws UnsupportedEncodingException if the US-ASCII character set is not supported 
     * in this environment
     */
    public String getFieldValueAsString(String fieldName)
            throws UnsupportedEncodingException;

    /**
     * Gets the ChdoFieldDefinition object for the indicated CHDO field.
     * 
     * @param fieldName name of the CHDO field to look for
     * @return ChdofieldDefinition object for the field, or null if not found
     */
    public IChdoFieldDefinition getChdoFieldDefinitionForFieldName(
            String fieldName);

    /**
     * Gets the data CHDO from the CHDO SFDU.
     * @return the Chdo object containing the CHDO data, or null if none present
     */
    public IChdo getDataChdo();

    /**
     * Answers the question: is this CHDO SFDU for a GIF frame?
     * 
     * @return true if GIF frame, false if not
     * @throws ChdoPropertyException if there is a problem accessing the necessary CHDO properties
     */
    public Boolean isGifFrame() throws ChdoPropertyException;

    /**
     * Answers the question: is this CHDO SFDU for a transfer frame?
     * 
     * @return true if transfer frame, false if not
     * @throws ChdoPropertyException if there is a problem accessing the necessary CHDO properties
     */
    public Boolean isFrame() throws ChdoPropertyException;

    /**
     * Answers the question: is this CHDO SFDU for a telemetry packet?
     * 
     * @return true if packet, false if not
     * @throws ChdoPropertyException if there is a problem accessing the necessary CHDO properties
     */
    public Boolean isPacket() throws ChdoPropertyException;

    /**
     * Answers the question: does this CHDO SFDU contain out of sync frame data?
     * 
     * @return true if out of sync data, false if not
     * @throws ChdoPropertyException if there is a problem accessing the necessary CHDO properties
     */
    public Boolean isOutOfSync() throws ChdoPropertyException;

    /**
     * Answers the question: does this CHDO SFDU contain idle/fill data?
     * 
     * @return true if idle data, false if not
     * @throws ChdoPropertyException if there is a problem accessing the necessary CHDO properties
     */
    public Boolean isIdle() throws ChdoPropertyException;

    /**
     * Answers the question: does this CHDO SFDU contain invalid data data?
     * 
     * @return true if invalid data, false if not
     * @throws ChdoPropertyException if there is a problem accessing the necessary CHDO properties
     */
    public Boolean isInvalid() throws ChdoPropertyException;

    /**
     * Answers the question: does this CHDO SFDU contain channelized data records (CDRs)?
     * 
     * @return true if CDR, false if not
     * @throws ChdoPropertyException if there is a problem accessing the necessary CHDO properties
     */
    public Boolean isCdr() throws ChdoPropertyException;

    /**
     * Answers the question: does this CHDO SFDU contain extended channelized data records (ECDRs)?
     * 
     * @return true if ECDR, false if not
     * @throws ChdoPropertyException if there is a problem accessing the necessary CHDO properties
     */
    public Boolean isEcdr() throws ChdoPropertyException;

    /**
     * Answers the question: does this CHDO SFDU contain quality and quantity accountability information?
     * 
     * @return true if QQC data, false if not
     * @throws ChdoPropertyException if there is a problem accessing the necessary CHDO properties
     */
    public Boolean isQqc() throws ChdoPropertyException;

    /**
     * Answers the question: does this CHDO SFDU contain DSN monitor information?
     * 
     * @return true if MON data, false if not
     * @throws ChdoPropertyException if there is a problem accessing the necessary CHDO properties
     */
    public Boolean isMonitor() throws ChdoPropertyException;

    /**
     * Answers the question: does this CHDO SFDU contain anomalous information?
     * 
     * @return true if anomalous data, false if not
     * @throws ChdoPropertyException if there is a problem accessing the necessary CHDO properties
     */
    public Boolean isAnomaly() throws ChdoPropertyException;

    /**
     * isPadded has been returned for SFDU packets but not frames.
     * Is Padded() has been returned, CHDO for SDFU packets does contain isDataPadded
     */
    /**
     * Answers the question: does this CHDO SFDU contain a padded frame?
     * 
     * @return true if padded frame, false if not
     * @throws ChdoPropertyException if there is a problem accessing the necessary CHDO properties
     */
    public Boolean isPadded() throws ChdoPropertyException;

    /**
     * Answers the question: does this CHDO SFDU contain a full packet?
     * 
     * @return true if full packet, false if not
     * @throws ChdoPropertyException if there is a problem accessing the necessary CHDO properties
     */
    public Boolean isPacketFull() throws ChdoPropertyException;

    /**
     * Answers the question: does this CHDO SFDU contain a turbo-encoded frame?
     * 
     * @return true if turbo frame, false if not
     * @throws ChdoPropertyException if there is a problem accessing the necessary CHDO properties
     */
    public Boolean isTurbo() throws ChdoPropertyException;

    /**
     * Gets the turbo rate if this is a CHDO SFDU containing a turbo-encoded frame.
     * @return turbo rate as a "1/<rate>" string
     */
    public String getTurboRate();

    /**
     * Fix incorrect calculation of frame size based on the bogus isPadded() method
     * Pulled this function from MPCS where this bug was previously fixed.
     * 
     *
     * @return number of data bits in frame based upon secondary CHDO #69's number_bits field.
     * @throws ChdoPropertyException if there is a problem extracting the fields from the CHDO
     */
    public int getNumberOfDataBits() throws ChdoPropertyException;

    /**
     * Evaluates a true/false property on this CHDO SFDU.
     * @param propertyName name of the true/false condition to evaluate (from the chdo.xml file)
     * @return true if property evaluates to true, false if not
     * @throws ChdoPropertyException if the property is not defined or cannot be evaluated on this 
     * CHDO SFDU
     */
    public Boolean getPropertyValue(String propertyName)
            throws ChdoPropertyException;

    /**
     * Gets the entire data buffer for the ChdoSfdu.
     * 
     * @return the byte buffer
     */
    public byte[] getBytes();

    /**
     * Gets the Sfdu Id object for this CHDO SFDU.
     * @return SfduId object; null if no CHDO SFDU has been read.
     */
    public SfduId getSfduId();

    /**
     * Gets the Sfdu Label object for this CHDO SFDU.
     * @return SfduLabel object; null if no CHDO SFDU has been read.
     */
    public SfduLabel getSfduLabel();

    /**
     * Get all headers above the actual data. The data chdo will
     * be last (see chart at top.)
     *
     * @return Header holder
     *
     * @throws SfduException If header size out of bounds
     */
    public HeaderHolder getEntireHeader() throws SfduException;

    /**
     * Gets the value of the indicated CHDO field as an array of bytes from the
     * given Chdo object.
     * @param chdo the Chdo object to access
     * @param fieldName name of the CHDO field to fetch
     * @return byte array containing the extracted field value
     */
    public static byte[] getFieldValueAsByteArray(final IChdo chdo,
            final String fieldName) {
    	final IChdoFieldDefinition fieldDef = chdo.getDefinition().getFieldDefinitionByName(fieldName);
    	if (fieldDef.getBitOffset() != 0) {
    		throw new IllegalArgumentException("The CHDO parser cannot parse values as byte arrays if they are not byte aligned (bit offset must equal zero).");
    	} else if ((fieldDef.getBitLength() % 8) != 0) {
    		throw new IllegalArgumentException("The CHDO parser cannot parse values as byte arrays if their bit length is not a multiple of 8.");
    	}
    
    	final byte[] value = new byte[fieldDef.getBitLength() / 8];
    	System.arraycopy(chdo.getRawValue(), fieldDef.getByteOffset(), value, 0, value.length);
    	return (value);
    }

    /**
     * Gets the value of the indicated CHDO field as an unsigned integer from the
     * given Chdo object.
     * @param chdo the CHDO object to search for the field
     * @param fieldName name of the CHDO field to fetch
     * @return value of the field, or null if not found
     */
    public static Long getFieldValueAsUnsignedInt(final IChdo chdo,
            final String fieldName) {
    	final IChdoFieldDefinition fieldDef = chdo.getDefinition().getFieldDefinitionByName(fieldName);
    
    	long value = 0;
    
    	if (fieldDef.getBitLength() <= Byte.SIZE) {
    		value = GDR.get_u8(chdo.getRawValue(), fieldDef.getByteOffset(), fieldDef.getBitOffset(), fieldDef.getBitLength());
    	} else if (fieldDef.getBitLength() <= Short.SIZE) {
    		value = GDR.get_u16(chdo.getRawValue(), fieldDef.getByteOffset(), fieldDef.getBitOffset(), fieldDef.getBitLength());
    	} else if (fieldDef.getBitLength() <= 24) {
    		value = GDR.get_u24(chdo.getRawValue(), fieldDef.getByteOffset(), fieldDef.getBitOffset(), fieldDef.getBitLength());
    	} else if (fieldDef.getBitLength() <= Integer.SIZE) {
    		value = GDR.get_u32(chdo.getRawValue(), fieldDef.getByteOffset(), fieldDef.getBitOffset(), fieldDef.getBitLength());
    	} else {
    		throw new IllegalArgumentException("Unsigned values greater than 32 bits are not supported by the CHDO parser.");
    	}
    
    	return (value);
    }

    /**
     * Gets the value of the indicated CHDO field as a signed integer from the
     * given Chdo object.
     * @param chdo the CHDO object to search for the field
     * @param fieldName name of the CHDO field to fetch
     * @return value of the field, or null if not found
     */
    public static Long getFieldValueAsSignedInt(final IChdo chdo,
            final String fieldName) {
    	final IChdoFieldDefinition fieldDef = chdo.getDefinition().getFieldDefinitionByName(fieldName);
    
    	long value = 0;
    	if (fieldDef.getBitLength() <= Byte.SIZE) {
    		value = GDR.get_i8(chdo.getRawValue(), fieldDef.getByteOffset(), fieldDef.getBitOffset(), fieldDef.getBitLength());
    	} else if (fieldDef.getBitLength() <= Short.SIZE) {
    		value = GDR.get_i16(chdo.getRawValue(), fieldDef.getByteOffset(), fieldDef.getBitOffset(), fieldDef.getBitLength());
    	} else if (fieldDef.getBitLength() <= 24) {
    		value = GDR.get_i24(chdo.getRawValue(), fieldDef.getByteOffset(), fieldDef.getBitOffset(), fieldDef.getBitLength());
    	} else if (fieldDef.getBitLength() <= Integer.SIZE) {
    		value = GDR.get_i32(chdo.getRawValue(), fieldDef.getByteOffset(), fieldDef.getBitOffset(), fieldDef.getBitLength());
    	} else if (fieldDef.getBitLength() <= Long.SIZE) {
    		value = GDR.get_i64(chdo.getRawValue(), fieldDef.getByteOffset(), fieldDef.getBitOffset(), fieldDef.getBitLength());
    	}
    
    	return (Long.valueOf(value));
    }

    /**
     * Gets the value of the indicated CHDO field as a floating point number from the
     * given Chdo object. Can only fetch values that are byte aligned.
     * 
     * @param chdo the CHDO object to search for the field
     * @param fieldName name of the CHDO field to fetch
     * @return value of the field, or null if not found
     */
    public static Double getFieldValueAsFloatingPoint(final IChdo chdo,
            final String fieldName) {
    	final IChdoFieldDefinition fieldDef = chdo.getDefinition().getFieldDefinitionByName(fieldName);
    	if (fieldDef.getBitOffset() != 0) {
    		throw new IllegalArgumentException("The CHDO parser cannot parse values as floating point values if they are not byte aligned.");
    	}
    
    	double value = 0;
    	switch (fieldDef.getBitLength()) {
    	case Float.SIZE:
    		value = GDR.get_float(chdo.getRawValue(), fieldDef.getByteOffset());
    		break;
    
    	case Double.SIZE:
    		value = GDR.get_double(chdo.getRawValue(), fieldDef.getByteOffset());
    		break;
    
    	default:
    		throw new IllegalArgumentException("Floating point values that are not "
    		        + Float.SIZE
    		        + " or "
    		        + Double.SIZE
    		        + " bits long are not supported by the CHDO parser.");
    	}
    
    	return (value);
    }

    /**
     * Gets the value of the indicated CHDO field as a Date from the
     * given Chdo object. Can only fetch values that are byte aligned.
     *
     * @param chdo the CHDO object to search for the field
     * @param fieldName name of the CHDO field to fetch
     * @return value of the field, or null if not found
     */
    public static IAccurateDateTime getFieldValueAsDate(final IChdo chdo,
            final String fieldName) {
    	final IChdoFieldDefinition fieldDef = chdo.getDefinition().getFieldDefinitionByName(fieldName);
    	if (fieldDef.getBitOffset() != 0) {
    		throw new IllegalArgumentException("The CHDO parser cannot parse values as Date objects if they are not byte aligned.");
    	}
    
    	int days = 0;
    	long msecsOfDay = 0;
    	long extended = 0;
    	int year = 0;
    	int dayOfYear = 0;
    	double secsOfDay = 0.0;
    	long time = 0;
    
    	switch (fieldDef.getBitLength()) {
    	case 48:
    		days = GDR.get_u16(chdo.getRawValue(), fieldDef.getByteOffset());
    		msecsOfDay = GDR.get_u32(chdo.getRawValue(), fieldDef.getByteOffset()
    		        + DAYS_BYTE_LENGTH);
    		
    		/*
    		 * If time is negative, set it to 0 to avoid 1958 epoch
    		 */
    		time = (days - (JULIAN_DATE_1970 - JULIAN_DATE_1958))
    		        * MILLISECONDS_PER_DAY + msecsOfDay;
    		if (time < 0) {
    			time = 0;
    		}
    		
    		return (new AccurateDateTime(time));
    
    	case 64:
    		days = GDR.get_u16(chdo.getRawValue(), fieldDef.getByteOffset());
    		msecsOfDay = GDR.get_u32(chdo.getRawValue(), fieldDef.getByteOffset()
    		        + DAYS_BYTE_LENGTH);
    		extended = GDR.get_u16(chdo.getRawValue(), fieldDef.getByteOffset()
    		        + DAYS_BYTE_LENGTH + MSECS_BYTE_LENGTH);
    
    		final IChdoFieldDefinition extFieldDef = chdo.getDefinition().getFieldDefinitionByName(fieldName
    		        + "_extended_resolution");
    
    		Boolean isTenthsOfMicros = Boolean.valueOf(false);
    		if (extFieldDef != null) {
    			final Boolean isExtendedValid = getFieldValueAsBoolean(chdo, fieldName
    			        + "_extended_resolution");
    
    			if (isExtendedValid == null || !isExtendedValid) {
    				extended = 0;
    			} else {
    				isTenthsOfMicros = getFieldValueAsBoolean(chdo, fieldName
    				        + "_ext_res_units");
    			}
    		}
    		
    		/*
    		 * If time is negative, set it to 0 to avoid 1958 epoch
    		 */
    		time = (days - (JULIAN_DATE_1970 - JULIAN_DATE_1958))
    				* MILLISECONDS_PER_DAY + msecsOfDay;
    		if (time < 0) {
    			time = 0;
    		}
    		
    		return (new AccurateDateTime(time, extended, isTenthsOfMicros.booleanValue()));
    
    	case 96:
    		year = GDR.get_u16(chdo.getRawValue(), fieldDef.getByteOffset());
    		dayOfYear = GDR.get_u16(chdo.getRawValue(), fieldDef.getByteOffset()
    		        + YEAR_BYTE_LENGTH);
    		secsOfDay = GDR.get_double(chdo.getRawValue(), fieldDef.getByteOffset()
    		        + YEAR_BYTE_LENGTH + DAY_OF_YEAR_BYTE_LENGTH);
    
    		final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    		cal.set(Calendar.YEAR, year);
    		cal.set(Calendar.DAY_OF_YEAR, dayOfYear);
    		cal.set(Calendar.HOUR_OF_DAY, 0);
    		cal.set(Calendar.MINUTE, 0);
    		cal.set(Calendar.SECOND, 0);
    		cal.set(Calendar.MILLISECOND, 0);
    
    		final Date yearAndDay = cal.getTime();
    		return (new AccurateDateTime(yearAndDay.getTime()
    		        + (Math.round(secsOfDay) * 1000)));
    
    	default:
    
    		throw new IllegalArgumentException("The CHDO parser cannot handle date fields whose length is "
    		        + fieldDef.getBitLength() + " bits.");
    	}
    }

    /**
     * Gets the value of the indicated CHDO field as a ISclk from the
     * given Chdo object. Can only fetch values that are byte aligned.
     * 
     * @param chdo the CHDO object to search for the field
     * @param fieldName name of the CHDO field to fetch
     * @return value of the field, or null if not found
     */
    public static ISclk getFieldValueAsSclk(final IChdo chdo,
            final String fieldName) {
    	final IChdoFieldDefinition fieldDef = chdo.getDefinition().getFieldDefinitionByName(fieldName);
    	if (fieldDef.getBitOffset() != 0) {
    		throw new IllegalArgumentException("The CHDO parser cannot parse values as SCLK objects if they are not byte aligned.");
    	}
    
    	// TODO: Some CHDOs, such as 086 bytes 14-21 have an 8-byte SCLK.
    	// However, they claim that the
    	// LAST 2 bytes are filler, which could cause issues in the future,
    	// though I think it's handled.
    	// Just need to test it to make sure (brn).
    
    	// This is really awful but I can think of no other solution that would not be really complex.
    	// Currently, CHDOs support both 8 and 16 bit fine SCLKs.  However, the mission may have an 8 bit fine
    	// SCLK but use a CHDO that supports 16. If we just pass the bytes from the CHDO into the SCLK
    	// constructor and the mission has an 8 bit fine SCLK, it will grab the first 8 bits of the 16 in 
    	// the CHDO, which will be 0. So we cannot load the SCLK class from. We must extract coarse and fine 
    	// separately, which means this code will break if any CHDO or mission deviates from either
    	// 32.8 or 32.16 SCLK precision.		
    	if (coarseLen != 4) {
    		throw new IllegalArgumentException("The CHDO parser cannot parse SCLKs that do not have a 32 bit coarse value.");
    	}
    	final byte[] bytes = chdo.getRawValue();
    	final long coarse = GDR.get_u32(bytes, fieldDef.getByteOffset());
    	long fine = 0;
    	if (fieldDef.getBitLength() == 40) {
    		GDR.get_u8(bytes, fieldDef.getByteOffset() + 4);
    	} else if (fieldDef.getBitLength() == 48) {
    		fine = GDR.get_u16(bytes, fieldDef.getByteOffset() + 4);
    	} else {
    		throw new IllegalArgumentException("The CHDO parser cannot parser SCLKs with a fine length greater than 16");
    	}
    	final ISclk sclk = new Sclk(coarse, fine);
    	return (sclk);
    }

    /**
     * Gets the value of the indicated CHDO field as a Boolean from the
     * given Chdo object. 
     * 
     * @param chdo the CHDO object to search for the field
     * @param fieldName name of the CHDO field to fetch
     * @return value of the field, or FALSE if not found
     */
    public static Boolean getFieldValueAsBoolean(final IChdo chdo,
            final String fieldName) {
    	final Long value = getFieldValueAsUnsignedInt(chdo, fieldName);
    	if (value == null) {
    		TraceManager.getDefaultTracer().error("Boolean field " + fieldName

    		        + " does not exist. Returning false by default.");
    		return (Boolean.FALSE);
    	}
    
    	return (value.longValue() != 0);
    }

    /**
     * Gets the value of the indicated CHDO field as a String from the
     * given Chdo object. 
     * 
     * @param chdo the CHDO object to search for the field
     * @param fieldName name of the CHDO field to fetch
     * @return value of the field, or null if not found
     * @throws UnsupportedEncodingException if the US-ASCII character set is not supported 
     * in this environment
     */
    public static String getFieldValueAsString(final IChdo chdo,
            final String fieldName) throws UnsupportedEncodingException {
    	final byte[] byteValue = getFieldValueAsByteArray(chdo, fieldName);
    	if (byteValue == null) {
    		return (null);
    	}
    
    	final String value = new String(byteValue, CHARSET);
    	return (value);
    }

}