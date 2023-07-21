/*
 * Copyright 2006-2021. California Institute of Technology.
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
package jpl.gds.station.impl.sle;

import jpl.gds.shared.io.BitExtractor;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.InvalidFrameCode;
import jpl.gds.station.api.sle.ESleFrameQuality;
import jpl.gds.station.api.sle.ISleHeader;
import jpl.gds.station.api.sle.annotation.ISlePrivateAnnotation;
import jpl.gds.station.api.sle.annotation.IV3SlePrivateAnnotation;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SLE Header adapted from CCSDS RCF Bluebook 911.2-B-3 and CCSDS RAF Bluebook 911.1-B-4 for MCSECLIV-852
 *
 */
public class SleHeader implements ISleHeader {

    /**
     * Number of bytes in the header for ERT days
     **/
    private static final int ERT_DAYS_BYTES = 2;

    /**
     * Number of bytes in the header for ERT milliseconds
     **/
    private static final int ERT_MILLIS_BYTES = 4;

    /**
     * Number of bytes in the header for ERT microseconds
     **/
    private static final int ERT_MICROS_BYTES = 2;

    /**
     * Number of bytes in the header for antenna id
     **/
    private static final int ANTENNA_ID_BYTES = 16;

    /**
     * Number of bytes in the header for data link continuity
     **/
    private static final int DATA_LINK_CONTINUITY_BYTES = 3;

    /**
     * Number of bytes in the header for frame quality
     **/
    private static final int FRAME_QUALITY_BYTES = 1;

    /**
     * Number of bytes in the header for data length (CADU length in bytes)
     **/
    private static final int DATA_LENGTH_BYTES = 3;

    /**
     * Total number of bytes in the standard header (sum of preceding fields)
     **/
    public static final int TOTAL_STANDARD_HEADER_LENGTH_BYTES =
            ERT_DAYS_BYTES + ERT_MILLIS_BYTES + ERT_MICROS_BYTES +
                    ANTENNA_ID_BYTES + DATA_LINK_CONTINUITY_BYTES +
                    FRAME_QUALITY_BYTES + DATA_LENGTH_BYTES;

    /**
     * Conversion ratio between bits and bytes
     **/
    private static final int BITS_PER_BYTE = 8;

    /**
     * Maximum CADU allowed in bytes, per TransferFrameDictionary.rnc. Maximum CADU in bits is 448000, so max in bytes
     * is 56000
     */
    private static final int MAX_DATA_LENGTH = 56000;

