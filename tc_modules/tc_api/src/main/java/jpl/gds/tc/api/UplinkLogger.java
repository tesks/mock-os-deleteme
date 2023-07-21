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
package jpl.gds.tc.api;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.SslConfigurator;
import org.springframework.context.ApplicationContext;

import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.security.ssl.ISslConfiguration;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;


/**
 * This class is a logging utility that will log messages using the provided
 * Tracer and optionally publish an PublishableLogMessage as a
 * LogMessageType.UPLINK type.
 * 
 * @since AMPCS R5
 * 
 * MPCS-7723 - 10/24/16 - Updated Jersey 1 to Jersey 2.
 */
public class UplinkLogger {
    /** The logger to use to log messages */
    private final Tracer logger;

    /** The message context to subscribe and publish to */
    private final IMessagePublicationBus context;

    /**
     * Flag to indicate whether or not to publish an PublishableLogMessage to the
     * internal message context
     */
    private final boolean publish;

    /** The log service URL */
    private final String logServiceUrl;

    /** The log level parameter */
    private final String logLevelParam;

    /** The log message parameter */
    private final String logMessageParam;

    /** Jersey client for REST calls */
    private Client jerseyClient;

    private final IStatusMessageFactory  statusMessageFactory;
    
    private final ISslConfiguration springSslConfig;

    /**
     * Constructor
     * 
     * @param appContext
     *            The current application context
     * 
     * @param tracer
     *            the Tracer object to use to log messages
     * @param publish
     *            true to have this logger publish an PublishableLogMessage to
     *            internal message context for each log, false otherwise.
     */
    public UplinkLogger(final ApplicationContext appContext, final Tracer tracer, final boolean publish) {
        this(appContext, tracer, publish, null, null, null);
    }

