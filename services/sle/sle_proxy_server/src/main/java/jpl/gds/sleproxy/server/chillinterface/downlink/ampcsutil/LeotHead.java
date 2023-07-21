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
package jpl.gds.sleproxy.server.chillinterface.downlink.ampcsutil;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

import org.slf4j.Logger;

/**
 * LEO transfer frame header Content is specified in SMAP ICD TBD (no version
 * Nov 22 2010)
 * 
 * This code is taken from AMPCS core's, but because we don't want to introduce
 * a dependency on AMPCS core just for this class, it was basically copied over.
 * In AMPCSR8, when SLE capability is integrated into AMPCS architecture itself,
 * this duplication can then be removed.
 * 
 *
 * 11/21/13 - javadoc, general code cleanup in conjunction with MPCS-5550.
 */
public class LeotHead {
    /** Defined fixed header size in bytes */
    public static final int LEOT_HEADER_SIZE = 10;

    /** Data class for supported version */
    public static final int SUPPORTED_VERSION = 1;

    /*
     * 11/21/13 - MPCS-5483. Removed expected data class for CCSDS frames.
     * Per Antonio, SMAP does not wish us to throw out any LEOT data based upon
     * data class (because frankly, they don't seem to have any clue what it
     * will be). Added data class for NEN_STATUS packets instead per MPCS-5195.
     * However, I am not dumb enough to hard code it this time. I put it in the
     * config so we can override in the SMAP config when they once again change
     * their minds.
     */
    /** Maximum Julian day allowed */
    public static final int MAX_JULIAN = 9999;

    /** Maximum seconds-of-day allowed */
    public static final int MAX_SECONDS = 86400;

    /** Maximum milliseconds allowed */
    public static final int MAX_MILLISECONDS = 999;

    /** Maximum milliseconds allowed */
    public static final int DATA_ORDER = 0;

    /** Date object for the Julian Day reference time */
    public static final Date JD_REF;

    private static StringBuilder sb;

    static {
        final SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");
        final Calendar cal = new GregorianCalendar(UTC);
        cal.set(1995, Calendar.OCTOBER, 10, 0, 0, 0); // Oct 10, 1995
        JD_REF = cal.getTime();
        sb = new StringBuilder(1024);
    }

    /** header version -- word 1, bits 1-2: 1=current specification */
    private int version = SUPPORTED_VERSION;
    /** length of TDU in bytes -- word 1 bits 3-16 */
    private int tduLength = 0;
    /** Reed-solomon decoding: word 2 bit 1; 0=disabled, 1=enabled (default) */
    private int rsEnabled = 1;
    /**
     * Reed-solomon error: word 2 bit 2; 0=frame was corrected; 1=frame could
     * not be corrected
     */
    private int rsError = 0;
    /** Frame CRC checking: word 2 bit 3; 0=disabled (default); 1=enabled */
    private int crcEnabled = 0;
    /** Frame CRC error: word 2 bit 4; 0=no error found; 1=error detected */
    private int crcError = 0;
    /** Master channel sequence checking -- N/A for SMAP; word 2 bit 5 */
    private final int chanSeqCheck = 0;
    /** Master channel sequence error -- N/A for SMAP; word 2 bit 6 */
    private final int chanSeqError = 0;
    /**
     * Data inversion; word 2 bits 7-8; 0=no inversion; 1=data inverted but not
     * corrected; 2=data inverted but corrected
     */
    private int dataInversion = 0;
    /**
     * Frame synchronization; word 2 bits 9-10; 0=search frame; 1=frame received
     * while in check 2=frame received while in lock 3=frame received while in
     * flywheel
     */
    private int frameSync = 2;
    /** Data order: word 2 bit 11; 0=msb first (only allowed value) */
    private final int dataOrder = 0;
    /** Data class; word 2 bits 12-16 (5 bits) */
    private int dataClass;
    /** Earth received time PB-5 flag (always zero) word 3 bit 1 */
    private final int ertPb5Flag = 0;
    /**
     * Earth received time truncated julian day where reference epoch is Oct 10,
     * 1995 word 3 bits 2-15 (12 bits)
     */
    private int ertJulianDay = 0;
    /**
     * Earth received time seconds of day. This is a 17-bit field whose MSB is
     * the last bit of word 3, and 16 LSB are in word 4.
     */
    private int ertSeconds = 0;
    /**
     * Earth received time milliseconds word 5 bits 1-10 (remaining bits of word
     * 5 are unused)
     */
    private int ertMilliseconds = 0;

    private static final int BOOL_BITS = 1;
    private static final int VERSION_BITS = 2;
    private static final int TDU_LENGTH_BITS = 14;
    private static final int DATA_INVERSION_BITS = 2;
    private static final int FRAME_SYNC_BITS = 2;
    private static final int DATA_CLASS_BITS = 5;
    private static final int ERT_JD_BITS = 14;
    private static final int ERT_SEC_MSB_BITS = 1;
    private static final int ERT_SEC_LSB_BITS = 16;
    private static final int ERT_SEC_BITS = ERT_SEC_MSB_BITS + ERT_SEC_LSB_BITS;
    private static final int ERT_MILLIS_BITS = 10;

