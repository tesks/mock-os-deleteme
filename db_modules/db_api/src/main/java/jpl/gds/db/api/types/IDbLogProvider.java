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

public interface IDbLogProvider extends IDbQueryable {

    /**
     * Get event time.
     *
     * @return Event time
     */
    IAccurateDateTime getEventTime();

    /**
     * Get RCT.
     *
     * @return RCT
     */
    IAccurateDateTime getRct();

    /**
     * Get classification.
     *
     * @return Classification
     */
    String getClassification();

    /**
     * Get message.
     *
     * @return Message
     */
    String getMessage();

    /**
     * Get type.
     *
     * @return Type
     */
    LogMessageType getType();

    /**
     * Get severity.
     *
     * @return Severity
     */
    TraceSeverity getSeverity();

}