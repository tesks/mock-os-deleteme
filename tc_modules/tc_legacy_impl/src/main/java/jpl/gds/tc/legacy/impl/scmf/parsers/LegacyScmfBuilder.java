/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.tc.legacy.impl.scmf.parsers;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.checksum.RotatedXorAlgorithm;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.IScmfInternalMessageFactory;
import jpl.gds.tc.api.IScmfSfduHeader;
import jpl.gds.tc.api.config.ScmfProperties;
import jpl.gds.tc.api.exception.ScmfParseException;
import jpl.gds.tc.api.exception.ScmfWrapUnwrapException;
import jpl.gds.tc.api.message.IScmfCommandMessage;
import jpl.gds.tc.api.scmf.IScmfBuilder;
import jpl.gds.tc.impl.scmf.Scmf;
import jpl.gds.tc.impl.scmf.ScmfCommandMessage;
import jpl.gds.tc.impl.scmf.ScmfInternalMessageFactory;
import jpl.gds.tc.impl.scmf.ScmfSfduHeader;
import org.springframework.context.ApplicationContext;

import java.io.*;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Pattern;

import static jpl.gds.tc.api.IScmfSfduHeader.MISSION_ID_LABEL;
import static jpl.gds.tc.api.IScmfSfduHeader.SPACECRAFT_NAME_LABEL;
import static jpl.gds.tc.api.scmf.IScmfSerializer.*;

/**
 * The LegacyScmfBuilder builds SCMFs utilizing the functionality that
 * was present in AMPCS before MPS was introduced.
 */
public class LegacyScmfBuilder implements IScmfBuilder {

    /** The byte length of an SFDU label */
    private static short            LABEL_BYTE_LENGTH            = 20;
    /** The text format used to interpret binary strings */
    public static String TEXT_BYTE_FORMAT          = "US-ASCII";
    // these are constants for the names of the fields in the actual document
    public static  String           DATA_SET_ID_LABEL            = "DATA_SET_ID";
    /** The data set ID is always the same for an SCMF */
    public static String           DATA_SET_ID                  = "SCMF";
    /** MPCS-6398 - 8/4/2014: Allow the \s character class in place of spaces according to the PVL standard */
    public static String           SFDU_DATA_REGEXP             = "[A-Z0-9_]{1,}[\\s]{0,}=[\\s]{0,}.{1,}";

    /** MPCS-6449 - 10/8/2014 : Using Java Patterns to handle SCMF header parsing now to
     * increase flexibility.
     */
    /** The pattern matching the end of the SFDU label line */
    public static Pattern SFDU_LABEL_DELIMITER = Pattern.compile("\r??\n");
    /** The pattern matching the delimiter of each PVL line */
    public static Pattern PVL_LINE_DELIMITER = Pattern.compile(";\r??\n");



    private final ScmfProperties scmfConfig;
    private IScmfInternalMessageFactory msgFactory;
    private final Tracer trace;

    private String filePath;
    private byte[] scmfData;


    private IScmf scmf;

    public LegacyScmfBuilder(final ApplicationContext appContext) {
        this(appContext.getBean(ScmfProperties.class), appContext.getBean(IScmfInternalMessageFactory.class), TraceManager.getTracer(appContext, Loggers.UPLINK));
    }

    public LegacyScmfBuilder(final ScmfProperties scmfConfig, final IScmfInternalMessageFactory msgFactory, final Tracer trace) {
        this.scmfConfig = scmfConfig;
        this.trace = trace;
        this.msgFactory = msgFactory;
    }


    @Override
    public IScmfBuilder setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public IScmfBuilder setScmfData(final byte[] data) {
        this.scmfData = data;
        return this;
    }

    @Override
    public IScmfBuilder setInternalMessageFactory(IScmfInternalMessageFactory scmfInternalMessageFactory) {
        this.msgFactory = scmfInternalMessageFactory;
        return this;
    }

    @Override
    public IScmfBuilder setMissionProperties(MissionProperties missionProperties) {
        return this;
    }

