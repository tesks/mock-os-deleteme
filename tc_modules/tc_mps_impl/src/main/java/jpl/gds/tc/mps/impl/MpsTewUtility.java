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

package jpl.gds.tc.mps.impl;

import gov.nasa.jpl.tcsession.TcSession;
import gov.nasa.jpl.uplinkutils.UplinkUtilsConstants;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.command.ICommandDefinition;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.types.Pair;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.ICommandFileLoad;
import jpl.gds.tc.api.IFileLoadInfo;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.IScmfInternalMessageFactory;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.ITelecommandPacket;
import jpl.gds.tc.api.ITewUtility;
import jpl.gds.tc.api.TcApiBeans;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.config.CltuProperties;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.ExecutionStringType;
import jpl.gds.tc.api.config.ScmfProperties;
import jpl.gds.tc.api.config.VirtualChannelType;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.CommandFileParseException;
import jpl.gds.tc.api.exception.CommandParseException;
import jpl.gds.tc.api.exception.FrameWrapUnwrapException;
import jpl.gds.tc.api.exception.ScmfParseException;
import jpl.gds.tc.api.exception.ScmfWrapUnwrapException;
import jpl.gds.tc.api.frame.FrameErrorControlFieldAlgorithm;
import jpl.gds.tc.api.frame.ITcTransferFrameSerializer;
import jpl.gds.tc.mps.impl.cltu.parsers.IMpsCltuParser;
import jpl.gds.tc.mps.impl.ctt.CommandTranslationTable;
import jpl.gds.tc.mps.impl.frame.MpsTcTransferFramesBuilder;
import jpl.gds.tc.mps.impl.frame.parsers.MpsTcTransferFrameBuilder;
import jpl.gds.tc.mps.impl.frame.parsers.MpsTcTransferFrameParser;
import jpl.gds.tc.mps.impl.scmf.parsers.MpsScmfBuilder;
import jpl.gds.tc.mps.impl.session.MpsSession;
import jpl.gds.tc.mps.impl.session.TranslatingMpsSession;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static jpl.gds.tc.api.ITcTransferFrame.BITMASK_EXECUTION_STRING;
import static jpl.gds.tc.api.ITcTransferFrame.EXECUTION_STRING_BIT_OFFSET;

/**
 * An TEW Utility class utilizing the JNI MPSA CTS libraries.
 *
 */
public class MpsTewUtility implements ITewUtility {

    private final Tracer                      tracer;
    private final CommandFrameProperties      frameProperties;
    private final MissionProperties           missionProperties;
    private final ScmfProperties              scmfProperties;
    private final int                         scid;
    private       CommandTranslationTable     ctt = null;
    private final IMpsCltuParser              cltuParser;
    private final ITcTransferFrameSerializer  tcSerializer;
    private final IScmfInternalMessageFactory scmfInternalMessageFactory;
    private final String                      commandDictPath;
    private final String                      sclkscetPath;
    private final CommandProperties           cmdProps;
    private final CltuProperties              cltuProperties;

    /**
     * Private constructor, use the Builder object to create instances
     *
     * @param builder MPS TEW Utility builder
     * @throws CommandFileParseException error parsing a command dictionary
     */
    private MpsTewUtility(final Builder builder) {
        this.tracer = builder.tracer;
        this.frameProperties = builder.cmdFrameProps;
        this.missionProperties = builder.missionProps;
        this.scid = builder.scid;
        this.cltuParser = builder.cltuParser;
        this.scmfInternalMessageFactory = builder.scmfInternalMessageFactory;
        this.scmfProperties = builder.scmfProperties;
        this.tcSerializer = builder.tcSerializer;

        this.commandDictPath = builder.commandDictPath;
        this.sclkscetPath = builder.sclkscetPath;
        this.cmdProps = builder.cmdProps;
        this.cltuProperties = builder.cltuProperties;
    }

    public static class Builder {

        private final int                         scid;
        private final String                      commandDictPath;
        private final String                      sclkscetPath;
        private       IScmfInternalMessageFactory scmfInternalMessageFactory;

        private Tracer tracer;

