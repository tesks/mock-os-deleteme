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

package jpl.gds.db.api.rest;

import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.fetch.ILogFetch;
import jpl.gds.db.api.sql.order.ILogOrderByType;
import jpl.gds.db.api.sql.order.IOrderByTypeFactory;
import jpl.gds.db.api.sql.order.ISessionOrderByType;
import jpl.gds.db.api.sql.order.OrderByType;
import jpl.gds.db.api.types.IDbContextInfoFactory;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbLogProvider;
import jpl.gds.db.api.types.IDbSessionFactory;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoUpdater;
import jpl.gds.db.api.types.IDbSessionProvider;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.string.StringResponse;
import org.springframework.context.ApplicationContext;

import java.util.Collections;
import java.util.List;

/**
 * Java object to encapsulate common DB operations needed by REST API's
 *
 */
public class RestfulDbHelper implements IRestfulDbHelper {

    private final int DEFAULT_SESSION_FETCH_BATCH_SIZE = 50;

    private final IStatusMessageFactory msgFactory;

    private final IDbSqlFetchFactory    fetchFactory;
    private final IDbSessionInfoFactory dbSessionInfoFactory;
    private final IDbContextInfoFactory dbContextInfoFactory;
    private final IOrderByTypeFactory   orderByTypeFactory;
    private final IDbSessionFactory     dbSessionFactory;
    private final MissionProperties     missionProperties;
    private final ConnectionProperties  connectionProperties;

    /**
     * Constructor
     *
     * @param appContext The spring application context
     */
    public RestfulDbHelper(final ApplicationContext appContext) {
        this.msgFactory = appContext.getBean(IStatusMessageFactory.class);
        this.fetchFactory = appContext.getBean(IDbSqlFetchFactory.class);
        this.dbSessionInfoFactory = appContext.getBean(IDbSessionInfoFactory.class);
        this.dbContextInfoFactory = appContext.getBean(IDbContextInfoFactory.class);
        this.orderByTypeFactory = appContext.getBean(IOrderByTypeFactory.class);
        this.dbSessionFactory = appContext.getBean(IDbSessionFactory.class);

        this.missionProperties = appContext.getBean(MissionProperties.class);
        this.connectionProperties = appContext.getBean(ConnectionProperties.class);
    }

    @Override public StringResponse logMessage(final String level, final String message, final Tracer log)
            throws Exception {
        TraceSeverity ts = TraceManager.mapMtakLevel(level);
        boolean okToPublish = (ts.equals(TraceSeverity.TRACE) || ts.equals(TraceSeverity.DEBUG)) ?
                log.isEnabledFor(ts) : true;
        if (okToPublish) {
            log.log(msgFactory.createPublishableLogMessage(ts, message, LogMessageType.REST));
            return new StringResponse("Queued " + message + " for insertion");
        }
        else {
            throw new Exception(level + " is not a publishable level");
        }
    }

    @Override public List<IDbLogProvider> getLogsFromContext(final Long contextKey, final String contextHost)
            throws DatabaseException {
        return getLogsFromContext(contextKey, contextHost, -1);

    }

    @Override public List<IDbLogProvider> getLogsFromContext(final Long contextKey, final String contextHost,
                                                             final int limit)
            throws DatabaseException {
        return getLogs(contextKey, contextHost, dbContextInfoFactory.createQueryableProvider(), true, limit);

    }

    @Override public List<IDbLogProvider> getLogsFromSession(final Long sessionKey, final String sessionHost)
            throws DatabaseException {
        return getLogsFromSession(sessionKey, sessionHost, -1);
    }

    @Override public List<IDbLogProvider> getLogsFromSession(final Long sessionKey, final String sessionHost,
                                                             final int limit)
            throws DatabaseException {
        return getLogs(sessionKey, sessionHost, dbSessionInfoFactory.createQueryableProvider(), false, limit);
    }

