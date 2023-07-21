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

package jpl.gds.tc.mps.impl.session;

import gov.nasa.jpl.tcsession.TcSession;
import gov.nasa.jpl.uplinkutils.UplinkUtils;
import jpl.gds.dictionary.api.command.CommandDefinitionType;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.CommandParseException;
import jpl.gds.tc.api.exception.FrameWrapUnwrapException;
import jpl.gds.tc.mps.impl.ctt.CommandTranslationTable;

import java.util.ArrayList;
import java.util.List;

/**
 * An MPS telecommand session that requires command translation
 *
 * @since R8.2
 *
 * MPCS-11285  - 09/24/19 - Added sequence command info
 */
public class TranslatingMpsSession extends AMpsSession {
    private final int HDW_COMMAND_VCID;
    private final int FSW_COMMAND_VCID;
    private final int SEQ_COMMAND_VCID;

    /**
     * Constructor
     *
     * @param ctt command translation table
     */
    public TranslatingMpsSession(final CommandTranslationTable ctt) {
        super(new TcSession(ctt.getCommandTranslationPointer(), ctt.getScid(), ctt.getSclkScetPath(), true));
        this.HDW_COMMAND_VCID = ctt.getHardwareCommandVcid();
        this.FSW_COMMAND_VCID = ctt.getFlightSoftwareCommandVcid();
        this.SEQ_COMMAND_VCID = ctt.getSequenceCommandVcid();
    }

    /**
     * Get translated command bytes from a command object
     *
     * @param command
     * @return translated command bytes
     * @throws CommandParseException
     */
    public byte[] getTranslatedCommandBytes(final IFlightCommand command) throws CommandParseException {
        final CommandDefinitionType commandType = command.getDefinition().getType();
        final TcSession.TcwrapGroup wrapGroup   = getTcwrapGroup(commandType);
        wrapGroup.MneFrmTcwrapToFrm(command.toString());
        final TcSession.bufitem commandBufferItem = getCommandBufferItem(wrapGroup);
        final String            commandHexString  = getCommandHexString(commandBufferItem);
        return BinOctHexUtility.toBytesFromHex(commandHexString);
    }

    public TcSession.frmitem getFrame(final IFlightCommand command) throws FrameWrapUnwrapException {
        final CommandDefinitionType commandType = command.getDefinition().getType();
        final TcSession.TcwrapGroup wrapGroup   = getTcwrapGroup(commandType);
        wrapGroup.MneFrmTcwrapToFrm(command.toString());
        final TcSession.bufitem bufferItem = wrapGroup.getTcwrapBuffer(0);

        return getFrameItem(bufferItem);
    }

    public List<TcSession.cltuitem> getCltus(final IFlightCommand command) throws CltuEndecException {
        final CommandDefinitionType commandType = command.getDefinition().getType();
        final TcSession.TcwrapGroup wrapGroup   = getTcwrapGroup(commandType);
        wrapGroup.MneFrmTcwrapToFrm(command.toString());

        List<TcSession.bufitem> cltuBufs = getLinearCltuBufferList();
        List<TcSession.cltuitem> cltus = new ArrayList<>();

        for(TcSession.bufitem cltuBuf : cltuBufs) {
            cltus.add(getCltuItem(cltuBuf));
        }

        return cltus;
    }

    private String getCommandHexString(final TcSession.bufitem commandBufferItem) {
        return UplinkUtils.bintoasciihex(commandBufferItem.buf, commandBufferItem.nbits, 0);
    }

    private TcSession.bufitem getCommandBufferItem(final TcSession.TcwrapGroup wrapGroup) throws CommandParseException {
        final TcSession.bufitem commandBufferItem = wrapGroup.getCmdBuffer(0);
        if (commandBufferItem.nerrors > 0) {
            throw new CommandParseException("Error translating command mnemonic to bytes.");
        }
        return commandBufferItem;
    }

    private TcSession.TcwrapGroup getTcwrapGroup(final CommandDefinitionType commandType) {
        final TcSession.TcwrapGroup wrapGroup;
        if (commandType == CommandDefinitionType.HARDWARE) {
            wrapGroup = session.create_tcwrap_group(HDW_COMMAND_VCID);
        } else if (commandType == CommandDefinitionType.FLIGHT) {
            wrapGroup = session.create_tcwrap_group(FSW_COMMAND_VCID, -1, -1);
        } else if (commandType == CommandDefinitionType.SEQUENCE_DIRECTIVE) {
            wrapGroup = session.create_tcwrap_group(SEQ_COMMAND_VCID, -1, -1);
        } else {
            throw new IllegalArgumentException("The command type " + commandType + " is not supported by TranslatingMpsSession");
        }
        return wrapGroup;
    }

}