    @Override
    public IScmfBuilder setScmfProperties(ScmfProperties scmfProperties) {
        return this;
    }

    @Override
    public IScmf build() throws ScmfParseException {
        try {
            if (scmfData != null) {
                return build(new ByteArrayInputStream(scmfData));
            } else {
                IScmf tmp = build(new FileInputStream(filePath));
                tmp.setFilePath(filePath);
                return tmp;
            }
        } catch (RuntimeException | IOException | ScmfWrapUnwrapException e) {
            throw new ScmfParseException(e);
        }
    }

    public IScmf build(final InputStream in) throws IOException, ScmfWrapUnwrapException {
        String primarySfduLabel;
        String catalogHeaderSfduLabel;
        String dataHeaderSfduLabel;

        long sfduHeaderLength;
        long totalFileLength;
        long spacecraftMessageFileLength;

        scmf = new Scmf(msgFactory, scmfConfig);

        // read the primary SFDU label and its info
        primarySfduLabel = readSfduLabel(in);
        totalFileLength = getLengthFromSfduLabel(primarySfduLabel);

        // read the catalog header SFDU label and its info
        catalogHeaderSfduLabel = readSfduLabel(in);
        sfduHeaderLength = getLengthFromSfduLabel(catalogHeaderSfduLabel);

        // read in the SCMF header and parse it
        final byte[] sfduHeaderBytes = new byte[(int) sfduHeaderLength];
        final int bytesRead = in
                .read(sfduHeaderBytes, 0, (int) sfduHeaderLength);
        if (bytesRead != sfduHeaderLength) {
            throw new ScmfWrapUnwrapException("The SFDU header is labeled as "
                    + sfduHeaderLength + " bytes long, but only "
                    + bytesRead + " bytes could be read.");
        }

        scmf.setSfduHeader(parseSfduHeaderFromBytes(sfduHeaderBytes));

        // read the data header SFDU label
        dataHeaderSfduLabel = readSfduLabel(in);
        spacecraftMessageFileLength = getLengthFromSfduLabel(dataHeaderSfduLabel);

        if (totalFileLength != (2 * LABEL_BYTE_LENGTH
                + sfduHeaderLength + spacecraftMessageFileLength)) {
            throw new ScmfWrapUnwrapException(
                    "The total defined length of the entire SCMF in the primary SFDU label "
                            + "("
                            + totalFileLength
                            + " bytes) does not match the total of the bytes given by "
                            + "the catalog header SFDU label ("
                            + sfduHeaderLength
                            + " bytes) plus the bytes given by "
                            + "the data header SFDU label ("
                            + spacecraftMessageFileLength
                            + " bytes) (NOTE that there is a "
                            + (2 * LABEL_BYTE_LENGTH)
                            + " byte discrepancy between these two values to account for "
                            + "the lengths of the catalog header SFDU label and the data header SFDU label themselves).");
        }

        // read the rest of the input SCMF
        parseFromInputStream(in, (int) spacecraftMessageFileLength);

        return scmf;
    }

    /**
     * Parse an SFDU label from an input stream
     *
     * @param in The input stream to read the SFDU label from
     *
     * @return A string representing the SFDU label read from the stream
     *
     * @throws ScmfWrapUnwrapException
     * @throws IOException
     */
    protected String readSfduLabel(final InputStream in)
            throws ScmfWrapUnwrapException, IOException {
        final byte[] labelBytes = new byte[LABEL_BYTE_LENGTH];

        final int bytesRead = in
                .read(labelBytes, 0, LABEL_BYTE_LENGTH);
        if (bytesRead != LABEL_BYTE_LENGTH) {
            throw new ScmfWrapUnwrapException(
                    "The SFDU Label should be 20 bytes long, but the parser"
                            + " was unable to read 20 bytes");
        }

        String labelString = null;
        try {
            labelString = new String(labelBytes, TEXT_BYTE_FORMAT);
        } catch (final UnsupportedEncodingException e) {
            throw new ScmfWrapUnwrapException(
                    "The SFDU bytes could not be parsed into a valid "
                            + TEXT_BYTE_FORMAT + " text format", e);
        }

        return (labelString);
    }