        private CommandFrameProperties cmdFrameProps;
        private CommandProperties      cmdProps;
        private MissionProperties      missionProps;
        private ScmfProperties         scmfProperties;
        private CltuProperties         cltuProperties;
        private boolean                propsSet;


        private IMpsCltuParser cltuParser;
        private boolean        parsersSet;

        private ITcTransferFrameSerializer tcSerializer;

        public Builder(final int scid, final String commandDictPath, final String sclkscetPath) {
            this.scid = scid;
            this.commandDictPath = commandDictPath;
            this.sclkscetPath = sclkscetPath;
        }

        /**
         * Create a builder from a Spring application context. Can immediately call build() afterward.
         *
         * @param appContext Spring application context
         * @throws DictionaryException dictionary exception
         */
        public Builder(final ApplicationContext appContext) throws DictionaryException {
            final IGeneralContextInformation generalContextInformation = appContext
                    .getBean(IGeneralContextInformation.class);

            this.scid = appContext.getBean(IContextConfiguration.class).getContextId().getSpacecraftId();
            this.commandDictPath = appContext.getBean(DictionaryProperties.class)
                    .findFileForSystemMission(DictionaryType.COMMAND);
            this.sclkscetPath = GdsSystemProperties.getMostLocalPath("sclkscet." +
                            appContext.getBean(IContextConfiguration.class).getContextId().getSpacecraftId(),
                    generalContextInformation.getSseContextFlag().isApplicationSse());

            tracer(TraceManager.getTracer(appContext, Loggers.UPLINK))
                    .properties(appContext.getBean(CommandProperties.class),
                            appContext.getBean(CommandFrameProperties.class),
                            appContext.getBean(MissionProperties.class),
                            appContext.getBean(ScmfProperties.class),
                            appContext.getBean(CltuProperties.class))
                    .parsers(appContext.getBean(TcApiBeans.MPS_CLTU_PARSER, IMpsCltuParser.class))
                    .messageFactory(appContext.getBean(IScmfInternalMessageFactory.class));

            serializer(appContext.getBean(ITcTransferFrameSerializer.class));

        }

        public Builder messageFactory(final IScmfInternalMessageFactory scmfInternalMessageFactory) {
            this.scmfInternalMessageFactory = scmfInternalMessageFactory;
            return this;
        }

        /**
         * Build the MPS TEW Utility
         *
         * @return MPS TEW utility
         */
        public MpsTewUtility build() {
            if (!propsSet) {
                throw new IllegalStateException("Properties not set.");
            }

            if (!parsersSet) {
                throw new IllegalStateException("Parsers not set.");
            }

            if (tracer == null) {
                throw new IllegalStateException("Tracer not set.");
            }

            if (tcSerializer == null) {
                throw new IllegalStateException("TC frame serializer not set.");
            }

            return new MpsTewUtility(this);
        }



        /**
         * Set a tracer
         *
         * @param tracer log tracer
         * @return builder
         */
        public Builder tracer(final Tracer tracer) {
            this.tracer = tracer;
            return this;
        }

        /**
         * Set the properties objects
         *
         * @param cmdProps      Command properties
         * @param cmdFrameProps Command frame properties
         * @return builder
         */
        public Builder properties(final CommandProperties cmdProps, final CommandFrameProperties cmdFrameProps,
                                  final MissionProperties missionProps, final ScmfProperties scmfProperties,
                                  final CltuProperties cltupProperties) {
            if (cmdProps == null || cmdFrameProps == null || missionProps == null || scmfProperties == null || cltupProperties == null) {
                throw new IllegalArgumentException("Properties objects must not be null.");
            }

            this.cmdProps = cmdProps;
            this.cmdFrameProps = cmdFrameProps;
            this.missionProps = missionProps;
            this.scmfProperties = scmfProperties;
            this.cltuProperties = cltupProperties;

            this.propsSet = true;

            return this;
        }

        /**
         * Set the parsers
         *
         * @param cltuParser CLTU parser
         * @return builder
         */
        public Builder parsers(final IMpsCltuParser cltuParser) {
            if (cltuParser == null) {
                throw new IllegalArgumentException("Parsers must not be null.");
            }

            this.cltuParser = cltuParser;

            this.parsersSet = true;

            return this;
        }