    private List<IDbLogProvider> getLogs(final Long key, final String host, final IDbContextInfoProvider dbInfo,
                                         final boolean fetchFromContext, final int limit) throws DatabaseException {

        final ILogFetch dbFetch = fetchFactory.getLogFetch();

        dbInfo.getHostPatternList().add(host);
        dbInfo.getSessionKeyList().add(key);

        // MPCS-11340 - Update to support all log classifications
        final Object[] params = new Object[5];
        params[0] = TraceSeverity.INFO.toString() + ',' + TraceSeverity.WARN.toString() + ',' + TraceSeverity.ERROR.toString();

        // MPCS-11890 - if limited, add order by descending event time
        if (limit > 0) {
            params[3] = orderByTypeFactory
                    .getOrderByType(OrderByType.LOG_ORDER_BY, ILogOrderByType.EVENT_TIME_DESC_TYPE);
        }

        List<IDbLogProvider> logs;
        try {
            logs = (List<IDbLogProvider>) dbFetch.get(dbInfo, null, 50, fetchFromContext, params);

            // MPCS-11890 - if limited, and limit has been reached, reverse/truncate/return. else keep going.
            if (!checkAndTruncateLogs(limit, logs)) {
                List<IDbLogProvider> query = (List<IDbLogProvider>) dbFetch.getNextResultBatch();
                while (!query.isEmpty()) {
                    logs.addAll(query);

                    // MPCS-11890 - if limited, and limit has been reached, reverse/truncate/return. else keep going.
                    if (checkAndTruncateLogs(limit, logs)) {
                        break;
                    }

                    query = (List<IDbLogProvider>) dbFetch.getNextResultBatch();
                }
            }
        }
        catch (final Exception e) {
            dbFetch.close();
            throw new DatabaseException(
                    "An exception occurred getting logs for key " + key + " and host " + host + " " + ExceptionTools.getMessage(e));
        }
        dbFetch.close();

        return logs;
    }

    /**
     * MPCS-11890 - Input list is assumed to be in order of descending event time. If limit has been reached,
     * the list is truncated and reversed to be the latest messages in order of ascending event time. The input list
     * is directly modified.
     *
     * @param limit log limit. positive integer indicates limit, 0 or negative indicates unlimited
     * @param logs  list of logs.
     * @return boolean, true if logs were truncated, false if not
     */
    private boolean checkAndTruncateLogs(int limit, List<IDbLogProvider> logs) {
        if (limit <= 0) {
            return false;
        } else if (logs.size() > limit) {
            logs.subList(limit, logs.size()).clear();
        }
        Collections.reverse(logs);
        return true;
    }

    @Override
    public List<IDbSessionProvider> getSessions(final Long key, final String host) throws DatabaseException {
        final IDbSessionInfoUpdater dbSessionInfo = dbSessionInfoFactory.createQueryableUpdater();
        dbSessionInfo.setSessionKey(key);
        dbSessionInfo.setHostPattern(host);

        final ISessionOrderByType startTimeOrderDesc = (ISessionOrderByType) orderByTypeFactory
                .getOrderByType(OrderByType.SESSION_ORDER_BY, ISessionOrderByType.ID_DESC_TYPE);

        final IDbSqlFetch sessionFetch = fetchFactory.getSessionFetch();

        List<IDbSessionProvider> databaseSessions = null;
        try {
            databaseSessions = (List<IDbSessionProvider>) sessionFetch.get(dbSessionInfo, null,
                                                                           DEFAULT_SESSION_FETCH_BATCH_SIZE, startTimeOrderDesc);

            List<IDbSessionProvider> query = (List<IDbSessionProvider>) sessionFetch.getNextResultBatch();
            while (!query.isEmpty()) {
                databaseSessions.addAll(query);
                query = (List<IDbSessionProvider>) sessionFetch.getNextResultBatch();
            }
        }
        catch (final DatabaseException e) {
            sessionFetch.close();
            throw new DatabaseException("Encountered error while fetching sessions from database: " + ExceptionTools.getMessage(e));
        }
        sessionFetch.close();
        return databaseSessions;
    }



    @Override
    public SessionConfiguration getSessionConfigurationFromDatabase(final Long key, final String host)
            throws DatabaseException {
        // TODO: Consolidate code here from
        // AbstractTelemetryApp
        // AbstractUplinkApp
        // Automanager
        // CFDP (not exactly sure where that lives )

        List<IDbSessionProvider> databaseSessions = getSessions(key, host);

        IDbSessionProvider matchingSession = null;
        while (databaseSessions != null && !databaseSessions.isEmpty()) {
                for (final IDbSessionProvider ds : databaseSessions) {
                    final TelemetryConnectionType telemConnType = ds.getConnectionType();
                    if ((telemConnType != null) && !telemConnType.equals(TelemetryConnectionType.UNKNOWN)) {
                        matchingSession = ds;
                        break;
                    }
                }
        }

        if (matchingSession == null) {
            throw new DatabaseException("No sessions found in the database.");
        }

        final SessionConfiguration sessionConfig = new SessionConfiguration(missionProperties,
                                                                            connectionProperties, false);


        dbSessionFactory.convertProviderToUpdater(matchingSession).setIntoContextConfiguration(sessionConfig);
        return sessionConfig;

    }
}