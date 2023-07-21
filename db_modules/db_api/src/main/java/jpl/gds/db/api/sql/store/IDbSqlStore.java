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

import java.util.Collections;
import java.util.List;

import jpl.gds.db.api.IDbInteractor;
import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.shared.performance.IPerformanceData;

public interface IDbSqlStore extends IDbInteractor, IDbTableNames {
    /**
     * Supplies performance data to the performance summary publisher. Consists
     * of one queue performance object, and only if running in async mode.
     * 
     * @return list of IPerformanceData, empty if not async
     *         * @version MPCS-7168 - Added method.
     */
    public default List<IPerformanceData> getPerformanceData() {
    	return Collections.emptyList();
    }
}