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
package jpl.gds.db.api.sql.store;

import jpl.gds.shared.log.IPublishableLogMessage;

@Deprecated

public interface IDbSpecialLogging {
    /**
     * @param lm
     */
    void logErrorDirect(IPublishableLogMessage lm);

    /**
     * @param text
     */
    void logInfo(Object... text);

    /**
     * @param text
     */
    void logInfoNoDb(Object... text);

    /**
     * @param text
     */
    void logWarning(Object... text);

    /**
     * @param text
     */
    void logError(Object... text);

    /**
     * @param text
     */
    void logErrorNoDb(Object... text);

    /**
     * @param text
     */
    void logDebug(Object... text);

    /**
     * @param text
     */
    void logTrace(Object... text);

    /**
     * @param text
     */
    void logWarningNoDb(Object... text);
}
