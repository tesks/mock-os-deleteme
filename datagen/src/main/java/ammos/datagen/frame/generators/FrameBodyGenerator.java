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
package ammos.datagen.frame.generators;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import ammos.datagen.frame.generators.seeds.FrameBodyGeneratorSeed;
import ammos.datagen.generators.ISeededGenerator;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.seeds.InvalidSeedDataException;
import ammos.datagen.generators.util.GeneratorStatistics;
import ammos.datagen.generators.util.TruthFile;
import jpl.gds.ccsds.api.packet.IPacketFormatDefinition;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.packet.PacketHeaderFactory;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderExtractor;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderLookup;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.shared.checksum.CcsdsCrc16ChecksumAdaptor;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.types.Triplet;

/**
 * This is the data generator for frame body. It creates the whole frame.
 * It must be supplied a seed object that tells it which frames to generate.
 * 
 *
 * MPCS-12212 Added support for packets spanning frames
 */
public class FrameBodyGenerator implements ISeededGenerator {
    // Fill PKT Header (6 bytes) + Fill PKT Data (1 bytes) = 7 bytes
    private static final int FILL_PACKET_LENGTH = 7;

    //frame / packet header length
    private static final int HEADER_LENGTH = 6;

    //CCSDS AOS spec (4.1.2.4.2) says this should be the max value
    private static final int VCFC_MAX = 16777215;

    // CCSDS AOS spec (4.1.4.2.3.4) MPDU header pointer will be set to 00000111 11111111
    // when no packet header starts in this frame
    private static final int MPDU_POINTER_CONT = 0x7FF;

    //fill packet byte content
    private static final byte FILL_PACKET_DATA = (byte) 0xFF;

    private static final byte[] mpduAosTfStaticHeader = new byte[HEADER_LENGTH];

    private FrameBodyGeneratorSeed             seedData;
    // values 0 - VCFC_MAX, rolls back to start value when reaching max
    private int                                vcfc;
    private int                                vcid;

    private final ISecondaryPacketHeaderLookup secPacketHeaderLookup;
    private final Tracer           statusLogger;
    private TruthFile              truthWriter;
    private final GeneratorStatistics    stats;
    private String                 frameType;
    private FileInputStream        fileInputStream;

    //flag in FrameRunConfig that controls whether packets can span frames
    private boolean packetSpanFrames;

    // only set if the current packet does not fit in the frame
    private Triplet<ISpacePacketHeader, ISclk, byte[]> longPacket = null;
    private int longPacketPos = 0;
    private boolean inLongPacket = false;

    //statistics
    private int packetsNo;
    private int longPacketsNo;

    /**
     * Constructor
     * 
     * @param inputFile
     *            Packet file to read
     * 
     * @param stats
     *            Generator Statistics object to write data to
     */
    public FrameBodyGenerator(final File inputFile, final GeneratorStatistics stats) {
        this.stats = stats;
        statusLogger = TraceManager.getTracer(Loggers.DATAGEN);
        secPacketHeaderLookup = SpringContextFactory.getSpringContext(true).getBean(ISecondaryPacketHeaderLookup.class);

        // open input file
        try {
            this.fileInputStream = new FileInputStream(inputFile.getAbsolutePath());
        }
        catch (final IOException e) {
            statusLogger.error("Could not open input file");
        }

	}

    /**
     * Constructor that sets the truth file writer
     * 
     * @param truthWriter
     *            TruthFile object for writing truth data to
     * @param inputFile
     *            Packet file to read
     * @param stats
     *            Generator Statistics object to write data to
     * 
     */
    public FrameBodyGenerator(final File inputFile, final GeneratorStatistics stats,
            final TruthFile truthWriter) {
        this(inputFile, stats);
        this.truthWriter = truthWriter;

	}

