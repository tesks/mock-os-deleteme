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
package jpl.gds.db.api.types;

import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.time.IAccurateDateTime;

public interface IDbLogUpdater extends IDbLogProvider {

    /**
     * Set event time.
     *
     * @param eventTime Event time
     */
    void setEventTime(IAccurateDateTime eventTime);

    /**
     * Set RCT.
     *
     * @param rct RCT
     */
    void setRct(IAccurateDateTime rct);

    /**
     * Set classification.
     *
     * @param classification Classification
     */
    void setClassification(String classification);

    /**
     * Set message.
     *
     * @param message Message
     */
    void setMessage(String message);

    /**
     * Set type.
     *
     * @param type Type
     */
    void setType(LogMessageType type);

    /**
     * Set severity.
     *
     * @param severity Severity
     */
    void setSeverity(TraceSeverity severity);

}