    /**
     * Given an SFDU label, pull out the length stored within the label
     *
     * @param sfduLabel A standard SFDU label as a string
     *
     * @return The numeric portion of the SFDU label that gives the length of a
     *         chunk of data that the SFDU label is labeling
     */
    protected long getLengthFromSfduLabel(final String sfduLabel) {
        final String lengthString = sfduLabel.substring(12);
        final long length = Long.parseLong(lengthString);

        return (length);
    }

    private IScmfSfduHeader parseSfduHeaderFromBytes(final byte[] sfduHeaderBytes) throws ScmfWrapUnwrapException {
        IScmfSfduHeader header = new ScmfSfduHeader();

        String sfduHeader;
        try {
            sfduHeader = new String(sfduHeaderBytes, TEXT_BYTE_FORMAT);
        } catch (final UnsupportedEncodingException e) {
            throw new ScmfWrapUnwrapException(
                    "The SFDU header bytes could not be parsed into a "
                            + "valid " + TEXT_BYTE_FORMAT
                            + " text format.", e);
        }

        final Scanner sfduScanner = new Scanner(sfduHeader);
        // The newline that may separate the SFDU label with the SCMF header is passed in the byte array-
        // skip it
        try {
            sfduScanner.skip(SFDU_LABEL_DELIMITER);
        } catch (final NoSuchElementException e) {
            // Ignore
        }

        /** MPCS-6398 - 8/4/2014: Change the delimiter, since according to the PVL standard
         * a newline is valid in place of a space.  Use the exact SFDU SCMF header termination string
         */
        sfduScanner.useDelimiter(PVL_LINE_DELIMITER);
        while (sfduScanner.hasNext()) {
            // grab a line from the SFDU header
            final String line = sfduScanner.next();

            if (line.trim().length() != 0
                    && !line.matches(SFDU_DATA_REGEXP)) {
                /** MPCS-6398 - 8/4/2014: Close scanner to prevent resource leak */
                sfduScanner.close();
                throw new ScmfWrapUnwrapException(
                        "The SFDU data line \""
                                + line
                                + "\" does not match the regular expression \""
                                + SFDU_DATA_REGEXP
                                + "\" that defines what an SFDU data line should look like.");
            }

            // find the = character in the line
            final int equalSignIndex = line.indexOf('=');

            if (equalSignIndex != -1) {
                // get the label up to the part before the equal sign and strip
                // off whitespace
                final String label = line.substring(0, equalSignIndex).trim();

                // MPCS-6398 - 8/4/2014: ';' is part of the token delimiter, no need to trim
                // get the value as part after the = to end of token and
                // strip off whitespace
                final String value = line.substring(equalSignIndex + 1).trim();

                if (label.equals(DATA_SET_ID_LABEL)) {
                    if (!value.equals(DATA_SET_ID)) {
                        /** MPCS-6398 - 8/4/2014: Close scanner to prevent resource leak */
                        sfduScanner.close();
                        throw new ScmfWrapUnwrapException("The value for the \""
                                + DATA_SET_ID_LABEL
                                + "\" in an SCMF must be \"" + DATA_SET_ID
                                + "\", but the value in the input data was \""
                                + value + "\".");
                    }
                }

                if (label.equals(IScmfSfduHeader.FILE_NAME_LABEL)) {
                    // MPCS-6398 - 8/4/2014: Use setFileName method so the length is
                    // validated
                    scmf.setFileName(value);
                    header.setFileName(value);
                }

                if (label.equals(IScmfSfduHeader.MISSION_NAME_LABEL)) {
                    header.setMissionName(value);
                }
                if (label.equals(MISSION_ID_LABEL)) {
                    header.setMissionId(value);
                    scmf.setMissionId(Long.parseLong(value));
                }
                if (label.equals(SPACECRAFT_NAME_LABEL)) {
                    header.setSpacecraftName(value);
                }
                if (label.equals(IScmfSfduHeader.SPACECRAFT_ID_LABEL)) {
                    header.setSpacecraftId(value);
                    scmf.setSpacecraftId(Long.parseLong(value));
                }
                if (label.equals(IScmfSfduHeader.PRODUCT_CREATION_TIME_LABEL)) {
                    header.setProductCreationTime(value);
                }
                if (label.equals(IScmfSfduHeader.PRODUCT_VERSION_LABEL)) {
                    header.setProductVersion(value);
                }
            }
        }

        if (sfduScanner != null) {
            sfduScanner.close();
        }

        return header;
    }

