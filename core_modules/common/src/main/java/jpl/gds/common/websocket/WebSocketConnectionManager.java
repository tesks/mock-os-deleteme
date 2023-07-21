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

package jpl.gds.common.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jpl.gds.shared.log.AmpcsLog4jMessage;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.metadata.context.IContextKey;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to manage websocket connections at the service level,
 * for example: TI server, TP server and CFDP Processor.
 *
 * <p>It is intended to be a Singleton within a Spring Application Context.
 * Maintains a list of connected WebSocketSession objects and handles the
 * distribution of "messages" to the connected clients.
 *
 */
public class WebSocketConnectionManager implements IWebSocketConnectionManager {

    /** List of connected web socket sessions */
    private List<WebSocketSession> sessionList;

    /** The Tracer object */
    private Tracer trace;

    /**
     * Constructor
     * @param trace The Tracer object
     */
    public WebSocketConnectionManager(final Tracer trace) {
        this.sessionList = new ArrayList<>();
        this.trace = trace;
    }

    @Override
    public void handleLogMessage(final AmpcsLog4jMessage msg) {

        final LogMessage logMessage = new LogMessage(
                msg.getContextKey().toString(),
                msg.getSeverity().toString(),
                msg.getEventTimeString(),
                msg.getMessage());

        String jsonString;
        try {
            jsonString = logMessage.toJsonString();
        } catch (JsonProcessingException e) {
            trace.error("Unable to convert log message to JSON sting: ", e.getMessage());
            return;
        }

        // Broadcast log message to all connected clients
        for (final WebSocketSession session : sessionList) {
            try {
                synchronized (session) {
                    if (session.isOpen()) {
                        trace.debug("Sending following message to connected websocket clients: ", jsonString);
                        session.sendMessage(new TextMessage(jsonString));
                    }
                }
            } catch (IOException e) {
                trace.error("Encountered the following unexpected exception:", e.getMessage(),
                        "when sending log message: ", jsonString, " to WebSocketSession: ", session.toString());
            }
        }
    }

    @Override
    public void addSession(final WebSocketSession session) {
        sessionList.add(session);
    }

    @Override
    public void removeSession(final WebSocketSession session) {
        sessionList.remove(session);
    }

    @Override
    public List<WebSocketSession> getSessions() {
        return sessionList;
    }


    /**
     * A simple container POJO used for the transfer of log events over
     * the Websocket connection.
     */
    private static class LogMessage {
        private String contextId;
        private String logLevel;
        private String dataTime;
        private String message;

        /**
         * Log message constructor
         *
         * @param contextId The context id associated with the log event
         * @param logLevel The log event level
         * @param dataTime The log event data & time stamp
         * @param message The log event message
         */
        public LogMessage(final String contextId, final String logLevel, final String dataTime, final String message) {
            this.contextId = contextId;
            this.logLevel = logLevel;
            this.dataTime = dataTime;
            this.message = message;
        }

        /**
         * Gets the context id of the log event
         *
         * @return the context id
         */
        public String getContextId() {
            return contextId;
        }

        /**
         * Sets the context id in string form
         *
         * @param contextId the context id
         */
        public void setContextId(final String contextId) {
            this.contextId = contextId;
        }

        /**
         * Gets the log level
         *
         * @return the log level
         */
        public String getLogLevel() {
            return logLevel;
        }

        /**
         * Sets the log level
         *
         * @param logLevel the log level
         */
        public void setLogLevel(final String logLevel) {
            this.logLevel = logLevel;
        }

        /**
         * Gets the log data and time stamp
         *
         * @return the data and time of log event
         */
        public String getDataTime() {
            return dataTime;
        }

        /**
         * Sets the log data time
         *
         * @param dataTime
         */
        public void setDataTime(final String dataTime) {
            this.dataTime = dataTime;
        }

        /**
         * Gets the log message
         *
         * @return the log message
         */
        public String getMessage() {
            return message;
        }

        /**
         * Sets the log message
         *
         * @param message the log message
         */
        public void setMessage(final String message) {
            this.message = message;
        }

        /**
         * Convert this object to a JSON String
         *
         * @return the JSON String
         * @throws JsonProcessingException
         */
        public String toJsonString() throws JsonProcessingException {
            return new ObjectMapper().writeValueAsString(this);
        }
    }
}