        public Builder serializer(final ITcTransferFrameSerializer tcSerializer) {
            if (tcSerializer == null) {
                throw new IllegalArgumentException("Serializer must not be null");
            }

            this.tcSerializer = tcSerializer;

            return this;
        }
    }

    @Override
    public byte[] validateAndTranslateCommand(final IFlightCommand command) throws
                                                                            CommandParseException,
                                                                            CommandFileParseException {
        try (final TranslatingMpsSession session = new TranslatingMpsSession(getCtt())) {
            return session.getTranslatedCommandBytes(command);
        }

    }

    @Override
    public ITelecommandPacket wrapBytesToPacket(final byte[] commandBytes, final int apid) {
        throw new UnsupportedOperationException("Wrapping bytes to packet is not yet supported.");
    }

    @Override
    public ITcTransferFrame wrapCommandToFrame(IFlightCommand command) throws FrameWrapUnwrapException {
        try {
            return wrapBytesToFrame(getVirtualChannelType(command.getDefinition()),
                    validateAndTranslateCommand(command));
        } catch (CommandParseException | CommandFileParseException e) {
            throw new FrameWrapUnwrapException(ExceptionTools.getMessage(e));
        }
    }

    private ITcTransferFrame getBeginningDelimiterFrame() throws FrameWrapUnwrapException {
        return getDelimiterFrame(frameProperties.getBeginDataHex());
    }

    private ITcTransferFrame getEndingDelimiterFrame() throws FrameWrapUnwrapException {
        return getDelimiterFrame(frameProperties.getEndDataHex());
    }

    private ITcTransferFrame getDelimiterFrame(final String frameData) throws FrameWrapUnwrapException {
        return wrapBytesToFrame(VirtualChannelType.DELIMITER, BinOctHexUtility.toBytesFromHex(frameData));

    }

    public ITcTransferFrame wrapBytesToFrame(VirtualChannelType vct, byte[] data) throws FrameWrapUnwrapException {
        return wrapBytesToFrame(data, frameProperties.getVirtualChannelNumber(vct));
    }

    private int getCombinedVcid(int rawVcid) {
        // make sure the VCID is actually raw (ie, only 3 least significant bits are set)
        rawVcid = 0x00000007 & rawVcid;
        return ((frameProperties.getStringIdVcidValue() & BITMASK_EXECUTION_STRING) << EXECUTION_STRING_BIT_OFFSET) +
                rawVcid;
    }

    private ITcTransferFrame extractFrame(final Pair<TranslatingMpsSession, TcSession.TcwrapGroup> session) throws
                                                                                                            FrameWrapUnwrapException {
        TcSession.bufitem frameBuf = session.getTwo().getTcwrapBuffer(0);

        if (frameBuf.nerrors != 0) {
            throw new FrameWrapUnwrapException(frameBuf.errmsg);
        }

        TcSession.frmitem frameItem = session.getOne().getFrameItem(frameBuf);

        return MpsTcTransferFrameBuilder.build(frameItem);
    }

    @Override
    public List<ITcTransferFrame> wrapCommandToFrames(IFlightCommand command) throws FrameWrapUnwrapException {
        List<ITcTransferFrame> frames = new ArrayList<>();
        try {

            frames.add(getBeginningDelimiterFrame());
            frames.add(wrapCommandToFrame(command));
            frames.add(getEndingDelimiterFrame());

        } catch (final FrameWrapUnwrapException e) {
            throw e;
        } catch (final Exception ex) {
            throw new FrameWrapUnwrapException(ex);
        }

        return frames;
    }

