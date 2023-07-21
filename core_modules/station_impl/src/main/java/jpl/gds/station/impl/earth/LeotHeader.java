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
package jpl.gds.station.impl.earth;

import java.io.IOException;
import java.time.Instant;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

import jpl.gds.shared.io.BitExtractor;
import jpl.gds.shared.io.BitOutputStream;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationTelemHeader;
import jpl.gds.station.api.IStationTelemHeaderUpdater;
import jpl.gds.station.api.InvalidFrameCode;

/**
 * LEO transfer frame header Content is specified in SMAP ICD TBD (no version
 * Nov 22 2010)
 * 
 *
 */
public class LeotHeader implements IStationTelemHeaderUpdater {
    
    private static final Tracer            logger            = TraceManager.getTracer(Loggers.TLM_INPUT);

    
    /** Data class for supported version */
    private static final int SUPPORTED_VERSION = 1;
    
    private static final int MAX_TDU_LENGTH = 6717;
    private static final int MIN_TDU_LENGTH = 1;

    /*
     * Removed expected data class for CCSDS frames.
     * Mission does not wish us to throw out any LEOT data based upon
     * data class (because frankly, they don't seem to have any clue what it
     * will be). Added data class for NEN_STATUS packets instead.
     * However, I am not dumb enough to hard code it this time. I put it in the
     * config so we can override in the  config
     */
  

    /** Maximum Julian day allowed */
    private static final int MAX_JULIAN = 9999;

    /** Maximum seconds-of-day allowed */
    private static final int MAX_SECONDS = 86400;

    /** Maximum milliseconds allowed */
    private static final int MAX_MILLISECONDS = 999;

    /** Maximum milliseconds allowed */
    private static final int DATA_ORDER = 0;

    /** IAccurateDateTime object for the Julian Day reference time */
    private static final IAccurateDateTime JD_REF;

