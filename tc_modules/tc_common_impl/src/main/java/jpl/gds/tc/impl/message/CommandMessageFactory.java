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
package jpl.gds.tc.impl.message;

import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.types.UnsignedLong;
import jpl.gds.tc.api.ICommandFileLoad;
import jpl.gds.tc.api.ICpdUplinkStatus;
import jpl.gds.tc.api.echo.ICommandEchoMessage;
import jpl.gds.tc.api.gui.IUplinkGuiLogMessage;
import jpl.gds.tc.api.message.*;
import jpl.gds.tc.impl.echo.CommandEchoMessage;

import java.util.List;

public class CommandMessageFactory implements ICommandMessageFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public IFileLoadMessage createFileLoadMessage(final ICommandFileLoad load) {
        return new FileLoadMessage(load);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IFlightCommandMessage createFlightSoftwareCommandMessage(
            final String commandString) {
        return new FlightSoftwareCommandMessage(commandString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IFlightCommandMessage createHardwareCommandMessage(
            final String commandString) {
        return new HardwareCommandMessage(commandString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IRawUplinkDataMessage createRawUplinkDataMessage(
            final String origFilename) {
        return new RawUplinkDataMessage(origFilename);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICpdUplinkMessage createScmfMessage(final String origScmfFile) {
        return new ScmfMessage(origScmfFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IFlightCommandMessage createSequenceDirectiveMessage(
            final String commandString) {
        return new SequenceDirectiveMessage(commandString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITransmittableCommandMessage createSseCommandMessage(final String commandString) {
        return new SseCommandMessage(commandString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICpdUplinkStatusMessage createCpdUplinkStatusMessage(
            final ICpdUplinkStatus status) {
        return new CpdUplinkStatusMessage(status);
    }

    @Override
    public IUplinkGuiLogMessage createUplinkGuiLogMessage(final String message) {
        return new UplinkGuiLogMessage(message);
    }

    @Override
    public IMessage createClearUplinkGuiLogMessage() {
        return new ClearUplinkGuiLogMessage();
    }

    @Override
    public IInternalCpdUplinkStatusMessage createInternalCpdUplinkStatusMessage(final List<ICpdUplinkStatus> statuses,
            final List<ICpdUplinkStatus> deltas) {
        return new InternalCpdUplinkStatusMessage(statuses, deltas);
    }
    
    @Override
    public ICommandEchoMessage createCommandEchoMessage( byte[] buff, int offset, int byteLen) {
        return new CommandEchoMessage(buff, offset, byteLen);
    }

    @Override
    public ITransmittableCommandMessage createCfdpCommandMessage(String message, final boolean success, final UnsignedLong sessionId) {
        return new FileCfdpMessage(message, success, sessionId);

    }

}
