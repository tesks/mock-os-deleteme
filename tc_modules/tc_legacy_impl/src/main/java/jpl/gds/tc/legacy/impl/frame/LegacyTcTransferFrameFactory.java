package jpl.gds.tc.legacy.impl.frame;

import jpl.gds.context.impl.ContextIdentification;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.*;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.VirtualChannelType;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.CommandFileParseException;
import jpl.gds.tc.api.exception.CommandParseException;
import jpl.gds.tc.api.exception.FrameWrapUnwrapException;
import jpl.gds.tc.api.frame.ITcTransferFrameBuilder;
import jpl.gds.tc.api.frame.ITcTransferFrameFactory;
import jpl.gds.tc.api.frame.ITcTransferFrameSerializer;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy Transfer frame factory
 */
public class LegacyTcTransferFrameFactory implements ITcTransferFrameFactory {

    private final CommandFrameProperties frameConfig;
    private final ITcTransferFrameSerializer frameSerializer;
    private final ITewUtility tewUtility;
    private final ApplicationContext appContext;
    private final ContextIdentification contextId;
    private final Tracer trace;

    /**
     * Constructor
     * @param appContext Application Context
     */
    public LegacyTcTransferFrameFactory(final ApplicationContext appContext) {
        this.appContext = appContext;
        frameConfig = appContext.getBean(CommandFrameProperties.class);
        // MPCS-11509 - use legacy frame serializer
        frameSerializer = appContext.getBean(TcApiBeans.LEGACY_COMMAND_FRAME_SERIALIZER, ITcTransferFrameSerializer.class);
        tewUtility = appContext.getBean(TcApiBeans.LEGACY_TEW_UTILITY, ITewUtility.class);
        contextId = appContext.getBean(ContextIdentification.class);

        trace = TraceManager.getTracer(appContext, Loggers.UPLINK);
    }


    @Override
    public ITcTransferFrame createTelecommandFrameFromPacket(ITelecommandPacket packet, int scid, int vcid) {
        final ITcTransferFrameBuilder builder = appContext.getBean(ITcTransferFrameBuilder.class);

        //build the frame header
        byte vcn = (byte)(vcid & ITcTransferFrame.BITMASK_VC_NUMBER);
        final VirtualChannelType vct = VirtualChannelType.getTypeFromNumber(frameConfig, vcn);
        boolean hasFecf = frameConfig.hasFecf(vct);

        builder.setVirtualChannelNumber(vcn)
                .setSpacecraftId(scid)
                .setVersion(frameConfig.getVersionNumber())
                .setExecutionString(frameConfig.getStringIdVcidValue())
                .setBypassFlag(0x01)
                .setCtrlCmdFlag(0x00)
                .setSequenceNumber(0)
                .setData(packet.getBytes()) //build the frame
                .setHasFecf(hasFecf)
                .setFrameLength(0); // dummy value so we can actually calculate the length

        if(hasFecf) { //if we have an FECF we need a temp value for the builder to work
            byte[] tmpFecf = new byte [frameConfig.getFecfLength()];
            builder.setFecf(tmpFecf);
        }

        int totalLength = frameSerializer.calculateLength(builder.build());

        if (totalLength > ITcTransferFrameSerializer.LENGTH_MAX_VALUE) {
            final long maxPduSize = ITcTransferFrameSerializer.LENGTH_MAX_VALUE + 1 - (ITcTransferFrameSerializer.TOTAL_HEADER_BYTE_LENGTH
                    + (hasFecf ? frameConfig.getFecfLength() : 0));
            throw new IllegalArgumentException("Supplied packet is too large for transmisison (" + packet.getBytes().length
                    + "). Maximum allowable size for VC-" + vcid + " (" + vct + ") is " + maxPduSize + " bytes");
        }

        builder.setFrameLength(totalLength);

        if(hasFecf) { //now actually calculate it
            byte[] fecf = frameSerializer.calculateFecf(builder.build(), frameConfig.getChecksumCalcluator(vcn));
            builder.setFecf(fecf);
        }

        trace.trace("Created TC frame from packet of size ", packet.getBytes().length, " with vcid ", vcid, ", scid ", scid, ", and vc type " , vct);

        return builder.build();
    }

