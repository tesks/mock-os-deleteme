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
package jpl.gds.watcher;

import jpl.gds.context.api.message.IEndOfContextMessage;
import jpl.gds.message.api.external.IExternalMessage;

/**
 * IMessageHandler is the interface implemented by all message handler
 * classes to be used by the message supervisor.
 */
public interface IMessageHandler {

    /**
     * Processes an incoming message. Only messages of the proper (registered)
     * message type for the handler will be received by this method, so it need
     * not check message type unless it handles more than one type.
     * @param m the message to process
     */
    public void handleMessage(IExternalMessage m);

    /**
     * Enables or displays verbose message display by the handler. If
     * enabled, the handler should display information about messages
     * processed to the console.
     * @param enable true to enable verbose output; false to disable
     */
    public void setVerbose(boolean enable);

    /**
     * Performs shutdown steps for the handler, whatever that implies.
     */
    public void shutdown();

    /**
     * Takes whatever steps the handler needs to when an end of context
     * message is seen.  The handler may choose to ignore the message
     * if it has no logic that must be executed.
     * @param m the EndOfContextMessage
     */
    public void handleEndOfContext(IEndOfContextMessage m);

    /**
     * Sets the message responder application helper instance.
     * @param app the IResponderAppHelper to set
     */
    public void setAppHelper(IResponderAppHelper app);
}
