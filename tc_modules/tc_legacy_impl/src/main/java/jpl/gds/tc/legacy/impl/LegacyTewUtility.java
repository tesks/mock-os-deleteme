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

package jpl.gds.tc.legacy.impl;

import jpl.gds.ccsds.api.config.CcsdsProperties;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.packet.PacketHeaderFactory;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.*;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.cltu.ICltuFactory;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.command.IFlightCommandTranslator;
import jpl.gds.tc.api.command.parser.UplinkInputParser;
import jpl.gds.tc.api.config.*;
import jpl.gds.tc.api.exception.*;
import jpl.gds.tc.api.packet.ITelecommandPacketFactory;
import jpl.gds.tc.api.plop.ICommandLoadBuilder;
import jpl.gds.tc.api.scmf.IScmfBuilder;
import jpl.gds.tc.impl.fileload.CommandFileLoad;
import jpl.gds.tc.impl.frame.TcTransferFrame;
import jpl.gds.tc.impl.plop.CommandLoadBuilder;
import jpl.gds.tc.legacy.impl.scmf.parsers.LegacyScmfBuilder;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A legacy utility class providing the connection to the legacy (standard AMPCS) command translation capabilities.
 *
 * MPCS-10745 - 07/01/19 - initial implementation of everything
 */
public class LegacyTewUtility implements ITewUtility {

    private final ApplicationContext        appContext;
    private final CommandFrameProperties    frameProps;
    private final ICommandObjectFactory     commandFactory;


    private final ITelecommandPacketFactory pktFactory;
    private final ICltuFactory              cltuFactory;
    private final ICommandLoadBuilder       commandLoadFactory = new CommandLoadBuilder();
    private final CcsdsProperties           ccsdsProps;
    private final IFlightCommandTranslator  commandTranslator;
    private final MissionProperties         missionProps;
    private final PlopProperties            plopProps;
    private final int                       scid;
    //    private final ITcTransferFrameFactory frameFactory;
    private final Tracer                    tracer;

    /**
     * Constructor
     *
     * @param appContext spring application context
     */
    public LegacyTewUtility(final ApplicationContext appContext) {
        this.appContext = appContext;
        this.frameProps = appContext.getBean(CommandFrameProperties.class);
        this.commandFactory = appContext.getBean(ICommandObjectFactory.class);
        this.pktFactory = appContext.getBean(ITelecommandPacketFactory.class);
        this.cltuFactory = appContext.getBean(ICltuFactory.class);
        this.ccsdsProps = appContext.getBean(CcsdsProperties.class);
        this.commandTranslator = appContext.getBean(IFlightCommandTranslator.class, appContext);
        this.missionProps = appContext.getBean(MissionProperties.class);
        this.plopProps = appContext.getBean(PlopProperties.class);
        this.scid = appContext.getBean(IContextIdentification.class).getSpacecraftId();
//        this.frameFactory = appContext.getBean(ITcTransferFrameFactory.class);
        this.tracer = TraceManager.getTracer(appContext, Loggers.UPLINK);
    }

    @Override
    public byte[] validateAndTranslateCommand(final IFlightCommand command) throws
            CommandParseException,
            BlockException {

        if (command == null) {
            throw new IllegalArgumentException("Supplied command is null");
        }

        //validate the  entire command
        for (int i = 0; i < command.getArgumentCount(); i++) {

            UplinkInputParser.validTransmittableCheck(appContext, command, i, command.getArgumentValue(i));

            if (command.getArgumentDefinition(i).getType().isRepeat()) {
                for (int j = 0; j < command.getArgumentCount(i); j++) {
                    UplinkInputParser
                            .validTransmittableCheck(appContext, command, i, j, command.getArgumentValue(i, j));
                }
            }
        }

        //it's valid - CommandParseException was thrown if it's not

        return BinOctHexUtility.toBytesFromBin(commandTranslator.setCommand(command).toBitString());

    }

    @Override
    public ITelecommandPacket wrapBytesToPacket(final byte[] commandBytes, final int apid) {
        return pktFactory.buildPacketFromPdu(commandBytes, apid);
    }

    @Override
    public ITcTransferFrame wrapCommandToFrame(IFlightCommand command) throws FrameWrapUnwrapException {
        throw new UnsupportedOperationException("unsupported at this time");
    }

    @Override
    public List<ITcTransferFrame> wrapCommandToFrames(IFlightCommand command) throws FrameWrapUnwrapException {
        throw new UnsupportedOperationException("unsupported at this time");
    }

    /**
     * Convert a byte array into an ITcTransferFrame
     *
     * @param data the frame data
     * @param vcid the VCID of the frame to be created
     * @return an ITcTransferFrame object
     */
    public ITcTransferFrame wrapBytesToFrame(final byte[] data, final int vcid) throws FrameWrapUnwrapException {
        return wrapBytesToFrame(data, vcid, -1);
    }