    @Override
    public ITcTransferFrame createTelecommandFrameFromPdu(byte[] pdu, int scid, int vcid) {
        final ITcTransferFrameBuilder builder = appContext.getBean(ITcTransferFrameBuilder.class);

        //build the frame header
        byte vcn = (byte)(vcid & ITcTransferFrame.BITMASK_VC_NUMBER);
        final VirtualChannelType vct = VirtualChannelType.getTypeFromNumber(frameConfig, vcn);
        boolean hasFecf = frameConfig.hasFecf(vct);

        builder.setVirtualChannelNumber(vcn)
                .setSpacecraftId(scid)
                .setVersion(frameConfig.getVersionNumber())
                .setExecutionString(frameConfig.getStringIdVcidValue())
                .setBypassFlag(0x01)
                .setCtrlCmdFlag(0x00)
                .setSequenceNumber(0)
                .setData(pdu) //build the frame
                .setHasFecf(hasFecf)
                .setFrameLength(0); // dummy value so we can actually calculate the length

        if(hasFecf) { //if we have an FECF we need a temp value for the builder to work
            byte[] tmpFecf = new byte [frameConfig.getFecfLength()];
            builder.setFecf(tmpFecf);
        }

        final int totalLength = frameSerializer.calculateLength(builder.build());
        if (totalLength > ITcTransferFrameSerializer.LENGTH_MAX_VALUE) {
            final long maxPduSize = ITcTransferFrameSerializer.LENGTH_MAX_VALUE + 1 - (ITcTransferFrameSerializer.TOTAL_HEADER_BYTE_LENGTH
                    + (hasFecf ? frameConfig.getFecfLength() : 0));
            throw new IllegalArgumentException("Supplied PDU is too large for transmisison (" + pdu.length
                    + "). Maximum allowable size for VC-" + vcid + " (" + vct + ") is " + maxPduSize + " bytes");
        }

        builder.setFrameLength(totalLength);

        if(hasFecf) { //now actually calculate it
            byte[] fecf = frameSerializer.calculateFecf(builder.build(), frameConfig.getChecksumCalcluator(vct));
            builder.setFecf(fecf);
        }

        trace.trace("Created TC frame from PDU of size ", pdu.length, " with vcid ", vcid, ", scid ", scid, ", and vc type " , vct);

        return builder.build();
    }

    @Override
    public List<ITcTransferFrame> createFileLoadFrames(ICommandFileLoad load) throws FrameWrapUnwrapException {
        final VirtualChannelType vct = VirtualChannelType.FILE_LOAD;
        final int maxFrameDataByteLength = frameConfig.getMaxFrameDataLength(vct);
        boolean seqCounter = frameConfig.hasSequenceCounter(vct);
        final List<ITcTransferFrame> frames = new ArrayList<>(load.getFileByteLength()/maxFrameDataByteLength + 1);
        int offset = 0;
        int sequenceNumber = 0;


        //loop through the file load bytes until we get all of them into frames
        final byte[] loadBytes = load.getFileLoadBytes();
        while(offset < loadBytes.length) {
            //allocate the proper size buffer...
            //see if we have enough bytes left to fill an entire frame
            byte[] frameDataBytes;
            if((loadBytes.length - offset) >= maxFrameDataByteLength) {
                frameDataBytes = new byte[maxFrameDataByteLength];
            } else {
                frameDataBytes = new byte[loadBytes.length-offset];
            }

            final ITcTransferFrameBuilder builder = appContext.getBean(ITcTransferFrameBuilder.class);

            //get the frame data for the current frame
            System.arraycopy(loadBytes,offset,frameDataBytes,0,frameDataBytes.length);
            offset += frameDataBytes.length;

            //build the frame header
            byte vcn = frameConfig.getVirtualChannelNumber(vct);
            boolean hasFecf = frameConfig.hasFecf(vct);

            builder.setVirtualChannelNumber(vcn)
                    .setSpacecraftId(contextId.getSpacecraftId())
                    .setVersion(frameConfig.getVersionNumber())
                    .setExecutionString(frameConfig.getStringIdVcidValue())
                    .setBypassFlag(0x01)
                    .setCtrlCmdFlag(0x00)
                    .setSequenceNumber(seqCounter ? sequenceNumber++ : sequenceNumber)
                    .setData(frameDataBytes) //build the frame
                    .setHasFecf(hasFecf)
                    .setFrameLength(0); // dummy value so we can actually calculate the length

            if(hasFecf) { //if we have an FECF we need a temp value for the builder to work
                byte[] tmpFecf = new byte [frameConfig.getFecfLength()];
                builder.setFecf(tmpFecf);
            }

            builder.setFrameLength(frameSerializer.calculateLength(builder.build()));

            if(hasFecf) { //now actually calculate it
                byte[] fecf = frameSerializer.calculateFecf(builder.build(), frameConfig.getChecksumCalcluator(vcn));
                builder.setFecf(fecf);
            }

            frames.add(builder.build());
        }

        return frames;
    }