    /**
     * Constructor for logging with a log service. The log servlet is an
     * internal restlet that handles centralized logging for an integrated log
     * file from many processes
     * 
     * @param tracer
     *            the Tracer object to use to log messages
     * @param publish
     *            true to have this logger publish an PublishableLogMessage to
     *            internal message context for each log, false otherwise.
     * @param logServiceUrl
     *            the log service URL
     * @param logLevelParam
     *            the log level parameter
     * @param logMessageParam
     *            the log message parameter
     */
    public UplinkLogger(final ApplicationContext appContext, Tracer tracer, final boolean publish,
            final String logServiceUrl, final String logLevelParam,
            final String logMessageParam) {
        this.context = appContext.getBean(IMessagePublicationBus.class);
        this.statusMessageFactory = appContext.getBean(IStatusMessageFactory.class);
        this.springSslConfig = appContext.getBean(ISslConfiguration.class);

        if (tracer == null) {
            tracer = TraceManager.getTracer(appContext, Loggers.AUTO_UPLINK);

        }

        this.logger = tracer;
        this.publish = publish;
        this.logServiceUrl = logServiceUrl;
        this.logLevelParam = logLevelParam;
        this.logMessageParam = logMessageParam;


        if (this.logServiceUrl != null) {
            // MPCS-9956 6/29/18: HTTPS support
            final ClientBuilder clientBuilder = ClientBuilder.newBuilder();
            springSslConfig.setSecure(this.logServiceUrl.contains("https"));

            if (springSslConfig.isSecure()) {
                logger.debug(springSslConfig);
                final SslConfigurator sslConfig = SslConfigurator.newInstance()
                                                                 .trustStoreFile(springSslConfig.getTruststorePath())
                                                                 .trustStorePassword(springSslConfig.getTruststorePassword())
                                                                 .keyStoreFile(springSslConfig.getKeystorePath())
                                                                 .keyPassword(springSslConfig.getKeystorePassword());

                final SSLContext sslContext = sslConfig.createSSLContext();
                clientBuilder.sslContext(sslContext);
                clientBuilder.hostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());
            }
            this.jerseyClient = clientBuilder.build();
        }

    }

    /**
     * @see jpl.gds.shared.log.Tracer#trace(Object)
     */
    public void trace(final Object message) {
        this.publishLogMessage(message, TraceSeverity.TRACE);
    }

    /**
     * @see jpl.gds.shared.log.Tracer#debug(Object)
     */
    public void debug(final Object message) {
        this.publishLogMessage(message, TraceSeverity.DEBUG);
    }

    /**
     * @see jpl.gds.shared.log.Tracer#warn(Object)
     */
    public void warn(final Object message) {
        this.publishLogMessage(message, TraceSeverity.WARNING);
    }

    /**
     * @see jpl.gds.shared.log.Tracer#info(Object)
     */
    public void info(final Object message) {
        this.publishLogMessage(message, TraceSeverity.INFO);
    }

    /**
     * @see jpl.gds.shared.log.Tracer#error(Object)
     */
    public void error(final Object message) {
        this.publishLogMessage(message, TraceSeverity.ERROR);
    }

    /**
     * @see jpl.gds.shared.log.Tracer#fatal(Object)
     */
    public void fatal(final Object message) {
        this.publishLogMessage(message, TraceSeverity.FATAL);
    }

    /**
     * @see jpl.gds.shared.log.Tracer#trace(java.lang.Object,
     *      java.lang.Throwable)
     */
    public void trace(final Object message, final Throwable t) {
        this.publishLogMessage(message, TraceSeverity.TRACE, t);
    }

    /**
     * @see jpl.gds.shared.log.Tracer#debug(java.lang.Object,
     *      java.lang.Throwable)
     */
    public void debug(final Object message, final Throwable t) {
        this.publishLogMessage(message, TraceSeverity.DEBUG, t);
    }

    /**
     * @see jpl.gds.shared.log.Tracer#warn(java.lang.Object,
     *      java.lang.Throwable)
     */
    public void warn(final Object message, final Throwable t) {
        this.publishLogMessage(message, TraceSeverity.WARNING, t);
    }

    /**
     * @see jpl.gds.shared.log.Tracer#info(java.lang.Object,
     *      java.lang.Throwable)
     */
    public void info(final Object message, final Throwable t) {
        this.publishLogMessage(message, TraceSeverity.INFO, t);
    }

    /**
     * @see jpl.gds.shared.log.Tracer#error(java.lang.Object,
     *      java.lang.Throwable)
     */
    public void error(final Object message, final Throwable t) {
        this.publishLogMessage(message, TraceSeverity.ERROR, t);
    }

    /**
     * @see jpl.gds.shared.log.Tracer#fatal(java.lang.Object,
     *      java.lang.Throwable)
     */
    public void fatal(final Object message, final Throwable t) {
        this.publishLogMessage(message, TraceSeverity.FATAL, t);
    }

    /**
     * @see jpl.gds.shared.log.Tracer#getLevel()
     */
    //public TraceSeverity getLevel() {
    //    return this.logger.getLevel();
    //}

    /**
     * Publish an external log message to the internal message context
     * 
     * @param message the message to publish
     * @param logLevel the message severity
     */
    public void publishLogMessage(final Object message,
            final TraceSeverity logLevel) {
        this.publishLogMessage(message, logLevel, null);
    }

    private void publishLogMessage(final Object message,
            final TraceSeverity logLevel, final Throwable t) {

        // MPCS-9932 6/20/18: Let logging config determine if it goes to db/bus
        if (t != null) { 
            this.logger.log(Markers.UPLINK, logLevel, message, t);
        } else { 
            final IPublishableLogMessage elm = statusMessageFactory.createPublishableLogMessage(logLevel,
                                                                             message.toString(),
                                                                             LogMessageType.UPLINK);
            this.logger.log(elm);

            // MPCS-9988 8/22/18: Re-added R7 checks to NOT publish DEBUG/TRACE messages
            // unless logging is enabled for that level
            if ((logLevel.equals(TraceSeverity.TRACE) || logLevel.equals(TraceSeverity.DEBUG))) {
                if (logger.isEnabledFor(logLevel)) {
                    context.publish(elm);
                }
            }
            else { // Always publish if it's not TRACE or DEBUG
                this.context.publish(elm);
            }
        }
        

        if (this.logServiceUrl != null) {
            // send log to log service
        	
			/*
			 * MPCS-7723 11/01/16 - because message strings can have invalid
			 * characters, like =, {, }, etc, the string needs to be encoded
			 * into UTF-8. Spaces get converted to plus signs, however, the log
			 * service may not decode this properly. Let the WebTarget handle
			 * them for transmission.
			 */
        	String encodedMessage;
        	try {
				/*
				 * Not sure if it's an issue with WebTarget or the log service
				 * destination, but there is a minor issue with
				 * encoding/decoding. In URLs + and %20 are both "acceptable"
				 * substitutes for spaces. Somewhere along the line either the
				 * space is getting double encoded and only being decoded once
				 * or is not being decoded at all. Using the replace is a bit of
				 * a kludge, but the alternative is to include Guava imports on
				 * here and in the various apps that use this class just to
				 * verify the text is percent encoded once in this single place.
				 */
				encodedMessage = URLEncoder.encode(message.toString(), "UTF-8").replace('+', ' ');
			} catch (final UnsupportedEncodingException e1) {
				encodedMessage = message.toString();
			}
        	
        	
            final WebTarget resource = 
            		this.jerseyClient
				        .target(this.logServiceUrl)
				        .queryParam(this.logLevelParam,
				                logLevel.getValueAsString())
				        .queryParam(this.logMessageParam,
				        		encodedMessage);
            

            // MPCS-8576 12/20/2016: Added accept() header to HTML POST and changed readEntity to use InputStream
            // MPCS-9956 06/29/2018: Updated accept() header for JSON response
            final Response response = resource.request().accept(MediaType.APPLICATION_JSON).post(Entity.entity(MediaType.TEXT_HTML, MediaType.TEXT_HTML_TYPE), Response.class);
            this.logger.debug(response.readEntity(InputStream.class).toString());
        }
    }
}
