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

import jpl.gds.shared.message.IMessage;
import jpl.gds.tc.api.command.IDatabaseArchivableCommand;

public interface IUplinkMessage extends IMessage, IDatabaseArchivableCommand {

    /** The original file associated with this message (if there was one) */
    public static final String DEFAULT_COMMANDED_SIDE = "";

    /**
     * Getter for commanded side.
     * 
     * @return Commanded side
     */
    public String getCommandedSide();

    /**
     * Setter for commanded side.
     * 
     * @param cs Commanded side
     */
    public void setCommandedSide(String cs);

}