    private void parseFromInputStream(final InputStream in, final int scmfLength)
            throws IOException, ScmfWrapUnwrapException {
        final byte[] scmfHeaderBytes = new byte[SCMF_HEADER_BYTE_LENGTH];

        final int bytesRead = in.read(scmfHeaderBytes, 0, scmfHeaderBytes.length);
        if (bytesRead != scmfHeaderBytes.length) {
            throw new IOException("Expected " + scmfHeaderBytes.length
                    + " of SCMF header, but could only read " + bytesRead
                    + " bytes.");
        }

        parseScmfHeader(scmfHeaderBytes, scmfLength);
        parseCommandMessageFile(in, scmfLength);

        if (!scmfConfig.isDisableChecksums()) {
            // TODO: calculate the checksum over the file and test it against
        }
    }

    /**
     * Parse an SCMF header from a byte array
     *
     * @param scmfHeaderBytes The byte array containing the SCMF header
     * @param scmfLength The length of the SCMF header to read in
     *
     * @throws ScmfWrapUnwrapException
     */
    protected void parseScmfHeader(final byte[] scmfHeaderBytes,
                                   final int scmfLength) throws ScmfWrapUnwrapException {
        IScmfSfduHeader sfduHeader = scmf.getSfduHeader();
        int offset = 0;
        try {
            // read the filename
            final byte[] filenameBytes = new byte[FILE_NAME_BYTE_LENGTH];
            System.arraycopy(scmfHeaderBytes, offset, filenameBytes, 0,
                    FILE_NAME_BYTE_LENGTH);
            offset += FILE_NAME_BYTE_LENGTH;
            final String filename = (new String(filenameBytes, TEXT_BYTE_FORMAT))
                    .trim();
            if (!sfduHeader.getFileName().startsWith(filename)) {
                trace.warn("The filename prefix \""
                        + filename
                        + "\" in the SCMF header "
                        + "does not match the beginning of the SCMF filename \""
                        + sfduHeader.getFileName()
                        + "\" in the SFDU header.");
            }
            scmf.setFileName(filename);

            // read the preparer
            final byte[] preparerBytes = new byte[PREPARER_BYTE_LENGTH];
            System.arraycopy(scmfHeaderBytes, offset, preparerBytes, 0,
                    PREPARER_BYTE_LENGTH);
            offset += PREPARER_BYTE_LENGTH;
            scmf.setPreparer(new String(preparerBytes, TEXT_BYTE_FORMAT));

            // read the file size
            final byte[] fileSizeBytes = new byte[FILE_SIZE_BYTE_LENGTH];
            System.arraycopy(scmfHeaderBytes, offset, fileSizeBytes, 0,
                    FILE_SIZE_BYTE_LENGTH);
            offset += FILE_SIZE_BYTE_LENGTH;
            final long fileSize = GDR.get_u32(fileSizeBytes, 0);
            if (fileSize != scmfLength) {
                trace.warn("The file size of "
                        + fileSize
                        + " bytes in the "
                        + "SCMF header does not match the size given in the preceding SFDU label.");
            }
            scmf.setFileByteSize(fileSize);

            // read the file header size
            final byte[] fileHeaderSizeBytes = new byte[FILE_HEADER_SIZE_BYTE_LENGTH];
            System.arraycopy(scmfHeaderBytes, offset, fileHeaderSizeBytes, 0,
                    FILE_HEADER_SIZE_BYTE_LENGTH);
            offset += FILE_HEADER_SIZE_BYTE_LENGTH;
            final long fileHeaderSize = GDR.get_u32(fileHeaderSizeBytes, 0);
            if (fileHeaderSize != SCMF_HEADER_BYTE_LENGTH) {
                trace.warn("The SCMF header size of "
                        + fileHeaderSize
                        + " bytes in the "
                        + "SCMF header values does not match the expected size of "
                        + SCMF_HEADER_BYTE_LENGTH + " bytes.");
            }

            // read the mission ID
            final byte[] missionIdBytes = new byte[MISSION_ID_BYTE_LENGTH];
            System.arraycopy(scmfHeaderBytes, offset, missionIdBytes, 0,
                    MISSION_ID_BYTE_LENGTH);
            offset += MISSION_ID_BYTE_LENGTH;
            /*
             * MPCS-7392 - 6/22/2015: SFDU header's mission ID value
             * is read as a string, so "-1" gets interpreted verbatim, as "-1".
             * The SCMF header's mission ID value was being parsed as an
             * unsigned int, which converted -1 to 65535. So we now read it as a
             * signed int to make it consistent.
             */
            final int missionId = GDR.get_i16(missionIdBytes, 0);
            scmf.setMissionId(missionId);
            sfduHeader.setMissionId(String.valueOf(missionId));

            // read the SCID
            final byte[] spacecraftIdBytes = new byte[SPACECRAFT_ID_BYTE_LENGTH];
            System.arraycopy(scmfHeaderBytes, offset, spacecraftIdBytes, 0,
                    SPACECRAFT_ID_BYTE_LENGTH);
            offset += SPACECRAFT_ID_BYTE_LENGTH;
            final int spacecraftId = GDR.get_u16(spacecraftIdBytes, 0);
            scmf.setSpacecraftId(spacecraftId);
            sfduHeader.setSpacecraftId(String.valueOf(spacecraftId));

            // read the reference #
            final byte[] referenceNumberBytes = new byte[REFERENCE_NUMBER_BYTE_LENGTH];
            System.arraycopy(scmfHeaderBytes, offset, referenceNumberBytes, 0,
                    REFERENCE_NUMBER_BYTE_LENGTH);
            offset += REFERENCE_NUMBER_BYTE_LENGTH;
            scmf.setReferenceNumber(GDR.get_u32(referenceNumberBytes, 0));

            // read the bit one radiation time
            final byte[] bitOneRadiationBytes = new byte[BIT_ONE_RADIATION_TIME_BYTE_LENGTH];
            System.arraycopy(scmfHeaderBytes, offset, bitOneRadiationBytes, 0,
                    BIT_ONE_RADIATION_TIME_BYTE_LENGTH);
            offset += BIT_ONE_RADIATION_TIME_BYTE_LENGTH;
            scmf.setBitOneRadiationTime(new String(bitOneRadiationBytes,
                    TEXT_BYTE_FORMAT));

            // read the bit rate
            final byte[] bitRateBytes = new byte[BIT_RATE_INDEX_BYTE_LENGTH];
            System.arraycopy(scmfHeaderBytes, offset, bitRateBytes, 0,
                    BIT_RATE_INDEX_BYTE_LENGTH);
            offset += BIT_RATE_INDEX_BYTE_LENGTH;
            scmf.setBitRateIndex((int) GDR.get_u32(bitRateBytes, 0));

            // read the comment field
            final byte[] commentFieldBytes = new byte[COMMENT_FIELD_BYTE_LENGTH];
            System.arraycopy(scmfHeaderBytes, offset, commentFieldBytes, 0,
                    COMMENT_FIELD_BYTE_LENGTH);
            offset += COMMENT_FIELD_BYTE_LENGTH;
            scmf.setCommentField(new String(commentFieldBytes, TEXT_BYTE_FORMAT));

            // read the creation time
            final byte[] creationTimeBytes = new byte[CREATION_TIME_BYTE_LENGTH];
            System.arraycopy(scmfHeaderBytes, offset, creationTimeBytes, 0,
                    CREATION_TIME_BYTE_LENGTH);
            offset += CREATION_TIME_BYTE_LENGTH;
            scmf.setCreationTime(new String(creationTimeBytes, TEXT_BYTE_FORMAT));

            // read the title
            final byte[] titleBytes = new byte[TITLE_BYTE_LENGTH];
            System.arraycopy(scmfHeaderBytes, offset, titleBytes, 0,
                    TITLE_BYTE_LENGTH);
            offset += TITLE_BYTE_LENGTH;
            scmf.setTitle(new String(titleBytes, TEXT_BYTE_FORMAT));

            // read the seqtran version
            final byte[] seqtranVersionBytes = new byte[SEQTRAN_VERSION_BYTE_LENGTH];
            System.arraycopy(scmfHeaderBytes, offset, seqtranVersionBytes, 0,
                    SEQTRAN_VERSION_BYTE_LENGTH);
            offset += SEQTRAN_VERSION_BYTE_LENGTH;
            scmf.setSeqtranVersion(new String(seqtranVersionBytes, TEXT_BYTE_FORMAT));

            // read the macro version
            final byte[] macroVersionBytes = new byte[MACRO_VERSION_BYTE_LENGTH];
            System.arraycopy(scmfHeaderBytes, offset, macroVersionBytes, 0,
                    MACRO_VERSION_BYTE_LENGTH);
            offset += MACRO_VERSION_BYTE_LENGTH;
            scmf.setMacroVersion(new String(macroVersionBytes, TEXT_BYTE_FORMAT));

            // read the file checksum
            final byte[] fileChecksumBytes = new byte[FILE_CHECKSUM_BYTE_LENGTH];
            System.arraycopy(scmfHeaderBytes, offset, fileChecksumBytes, 0,
                    FILE_CHECKSUM_BYTE_LENGTH);
            offset += FILE_CHECKSUM_BYTE_LENGTH;
            final int checksum = GDR.get_u16(fileChecksumBytes, 0);
            scmf.setFileChecksum(checksum);

            if (offset != scmfHeaderBytes.length) {
                throw new ScmfWrapUnwrapException(
                        "Incorrect number of bytes read from the SCMF header.");
            }
        } catch (final UnsupportedEncodingException e) {
            throw new ScmfWrapUnwrapException(
                    "The string fields in the SCMF header cannot be parsed using the "
                            + TEXT_BYTE_FORMAT + " encoding.", e);
        }
    }