    @Override
    public ITcTransferFrame wrapBytesToFrame(final byte[] bytes, final int vcid, final int fsn) throws FrameWrapUnwrapException {
        VirtualChannelType vct = VirtualChannelType.getTypeFromNumber(frameProperties, vcid);

        boolean hasFecf = frameProperties.hasFecf(vct);
        int combinedVcid = getCombinedVcid(vcid);
        MpsTcTransferFramesBuilder builder = new MpsTcTransferFramesBuilder();
        builder.setTransferFrameVersionNumber(frameProperties.getVersionNumber())
               .setControlCommandFlagOff()
               .setBypassFlagOn()
               .setScid(scid)
               .setVcid(combinedVcid);

        // MPCS-11666 added check for setting frame sequence number
        if (fsn >= 0) {
            builder.setFrameSequenceNumber(fsn);
        }

        if (hasFecf) {
            builder.setFrameErrorControlFieldAlgorithm(frameProperties.getChecksumCalcluator(vct));
            int fecfLength = frameProperties.getFecfLength();
            if (fecfLength != 2) {
                builder.setFrameErrorControlFieldLength(fecfLength);
            }
        } else {
            builder.noFrameErrorControlField();
        }

        builder.setData(bytes);

        ITcTransferFrame retFrame = builder.build().get(0);
        byte[]           newData  = retFrame.getData();


        //MPCS-11285  - 09/24/19 - super wonky, but currently CTS doesn't like to append put the CRC16 FECF as the FECF.
        //     Instead it likes to put it at the end of the frame data.
        if (!ArrayUtils.isEquals(bytes, newData) && hasFecf) {
            byte[] fecf = Arrays.copyOfRange(newData, bytes.length, newData.length);

            retFrame.setData(bytes);
            retFrame.setFecf(fecf);
        }

        return retFrame;
    }

    @Override
    public ITcTransferFrame wrapBytesToFrame(byte[] bytes, int vcid) throws FrameWrapUnwrapException {
        return wrapBytesToFrame(bytes, vcid, -1);
    }

    @Override
    public ICltu wrapFrameToCltu(final ITcTransferFrame frame) throws CltuEndecException {
        return wrapBytesToCltu(tcSerializer.getBytes(frame), frame.getVirtualChannelId());
    }

    @Override
    public ICltu wrapBytesToCltu(final byte[] frameBytes, final int vcid) throws
                                                                          CltuEndecException {
        try (final MpsSession session = new MpsSession(scid)) {
            final TcSession.cltuitem cltuItem = session.wrapFrameToCltu(frameBytes, vcid);
            return cltuParser.parse(cltuItem);
        } catch (final RuntimeException e) {
            throw new CltuEndecException(e);
        }
    }

    @Override
    public ICltu wrapBytesToCltu(final byte[] commandBytes, final ExecutionStringType esType,
                                 final VirtualChannelType vcType) throws CltuEndecException {
        return wrapBytesToCltu(commandBytes, getVcid(esType, vcType));
    }

    @Override
    public List<ICommandFileLoad> wrapFileToFileLoads(final IFileLoadInfo info) {
        throw new UnsupportedOperationException("Wrapping to file load is not yet supported.");
    }

    @Override
    public List<ITcTransferFrame> wrapFileLoadToFrames(final ICommandFileLoad load) {
        throw new UnsupportedOperationException("Wrapping to file load is not yet supported.");
    }

    @Override
    public List<ICltu> createPlopCltus(final List<ICltu> cltus) {

        throw new UnsupportedOperationException("Creating PLOP CLTUs is not yet supported.");
    }

    @Override
    public IScmf reverseScmf(final byte[] scmf) throws ScmfParseException, ScmfWrapUnwrapException {
        try {
            final File scmfFile = File.createTempFile("scmf-tmp-out", ".scmf");
            FileUtils.writeByteArrayToFile(scmfFile, scmf);

            final IScmf reversedScmf = reverseScmf(scmfFile.getAbsolutePath());

            Files.delete(scmfFile.toPath());

            return reversedScmf;

        } catch (final IOException e) {
            throw new ScmfParseException("An error occurred reversing the given SCMF bytes. " + e.getMessage());
        }
    }

    @Override
    public IScmf reverseScmf(final String filePath) throws ScmfParseException, IOException, ScmfWrapUnwrapException {
        final File validateFile = new File(filePath);
        if (!validateFile.exists()) {
            throw new IllegalArgumentException("SCMF file path is invalid.");
        }

        final MpsScmfBuilder builder = new MpsScmfBuilder();
        builder.setFrameProperties(frameProperties);
        builder.setCltuProperties(cltuProperties);
        return builder.setFilePath(filePath)
                .setInternalMessageFactory(scmfInternalMessageFactory)
                .setMissionProperties(missionProperties)
                .setScmfProperties(scmfProperties)
                .build();
    }


