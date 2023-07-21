/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.cfdp.processor.controller;

import java.text.DateFormat;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import jpl.gds.cfdp.message.api.ICfdpRequestReceivedMessage;
import jpl.gds.cfdp.message.api.ICfdpRequestResultMessage;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.idgen.RequestIdGenerator;
import jpl.gds.cfdp.processor.message.disruptor.MessageDisruptorManager;
import jpl.gds.cfdp.processor.message.disruptor.MessageEvent;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoUpdater;
import jpl.gds.db.api.types.IDbSessionUpdater;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.types.UnsignedLong;

/**
 * Class ARequestPublishingController
 * MPCS-10869  - 06/03/19 - Updated to retrieve session info and add it to the message
 */
public abstract class ARequestPublishingController {

    protected Tracer log;

    private DateFormat dateFormatter;

    protected String requestId;

    @Autowired
    private RequestIdGenerator requestIdGenerator;

    @Autowired
    protected ApplicationContext appContext;

    @Autowired
    private MessageDisruptorManager messageDisruptorManager;

    @Autowired
    private ConfigurationManager configurationManager;
    
    @Autowired
    protected IContextConfiguration parentContext;

    @PostConstruct
    public void init() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);
        requestId = Long.toUnsignedString(requestIdGenerator.getNewId());
        dateFormatter = TimeUtility.getFormatterFromPool();
    }

    @PreDestroy
    public void release() {
        TimeUtility.releaseFormatterToPool(dateFormatter);
    }
    
    protected ICfdpRequestReceivedMessage getRequestReceivedMessage(final String requesterId, final String httpUser, final String httpHost,
            final UnsignedLong sessionId, final String requestContent) {
        final ICfdpRequestReceivedMessage m = appContext.getBean(ICfdpRequestReceivedMessage.class);

        m.getHeader().setCfdpProcessorInstanceId(configurationManager.getInstanceId());
        m.setRequestId(requestId).setRequesterId(requesterId).setHttpUser(httpUser).setHttpHost(httpHost).setRequestContent(requestContent);
        if(sessionId != null) {
            m.setContextKey(getContextKey(sessionId));
        }
        
        return m;
    }

    protected void publishRequest(final String requesterId, final String httpUser, final String httpHost,
                                  final String requestContent) {
        publishRequest(requesterId, httpUser, httpHost, null, requestContent);
    }
    
    protected void publishRequest(final String requesterId, final String httpUser, final String httpHost,
            final UnsignedLong sessionId, final String requestContent) {
        final ICfdpRequestReceivedMessage m = getRequestReceivedMessage(requesterId, httpUser, httpHost, sessionId, requestContent);
        messageDisruptorManager.getDisruptor().getRingBuffer().publishEvent(MessageEvent::translate, m);
    }

    protected ICfdpRequestResultMessage getRequestResultMessage(final boolean rejected, final UnsignedLong sessionId, final String requestContent) {
        final ICfdpRequestResultMessage m = appContext.getBean(ICfdpRequestResultMessage.class);
        m.getHeader().setCfdpProcessorInstanceId(configurationManager.getInstanceId());
        if(sessionId != null) {
            m.setContextKey(getContextKey(sessionId));
        }
        
        return m;
    }
    
    protected void publishResult(final boolean rejected, final String requestContent) {
        this.publishResult(rejected, null, requestContent);
    }
    
    protected void publishResult(final boolean rejected, final UnsignedLong sessionId, final String requestContent) {
        final ICfdpRequestResultMessage m = getRequestResultMessage(rejected, sessionId, requestContent);
        messageDisruptorManager.getDisruptor().getRingBuffer().publishEvent(MessageEvent::translate,
                m.setRequestId(requestId).setRejected(rejected).setResultContent(requestContent));
    }
    
    protected IContextKey getContextKey(final UnsignedLong sessionId) {
        
        if(sessionId == null || sessionId.longValue() <= 0) {
            return null;
        }

        // Downlink session exists

        final IDbSqlFetch tsf = appContext.getBean(IDbSqlFetchFactory.class).getSessionFetch(false);
        try {
            final IContextConfiguration sessionConfig = new SessionConfiguration(
                    parentContext.getMissionProperties(),
                    parentContext.getConnectionConfiguration().getConnectionProperties(),
                    false);

            // Copied from jpl.gds.tcapp.app.AbstractUplinkApp.queryForSession
            final IDbSessionInfoFactory dbSessionInfoFactory =
                    appContext.getBean(IDbSessionInfoFactory.class);
            final IDbSessionInfoUpdater tsi = dbSessionInfoFactory.createQueryableUpdater();
            tsi.addSessionKey(sessionId.longValue());

            final List<? extends IDbRecord> testSessions;
            testSessions = tsf.get(tsi, null, 1, (IDbOrderByType) null);

            if (testSessions.isEmpty()) {
                tsf.close();
                throw new ParseException("Downlink transaction specified a non-existent session number: " +
                        sessionId);
            }

            final IDbSessionUpdater dsc = (IDbSessionUpdater) testSessions.get(0);
            dsc.setIntoContextConfiguration(sessionConfig);

            // Set the context and session hierarchy
            sessionConfig.getContextId().getContextKey().setParentNumber(parentContext.getContextId().getNumber());
            sessionConfig.getContextId().getContextKey().setParentHostId(parentContext.getContextId().getHostId());

            return sessionConfig.getContextId().getContextKey();

        } catch (DatabaseException | ParseException e) {
            //do nothing
            tsf.close();
            return null;
        } finally {
            tsf.close();
        }
    }

}