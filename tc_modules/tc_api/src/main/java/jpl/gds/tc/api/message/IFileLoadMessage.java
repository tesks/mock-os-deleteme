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

import jpl.gds.tc.api.ICommandFileLoad;

public interface IFileLoadMessage extends ICpdUplinkMessage {

    /**
     * Returns the file load that caused this message to be generated.
     * 
     * @return the CommandFileLoad object
     */
    public ICommandFileLoad getFileLoad();

    /**
     * Sets the file load that caused this message to be generated.
     * 
     * @param load The file load that caused this message to be generated
     */
    public void setFileLoad(ICommandFileLoad load);

}