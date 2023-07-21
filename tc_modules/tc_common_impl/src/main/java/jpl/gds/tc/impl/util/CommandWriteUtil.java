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
package jpl.gds.tc.impl.util;

import jpl.gds.dictionary.api.OpcodeUtil;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.ICommandFileLoad;
import jpl.gds.tc.api.ICommandObjectFactory;
import jpl.gds.tc.api.ICommandWriteUtility;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.command.IFlightCommandTranslator;
import jpl.gds.tc.api.config.CltuProperties;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.ExecutionStringType;
import jpl.gds.tc.api.config.PlopProperties;
import jpl.gds.tc.api.config.VirtualChannelType;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;
import jpl.gds.tc.api.frame.ITcTransferFrameSerializer;
import jpl.gds.tc.impl.fileload.CommandFileLoad;
import org.springframework.context.ApplicationContext;

import java.io.PrintWriter;
import java.util.List;

/**
 * Utility class for writing command objects to PrintWriter streams
 *
 */
public class CommandWriteUtil implements ICommandWriteUtility {

	private final ApplicationContext appContext;
	private final ICommandObjectFactory commandFactory;
	private final CommandFrameProperties commandFrameProps;
	private final CltuProperties cltuConfig;
    private final PlopProperties plopConfig;
    private final DictionaryProperties dictConfig;
    private final IFlightCommandTranslator commandTranslator;
    private final ITcTransferFrameSerializer frameSerializer;