    /**
     * Constructs a LeotHead object with a current ERT.
     */
    public LeotHead() {

        this(new Date());
    }

    /**
     * Constructs a LeotHead object with the given ERT and TDU length.
     * 
     * @param size
     *            the TDU length in bytes
     * @param d
     *            the ERT as a Date object
     */
    public LeotHead(final int size, final Date d) {

        this(d);
        this.tduLength = size + LEOT_HEADER_SIZE;
    }

    /**
     * Constructs a LeotHead object with the given ERT.
     * 
     * @param d
     *            the ERT as a Date object
     */
    public LeotHead(final Date d) {

        final EarthReceivedTime ert = new EarthReceivedTime(d);
        this.setErtMilliseconds(ert.getMilliseconds());
        this.setErtSeconds(ert.getSecondsOfDay());
        this.setErtJulianDay(ert.getJulianOffset(JD_REF));
    }

    /**
     * Write this header into the given output buffer starting at the byte
     * offset given by start parameter.
     * 
     * @param out
     *            output stream to write to
     * @return number of bytes written (always size())
     * 
     * @throws IOException if the data cannot be written
     */
    public int write(final BitOutputStream out) throws IOException {

        out.writeBits(this.version, VERSION_BITS);
        out.writeLongBits(this.tduLength, TDU_LENGTH_BITS);
        out.writeBits(this.rsEnabled, BOOL_BITS);
        out.writeBits(this.rsError, BOOL_BITS);
        out.writeBits(this.crcEnabled, BOOL_BITS);
        out.writeBits(this.crcError, BOOL_BITS);
        out.writeBits(this.chanSeqCheck, BOOL_BITS);
        out.writeBits(this.chanSeqError, BOOL_BITS);
        out.writeBits(this.dataInversion, DATA_INVERSION_BITS);
        out.writeBits(this.frameSync, FRAME_SYNC_BITS);
        out.writeBits(this.dataOrder, BOOL_BITS);
        out.writeBits(this.dataClass, DATA_CLASS_BITS);
        out.writeBits(this.ertPb5Flag, BOOL_BITS);
        out.writeLongBits(this.ertJulianDay, ERT_JD_BITS);
        out.writeLongBits(this.ertSeconds, ERT_SEC_BITS);
        out.writeLongBits(this.ertMilliseconds, ERT_MILLIS_BITS);
        out.writeBits(0, 6); // FILLER AT END
        out.flush();

        return size();
    }

    /**
     * Dumps frame header values to the console.
     * 
     * @param log the Tracer to dump to
     */
    public void dump(final Logger log) {
        log.debug("LEO Transfer Frame");

        sb.setLength(0);
        sb.append("version id = ");
        sb.append(this.version);
        log.debug(sb.toString());

        sb.setLength(0);
        sb.append("TDU length (bytes) = ");
        sb.append(this.tduLength);
        log.debug(sb.toString());

        sb.setLength(0);
        sb.append("Reed-solomon decoding = ");
        sb.append(this.rsEnabled);
        log.debug(sb.toString());

        sb.setLength(0);
        sb.append("Reed-solomon error = ");
        sb.append(this.rsError);
        log.debug(sb.toString());

        sb.setLength(0);
        sb.append("CRC checking = ");
        sb.append(this.crcEnabled);
        log.debug(sb.toString());

        sb.setLength(0);
        sb.append("CRC error = ");
        sb.append(this.crcError);
        log.debug(sb.toString());

        // Skip unused Master channel sequence bits

        sb.setLength(0);
        sb.append("Data inversion = ");
        sb.append(this.dataInversion);
        log.debug(sb.toString());

        sb.setLength(0);
        sb.append("Frame synchronization = ");
        sb.append(this.frameSync);
        log.debug(sb.toString());

        sb.setLength(0);
        sb.append("Data order = ");
        sb.append(this.dataOrder);
        log.debug(sb.toString());

        sb.setLength(0);
        sb.append("Data class = ");
        sb.append(this.dataClass);
        log.debug(sb.toString());

        sb.setLength(0);
        sb.append("ERT JD = ");
        sb.append(this.ertJulianDay);
        log.debug(sb.toString());

        sb.setLength(0);
        sb.append("ERT seconds = ");
        sb.append(this.ertSeconds);
        log.debug(sb.toString());

        sb.setLength(0);
        sb.append("ERT milliseconds = ");
        sb.append(this.ertMilliseconds);
        log.debug(sb.toString());
    }

    /**
     * Gets the header size.
     * 
     * @return the size of the frame header in bytes
     */
    public int size() {

        return LEOT_HEADER_SIZE;
    }

    /**
     * Gets the frame version field.
     * 
     * @return version from frame header
     */
    public int getVersion() {

        return this.version;
    }

    /**
     * Sets the frame version field.
     * 
     * @param version
     *            the version to set
     */
    public void setVersion(final int version) {

        this.version = version;
    }

