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
package jpl.gds.tc.legacy.impl.command;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.OpcodeUtil;
import jpl.gds.dictionary.api.command.CommandArgumentType;
import jpl.gds.dictionary.api.command.ICommandDefinition;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.types.AmpcsStringBuffer;
import jpl.gds.tc.api.ICommandObjectFactory;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.command.IFlightCommandTranslator;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;
import jpl.gds.tc.impl.FlightCommand;
import jpl.gds.tc.legacy.impl.args.LegacyArgumentTranslator;

/**
 * This class provides the command translation to bytes that used to be performed by FlightCommand
 *
 */
public class LegacyFlightCommandTranslator implements IFlightCommandTranslator {

    private final DictionaryProperties dictProps;
    private final ICommandObjectFactory commandFactory;

    private IFlightCommand cmd = null;

    private final LegacyArgumentTranslator argumentTranslator;

    public LegacyFlightCommandTranslator(final ApplicationContext appContext) {
        dictProps = appContext.getBean(DictionaryProperties.class);
        commandFactory = appContext.getBean(ICommandObjectFactory.class);

        argumentTranslator = new LegacyArgumentTranslator(appContext);
    }

    public LegacyFlightCommandTranslator(final ApplicationContext appContext, final IFlightCommand command) {
        this(appContext);
        setCommand(command);
    }

    public IFlightCommandTranslator setCommand(final IFlightCommand command) {
        if(command == null) {
            throw new IllegalArgumentException("Constructor must be supplied with a valid command");
        }
        if(!FlightCommand.class.isInstance(command)) {
            throw new IllegalArgumentException ("Supplied IFlightCommand must be a FlightCommand for this translator");
        }

        this.cmd = command;

        return this;
    }

    public void verifyCommand() {
        if(this.cmd == null) {
            throw new IllegalStateException("Command must be set before operations can be performed");
        }
    }

    @Override
    public int parseAndSetArgumentValueFromBitString(final int index, final String bitString, final int offset) throws UnblockException {

        verifyCommand();

        AmpcsStringBuffer bitStringBuff = new AmpcsStringBuffer(bitString, offset);

        String argValue = argumentTranslator.parseFromBitString(cmd.getArgumentDefinition(index), bitStringBuff);

        if(cmd.getArgumentDefinition(index).getType().isRepeat()) {
            cmd.setArgumentValues(index, argValue);
        } else {
            cmd.setArgumentValue(index, argValue);
        }

        return (bitStringBuff.getOffset() - offset);
    }

    @Override
    public int parseAndSetArgumentValueFromBitString(final int index, final int subIndex, final String bitString, final int offset) throws UnblockException {

        verifyCommand();

        AmpcsStringBuffer bitStringBuff = new AmpcsStringBuffer(bitString, offset);

        String argValue = argumentTranslator.parseFromBitString(cmd.getArgumentDefinition(index, subIndex), bitStringBuff);

        cmd.setArgumentValue(index, subIndex, argValue);

        return (bitStringBuff.getOffset() - offset);
    }

    @Override
    public String getArgumentBitString(final int index) throws BlockException {

        verifyCommand();

        CommandArgumentType type = cmd.getArgumentDefinition(index).getType();
        if(type.isRepeat() || type.equals(CommandArgumentType.BITMASK)) {
            return argumentTranslator.toBitString(cmd.getArgumentDefinition(index), cmd.getRepeatArgumentString(index));
        } else {
            return argumentTranslator.toBitString(cmd.getArgumentDefinition(index), cmd.getArgumentValue(index));
        }
    }

    @Override
    public String getArgumentBitString(final int index, final int subIndex) throws BlockException {

        verifyCommand();

        return argumentTranslator.toBitString(cmd.getArgumentDefinition(index, subIndex), cmd.getArgumentValue(index, subIndex));
    }

    @Override
    public String toBitString() throws BlockException {

        verifyCommand();

        StringBuilder bitStr = new StringBuilder();

        bitStr.append(getOpcodeBits());

        for(int i = 0 ; i < cmd.getArgumentCount() ; i++) {
            bitStr.append(getArgumentBitString(i));
        }

        return bitStr.toString();
    }

    public String getOpcodeBits() throws BlockException {

        verifyCommand();

        // If an opcode is "NULL", it means the command is unimplemented (NOTE:
        // I don't mean Java null,
        // I literally mean the string "NULL")

        String opcodeStr = cmd.getEnteredOpcode().toLowerCase();

        if (ICommandDefinition.NULL_OPCODE_VALUE.equalsIgnoreCase(opcodeStr) ||
                opcodeStr.isEmpty()) {
            throw new BlockException(
                    "The command with the stem '"
                            + cmd.getDefinition().getStem()
                            + "' has not been implemented. Its dictionary opcode value is '"
                            + opcodeStr + "'.");
        }

        opcodeStr = OpcodeUtil.stripHexPrefix(opcodeStr);

        return OpcodeUtil.toBinFromHex(opcodeStr);
    }


    public String getOpcodeFromBits(final String opcodeBitString) {
        /* MPCS-6355 - 7/8/14 - Use DictionaryConfiguration instead of CommandConfiguration. */
        if (opcodeBitString == null) {
            throw new IllegalArgumentException("Null input bit string");
        } else if (opcodeBitString.length() != dictProps.getOpcodeBitLength()) {
            throw new IllegalArgumentException(
                    "An opcode bit string must have a length of "
                            + dictProps
                            .getOpcodeBitLength()
                            + " bits.  The input bit string "
                            + "had a length of " + opcodeBitString.length()
                            + " bits.");
        }

        // get the hex representation of the opcode
        /* MPCS-7725 01/20/16 Use OpcodeUtil */
        return OpcodeUtil.toHexFromBin(opcodeBitString);
    }

    @Override
    public IFlightCommand parseFromBitString(final String bitString, final int offset) throws UnblockException {
        /* MPCS-6355 - 7/8/14 - Use DictionaryConfiguration instead of CommandConfiguration. */
        final int opCodeLength = dictProps.getOpcodeBitLength();
        String opcodeBits = bitString.substring(offset, offset + opCodeLength);
        String opcodeHex = getOpcodeFromBits(opcodeBits);

        IFlightCommand cmd;
        int index = offset + opCodeLength;

        try {
            cmd = commandFactory.getCommandObjectFromOpcode(opcodeHex);
        } catch (DictionaryException e) {
            throw new UnblockException(
                    "Could not read the command dictionary to unblock the command: "
                            + e.getMessage(), e);
        }

        if (cmd == null) {

            try {
                throw new UnblockException(
                        "The command with opcode 0x"
                                + OpcodeUtil.toHexFromBin(opcodeBits)
                                + " does not exist in the "
                                + "command dictionary "
                                + dictProps
                                .findFileForSystemMission(DictionaryType.COMMAND));
            } catch (DictionaryException ex) {
                throw new UnblockException("The command with opcode 0x"
                        + OpcodeUtil.toHexFromBin(opcodeBits)
                        + " does not exist in the " + "command dictionary");
            }
        }

        setCommand(cmd);

        for (int i = 0 ; i < cmd.getArgumentCount() ; i++) {
            index += parseAndSetArgumentValueFromBitString(i, bitString, index);
        }

        return (cmd);
    }

}