    /**
     * Epoch used by SLE format, per CCSDS specs cited above
     **/
    private static final ZonedDateTime EPOCH_1958 =
            ZonedDateTime.of(1958, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

    /**
     * Epoch used by AMPCS, Java, and Linux
     **/
    private static final ZonedDateTime EPOCH_1970 =
            ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

    /**
     * Difference between 1958 and 1970 epochs in days. Necessary for conversion between the two
     **/
    private static final long DAYS_BETWEEN_1958_AND_1970 = ChronoUnit.DAYS.between(EPOCH_1958, EPOCH_1970);

    /**
     * Maximum milliseconds allowed (millis per day)
     **/
    private static final int MAX_MILLISECONDS = 86400000;

    /**
     * Maximum microseconds allowed (micros per millisecond)
     **/
    private static final int MAX_MICROSECONDS = 1000;


    /**
     * ERT containing microsecond precision
     **/
    private ZonedDateTime earthReceivedTime;

    /**
     * Milliseconds of the day
     **/
    private int ertMilliseconds;

    /**
     * Microseconds of the millisecond
     **/
    private int ertMicroseconds;

    /**
     * Antenna Id
     **/
    private String antennaId;

    /**
     * Data link continuity (-1 .. 16777215)
     **/
    private int datalinkContinuity;

    /**
     * Delivered frame quality (0, 1, 2)
     **/
    private ESleFrameQuality returnFrameQuality;

    /**
     * Length in Bytes of the data field (corresponds to the CADU length in bytes of the frame contained)
     **/
    private int dataFieldLength;

    /**
     * Entire header, including any private annotation
     **/
    private byte[] headerBytes;

    /**
     * the parsed private annotation
     **/
    private ISlePrivateAnnotation privateAnnotation;

    /**
     * Constructs a SleHeader object with the given private annotation parser.
     *
     * @param slePrivateAnnotation private annotation handler
     */
    public SleHeader(final ISlePrivateAnnotation slePrivateAnnotation) {
        privateAnnotation = slePrivateAnnotation;
    }

    /**
     * Constructs a SleHeader object with the given ERT, data length, and private annotation parser.
     *
     * @param dataSize             the data length in bytes
     * @param ert                  the ERT as an IAccurateDateTime object
     * @param slePrivateAnnotation the private annotation handler for this SLE Header
     */
    public SleHeader(final int dataSize, final IAccurateDateTime ert,
                     final ISlePrivateAnnotation slePrivateAnnotation) {
        dataFieldLength = dataSize;
        earthReceivedTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ert.getTime()), ZoneId.of("UTC"));
        privateAnnotation = slePrivateAnnotation;
    }

    /**
     * Indicates if the header is valid.
     *
     * @return true or false
     */
    @Override
    public boolean isValid() {
        if ((returnFrameQuality.getValue() > 2) ||
                (returnFrameQuality.getValue() < 0) ||
                (getDataLength() == 0) ||
                (getDataLength() > MAX_DATA_LENGTH) ||
                (ertMilliseconds > MAX_MILLISECONDS) ||
                (ertMicroseconds > MAX_MICROSECONDS)) {

            return false;
        }

        return true;
    }

    /**
     * Loads Standard SLE Header fields and the private annotation. Default private annotation parser is the No Op one
     * (which assumes a non-existent private annotation).
     *
     * @param buff  the buffer containing the station data
     * @param start the starting offset of the station header in the buffer
     * @return index of the buffer after header
     */
    @Override
    public int load(byte[] buff, int start) {

        // headerBytes will be a subset of the incoming buffer
        headerBytes = Arrays.copyOfRange(buff, start,
                start + ISleHeader.SLE_HEADER_SIZE + privateAnnotation.getPrivateAnnotationSizeBytes());

        int runningOffset = 0;

        // Set ERT
        int ertDaysSince1958Epoch = getUnsignedInt(headerBytes, runningOffset, ERT_DAYS_BYTES);
        runningOffset += ERT_DAYS_BYTES;

        ertMilliseconds = getUnsignedInt(headerBytes, runningOffset, ERT_MILLIS_BYTES);
        runningOffset += ERT_MILLIS_BYTES;

        ertMicroseconds = getUnsignedInt(headerBytes, runningOffset, ERT_MICROS_BYTES);
        runningOffset += ERT_MICROS_BYTES;

        setEarthReceivedTime(ertDaysSince1958Epoch, ertMilliseconds, ertMicroseconds);

        // Set Antenna Id
        byte[] antennaIdBuffer = Arrays.copyOfRange(headerBytes, runningOffset, runningOffset + ANTENNA_ID_BYTES);
        antennaId = new String(antennaIdBuffer, StandardCharsets.UTF_8);
        runningOffset += ANTENNA_ID_BYTES;

        // Set Continuity
        datalinkContinuity = getUnsignedInt(headerBytes, runningOffset, DATA_LINK_CONTINUITY_BYTES);
        runningOffset += DATA_LINK_CONTINUITY_BYTES;

        // Set Quality
        returnFrameQuality = ESleFrameQuality.getByValue(
                getUnsignedInt(headerBytes, runningOffset, FRAME_QUALITY_BYTES));
        runningOffset += FRAME_QUALITY_BYTES;

        // Set Data Field Length (CADU in bytes)
        dataFieldLength = getUnsignedInt(headerBytes, runningOffset, DATA_LENGTH_BYTES);
        runningOffset += DATA_LENGTH_BYTES;

        // Set Private Annotation
        privateAnnotation.load(headerBytes, runningOffset);
        runningOffset += privateAnnotation.getPrivateAnnotationSizeBytes();

        // returns index after header
        return runningOffset + start;
    }

    /**
     * Gets the header size.
     *
     * @return the size of the frame header in bytes
     */
    @Override
    public int getSizeBytes() {
        if (privateAnnotation.isPresent()) {
            return TOTAL_STANDARD_HEADER_LENGTH_BYTES + privateAnnotation.getPrivateAnnotationSizeBytes();
        } else {
            return TOTAL_STANDARD_HEADER_LENGTH_BYTES;
        }
    }

    /**
     * Gets a flag indicating if the station header indicated that what follows is a bad frame.
     *
     * @return true if frame is bad, false if not
     */
    @Override
    public boolean isBadFrame() {
        return false;
    }

    /**
     * Gets the reason a frame was bad, if the station header so indicates.
     *
     * @return the reason code
     */
    @Override
    public InvalidFrameCode getBadReason() {
        return null;
    }

    /**
     * Gets the data length from the station header.
     * <p>
     * Length of the full data field (should match the CADU length of the transfer frame that the SLE frame wraps)
     *
     * @return the data length in bytes.
     */
    @Override
    public int getDataLength() {
        return dataFieldLength;
    }

    /**
     * Gets the data class from the header. Use and support of this value is station-specific.
     *
     * @return data class value
     */
    @Override
    public int getDataClass() {
        return 0;
    }

    /**
     * Get frame sequence error state from the header.
     *
     * @return True if the frame the header applies to is out-of-sequence with the previous frame
     */
    @Override
    public boolean isOutOfSequence() {
        return false;
    }


    /**
     * @return Earth Received Time
     */
    @Override
    public Instant getErt() {
        return earthReceivedTime.toInstant();
    }

    /**
     * @return the byte array used to parse out this header
     */
    @Override
    public byte[] getHeader() {
        if (privateAnnotation.isPresent()) {
            return headerBytes;
        } else {
            return Arrays.copyOfRange(headerBytes, 0, TOTAL_STANDARD_HEADER_LENGTH_BYTES);
        }
    }

    /**
     * Get the Private Annotation. If no SLE Private Annotation is present, expect the NoOp SLE Private Annotation.
     *
     * @return the SLE Private Annotation
     */
    @Override
    public ISlePrivateAnnotation getPrivateAnnotation() {
        return privateAnnotation;
    }

    /**
     * @return Antenna ID
     */
    @Override
    public String getAntennaId() {
        return antennaId;
    }

    @Override
    public int getIntAntennaId() {
        return SleTfDssIdExtractor.extractDssId(getAntennaId());
    }

    /**
     * Per SLE RAF Spec, dataLinkContinuity will be an integer in the range (-1 .. 16777215)
     *
     * @return Datalink Continuity
     */
    @Override
    public int getDatalinkContinuity() {
        return datalinkContinuity;
    }

    /**
     * Per SLE RAF Spec, frame quality will be an integer from the following list
     * <p>
     * good (0) erred (1) undetermined (2)
     *
     * @return Delivered Frame Quality
     */
    @Override
    public ESleFrameQuality getFrameQuality() {
        return returnFrameQuality;
    }

    /**
     * Helper function to set ERT from SLE ERT parts. Note, SLE transmits time based on the 1958 epoch, while Java and
     * Linux use 1970.
     *
     * @param days   per SLE spec, number of days since 1958-01-01
     * @param millis milliseconds in the day
     * @param micros microseconds remaining
     */
    private void setEarthReceivedTime(int days, int millis, int micros) {

        // convert days to the correct (Java) epoch
        long      daysSince1970 = days - DAYS_BETWEEN_1958_AND_1970;
        LocalDate d             = LocalDate.ofEpochDay(daysSince1970);

        // add days since epoch
        earthReceivedTime = ZonedDateTime.ofLocal(d.atStartOfDay(), ZoneId.of("UTC"), null);

        // add millis
        earthReceivedTime = earthReceivedTime.plus(millis, ChronoUnit.MILLIS);

        // add microseconds
        earthReceivedTime = earthReceivedTime.plus(micros, ChronoUnit.MICROS);
    }

    /**
     * Helper function to extract an integer from a byte array. ByteBuffer doesn't parse integers of fewer than 4 bytes
     * and BitExtractor doesn't parse integers of more then 2 bytes. Hence the use of two different tools to do one
     * job.
     *
     * @param buffer    byte array to parse for an integer
     * @param start     start index of the integer inside the "buffer" byte array
     * @param takeBytes how many bytes comprise the integer (may be fewer than 4 depending)
     * @return the integer parsed from bytes
     */
    protected int getUnsignedInt(byte[] buffer, int start, int takeBytes) {
        if (takeBytes < 3) {
            BitExtractor extractor = new BitExtractor(buffer, start);
            if (takeBytes == 1) {
                return extractor.getByte(takeBytes * BITS_PER_BYTE);
            }
            return extractor.getInt(takeBytes * BITS_PER_BYTE);
        } else {
            int    allocation = takeBytes < 4 ? 4 : takeBytes;
            byte[] intHolder  = new byte[allocation];
            System.arraycopy(buffer, start, intHolder, allocation - takeBytes, takeBytes);
            return ByteBuffer.wrap(intHolder).getInt();
        }
    }

    /**
     * Get metadata formatted as key value in readable format
     * @return SLE metadata map
     */
    public Map<String, String > getMetadata() {
        final Map<String, String> sleMeta = new LinkedHashMap<>();
        //SLE metadata
        sleMeta.put("is_valid", getMetaValue(this.isValid()));
        sleMeta.put("antenna_id", this.getAntennaId());
        sleMeta.put("ert", this.getErt().toString());
        sleMeta.put("data_link_continuity", getMetaValue(this.getDatalinkContinuity()));
        sleMeta.put("frame_quality", getMetaValue(this.getDatalinkContinuity()));

        //add private annotation metadata
        final ISlePrivateAnnotation pvta = this.getPrivateAnnotation();
        if (pvta instanceof IV3SlePrivateAnnotation) {
            final Map<String, String> pvtMeta = pvta.getMetadata();
            for (String pvtKey : pvtMeta.keySet()) {
                sleMeta.put(pvtKey, pvtMeta.get(pvtKey));
            }
        }
        return sleMeta;
    }

    private String getMetaValue(Object obj){
        return String.valueOf(obj);
    }
}