    /**
     * Parse all the command messages from the SCMF
     *
     * @param in The stream to read the command messages from
     * @param scmfLength The length of the entire SCMF
     *
     * @throws IOException
     * @throws ScmfWrapUnwrapException
     */
    protected void parseCommandMessageFile(final InputStream in,
                                           final long scmfLength) throws IOException, ScmfWrapUnwrapException {
        final long commandMessageFileLength = scmfLength - SCMF_HEADER_BYTE_LENGTH;
        long totalBytesRead = 0;
        final long messageCount = 1;

        // loop through reading one command message at a time
        while (totalBytesRead < commandMessageFileLength) {
            int bytesRead = 0;
            long messageLength = 0;

            // read the message #
            final byte[] messageNumberBytes = new byte[MESSAGE_NUMBER_BYTE_LENGTH];
            bytesRead = in.read(messageNumberBytes, 0,
                    messageNumberBytes.length);
            if (bytesRead != messageNumberBytes.length) {
                throw new ScmfWrapUnwrapException("Expected "
                        + messageNumberBytes.length
                        + " bytes of SCMF file data, but could only read "
                        + bytesRead + " bytes.");
            }
            totalBytesRead += bytesRead;

            // read the message length
            final byte[] messageLengthBytes = new byte[MESSAGE_BIT_LENGTH_BYTE_LENGTH];
            bytesRead = in.read(messageLengthBytes, 0,
                    messageLengthBytes.length);
            if (bytesRead != messageLengthBytes.length) {
                throw new ScmfWrapUnwrapException("Expected "
                        + messageLengthBytes.length
                        + " bytes of SCMF file data, but could only read "
                        + bytesRead + " bytes.");
            }
            final long bitLength = GDR.get_u32(messageLengthBytes, 0);
            messageLength = bitLength / 8
                    + MESSAGE_HEADER_BYTE_LENGTH;
            if ((bitLength % 8) != 0) {
                messageLength += 1;
            }
            if (bitLength > MESSAGE_BIT_LENGTH_MAX_VALUE
                    || bitLength < MESSAGE_BIT_LENGTH_MIN_VALUE) {
                trace.error("The message length value of " + bitLength
                        + " bits for command message #" + messageCount
                        + " is outside the legal value range" + " of "
                        + MESSAGE_BIT_LENGTH_MIN_VALUE
                        + " to "
                        + MESSAGE_BIT_LENGTH_MAX_VALUE);
            }
            totalBytesRead += bytesRead;

            // read the message itself
            final byte[] messageBytes = new byte[(int) messageLength];
            System.arraycopy(messageNumberBytes, 0, messageBytes, 0,
                    messageNumberBytes.length);
            System.arraycopy(messageLengthBytes, 0, messageBytes,
                    messageNumberBytes.length, messageLengthBytes.length);
            final int remainingBytes = messageBytes.length
                    - messageNumberBytes.length - messageLengthBytes.length;
            bytesRead = in.read(messageBytes, messageNumberBytes.length
                    + messageLengthBytes.length, remainingBytes);
            if (bytesRead != remainingBytes) {
                throw new ScmfWrapUnwrapException("Expected " + remainingBytes
                        + " bytes of SCMF file data, but could only read "
                        + bytesRead + " bytes.");
            }
            totalBytesRead += bytesRead;

            scmf.addCommandMessage(parseCommandMessage(messageBytes, 0, messageBytes.length));
        }

        if (totalBytesRead != commandMessageFileLength) {
            trace.error("The number of bytes that were able to be read from the data portion of the SCMF was "
                    + totalBytesRead
                    + ", but the "
                    + "SCMF data portion length was "
                    + commandMessageFileLength
                    + " according to data in the file.");
        }
    }