    /**
     * Gets the TDU length from the frame header.
     * 
     * @return the TDU length in bytes. This includes header bytes
     */
    public int getTduLength() {

        return this.tduLength;
    }

    /**
     * Sets the TDU length from the frame header.
     * 
     * @param tduLength
     *            TDU length in bytes set set. This includes header bytes
     */
    public void setTduLength(final int tduLength) {

        this.tduLength = tduLength + LEOT_HEADER_SIZE;
    }

    /**
     * Gets the RS Enabled flag from the frame header.
     * 
     * @return RS enabled flag
     */
    public int getRsEnabled() {

        return this.rsEnabled;
    }

    /**
     * Sets the RS Enabled flag in the frame header.
     * 
     * @param rsEnabled
     *            flag to set
     */
    public void setRsEnabled(final int rsEnabled) {

        this.rsEnabled = rsEnabled;
    }

    /**
     * Gets the RS error flag from the frame header.
     * 
     * @return RS error flag
     */
    public int getRsError() {

        return this.rsError;
    }

    /**
     * Sets the RS error flag in the frame header.
     * 
     * @param rsError
     *            error flag to set
     */
    public void setRsError(final int rsError) {

        this.rsError = rsError;
    }

    /**
     * Gets the CRC Enabled flag from the frame header.
     * 
     * @return CRC enabled flag
     */
    public int getCrcEnabled() {

        return this.crcEnabled;
    }

    /**
     * Sets the CRC Enabled flag in the frame header.
     * 
     * @param crcEnabled
     *            flag to set
     */
    public void setCrcEnabled(final int crcEnabled) {

        this.crcEnabled = crcEnabled;
    }

    /**
     * Gets the CRC error flag from the frame header.
     * 
     * @return CRC error flag
     */
    public int getCrcError() {

        return this.crcError;
    }

    /**
     * Sets the CRC error flag in the frame header.
     * 
     * @param crcError
     *            error flag to set
     */
    public void setCrcError(final int crcError) {

        this.crcError = crcError;
    }

    /**
     * Gets the data inversion flag from the frame header.
     * 
     * @return data inversion flag
     */
    public int getDataInversion() {

        return this.dataInversion;
    }

    /**
     * Sets the data inversion flag in the frame header.
     * 
     * @param dataInversion
     *            inversion flag to set
     */
    public void setDataInversion(final int dataInversion) {

        this.dataInversion = dataInversion;
    }

    /**
     * Gets the frame sync flag from the frame header.
     * 
     * @return frame sync flag
     */
    public int getFrameSync() {

        return this.frameSync;
    }

    /**
     * Sets the frame sync flag in the frame header.
     * 
     * @param frameSync
     *            sync flag to set
     */
    public void setFrameSync(final int frameSync) {

        this.frameSync = frameSync;
    }

    /**
     * Read-only property because value is always zero.
     * 
     * @return always zero
     */
    public int getDataOrder() {

        return this.dataOrder;
    }

    /**
     * Gets the data class from the frame header.
     * 
     * @return data class value
     */
    public int getDataClass() {

        return this.dataClass;
    }

    /**
     * Sets the data class in the frame header.
     * 
     * @param dataClass
     *            data class value to set
     */
    public void setDataClass(final int dataClass) {

        this.dataClass = dataClass;
    }

    /**
     * Gets the Julian day from the frame header.
     * 
     * @return Julian day value
     */
    public int getErtJulianDay() {

        return this.ertJulianDay;
    }

    /**
     * Sets the Julian day in the frame header.
     * 
     * @param ertJulianDay Julian day value to set
     */
    public void setErtJulianDay(final int ertJulianDay) {

        this.ertJulianDay = ertJulianDay;
    }

    /**
     * Gets the ERT seconds value from the frame header.
     * 
     * @return ERT seconds, relative to JD epoch
     */
    public int getErtSeconds() {

        return this.ertSeconds;
    }

    /**
     * Sets the ERT seconds value in the frame header.
     * 
     * @param ertSeconds
     *            ERT seconds to set, relative to JD epoch
     */
    public void setErtSeconds(final int ertSeconds) {

        this.ertSeconds = ertSeconds;
    }

    /**
     * Gets the ERT milliseconds value from the frame header.
     * 
     * @return ERT milliseconds, relative to ERT seconds
     */
    public int getErtMilliseconds() {

        return this.ertMilliseconds;
    }

    /**
     * Sets the ERT milliseconds value in the frame header.
     * 
     * @param ertMilliseconds
     *            ERT milliseconds to set, relative to ERT seconds
     */
    public void setErtMilliseconds(final int ertMilliseconds) {

        this.ertMilliseconds = ertMilliseconds;
    }

    /**
     * Get frame sequence error state.
     * 
     * @return True if out-of-sequence
     */
    public boolean getSeqError() {

        return (this.chanSeqError != 0);
    }

    /**
     * Get ERT (PB5) flag.
     * 
     * @return Flag
     */
    public int getErtFlag() {

        return this.ertPb5Flag;
    }

}
