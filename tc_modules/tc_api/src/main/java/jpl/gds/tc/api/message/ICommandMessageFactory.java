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
package jpl.gds.tc.api.message;

import java.util.List;

import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.types.UnsignedLong;
import jpl.gds.tc.api.ICommandFileLoad;
import jpl.gds.tc.api.ICpdUplinkStatus;
import jpl.gds.tc.api.echo.ICommandEchoMessage;
import jpl.gds.tc.api.gui.IUplinkGuiLogMessage;

public interface ICommandMessageFactory {

    IFileLoadMessage createFileLoadMessage(ICommandFileLoad load);

    IFlightCommandMessage createFlightSoftwareCommandMessage(String commandString);

    IFlightCommandMessage createHardwareCommandMessage(String commandString);

    IRawUplinkDataMessage createRawUplinkDataMessage(String origFilename);

    ICpdUplinkMessage createScmfMessage(String origScmfFile);

    IFlightCommandMessage createSequenceDirectiveMessage(String commandString);

    ITransmittableCommandMessage createSseCommandMessage(String commandString);

    ICpdUplinkStatusMessage createCpdUplinkStatusMessage(ICpdUplinkStatus status);
    
    IUplinkGuiLogMessage createUplinkGuiLogMessage(String message);
    
    IMessage createClearUplinkGuiLogMessage();
    
    IInternalCpdUplinkStatusMessage createInternalCpdUplinkStatusMessage(final List<ICpdUplinkStatus> statuses,
            final List<ICpdUplinkStatus> deltas);
    
    ICommandEchoMessage createCommandEchoMessage(final byte[] buff, final int offset, final int byteLen);

    /**
     *
     * @param message command message string to log (response from CFDP server request)
     * @param successful whether or not the CFDP request was successful
     * @param sessionId the session ID to log this command with
     * @return a CFDP command message
     */
    ITransmittableCommandMessage createCfdpCommandMessage(final String message, final boolean successful,
                                                          final UnsignedLong sessionId);

}