    @Override
    public List<ITcTransferFrame> unwrapCltuToFrames(final ICltu cltu) throws
                                                                       FrameWrapUnwrapException,
                                                                       CltuEndecException {
        try (final MpsSession session = new MpsSession(scid)) {
            final TcSession.cltuitem cltuItem  = session.getCltuItem(cltu.getBytes());
            final TcSession.frmitem  frameItem = session.getFrameItem(cltuItem);
            final ITcTransferFrame   frame     = new MpsTcTransferFrameParser(scid).parse(frameItem, true);
            return Collections.singletonList(frame);
        }
    }

    @Override
    public List<ITelecommandPacket> unwrapFrameToPackets(final ITcTransferFrame frame) {
        throw new UnsupportedOperationException("Unwrapping a frame to packets is not yet supported.");
    }

    @Override
    public IFlightCommand reverseFrameToCommand(final ITcTransferFrame frame) {
        throw new UnsupportedOperationException("Reversing a frame to a command is not yet supported.");
    }

    @Override
    public IFlightCommand reversePacketToCommand(final ITelecommandPacket packet) {
        return null;
    }

    @Override
    public ICommandFileLoad reverseFramesToFileLoad(final List<ITcTransferFrame> frames) {
        return null;
    }

    @Override
    public ICommandFileLoad reversePacketsToFileLoad(final List<ITelecommandPacket> packets) {
        return null;
    }

    @Override
    public ITelecommandPacket rewrapPacket(final ITelecommandPacket packet) {
        return null;
    }

    @Override
    public ICltu rewrapCltu(final ICltu cltu) throws CltuEndecException {
        try (final MpsSession session = new MpsSession(scid)) {
            final TcSession.cltuitem cltuItem  = session.getCltuItem(cltu.getBytes());
            final TcSession.frmitem  frameItem = session.getFrameItem(cltuItem);
            final ITcTransferFrame   frame     = new MpsTcTransferFrameParser(scid).parse(frameItem, true);
            return wrapBytesToCltu(tcSerializer.getBytes(frame), frame.getVirtualChannelId());
        } catch (final FrameWrapUnwrapException e) {
            throw new CltuEndecException(e);
        }
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
        final byte esByte = esType.getVcidValue(frameProperties);
        final byte vcByte = frameProperties.getVirtualChannelNumber(vcType);
        return (byte) (((esByte & 0x07) << 3) | (vcByte & 0x07));
    }

    /**
     * Temporary accessor for the command translation table. Once Tim's UplinkUtils allows instantiating TcSession
     * without a command translation table, this accessor should be removed.
     *
     * @return the command translation table
     */
    public CommandTranslationTable getCtt() throws CommandFileParseException {
        if (this.ctt == null) {
            ctt = new CommandTranslationTable(commandDictPath, sclkscetPath, scid,
                    cmdProps, frameProperties, tracer);
        }
        return ctt;
    }

    private int fecfAlgoEnumToFecType(final boolean hasFecf, final FrameErrorControlFieldAlgorithm algoVal) {
        if (!hasFecf) {
            return UplinkUtilsConstants.TC_FEC_NONE;
        } else if (algoVal.equals(FrameErrorControlFieldAlgorithm.EACSUM55AA)) {
            return UplinkUtilsConstants.TC_FEC_EACSUM55AA;
        } else { //algoVal.equals(FrameErrorControlFieldAlgorithm.CRC16CCITT
            return UplinkUtilsConstants.TC_FEC_SDLC;
        }
    }

    protected VirtualChannelType getVirtualChannelType(ICommandDefinition definition) {
        VirtualChannelType vct;
        switch (definition.getType()) {
            case HARDWARE:
                vct = VirtualChannelType.HARDWARE_COMMAND;
                break;
            case FLIGHT:
                vct = VirtualChannelType.FLIGHT_SOFTWARE_COMMAND;
                break;
            case SEQUENCE_DIRECTIVE:
                vct = VirtualChannelType.SEQUENCE_DIRECTIVE;
                break;
            default:
                vct = VirtualChannelType.UNKNOWN;
        }

        return vct;
    }

}