	@Override
	public void setSeedData(final ISeedData seed) throws InvalidSeedDataException, IllegalArgumentException {
		if (!(seed instanceof FrameBodyGeneratorSeed)) { 
            throw new IllegalArgumentException("Seed must be of type FrameBodyGeneratorSeed");
		}
		reset();
        seedData = (FrameBodyGeneratorSeed) seed;
        this.frameType = seedData.getFrameType();
        vcid = seedData.getVcid();
        vcfc = seedData.getStartVcfc();
        packetSpanFrames = seedData.isPacketSpanFrames();
        createStaticHeader();
	}


	@Override
	public void reset() {
        seedData = null;
        vcfc = 0;
	}

	@Override
    public Object getNext() {
        if (seedData == null) {
            throw new IllegalStateException("Frame body generator is unseeded");
        }
        return getFrameBody();
	}


	@Override
	public Object getRandom() {
        // not supported
        throw new UnsupportedOperationException();
	}


    /**
     * Actual return type is Triplet<byte[], byte[], byte[]> containing frame data
     * {@inheritDoc}
     */
	@Override
	public Object get() {
        if (seedData == null) {
            throw new IllegalStateException("Frame body generator is unseeded");
        }
        return getNext();
	}
	
    /**
     * Writes a line of test to the truth file. Does nothing if no truth file
     * writer has been set.
     * 
     * @param truth
     *            the truth data to write
     */
    private void writeTruthLine(final String truth) {
        if (this.truthWriter != null) {
            this.truthWriter.writeLine(truth);
        }
    }

    /**
     * Writes the given Frame and its metadata to the truth file.
     * 
     * @param length
     *            Frame length
     */
    private void writeFrameToTruth(final int length) {
        // format: FrameType, length, VCFC, VCID
        final StringBuilder builder = new StringBuilder();
        builder.append("Frame: ");
        builder.append(frameType);
        builder.append(",");
        builder.append(length);
        builder.append(",");
        builder.append(vcfc);
        builder.append(",");
        builder.append(vcid);
        writeTruthLine(builder.toString());
    }

    /**
     * Closes the truth file.
     */
    public void closeInputFile() {
        try {
            if (this.fileInputStream != null) {
                this.fileInputStream.close();
                this.fileInputStream = null;
            }
        }
        catch (final IOException e) {
            this.statusLogger.error("IO Error closing the input file", e);
        }
        this.statusLogger.info("Packets: " + packetsNo);
        this.statusLogger.info("Packets spanning frames: " + longPacketsNo);
        this.statusLogger.info("Frames: " + vcfc);
    }