    /** Default constructor */
    public CommandWriteUtil(final ApplicationContext appContext) {
        this.appContext = appContext;
        commandFactory = appContext.getBean(ICommandObjectFactory.class);
        commandFrameProps = appContext.getBean(CommandFrameProperties.class);
        cltuConfig = appContext.getBean(CltuProperties.class);
        plopConfig = appContext.getBean(PlopProperties.class);
        dictConfig = appContext.getBean(DictionaryProperties.class);
        commandTranslator = appContext.getBean(IFlightCommandTranslator.class, appContext);
        frameSerializer = appContext.getBean(ITcTransferFrameSerializer.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeCltu(final PrintWriter pw, final ICltu cltu, final int cltuNumber) throws UnblockException, BlockException
    {
        try {
            pw.write("Message #" + (cltuNumber + 1) + " command bit length = " + (cltu.getBytes().length * 8) + "\n");
        } catch (IllegalStateException e) {
            // no bch blocks to generate data, skip
        }
        //write acquisition sequence
        if(cltu.getAcquisitionSequence() != null)
        {
            pw.write("Leading Acquisition Sequence Found.\n");
            pw.write("Acquisition Sequence Byte Pattern (Hex) = " + BinOctHexUtility
                    .formatHexString(plopConfig.getAcquisitionSequenceByteHex(), HEX_CHARS_PER_LINE) + "\n");
            pw.write("Acquisition Sequence Bit Length = " + cltu
                    .getAcquisitionSequence().length * 8 + ICommandWriteUtility.DOUBLE_LINE);
        }
        else
        {
            pw.write("No Leading Acquisition Sequence Found.\n\n");
        }

        //write CLTU
        pw.write("~~~~~~~~~~~~~~~~~~~~~~~CLTU #" + (cltuNumber+1) + "~~~~~~~~~~~~~~~~~~~~~~~\n");

        pw.write("\nStart Sequence Found.\n");
        pw.write("Start Sequence (Hex) = " + BinOctHexUtility.formatHexString(cltuConfig.getStartSequenceHex(),HEX_CHARS_PER_LINE) + "\n");
        pw.write("Start Sequence Bit Length = " + cltuConfig.getStartSequenceHex().length()*4 + ICommandWriteUtility.DOUBLE_LINE);

        final List<ITcTransferFrame> frames = cltu.getFrames();
        pw.write("Total Frames in CLTU = " + frames.size() + ICommandWriteUtility.DOUBLE_LINE);

        //write frames out from inside CLTU
        for(int j=0; j < frames.size(); j++)
        {
            writeFrame(pw, frames.get(j),j);
        }

        pw.write("\nTail Sequence Found.\n");
        pw.write("Tail Sequence (Hex) = " + BinOctHexUtility.formatHexString(cltuConfig.getTailSequenceHex(),HEX_CHARS_PER_LINE) + "\n");
        pw.write("Tail Sequence Bit Length = " + cltuConfig.getTailSequenceHex().length()*4 + ICommandWriteUtility.DOUBLE_LINE);

        pw.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n\n");

        //write idle sequence
        if(cltu.getIdleSequence() != null)
        {
            pw.write("Trailing Idle Sequence Found.\n");
            pw.write("Idle Sequence Byte Pattern (Hex) = " + BinOctHexUtility.formatHexString(plopConfig.getIdleSequenceByteHex(),HEX_CHARS_PER_LINE) + "\n");
            pw.write("Idle Sequence Bit Length = " + cltu.getIdleSequence().length*8 + "\n\n\n\n");
        }
        else
        {
            pw.write("No Trailing Idle Sequence Found.\n\n\n\n");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeFrame(final PrintWriter pw, final ITcTransferFrame frame, final int frameNumber) throws UnblockException, BlockException
    {
    	pw.write("---------------FRAME #" + (frameNumber+1) + "--------------\n");

    	//write the frame header
    	pw.write(getFrameHeaderString(frame));

        pw.write("\nFrame Header (Hex) = " + BinOctHexUtility
                .formatHexString(BinOctHexUtility.toHexFromBytes(frameSerializer.getHeaderBytes(frame)),
                        HEX_CHARS_PER_LINE) + "\n");

    	//write the frame's data according to what type of data it contains
    	final VirtualChannelType vct = VirtualChannelType.getTypeFromNumber(commandFrameProps, frame.getVirtualChannelNumber());

    	switch(vct)
    	{
    	case HARDWARE_COMMAND:
    	case FLIGHT_SOFTWARE_COMMAND:

            pw.write("Frame Data (Hex): \n" + BinOctHexUtility
                    .formatHexString(BinOctHexUtility.toHexFromBytes(frame.getData()), HEX_CHARS_PER_LINE) + "\n");

    		if (frame.hasFecf()) {
                pw.write("Frame FECF (Hex): \n" + BinOctHexUtility
                        .formatHexString(BinOctHexUtility.toHexFromBytes(frame.getFecf()),
                                HEX_CHARS_PER_LINE) + ICommandWriteUtility.DOUBLE_LINE);
            }

    		final String frameDataBits = BinOctHexUtility.toBinFromBytes(frame.getData());
    		final IFlightCommand command;
    		try
    		{
    			command = commandFactory.getCommandObjectFromBits(frameDataBits,0);
    			writeCommand(pw,command);
    		}
    		catch(final Exception e)
    		{
    			pw.write("Error interpreting the command frame.  Only opcode and argument data will be dumped.\n\n");
    			writeBadCommand(pw, frameDataBits);
    		}

    		break;

    	case FILE_LOAD:

    		pw.write("\n");
    		try
    		{
    			if(frame.getSequenceNumber() == 0)
    			{
    				//this is a slight hack...we only partially recreate the entire file load
    				//object...we don't give it all the file load data back from all the frames...the first
    				//file load contains all the metadata, the rest just contain pieces of the file
    				final ICommandFileLoad load = new CommandFileLoad(appContext, frame.getData(),0);
    				writeFirstFileLoad(pw,load);
    			}
    			else
    			{
    				writeFileLoad(pw,frame.getData());
    			}
    		}
    		catch(final Exception e)
    		{
    			pw.write("Error interpreting the file load frame.  Cannot display any more information.\n\n");
    		}

    		break;

    	case DELIMITER:

            pw.write("Frame Data (Hex): \n" + BinOctHexUtility
                    .formatHexString(BinOctHexUtility.toHexFromBytes(frame.getData()), HEX_CHARS_PER_LINE) + "\n");
    		if (frame.hasFecf()) {
                pw.write("Frame FECF (Hex): " + BinOctHexUtility
                        .formatHexString(BinOctHexUtility.toHexFromBytes(frame.getFecf()),
                                HEX_CHARS_PER_LINE) + ICommandWriteUtility.DOUBLE_LINE);
            }

    		break;

    	default:

    		pw.write("Unknown virtual channel type.\n");
            pw.write("Frame Data (Hex): \n" + BinOctHexUtility
                    .formatHexString(BinOctHexUtility.toHexFromBytes(frame.getData()),
                            HEX_CHARS_PER_LINE) + ICommandWriteUtility.DOUBLE_LINE);

    		break;
    	}

        pw.write("-----------------------------\n");
    }

    private String getFrameHeaderString(final ITcTransferFrame frame) {
        final StringBuilder buf = new StringBuilder(1024);
        ExecutionStringType est =  null;
        VirtualChannelType  vct = null;

        if(commandFrameProps != null) {
            est = ExecutionStringType.getTypeFromVcidValue(commandFrameProps, frame.getExecutionString());
            vct = VirtualChannelType.getTypeFromNumber(commandFrameProps, frame.getVirtualChannelNumber());
        }

        buf.append("Frame Version = ").append(frame.getVersionNumber() + 1).append("\n");
        buf.append("Bypass Flag = ").append(frame.getBypassFlag()).append("\n");
        buf.append("Control Command Flag = ").append(frame.getControlCommandFlag()).append("\n");
        buf.append("Spare Bits = ").append(frame.getSpare()).append("\n");
        buf.append("Spacecraft ID = ").append(frame.getSpacecraftId()).append("\n");
        buf.append("RCE String = ").append(frame.getExecutionString());
        if(est != null) {
            buf.append(" (").append(est.toString()).append(")");
        }
        buf.append("\n");
        buf.append("Virtual Channel Number = ").append(frame.getVirtualChannelNumber());
        if(vct != null) {
            buf.append(" (").append(vct.toString()).append(")");
        }
        buf.append("\n");
        buf.append("Sequence Number = ").append(frame.getSequenceNumber()).append("\n");
        buf.append("Length = ").append(frame.getLength() + 1).append("\n");

        return (buf.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBadCltu(final PrintWriter pw, final byte[] bytes, final int cltuNumber)
    {
        pw.write("~~~~~~~~~~~~~~~~~~~~~~~CLTU #" + (cltuNumber+1) + "~~~~~~~~~~~~~~~~~~~~~~~\n");

        pw.write("\nCLTU #" + (cltuNumber+1) + " could not be interpreted as a CLTU.\n\n");
        pw.write("CLTU Data (Hex) = \n\n" + BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(bytes),
                HEX_CHARS_PER_LINE) + ICommandWriteUtility.DOUBLE_LINE);

        pw.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n\n");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBadCommand(final PrintWriter pw, final String frameDataBits)
    {
        String opcodeBits = null;
        String dataBits = null;

        //grab the opcode
        pw.write("Opcode (Hex) = ");
        /* MPCS-6355 - 7/8/14 - Use DictionaryConfiguration instead of CommandProperties. */
        if(frameDataBits.length() >= dictConfig.getOpcodeBitLength())
        {
            opcodeBits = frameDataBits.substring(0, dictConfig.getOpcodeBitLength());

            /** MPCS-7725 01/21/16 Use OpcodeUtil */
            pw.write(OpcodeUtil.formatHexString(OpcodeUtil.toHexFromBin(opcodeBits), HEX_CHARS_PER_LINE) + "\n");
        }

        //grab the command args
        pw.write("Command Arguments (Hex) = ");
        if(frameDataBits.length() > dictConfig.getOpcodeBitLength())
        {
            dataBits = frameDataBits.substring(dictConfig.getOpcodeBitLength());

            /** MPCS-7725 01/21/16 Use OpcodeUtil */
            pw.write(OpcodeUtil.formatHexString(OpcodeUtil.toHexFromBin(dataBits),HEX_CHARS_PER_LINE));
        }
        pw.write("\n");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeCommand(final PrintWriter pw, final IFlightCommand command) throws BlockException
    {
        /*
         * MPCS-6304 - 6/22/14. Go through the definition object to get the stem. Must
         * still fetch the opcode from the command argument itself, because it is modifiable.
         */
        pw.write("Command Stem = " + command.getDefinition().getStem() + "\n");

        /** MPCS-7725 01/21/16 Use OpcodeUtil */
        pw.write("Opcode (Hex) = " + OpcodeUtil.formatHexString(command.getEnteredOpcode(), HEX_CHARS_PER_LINE) + "\n");

        pw.write("Bit Length = " + commandTranslator.setCommand(command).getOpcodeBits()
                .length() + ICommandWriteUtility.DOUBLE_LINE);

        /*
         * 11/8/13 - MPCS-5521. Changed to use new ICommandArgumentDefinition type.
         * 6/22/14 - MPCS-6304. Definition now split from command argument. Go back
         * to using argument object.
         */
        for(int k=0; k < command.getArgumentCount(); k++)
        {
            /*
             * 11/8/13 - MPCS-5521. Cast from interface type to internal type.
             */
            writeCommandArgument(pw, command, k);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeCommandArgument(final PrintWriter pw, final IFlightCommand cmd, final int index) throws BlockException
    {

        commandTranslator.setCommand(cmd);

        /*
         * MPCS-6304 - 6/22/14. Go through the definition object to get dictionary info.
         */
        pw.write("Parameter = " + cmd.getArgumentDefinition(index).getDictionaryName() + "\n");
        pw.write("FSW Name = " + cmd.getArgumentDefinition(index).getFswName() + "\n");
        pw.write("Bit Length = " + cmd.getArgumentDefinition(index).getBitLength() + "\n");

        if(cmd.getArgumentType(index).isRepeat())
        {
            pw.write("Value = " + cmd.getArgumentValue(index) + "\n");
            pw.write("Value (Hex) = " + BinOctHexUtility
                    .formatHexString(Integer.toHexString(Integer.parseInt(cmd.getArgumentValue(index))),
                            HEX_CHARS_PER_LINE) + ICommandWriteUtility.DOUBLE_LINE);

            /*
             * 11/8/13 - MPCS-5521. Changed to use new ICommandArgumentDefinition type.
             */
            final int repeatArgsCount = cmd.getArgumentCount(index);
            for(int j=0; j < repeatArgsCount; j++)
            {
                writeCommandArgument(pw, cmd, index, j);
            }
        }
        else if(cmd.getArgumentType(index).isEnumeration())
        {
            final String value = cmd.getArgumentEnumValue(index) != null ? cmd.getArgumentEnumValue(index)
                    .getDictionaryValue() : cmd.getArgumentValue(index);
            pw.write("Value = " + value + "\n");
            pw.write("Value (Hex) = " + BinOctHexUtility
                    .formatHexString(BinOctHexUtility.toHexFromBin(commandTranslator.getArgumentBitString(index)),
                            HEX_CHARS_PER_LINE) + ICommandWriteUtility.DOUBLE_LINE);
        }
        else
        {
            pw.write("Value = " + cmd.getArgumentValue(index) + "\n");
            pw.write("Value (Hex) = " + BinOctHexUtility
                    .formatHexString(BinOctHexUtility.toHexFromBin(commandTranslator.getArgumentBitString(index)),
                            HEX_CHARS_PER_LINE) + ICommandWriteUtility.DOUBLE_LINE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeCommandArgument(final PrintWriter pw, final IFlightCommand cmd, final int index, final int subIndex) throws BlockException
    {

        commandTranslator.setCommand(cmd);

        /*
         * MPCS-6304 - 6/22/14. Go through the definition object to get dictionary info.
         */
        pw.write("Parameter = " + cmd.getArgumentDefinition(index, subIndex).getDictionaryName() + "\n");
        pw.write("FSW Name = " + cmd.getArgumentDefinition(index, subIndex).getFswName() + "\n");
        pw.write("Bit Length = " + cmd.getArgumentDefinition(index, subIndex).getBitLength() + "\n");

        //can't have nested REPEAT arguments
        if(cmd.getArgumentType(index, subIndex).isEnumeration())
        {
            final String value = cmd.getArgumentEnumValue(index, subIndex) != null ? cmd.getArgumentEnumValue(index, subIndex)
                    .getDictionaryValue() : cmd.getArgumentValue(index, subIndex);
            pw.write("Value = " + value + "\n");
            pw.write("Value (Hex) = " + BinOctHexUtility.formatHexString(
                    BinOctHexUtility.toHexFromBin(commandTranslator.getArgumentBitString(index, subIndex)),
                    HEX_CHARS_PER_LINE) + DOUBLE_LINE);
        }
        else
        {
            pw.write("Value = " + cmd.getArgumentValue(index, subIndex) + "\n");
            pw.write("Value (Hex) = " + BinOctHexUtility.formatHexString(
                    BinOctHexUtility.toHexFromBin(commandTranslator.getArgumentBitString(index, subIndex)),
                    HEX_CHARS_PER_LINE) + DOUBLE_LINE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeFirstFileLoad(final PrintWriter pw,final ICommandFileLoad load)
    {
        pw.write(load.getHeaderString() + "\n");
        writeFileLoad(pw,load.getData());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeFileLoad(final PrintWriter pw,final byte[] data)
    {
        pw.write("File Load Data (Hex): \n");
        pw.write(BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(data),
                HEX_CHARS_PER_LINE) + ICommandWriteUtility.DOUBLE_LINE);
    }

}