    static {
        final SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");
        final Calendar cal = new GregorianCalendar(UTC);
        cal.set(1995, Calendar.OCTOBER, 10, 0, 0, 0); // Oct 10, 1995
        JD_REF = new AccurateDateTime(cal.getTime());
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
    /** Master channel sequence checking -- word 2 bit 5 */
    private int chanSeqCheck = 0;
    /** Master channel sequence error -- word 2 bit 6 */
    private int chanSeqError = 0;
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
    private int dataOrder = 0;
    /** Data class; word 2 bits 12-16 (5 bits) */
    private int dataClass;
    /** Earth received time PB-5 flag (always zero) word 3 bit 1 */
    private int ertPb5Flag = 0;
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

    /** Save for populating Frame header */
    private byte[] header =  new byte[1024];

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
     * Constructs a LeotHeader object with a current ERT.
     */
    public LeotHeader() {

        this(new AccurateDateTime());
    }

    /**
     * Constructs a LeotHeader object with the given ERT and TDU length.
     * 
     * @param size
     *            the TDU length in bytes
     * @param d
     *            the ERT as an IAccurateDateTime object
     */
    public LeotHeader(final int size, final IAccurateDateTime d) {

        this(d);
        this.tduLength = size + getSizeBytes();
    }

    /**
     * Constructs a LeotHeader object with the given ERT.
     * 
     * @param d
     *            the ERT as an IAccurateDateTime object
     */
    public LeotHeader(final IAccurateDateTime d) {

        final EarthReceivedTime ert = new EarthReceivedTime(d);
        this.setErtMilliseconds(ert.getMilliseconds());
        this.setErtSeconds(ert.getSecondsOfDay());
        this.setErtJulianDay(ert.getJulianOffset(JD_REF));
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemHeader#isValid()
     */
    @Override
    public boolean isValid() {

        /*
         * Remove check for data class for LEOT
         * frames. Class of header will be checked outside this method to detect NEN 
         * status packets.
         */
        if ((getVersion()         != LeotHeader.SUPPORTED_VERSION) ||
                (getDataOrder()       != LeotHeader.DATA_ORDER)        ||
                (getErtFlag()         != 0)                          ||
                (getDataLength()                 <  MIN_TDU_LENGTH)             ||
                (getDataLength()                >  MAX_TDU_LENGTH)             ||
                (getErtJulianDay()    >  LeotHeader.MAX_JULIAN)        ||
                (getErtSeconds()      >  LeotHeader.MAX_SECONDS)       ||
                (getErtMilliseconds() >  LeotHeader.MAX_MILLISECONDS))
        {
            logger.debug("    ------ LEO-T header not found; dump start ------");
            logger.debug(toString());
            logger.debug("    ------ LEO-T header dump end ------");

            return false;
        }

        return true;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemHeader#load(byte[], int)
     */
    @Override
    public int load(final byte[] buff, final int start) throws IOException {
        System.arraycopy(buff, start, header, 0, IStationTelemHeader.LEOT_HEADER_SIZE);

        final BitExtractor extractor = new BitExtractor(buff, start);
        this.version = extractor.getBits(VERSION_BITS);
        this.tduLength = extractor.getLongbits(TDU_LENGTH_BITS);

        this.rsEnabled = extractor.getBits(BOOL_BITS);
        this.rsError = extractor.getBits(BOOL_BITS);
        this.crcEnabled = extractor.getBits(BOOL_BITS);
        this.crcError = extractor.getBits(BOOL_BITS);
        this.chanSeqCheck = extractor.getBits(BOOL_BITS);
        this.chanSeqError = extractor.getBits(BOOL_BITS);
        this.dataInversion = extractor.getBits(DATA_INVERSION_BITS);
        this.frameSync = extractor.getBits(FRAME_SYNC_BITS);
        this.dataOrder = extractor.getBits(BOOL_BITS);
        this.dataClass = extractor.getBits(DATA_CLASS_BITS);

        this.ertPb5Flag = extractor.getBits(BOOL_BITS);
        this.ertJulianDay = extractor.getLongbits(ERT_JD_BITS);

        this.ertSeconds = extractor.getLongbits(ERT_SEC_BITS);

        this.ertMilliseconds = extractor.getLongbits(ERT_MILLIS_BITS);
        // WE SHOULD PROBABLY ASSERT HERE THAT EXTRACTOR NEVER READ BEYOND
        // size()

        // THIS VALUE IS FIXED
        return start + getSizeBytes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

        return getSizeBytes();
    }


    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemHeader#getSizeBytes()
     */
    @Override
    public int getSizeBytes() {

        return IStationTelemHeader.LEOT_HEADER_SIZE;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemHeader#isBadFrame()
     */
    @Override
    public boolean isBadFrame() {
        if (getRsEnabled() != 0 && getRsError() != 0)
        {
            return true;
        }
        else if (getCrcEnabled() != 0 && getCrcError() != 0)
        {
            return true;
        }
        return false;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemHeader#getBadReason()
     */
    @Override
    public InvalidFrameCode getBadReason() {
        if (getRsEnabled() != 0 && getRsError() != 0)
        {
            return InvalidFrameCode.RS_ERROR;
        }
        else if (getCrcEnabled() != 0 && getCrcError() != 0)
        {
            return InvalidFrameCode.CRC_ERROR;
        }
        return null;
    }

    /**
     * Gets the frame version field.
     * 
     * @return version from frame header
     */
    private int getVersion() {

        return this.version;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemHeader#getDataLength()
     */
    @Override
    public int getDataLength() {

        return Math.max(0, this.tduLength - getSizeBytes());
    }

    /**
     * Gets the RS Enabled flag from the frame header.
     * 
     * @return RS enabled flag
     */
    private int getRsEnabled() {

        return this.rsEnabled;
    }

    /**
     * Gets the RS error flag from the frame header.
     * 
     * @return RS error flag
     */
    private int getRsError() {

        return this.rsError;
    }

    /**
     * Gets the CRC Enabled flag from the frame header.
     * 
     * @return CRC enabled flag
     */
    private int getCrcEnabled() {

        return this.crcEnabled;
    }

    /**
     * Gets the CRC error flag from the frame header.
     * 
     * @return CRC error flag
     */
    private int getCrcError() {

        return this.crcError;
    }
    
    /**
     * Read-only property because value is always zero.
     * 
     * @return always zero
     */
    private int getDataOrder() {

        return this.dataOrder;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemHeader#getDataClass()
     */
    @Override
    public int getDataClass() {

        return this.dataClass;
    }


    /**
     * Gets the Julian day from the frame header.
     * 
     * @return Julian day value
     */
    private int getErtJulianDay() {

        return this.ertJulianDay;
    }

    /**
     * Sets the Julian day in the frame header.
     * 
     * @param ertJulianDay Julian day value to set
     */
    private void setErtJulianDay(final int ertJulianDay) {

        this.ertJulianDay = ertJulianDay;
    }

    /**
     * Gets the ERT seconds value from the frame header.
     * 
     * @return ERT seconds, relative to JD epoch
     */
    private int getErtSeconds() {

        return this.ertSeconds;
    }

    /**
     * Sets the ERT seconds value in the frame header.
     * 
     * @param ertSeconds
     *            ERT seconds to set, relative to JD epoch
     */
    private void setErtSeconds(final int ertSeconds) {

        this.ertSeconds = ertSeconds;
    }

    /**
     * Gets the ERT milliseconds value from the frame header.
     * 
     * @return ERT milliseconds, relative to ERT seconds
     */
    private int getErtMilliseconds() {

        return this.ertMilliseconds;
    }

    /**
     * Sets the ERT milliseconds value in the frame header.
     * 
     * @param ertMilliseconds
     *            ERT milliseconds to set, relative to ERT seconds
     */
    private void setErtMilliseconds(final int ertMilliseconds) {

        this.ertMilliseconds = ertMilliseconds;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemHeader#isOutOfSequence()
     */
    @Override
    public boolean isOutOfSequence() {

        return (this.chanSeqError != 0);
    }

    /**
     * Get ERT (PB5) flag.
     * 
     * @return Flag
     */
    private int getErtFlag() {

        return this.ertPb5Flag;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemHeader#getErt()
     */
    @Override
    public Instant getErt(){
        final EarthReceivedTime ert = new EarthReceivedTime(JD_REF,
                getErtJulianDay(), getErtSeconds(), getErtMilliseconds());
        return Instant.ofEpochMilli(ert.getTime());
    }

    /**
     * Convert to string form.
     * 
     * @return String
     */
    @Override
    public String toString() {

        final StringBuilder sb = new StringBuilder();

        sb.append("LEO Transfer Frame");
        sb.append(" version id ").append(this.version);
        sb.append(" TDU length ").append(this.tduLength);
        sb.append(" Reed-solomon decoding ").append(this.rsEnabled);
        sb.append(" Reed-solomon error ").append(this.rsError);
        sb.append("\nCRC checking ").append(this.crcEnabled);
        sb.append(" CRC error ").append(this.crcError);
        sb.append(" Data inversion ").append(this.dataInversion);
        sb.append(" Frame synchronization ").append(this.frameSync);
        sb.append(" Data order ").append(this.dataOrder);
        sb.append(" Data class ").append(this.dataClass);
        sb.append("\nERT JD ").append(this.ertJulianDay);
        sb.append(" ERT seconds ").append(this.ertSeconds);
        sb.append(" ERT milliseconds ").append(this.ertMilliseconds);
        sb.append(" ERT ").append(getErt());

        return sb.toString();
    }

    @Override
    public byte[] getHeader() {
        return header;
    }
    
    /**
     * Wrapper for IAccurateDateTime for retrieving Earth Received Time UTC label values.
     * 
     *
     */
    static class EarthReceivedTime extends AccurateDateTime {
        private static final long serialVersionUID = 1L;
        private static final int SEC_PER_DAY = 60 * 60 * 24;
        private static final int MS_PER_DAY = 1000 * SEC_PER_DAY;
        
        /**
         * Construct ERT using current UTC time
         */
        public EarthReceivedTime() {
            super();
        }
        
        /**
         * Construct ERT from given UTC time
         * 
         * @param d time
         */
        public EarthReceivedTime(final IAccurateDateTime d) {
            super(d.getTime());
        }

        private static long reconstruct(final IAccurateDateTime jdref, final int jdoffset, final int seconds,
                                        final int millis) {
            return ((jdref.getTime()/MS_PER_DAY + jdoffset)*SEC_PER_DAY + seconds)*1000 + millis;
        }
        
        /**
         * Reconstruct an IAccurateDateTime from fragments and reference date
         * 
         * @param jdref
         *            Julian reference date (Epoch)
         * @param jdoffset
         *            offset from Julian epoch, days
         * @param seconds
         *            seconds of day
         * @param millis
         *            milliseconds
         */
        public EarthReceivedTime(final IAccurateDateTime jdref, final int jdoffset, final int seconds,
                final int millis) {
            super(reconstruct(jdref, jdoffset, seconds, millis));
        }
        
        public int getJulianOffset(final IAccurateDateTime ref) {
            final long refjd = ref.getTime()/MS_PER_DAY;
            final long day = this.getTime()/MS_PER_DAY;
            return (int)(day-refjd);
        }
        
        public int getMilliseconds() {
            return (int)(this.getTime()%1000);
        }
        
        public int getSecondsOfDay() {
            return (int)((this.getTime()/1000)%SEC_PER_DAY);
        }
    }

}
