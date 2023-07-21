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
package jpl.gds.shared.log;

import java.io.Serializable;
import java.nio.charset.Charset;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.SyslogAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.LoggerFields;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.layout.Rfc5424Layout;
import org.apache.logging.log4j.core.layout.SyslogLayout;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.net.SocketOptions;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.EnglishEnums;

/**
 * The AMPCS log4j2 SyslogAppender implementation
 * 
 * Routes log messages to a configured syslog host/port/facility
 * 
 * @see "https://wiki.jpl.nasa.gov/display/AMPCS/Configuring+Syslog+Report+Messages"
 * 
 *
 */
@Plugin(name = "AmpcsSyslogAppender", category = "Core", elementType = "appender", printObject = true)
public class AmpcsSyslogAppender extends SyslogAppender {

    @SuppressWarnings("javadoc")
    protected AmpcsSyslogAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
            final boolean ignoreExceptions, final boolean immediateFlush, final AbstractSocketManager manager,
            final Advertiser advertiser) {
        super(name, layout, filter, ignoreExceptions, immediateFlush, manager, advertiser);
    }

    /**
     * @param host
     *            Syslog host
     * @param port
     *            Syslog port
     * @param protocolStr
     *            protocol to use
     * @param sslConfig
     *            ssl configuration
     * @param connectTimeoutMillis
     *            connection timeout
     * @param reconnectionDelayMillis
     *            reconnection delay
     * @param immediateFail
     *            immediately fail
     * @param name
     *            Appender name
     * @param immediateFlush
     *            immediate flush
     * @param ignoreExceptions
     *            ignore exceptions
     * @param facility
     *            Syslog facility
     * @param id
     *            Syslog id
     * @param mdcId
     *            mdc id
     * @param enterpriseNumber
     *            enterprise number
     * @param includeMdc
     *            if mdc should be included
     * @param mdcPrefix
     *            mdc prefix
     * @param eventPrefix
     *            event prefix
     * @param newLine
     *            newline
     * @param escapeNL
     *            escape new lines
     * @param appName
     *            application name
     * @param msgId
     *            message id
     * @param excludes
     *            exclusion field
     * @param includes
     *            inclusion field
     * @param required
     *            required attribute
     * @param format
     *            format to use
     * @param filter
     *            filter to use
     * @param config
     *            configuration
     * @param charsetName
     *            valid character set
     * @param exceptionPattern
     *            exception logging pattern
     * @param loggerFields
     *            logger fields
     * @param advertise
     *            if appender should advertise
     * @param layout
     *            configured layout
     * @return AmpcsSyslogAppender
     */
    @SuppressWarnings("deprecation")
    @PluginFactory
    public static AmpcsSyslogAppender createAppender(@PluginAttribute(value = "host", defaultString = "localhost") final String host,
                                                     @PluginAttribute(value = "port", defaultInt = 514) final int port,
                                                     @PluginAttribute(value = "protocol", defaultString = "UDP") final String protocolStr,
                                                     @PluginElement("SSL") final SslConfiguration sslConfig,
                                                     @PluginAttribute(value = "connectTimeoutMillis", defaultInt = 0) final int connectTimeoutMillis,
                                                     @PluginAliases("reconnectionDelay")// deprecated
                                                     @PluginAttribute(value = "reconnectionDelayMillis", defaultInt = 0) final int reconnectionDelayMillis,
                                                     @PluginAttribute(value = "immediateFail", defaultBoolean = true) final boolean immediateFail,
                                                     @PluginAttribute("name") final String name,
                                                     @PluginAttribute(value = "immediateFlush", defaultBoolean = true) final boolean immediateFlush,
                                                     @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions,
                                                     @PluginAttribute(value = "facility", defaultString = "LOCAL0") final Facility facility,
                                                     @PluginAttribute(value = "id", defaultString = "NoId") final String id,
                                                     @PluginAttribute(value = "mdcId", defaultString = "mdc") final String mdcId,
                                                     @PluginAttribute(value = "enterpriseNumber", defaultInt = Rfc5424Layout.DEFAULT_ENTERPRISE_NUMBER) final int enterpriseNumber,
                                                     @PluginAttribute(value = "includeMdc", defaultBoolean = false) final boolean includeMdc,
                                                     @PluginAttribute("mdcPrefix") final String mdcPrefix,
                                                     @PluginAttribute("eventPrefix") final String eventPrefix,
                                                     @PluginAttribute(value = "newLine", defaultBoolean = false) final boolean newLine,
                                                     @PluginAttribute("newLineEscape") final String escapeNL,
                                                     @PluginAttribute(value = "appName", defaultString = "AMPCS") final String appName,
                                                     @PluginAttribute("messageId") final String msgId,
                                                     @PluginAttribute("mdcExcludes") final String excludes,
                                                     @PluginAttribute(value = "mdcIncludes", defaultString = "false") final String includes,
                                                     @PluginAttribute("mdcRequired") final String required,
                                                     @PluginAttribute(value = "format", defaultString = "RFC5424") final String format,
                                                     @PluginElement("Filter") final Filter filter,
                                                     @PluginConfiguration final Configuration config,
                                                     @PluginAttribute(value = "charset", defaultString = "UTF-8") final Charset charsetName,
                                                     @PluginAttribute("exceptionPattern") final String exceptionPattern,
                                                     @PluginElement("LoggerFields") final LoggerFields[] loggerFields,
                                                     @PluginAttribute(value = "advertise", defaultBoolean = false) final boolean advertise,
                                                     @PluginElement("Layout") Layout<? extends Serializable> layout)

    {
        final Protocol protocol = EnglishEnums.valueOf(Protocol.class, protocolStr);
        final boolean useTlsMessageFormat = sslConfig != null || protocol == Protocol.SSL;

        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }

        if (RFC5424.equalsIgnoreCase(format)) {
            Rfc5424Layout.createLayout(facility, id, enterpriseNumber, includeMdc, mdcId, mdcPrefix, eventPrefix,
                                       newLine, escapeNL, appName, msgId, excludes, includes, required,
                                       exceptionPattern, useTlsMessageFormat, loggerFields, config);
        }
        else if (layout == null) {
            SyslogLayout.createLayout(facility, newLine, escapeNL, charsetName);
        }

        if (name == null) {
            LOGGER.error("No name provided for SyslogAppender");
            return null;
        }
        final AbstractSocketManager manager = createSocketManager(name, protocol, host, port, connectTimeoutMillis,
                                                                  sslConfig, reconnectionDelayMillis, immediateFail,
                                                                  layout, Constants.ENCODER_BYTE_BUFFER_SIZE,
                                                                  SocketOptions.newBuilder().build());

        return new AmpcsSyslogAppender(name, layout, filter, ignoreExceptions, immediateFlush, manager,
                                       advertise ? config.getAdvertiser() : null);
    }

    @Override
    public synchronized void append(final LogEvent event) {
        /*
         * For some reason the LogEvent has a 'null' message, but valid 'messageText'.
         * - The null message blocks this LogEvent from going to the appender destination.
         * 
         * So, transform the message to our internal AmpcsLog4jMessage then rebuild a LogEvent
         * The new LogEvent will have a non-null message object
         */
        final Message m = event.getMessage();
        final AmpcsLog4jMessage msg = (AmpcsLog4jMessage) m.getParameters()[0];
        final LogEvent evt = new Log4jLogEvent.Builder(event).setMessage(msg).build();
        super.append(evt);

    }
}
