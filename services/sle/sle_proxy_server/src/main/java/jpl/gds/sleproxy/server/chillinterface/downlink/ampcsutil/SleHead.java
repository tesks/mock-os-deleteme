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
package jpl.gds.sleproxy.server.chillinterface.downlink.ampcsutil;

import com.lsespace.sle.user.service.data.TmProductionData;
import jpl.gds.sleproxy.server.chillinterface.internal.config.ChillInterfaceInternalConfigManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

/**
 * SLE Header format adapted from CCSDS RCF Bluebook 911.2-B-3 and CCSDS RAF Bluebook 911.1-B-4
 * for MCSECLIV-852
 * We added a frame length field to the standard header to improve ease of parsing.
 */
public class SleHead {

    private static final int ERT_DAYS_BYTES             = 2;
    private static final int ERT_MILLIS_BYTES           = 4;
    private static final int ERT_MICROS_BYTES           = 2;
    private static final int ANTENNA_ID_BYTES           = 16;
    private static final int DATA_LINK_CONTINUITY_BYTES = 3;
    private static final int FRAME_QUALITY_BYTES        = 1;
    private static final int FRAME_LENGTH_BYTES         = 3;

    private TmProductionData frame;

    public void load(final TmProductionData frame) {
        this.frame = frame;
    }