    /**
     * Generates a binary Frame body and writes to truth file
     * 
     * @return Triplet<asm, frameArray, encoding> object with the Frame data,
     *         or null if there was a problem or if there is no more data
     */
    private Triplet<byte[], byte[], byte[]> getFrameBody() {
        final ITransferFrameDefinition frameDef = seedData.getFrameDict().findFrameDefinition(frameType);
        final byte[] asm = frameDef.getASM();
        byte[] mpduHeader = new byte[] { 0, 0 };
        if(longPacket != null) {
            //first header pointer - continuation packet
            GDR.set_u16(mpduHeader, 0, MPDU_POINTER_CONT);
        }

        final byte[] encoding = new byte[frameDef.getEncodingSizeBytes()];
        final byte[] frameArray = new byte[frameDef.getCADUSizeBytes() - asm.length - encoding.length];
        int currentPos = 0;
        
        // roll back to starting value
        if (vcfc == VCFC_MAX) {
            vcfc = seedData.getStartVcfc();
        }
        GDR.set_u24(mpduAosTfStaticHeader, 2, vcfc++); // Increment VCFC in the TF Primary Header

        Triplet<ISpacePacketHeader, ISclk, byte[]> pkt = null;

        //existing packet that spans more frames
        if (longPacket != null) {
            pkt = longPacket;
            //increment once per long packet
            if(!inLongPacket) {
                longPacketsNo++;
                inLongPacket = true;
            }
        }
        //new packet
        else {
            // reads a RAW_PKT from the file input stream
            try {
                if (fileInputStream.available() != 0) { // check for end of input stream
                    pkt = readPacket(fileInputStream);
                }
                else {
                    return null;
                }
            }
            catch (final IOException e) {
                statusLogger.error("Error reading packet from input file " + e.getMessage());
                return null;
            }
        }

        //write frame static header
        System.arraycopy(mpduAosTfStaticHeader, 0, frameArray, currentPos, mpduAosTfStaticHeader.length);
        currentPos += mpduAosTfStaticHeader.length;

        //write frame MPDU header
        System.arraycopy(mpduHeader, 0, frameArray, currentPos, mpduHeader.length);
        currentPos += mpduHeader.length;

        //MPDU data

        //if new packet, copy header
        if(longPacket == null) {
            System.arraycopy(pkt.getOne().getBytes(), 0, frameArray, currentPos, pkt.getOne().getBytes().length);
            currentPos += pkt.getOne().getBytes().length;
        }

        boolean tooLarge = pkt.getThree().length - longPacketPos > frameArray.length - currentPos;
        int lenToCopy = 0;

        //packet too long to fit in one frame, only copy what fits in current frame
        if (tooLarge) {
            if(!packetSpanFrames){
                statusLogger.error("The parsed RAW_PKT input is too large to fit in this frame " +
                                           "and PacketSpanFrames is disabled in frame run config" + "\n"
                                           + "Remaining frame space available: " + (frameArray.length - currentPos) + "\n"
                                           + "RAW_PKT data buffer size: " + pkt.getThree().length);
                return null;
            }

            lenToCopy = frameArray.length - currentPos;
            System.arraycopy(pkt.getThree(), longPacketPos, frameArray, currentPos, lenToCopy);

            currentPos += lenToCopy;
        }
        else{
            lenToCopy = pkt.getThree().length - longPacketPos;

            //copy remaining data
            System.arraycopy(pkt.getThree(), longPacketPos, frameArray, currentPos, lenToCopy);
            currentPos += lenToCopy;

            int bytesLeft = frameDef.getDataAreaSizeBytes() - lenToCopy - mpduHeader.length;
            if(longPacket == null) {
                bytesLeft -= pkt.getOne().getPrimaryHeaderLength();
            }

            // Make sure enough space left in TF DATA field for fill packet
            if (bytesLeft >= FILL_PACKET_LENGTH) {
                //create Fill Packet
                bytesLeft -= HEADER_LENGTH;
                final byte[] fillPacket = createFillPacket(bytesLeft);

                System.arraycopy(fillPacket, 0, frameArray, currentPos, fillPacket.length);
                currentPos += fillPacket.length;

            }
            else {
                statusLogger.info("Not enough space for fill packet at frame no " , vcfc, ", bytes left ",  bytesLeft);
                return null;
            }
        }

        //save for later processing
        if(tooLarge){
            longPacket = pkt;
            longPacketPos += lenToCopy;
        }
        else {
            longPacket = null;
            longPacketPos = 0;
            inLongPacket = false;
            packetsNo++;
        }

        if (frameDef.hasFrameErrorControl()) {
            final CcsdsCrc16ChecksumAdaptor calculator = new CcsdsCrc16ChecksumAdaptor();
            final long checksum = calculator.calculateChecksum(frameArray, 0, currentPos);
            final byte[] crc = new byte[] { (byte) (checksum >>> 8), (byte) checksum };

            System.arraycopy(crc, 0, frameArray, currentPos, crc.length);
        }

        if (longPacket == null && this.truthWriter != null) {
            truthWriter.writePacket(pkt.getOne(), pkt.getTwo());
        }

        // update truth file with frame data
        final int frameLength = asm.length + frameArray.length + encoding.length;
        writeFrameToTruth(frameLength);
        
        // update packet statistics
        if(longPacket == null) {
            final int packetSize = pkt.getOne().getBytes().length + pkt.getTwo().getBytes().length + lenToCopy;
            stats.updatePacketStatistics(packetSize, pkt.getOne().getApid(), pkt.getTwo(), System.currentTimeMillis());
        }

        return new Triplet<>(asm, frameArray, encoding);
    }