    private IScmfCommandMessage parseCommandMessage(final byte[] messageBytes, final int inOffset, final int length) throws ScmfWrapUnwrapException {
        int offset = inOffset;
        int bytesRead = 0;

        IScmfCommandMessage cmdMsg = new ScmfCommandMessage();

        try {
            //read the message number
            byte[] messageNumberBytes = new byte[MESSAGE_NUMBER_BYTE_LENGTH];
            System.arraycopy(messageBytes, offset, messageNumberBytes, 0, messageNumberBytes.length);
            cmdMsg.setMessageNumber(GDR.get_u32(messageNumberBytes, 0));
            offset += messageNumberBytes.length;
            bytesRead += messageNumberBytes.length;

            //read the length of the message and validate it
            byte[] messageLengthBytes = new byte[MESSAGE_BIT_LENGTH_BYTE_LENGTH];
            System.arraycopy(messageBytes, offset, messageLengthBytes, 0, messageLengthBytes.length);
            long dataBitLength = GDR.get_u32(messageLengthBytes, 0);
            long messageLength = dataBitLength / 8 + MESSAGE_HEADER_BYTE_LENGTH;
            if ((dataBitLength % 8) != 0) {
                messageLength += 1;
            }
            if (dataBitLength > MESSAGE_BIT_LENGTH_MAX_VALUE ||
                    dataBitLength < MESSAGE_BIT_LENGTH_MIN_VALUE) {
                throw new ScmfWrapUnwrapException("The message length value of " + dataBitLength + " bits for command message #" + cmdMsg.getMessageNumber() + " is outside the legal value range" +
                        " of " + MESSAGE_BIT_LENGTH_MIN_VALUE + " to " + MESSAGE_BIT_LENGTH_MAX_VALUE);
            } else if (messageLength != length) {
                throw new ScmfWrapUnwrapException("Specified parse length of " + length + " bytes does not match the command message header length"
                        + " length value of " + messageLength + " bytes.");
            }

            offset += messageLengthBytes.length;
            bytesRead += messageLengthBytes.length;

            //read the transmission start time
            byte[] transmissionStartTimeBytes = new byte[TIME_BYTE_LENGTH];
            System.arraycopy(messageBytes, offset, transmissionStartTimeBytes, 0, transmissionStartTimeBytes.length);
            cmdMsg.setTransmissionStartTime(new String(transmissionStartTimeBytes, TEXT_BYTE_FORMAT));
            offset += transmissionStartTimeBytes.length;
            bytesRead += transmissionStartTimeBytes.length;

            //read the open window
            byte[] openWindowBytes = new byte[TIME_BYTE_LENGTH];
            System.arraycopy(messageBytes, offset, openWindowBytes, 0, openWindowBytes.length);
            cmdMsg.setOpenWindow(new String(openWindowBytes, TEXT_BYTE_FORMAT));
            offset += openWindowBytes.length;
            bytesRead += openWindowBytes.length;

            //read the close window
            byte[] closeWindowBytes = new byte[TIME_BYTE_LENGTH];
            System.arraycopy(messageBytes, offset, closeWindowBytes, 0, closeWindowBytes.length);
            cmdMsg.setCloseWindow(new String(closeWindowBytes, TEXT_BYTE_FORMAT));
            offset += closeWindowBytes.length;
            bytesRead += closeWindowBytes.length;

            //read the message comment
            byte[] messageCommentBytes = new byte[MESSAGE_COMMENT_BYTE_LENGTH];
            System.arraycopy(messageBytes, offset, messageCommentBytes, 0, messageCommentBytes.length);
            cmdMsg.setMessageComment(new String(messageCommentBytes, TEXT_BYTE_FORMAT));
            offset += messageCommentBytes.length;
            bytesRead += messageCommentBytes.length;

            //read the message checksum
            byte[] messageChecksumBytes = new byte[MESSAGE_CHECKSUM_BYTE_LENGTH];
            System.arraycopy(messageBytes, offset, messageChecksumBytes, 0, messageChecksumBytes.length);
            cmdMsg.setMessageChecksum(GDR.get_u16(messageChecksumBytes, 0));
            offset += messageChecksumBytes.length;
            bytesRead += messageChecksumBytes.length;

            byte[] data = new byte[messageBytes.length - bytesRead];
            System.arraycopy(messageBytes, offset, data, 0, data.length);
            bytesRead += data.length;

            if (bytesRead != length) {
                throw new ScmfWrapUnwrapException(bytesRead + " bytes were read from command message #" + cmdMsg.getMessageNumber() +
                        ", but the input message length was " + length);
            }

            cmdMsg.setData(data);

            //validate the checksum if configured to do so
            long calculatedChecksum = RotatedXorAlgorithm.calculate16BitChecksum(data);
            if (cmdMsg.getMessageChecksum() != calculatedChecksum && !scmfConfig.isDisableChecksums()) {
                throw new ScmfWrapUnwrapException("Message checksum failed for command message #" + cmdMsg.getMessageNumber() + ".  " +
                        "The message checksum in the file was 0x" + Integer.toHexString(cmdMsg.getMessageChecksum()) + ", but the calculated checksum was 0x" +
                        Integer.toHexString((int) calculatedChecksum));
            }
        } catch (UnsupportedEncodingException e) {
            throw new ScmfWrapUnwrapException("The string fields in the command message header cannot be parsed using the " +
                    TEXT_BYTE_FORMAT + " encoding.", e);
        }

        return cmdMsg;
    }
}