    @Override
    public ITcTransferFrame wrapBytesToFrame(byte[] bytes, int vcid, int fsn)
            throws FrameWrapUnwrapException {
        throw new UnsupportedOperationException("unsupported at this time");
    }

    @Override
    public ICltu wrapBytesToCltu(final byte[] commandBytes, final int vcid) throws CltuEndecException {

        final List<ITcTransferFrame> frames = new ArrayList<>(1);
        try {
            frames.add(wrapBytesToFrame(commandBytes, vcid));
        } catch (final FrameWrapUnwrapException e) {
            throw new CltuEndecException(e);
        }

        final List<ICltu> cltus = cltuFactory.createCltusFromFrames(frames);

        return cltus.get(0);
    }

    @Override
    public ICltu wrapFrameToCltu(ITcTransferFrame frame) throws CltuEndecException {
        return null;
    }

    @Override
    public ICltu wrapBytesToCltu(final byte[] commandBytes, final ExecutionStringType esType,
                                 final VirtualChannelType vcType) throws
            CltuEndecException {
        return wrapBytesToCltu(commandBytes, getVcid(esType, vcType));
    }

    @Override
    public List<ICommandFileLoad> wrapFileToFileLoads(final IFileLoadInfo info) throws FileLoadParseException {
        return UplinkInputParser.createFileLoadsFromInfo(appContext, info);
    }

    @Override
    public List<ICltu> createPlopCltus(final List<ICltu> cltus) {
        commandLoadFactory.clear();
        commandLoadFactory.addCltus(cltus);

        final List<ICltu> plopCltus = commandLoadFactory.getPlopCltus(plopProps);

        commandLoadFactory.clear();

        return plopCltus;
    }

    @Override
    public IScmf reverseScmf(final byte[] scmf) throws ScmfWrapUnwrapException, IOException, ScmfParseException {
        LegacyScmfBuilder legacyBldr = new LegacyScmfBuilder(appContext);

        return legacyBldr.setScmfData(scmf).build();
    }

    @Override
    public IScmf reverseScmf(final String scmfFilePath) throws ScmfWrapUnwrapException, IOException, ScmfParseException {
        if (scmfFilePath == null || scmfFilePath.isEmpty()) {
            throw new IllegalArgumentException("The supplied filepath cannot be empty");
        }

        final File scmfFile = new File(scmfFilePath);

        if (!scmfFile.exists() || scmfFile.isDirectory()) {
            throw new IllegalArgumentException("The supplied filepath must point to a valid SCMF file");
        }

        if (!scmfFile.canRead()) {
            throw new IOException("Cannot read for the supplied SCMF file");
        }

        IScmfBuilder legacyBldr = new LegacyScmfBuilder(appContext);

        return legacyBldr.setFilePath(scmfFilePath).build();
    }

    @Override
    public List<ITcTransferFrame> unwrapCltuToFrames(final ICltu cltu) {
        return cltu.getFrames();
    }

    @Override
    public List<ITelecommandPacket> unwrapFrameToPackets(final ITcTransferFrame frame) {
        final byte[]                   pktBytes   = frame.getData();
        byte[]                         pktData;
        final List<ITelecommandPacket> pkts       = new ArrayList<>();
        int                            dataOffset = 0;
        int                            dataLen;

        ISpacePacketHeader pktHdr;
        while (dataOffset < pktBytes.length) {
            pktHdr = PacketHeaderFactory.create(ccsdsProps.getPacketHeaderFormat());

            dataOffset = pktHdr.setPrimaryValuesFromBytes(pktBytes, 0);
            dataLen = pktHdr.getPacketDataLength();
            pktData = Arrays.copyOfRange(pktBytes, dataOffset, dataOffset + dataLen);

            dataOffset += dataLen;

            pkts.add(pktFactory.buildPacketFromHeader(pktHdr, pktData));
        }

        return pkts;
    }

    @Override
    public IFlightCommand reverseFrameToCommand(final ITcTransferFrame frame) throws UnblockException {
        return commandFactory.getCommandObjectFromBits(BinOctHexUtility.toBinFromBytes(frame.getData()), 0);
    }

    @Override
    public IFlightCommand reversePacketToCommand(final ITelecommandPacket packet) throws UnblockException {
        final byte[] pktBytes = packet.getBytes();
        final int    hdrLen   = packet.getHeaderBytes().length;
        final byte[] cmdData  = Arrays.copyOfRange(pktBytes, hdrLen, pktBytes.length);

        return commandFactory.getCommandObjectFromBits(BinOctHexUtility.toBinFromBytes(cmdData), 0);
    }

