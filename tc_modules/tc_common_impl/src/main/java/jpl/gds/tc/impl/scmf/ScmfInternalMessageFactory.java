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

package jpl.gds.tc.impl.scmf;

import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.ICommandObjectFactory;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.IScmfInternalMessageFactory;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.VirtualChannelType;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.UnblockException;
import jpl.gds.tc.api.message.ICommandMessageFactory;
import jpl.gds.tc.api.message.ICpdUplinkMessage;
import jpl.gds.tc.api.message.IUplinkMessage;
import jpl.gds.tc.impl.fileload.CommandFileLoad;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating internal uplink messages from an SCMF.
 *
 */
public class ScmfInternalMessageFactory implements IScmfInternalMessageFactory {

    private final ApplicationContext     appContext;
    private final ICommandMessageFactory msgFactory;

    /**
     * Constructor
     *
     * @param appContext spring application context
     */
    public ScmfInternalMessageFactory(final ApplicationContext appContext) {
        this.appContext = appContext;
        this.msgFactory = appContext.getBean(ICommandMessageFactory.class);
    }

    @Override
    public List<IUplinkMessage> createInternalUplinkMessages(final IScmf scmf) throws
                                                                               CltuEndecException,
                                                                               UnblockException {
        final List<ITcTransferFrame> frames   = scmf.getFramesFromScmf();
        final List<IUplinkMessage>   messages = new ArrayList<>(frames.size());
        // some of this logic is very similar to that contained in the
        // jpl.gds.tc.impl.app.ScmfReverseApp

        for (final ITcTransferFrame frame : frames) {
            final VirtualChannelType vct = VirtualChannelType.getTypeFromNumber(appContext.getBean(
                    CommandFrameProperties.class), frame
                    .getVirtualChannelNumber());

            IUplinkMessage msg = null;
            switch (vct) {
                case HARDWARE_COMMAND:
                case FLIGHT_SOFTWARE_COMMAND:

                    msg = getCommandMessage(frame, vct);

                    break;

                case FILE_LOAD:

                    if (frame.getSequenceNumber() == 0) {
                        msg = getFileLoadMessage(frame);
                    } else {
                        continue;
                    }

                    break;

                case DELIMITER:
                case UNKNOWN:
                default:

                    continue;
            }

            if (msg != null) {

                if (msg instanceof ICpdUplinkMessage) {
                    ((ICpdUplinkMessage) msg)
                            .setOriginalFilename(scmf.getOriginalFile());
                }

                messages.add(msg);
            }
        }

        return (messages);
    }

    private IUplinkMessage getFileLoadMessage(final ITcTransferFrame frame) {
        final IUplinkMessage msg;
        // this is a slight hack...we only partially recreate
        // the entire file load object...we don't give it all the file load data
        // back from all the frames
        final CommandFileLoad load = new CommandFileLoad(
                appContext, frame.getData(), 0);
        msg = msgFactory
                .createFileLoadMessage(load);

        // TODO: There should really be a configuration file
        // entry to define what value of the file type denotes
        // what type of file (for now all missions are using
        // "1" to denote a sequence file)
        if ("SEQUENCE"
                .equalsIgnoreCase(appContext.getBean(CommandProperties.class)
                        .mapFileLoadTypeToString(
                                load.getFileType()))) {
            ((ICpdUplinkMessage) msg)
                    .setOriginalFilename(load.getFileName());
        }
        return msg;
    }

    private IUplinkMessage getCommandMessage(final ITcTransferFrame frame, final VirtualChannelType vct) throws
                                                                                                         UnblockException {
        final IUplinkMessage msg;
        final String frameDataBits = BinOctHexUtility
                .toBinFromBytes(frame.getData());
        final IFlightCommand command = appContext.getBean(ICommandObjectFactory.class)
                .getCommandObjectFromBits(frameDataBits, 0);


        if (vct.equals(VirtualChannelType.HARDWARE_COMMAND)) {
            msg = msgFactory
                    .createHardwareCommandMessage(command.getDatabaseString());
        } else {
            msg = msgFactory
                    .createFlightSoftwareCommandMessage(command.getDatabaseString());
        }
        return msg;
    }
}