    /**
     * Create fill packet
     * @param size Data size
     * @return Fill packet bytes, length size + FILL_PACKET_LENGTH
     */
    private byte[] createFillPacket(int size){
        if(size < 0){
            statusLogger.error("Fill packet - size must not be negative");
            return new byte[]{0};
        }

        //create fill packet
        final ISpacePacketHeader fillHeader = (ISpacePacketHeader) seedData.getFillPacketGenerator().getNext();
        //no secondary header
        fillHeader.setSecondaryHeaderFlag((byte) 0);
        fillHeader.setPacketDataLength(size + 1);

        final byte[] headerBytes = fillHeader.getBytes();
        final byte[] packetBytes = new byte[size + 1];
        Arrays.fill(packetBytes, FILL_PACKET_DATA);

        byte[] fillPacket = new byte[headerBytes.length + packetBytes.length];
        System.arraycopy(headerBytes, 0, fillPacket, 0, headerBytes.length);
        System.arraycopy(packetBytes, 0, fillPacket, headerBytes.length, packetBytes.length);

        return fillPacket;
    }

    /**
     * Initializes a 6 byte Transfer Frame header using version '01' and utilizing a single VCID
     * Needs seed data to be initialized
     */
    private void createStaticHeader() {
        final int scid = seedData.getScid();

        // First two bytes are VERSION, SCID, and VCID
        // For now, VCID expected to remain same for all frames
        mpduAosTfStaticHeader[0] = 0b01; // this is the AOS TF version
        mpduAosTfStaticHeader[0] <<= 6;
        mpduAosTfStaticHeader[0] |= (byte) (scid >> 2);
        mpduAosTfStaticHeader[1] |= (byte) (scid << 6);
        mpduAosTfStaticHeader[1] |= (byte) vcid;

        GDR.set_u24(mpduAosTfStaticHeader, 2, vcfc); // these 3 bytes are incremented every iteration
    }

    /**
     * Reads and extracts a RAW Packet from a FileInputStream
     * 
     * @param stream
     *            The FileInputStream
     * @return A Triplet containing the RAW_PKT Header, the SCLK, and the RAW_PKT data
     * @throws IOException
     */
    private Triplet<ISpacePacketHeader, ISclk, byte[]> readPacket(final FileInputStream stream) throws IOException {
        try {
            final ISpacePacketHeader packetHeader = PacketHeaderFactory.create(IPacketFormatDefinition.TypeName.CCSDS);
            final byte[] packetHeaderBuffer = new byte[packetHeader.getPrimaryHeaderLength()];

            int bytesRead = stream.read(packetHeaderBuffer, 0, packetHeaderBuffer.length);

            if (bytesRead == -1) {
                return null;
            }

            if (bytesRead != packetHeaderBuffer.length) {
                throw new IOException("Ran out of bytes in packet file trying to read packet header");
            }
            packetHeader.setPrimaryValuesFromBytes(packetHeaderBuffer, 0);

            final byte[] dataBuffer = new byte[packetHeader.getPacketDataLength() + 1];
            bytesRead = stream.read(dataBuffer, 0, dataBuffer.length);

            if (bytesRead != dataBuffer.length) {
                throw new IOException("Ran out of bytes in packet file when trying to read packet data");
            }

            final ISecondaryPacketHeaderExtractor secHdrExtractor = secPacketHeaderLookup.lookupExtractor(packetHeader);
            if (!secHdrExtractor.hasEnoughBytes(dataBuffer, 0)) {
                throw new IOException("Ran out of bytes in packet file when trying to read secondary packet header");
            }

            final ISclk sclk = secHdrExtractor.extract(dataBuffer, packetHeader.getPrimaryHeaderLength()).getSclk();

            return new Triplet<>(packetHeader, sclk, dataBuffer);

        }
        catch (final Exception e) {
            throw new IOException("Encountered exception when parsing Packet File", e);
        }
    }
}