    //    @Override
    public List<ITcTransferFrame> wrapFileLoadToFrames(final ICommandFileLoad fileLoad) {
        final CommandFrameProperties frameConfig            = appContext.getBean(CommandFrameProperties.class);
        final VirtualChannelType     vct                    = VirtualChannelType.FILE_LOAD;
        final int                    maxFrameDataByteLength = frameConfig.getMaxFrameDataLength(vct);
        final List<ITcTransferFrame> frames                 = new ArrayList<>(
                fileLoad.getFileByteLength() / maxFrameDataByteLength + 1);
        int                          offset                 = 0;

        //loop through the file load bytes until we get all of them into frames
        final byte[] loadBytes = fileLoad.getFileLoadBytes();
        while (offset < loadBytes.length) {
            //allocate the proper size buffer...
            //see if we have enough bytes left to fill an entire frame
            final byte[] frameDataBytes;
            if ((loadBytes.length - offset) >= maxFrameDataByteLength) {
                frameDataBytes = new byte[maxFrameDataByteLength];
            } else {
                frameDataBytes = new byte[loadBytes.length - offset];
            }

            final ITcTransferFrame frame = createDefaultFrame();

            //get the frame data for the current frame
            System.arraycopy(loadBytes, offset, frameDataBytes, 0, frameDataBytes.length);
            offset += frameDataBytes.length;

            //build the frame header
            frame.setVirtualChannelNumber(frameConfig.getVirtualChannelNumber(vct));
            frame.setHasFecf(frameConfig.hasFecf(vct));
            frame.setVersionNumber(frameConfig.getVersionNumber());
            frame.setExecutionString(frameConfig.getStringIdVcidValue());

            //build the frame
            frame.setData(frameDataBytes);

            frames.add(frame);
        }

        return (frames);
    }

    @Override
    public ICommandFileLoad reverseFramesToFileLoad(final List<ITcTransferFrame> frames) {
        if (frames == null) {
            throw new IllegalArgumentException("supplied frame list cannot be null");
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (final ITcTransferFrame frame : frames) {
            try {
                baos.write(frame.getData());
            } catch (final IOException e) {
                tracer.warn("Error encountered while reading frame data. File load may not be complete");
            }
        }

        return new CommandFileLoad(appContext, baos.toByteArray(), 0);
    }

    @Override
    public ICommandFileLoad reversePacketsToFileLoad(final List<ITelecommandPacket> packets) {
        if (packets == null) {
            throw new IllegalArgumentException("supplied packet list cannot be null");
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (final ITelecommandPacket packet : packets) {
            try {
                final byte[] pktBytes = Arrays
                        .copyOfRange(packet.getBytes(), packet.getHeaderBytes().length, packet.getBytes().length);
                baos.write(pktBytes);
            } catch (final IOException e) {
                tracer.warn("Error encountered while reading frame data. File load may not be complete");
            }
        }

        return new CommandFileLoad(appContext, baos.toByteArray(), 0);
    }

    @Override
    public ITelecommandPacket rewrapPacket(final ITelecommandPacket packet) {
        final byte[] pktHdrBytes  = packet.getHeaderBytes();
        final byte[] fullPktBytes = packet.getBytes();
        final byte[] pktDataBytes = Arrays.copyOfRange(fullPktBytes, pktHdrBytes.length, fullPktBytes.length);

        final ISpacePacketHeader pktHdr = PacketHeaderFactory.create(ccsdsProps.getPacketHeaderFormat());
        pktHdr.setHeaderValuesFromBytes(pktHdrBytes, 0);

        return pktFactory.buildPacketFromHeader(pktHdr, pktDataBytes);
    }

    @Override
    public ICltu rewrapCltu(final ICltu cltu) throws CltuEndecException {
        return cltuFactory.parseCltusFromBytes(cltu.getBytes()).get(0);
    }

    /**
     * Convert execution string and virtual channel type to an integer VCID. The returned integer is produced from a
     * 3-bit execution string and a 3-bit virtual channel number concatenated together to form the 6-bit VCID for a
     * telecommand frame header.
     *
     * @param esType execution string type
     * @param vcType virtual channel type
     * @return virtual channel id, composed of execution string and virtual channel number
     */
    public int getVcid(final ExecutionStringType esType, final VirtualChannelType vcType) {
        final byte esByte = esType.getVcidValue(frameProps);
        final byte vcByte = frameProps.getVirtualChannelNumber(vcType);
        return (byte) (((esByte & 0x07) << 3) | (vcByte & 0x07));
    }

    private ITcTransferFrame createDefaultFrame() {

        final CommandFrameProperties frameConfig = appContext.getBean(CommandFrameProperties.class);
        final ITcTransferFrame       frame       = new TcTransferFrame(frameConfig);

        frame.setVersionNumber(frameConfig.getVersionNumber());
        frame.setSpacecraftId(scid);

        frame.setExecutionString(frameConfig.getStringIdVcidValue());
        frame.setVirtualChannelNumber(frameConfig.getVirtualChannelNumber(VirtualChannelType.DELIMITER));

        return frame;
    }

}