    public void serialize(OutputStream stream) throws IOException {
        if (frame == null) {
            return;
        }
        /**
         * From CCSDS RAF Service Spec
         * CCSDS 911.1-B-4
         * Page A-22
         *
         * RafTransferDataInvocation ::= SEQUENCE
         * { invokerCredentials Credentials
         * , earthReceiveTime Time
         * , antennaId AntennaId
         * , dataLinkContinuity INTEGER (-1 .. 16777215)
         * , deliveredFrameQuality FrameQuality
         *
         * NOTE!! We added a Frame Length Field here -- this is not in the spec
         *
         * , privateAnnotation CHOICE
         *  { null [0] NULL
         *  , notNull [1] OCTET STRING (SIZE (1 .. 128))
         *  }
         * , data SpaceLinkDataUnit
         * }
         */

        /** RAF Spec data structure with byte size guesses
         *
         * 		 * RafTransferDataInvocation ::= SEQUENCE -------- BYTE SIZE GUESSES
         * 		 * { invokerCredentials Credentials -------------- 0 since not included in TmProductionData
         * 		 * , earthReceiveTime Time ----------------------- 8 since assuming TimeCCSDS (2 + 4 + 2, see below)
         * 		 * , antennaId AntennaId ------------------------- 16 (assuming based on Antenna definition and String value in TmProductionData)
         * 		 * , dataLinkContinuity INTEGER (-1 .. 16777215) - 3 corresponds to given max int value.
         * 		 * , deliveredFrameQuality FrameQuality ---------- 1 since contents are small numbers
         * 		 <THIS IS WHERE WE INSERTED FRAME LENGTH>
         * 		 * , privateAnnotation CHOICE -------------------- 128 max size (note! header ends here)
         * 		 *  { null [0] NULL
         * 		 *  , notNull [1] OCTET STRING (SIZE (1 .. 128))
         * 		 *  }
         * 		 * , data SpaceLinkDataUnit ---------------------- 65536 max size
         * 		 * }
         */

        // CREDENTIALS *************************************************************
        // Skipping Credentials, since they're not present on the TmProductionData

        // ERT - Time object *************************************************************
        /**
         * From CCSDS RAF Service Spec
         * CCSDS 911.1-B-4
         * Page A-5
         *
         * Time			::= CHOICE
         * {  ccsdsFormat 		[0] TimeCCSDS
         * ,  ccsdsPicoFormat   [1]   TimeCCSDSpico
         * }
         *
         * TimeCCSDS ::= OCTET STRING (SIZE(8))
         * -- P-field is implicit (not present, defaulted to 41 hex
         * -- T-field:
         * -- 2 octets: number of days since 1958/01/01 00:00:00
         * -- 4 octets: number of milliseconds of the day
         * -- 2 octets: number of microseconds of the millisecond
         * -- (set to 0 if not used)
         * -- This definition reflects exactly the format of the CCSDS defined
         * -- time tag as used in spacelink data units (see reference [7]).
         *
         * TimeCCSDSpico ::= OCTET STRING (SIZE(10))
         * -- P-field is implicit (not present, defaulted to 42 hex
         * -- T-field:
         * -- 2 octets: number of days since 1958/01/01 00:00:00
         * -- 4 octets: number of milliseconds of the day
         * -- 4 octets: number of picoseconds of the millisecond
         * -- (set to 0 if not used)
         * -- This definition reflects exactly the format of the CCSDS defined
         * -- time tag as used in spacelink data units (see reference [7]).
         */
        // Using TimeCCSDS, since our requirement is only for microseconds, not picoseconds
        // Therefore, 8 octets = 2 + 4 + 2

        // NOTE - I confirmed via data inspection that the JavaTimeTag uses the 1970/1/1 epoch
        ZonedDateTime dateTime1958 = ZonedDateTime.of(1958, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime dateTime1970 = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime frameZDT = ZonedDateTime.ofInstant(Instant.ofEpochMilli(frame.getErt().getMilliseconds()), ZoneId.of("UTC"));
        ZonedDateTime frameDayZDT = ZonedDateTime.of(frameZDT.getYear(), frameZDT.getMonthValue(), frameZDT.getDayOfMonth(), 0, 0, 0, 0, ZoneId.of("UTC"));

        // ERT : DAYS *************************************************************
        // per SLE spec, number of days between frame and 1958/01/01 00:00:00
        // allocating 2 bytes, per CCDSDS spec
        long daysBetween1958andFrame = ChronoUnit.DAYS.between(dateTime1958, frameDayZDT);
        writeInt(ERT_DAYS_BYTES, Math.toIntExact(daysBetween1958andFrame), stream);

        // ERT : MILLISECONDS *************************************************************
        // milliseconds should be (ert millis (total millis since epoch) - millis between this date and ert epoch (assume 1970))
        // allocating 4 bytes, per CCDSDS spec
        long millisBetween1970andFrameDay = ChronoUnit.MILLIS.between(dateTime1970, frameDayZDT);
        long millisecondsInTheDay = frame.getErt().getMilliseconds() - millisBetween1970andFrameDay;
        writeInt(ERT_MILLIS_BYTES, Math.toIntExact(millisecondsInTheDay), stream);

        // ERT : MICROSECONDS *************************************************************
        // write microseconds - no conversion necessary
        // allocating 2 bytes, per CCDSDS spec (TimeCCSDS)
        writeInt(ERT_MICROS_BYTES, Math.toIntExact(frame.getErt().getMicroseconds()), stream);

        // ANTENNA ID *************************************************************
        // Haven't been able to find documentation on the global object identifier, will default to localForm pending further research
        // Assuming this format "OCTET STRING (SIZE (1 .. 16))" means 1 to 16 OCTETS
        /**
         * From CCSDS RAF Service Spec
         * CCSDS 911.1-B-4
         * Page A-15
         *
         * AntennaId ::= CHOICE
         * {   globalForm [0] OBJECT IDENTIFIER
         * ,   localForm [1] OCTET STRING (SIZE (1 .. 16))
         * }
         *
         *
         * Object identifier definition
         * From Information Technology—Abstract Syntax Notation One (ASN.1): Specification of Basic Notation. 4th ed. International Standard, ISO/IEC 8824-1:2008. Geneva: ISO, 2008.
         *
         * OBJECT IDENTIFIER, A globally unique value associated with an object to unambiguously identify it.
         */
        // Assume the maximum, 16 octets
        writeByteArray(ANTENNA_ID_BYTES, frame.getAntennaId().getBytes(), stream);

        // DATA LINK CONTINUITY *************************************************************
        // dataLinkContinuity  INTEGER (-1 .. 16777215)
        /**
         * Java integer has 4 bytes, but only using 3 here, since spec shows int max value that corresponds to 3-byte number
         */
        // assign 3 octets for dataLinkContinuity
        writeInt(DATA_LINK_CONTINUITY_BYTES, frame.getDatalinkContinuity(), stream);

        // DELIVERED FRAME QUALITY *************************************************************
        /**
         * From CCSDS RAF Service Spec
         * CCSDS 911.1-B-4
         * Page A-16
         *
         * FrameQuality ::= INTEGER
         * {   good (0)
         * ,   erred (1)
         * ,   undetermined (2)
         * }
         *
         * Java has 4 bytes for integer, but we really only need 2 bits (less than 1 byte)
         * to represent these small numbers. Going with 1 byte.
         */
        writeInt(FRAME_QUALITY_BYTES, frame.getQuality().getValue(), stream);

        // 9/22/2021 UPDATE
        // Per discussions with Quyen and Julia, SLE header should include length
        // max 448000 per current version of transfer_frame.xml (CADU size in bytes)
        // NB - CADU includes ASM size
        writeInt(FRAME_LENGTH_BYTES, frame.getData().getLength() + ChillInterfaceInternalConfigManager.INSTANCE.getDownlinkASMHeader().length, stream);

        // PRIVATE ANNOTATION *************************************************************
        // privateAnnotation - OCTET STRING (SIZE (1 .. 128))
        // Use of private annotation is determined on a mission-by-mission basis.
        // Assume for now private annotation will not be used.
        if (frame.getPrivateAnnotation() != null) {
            // MPCS-12335  - write the private annotations as actual size instead of fixed 128-byte array
            byte[] privateAnnotation = frame.getPrivateAnnotation();
            writeByteArray(privateAnnotation.length, privateAnnotation, stream);
        }

    }

    /**
     * Writes byte array to file output stream
     *
     * @param bufferSize
     * 			The size of the byte array to write
     * @param dataToWrite
     * 			The byte array to write to file
     * @throws IOException
     * 			Thrown when attempt to write bytes to a file causes an exception
     */
    private void writeByteArray(int bufferSize, byte[] dataToWrite, final OutputStream stream) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
        if (dataToWrite == null) {
            byteBuffer.put(new byte[bufferSize]);
        } else {
            byteBuffer.put(dataToWrite);
        }
        stream.write(byteBuffer.array());
        byteBuffer.clear();
    }

    /**
     * Writes an integer to file output stream. Can truncate a smaller integer to smaller byte size.
     *
     * @param bufferSize
     * 			The size of the byte array to write - may be smaller than the standard 4
     * @param dataToWrite
     *			The integer to write to file
     * @throws IOException
     * 			Thrown when attempt to write bytes to a file causes an exception
     */
    private void writeInt(int bufferSize, int dataToWrite, final OutputStream stream) throws IOException {
        int intSize = 4;
        ByteBuffer byteBuffer;
        byteBuffer = ByteBuffer.allocate(intSize);
        byteBuffer.putInt(dataToWrite);

        if (bufferSize < intSize) {
            // now we need to cut the data down to size. assuming big-endian data
            byte[] dataArray = Arrays.copyOfRange(byteBuffer.array(), intSize - bufferSize, intSize);
            writeByteArray(bufferSize, dataArray, stream);
            return;
        } // no case where bufferSize > 4 because it would have failed before this point. Java int has 4 byte max.

        stream.write(byteBuffer.array());
        byteBuffer.clear();
    }
}