    @Override
    public ITcTransferFrame createCommandFrame(IFlightCommand command) throws BlockException, CommandParseException, CommandFileParseException {
        VirtualChannelType vct;
        switch(command.getDefinition().getType()) {
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

        final ITcTransferFrameBuilder builder = appContext.getBean(ITcTransferFrameBuilder.class);

        //build the frame header
        byte vcn = frameConfig.getVirtualChannelNumber(vct);
        boolean hasFecf = frameConfig.hasFecf(vct);

        builder.setVirtualChannelNumber(vcn)
                .setSpacecraftId(contextId.getSpacecraftId())
                .setVersion(frameConfig.getVersionNumber())
                .setExecutionString(frameConfig.getStringIdVcidValue())
                .setBypassFlag(0x01)
                .setCtrlCmdFlag(0x00)
                .setSequenceNumber(0)
                .setData(tewUtility.validateAndTranslateCommand(command)) //build the frame
                .setHasFecf(hasFecf)
                .setFrameLength(0); // dummy value so we can actually calculate the length

        if(hasFecf) { //if we have an FECF we need a temp value for the builder to work
            byte[] tmpFecf = new byte [frameConfig.getFecfLength()];
            builder.setFecf(tmpFecf);
        }

        builder.setFrameLength(frameSerializer.calculateLength(builder.build()));

        if(hasFecf) { //now actually calculate it
            byte[] fecf = frameSerializer.calculateFecf(builder.build(), frameConfig.getChecksumCalcluator(vcn));
            builder.setFecf(fecf);
        }

        return builder.build();
    }

    @Override
    public ITcTransferFrame createBeginDelimiterFrame() {
        return createDelimiterFrame(true);
    }

    @Override
    public ITcTransferFrame createEndDelimiterFrame() {
        return createDelimiterFrame(false);
    }

    /**
     * Create a delimiter frame. If the supplied argument is TRUE, a beginning
     * delimiter frame is created. If FALSE, an ending delimiter frame is
     * created.
     *
     * @param start
     *            true if the beginning frame is to be created, false if the
     *            ending frame is to be created
     *
     * @return A delimiter telecommand frame
     *
     * MPCS-8534 - 01/13/17 - changed access control to private, added boolean argument.
     */
    private ITcTransferFrame createDelimiterFrame(final boolean start) {
        ITcTransferFrameBuilder builder = appContext.getBean(ITcTransferFrameBuilder.class);
        final VirtualChannelType vct = VirtualChannelType.DELIMITER;

        ///build the frame header
        byte vcn = frameConfig.getVirtualChannelNumber(vct);
        boolean hasFecf = frameConfig.hasFecf(vct);

        builder.setVirtualChannelNumber(vcn)
                .setSpacecraftId(contextId.getSpacecraftId())
                .setVersion(frameConfig.getVersionNumber())
                .setExecutionString(frameConfig.getStringIdVcidValue())
                .setBypassFlag(0x01)
                .setCtrlCmdFlag(0x00)
                .setSequenceNumber(0)
                .setHasFecf(hasFecf)
                .setFrameLength(0); // dummy value so we can actually calculate the length;

        //build the frame
        if(start){
            builder.setData(BinOctHexUtility.toBytesFromHex(frameConfig.getBeginDataHex()));
        }
        else{
            builder.setData(BinOctHexUtility.toBytesFromHex(frameConfig.getEndDataHex()));
        }

        if(hasFecf) { //if we have an FECF we need a temp value for the builder to work
            byte[] tmpFecf = new byte [frameConfig.getFecfLength()];
            builder.setFecf(tmpFecf);
        }

        builder.setFrameLength(frameSerializer.calculateLength(builder.build()));

        if(hasFecf) { //now actually calculate it
            byte[] fecf = frameSerializer.calculateFecf(builder.build(), frameConfig.getChecksumCalcluator(vcn));
            builder.setFecf(fecf);
        }

        builder.setFrameLength(frameSerializer.calculateLength(builder.build()));

        return builder.build();
    }
}
