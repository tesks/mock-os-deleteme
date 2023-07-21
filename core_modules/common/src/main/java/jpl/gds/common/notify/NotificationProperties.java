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
package jpl.gds.common.notify;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.email.EmailCenter;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.types.UnsignedInteger;


/**
 * Configuration object for alarm and EVR notification.
 *
 */
public final class NotificationProperties extends GdsHierarchicalProperties {

    /**
     * Name of the default properties file.
     */
    public static final String PROPERTY_FILE = "notification.properties";

    private static final String PROPERTY_PREFIX = "notification.";
    
    private static final String ALARM_NOTIFICATION_BLOCK = PROPERTY_PREFIX + "alarm.";
    private static final String ENABLE_REALTIME_ALARM_NOTIFICATION_PROPERTY = ALARM_NOTIFICATION_BLOCK + "enableRealtimeNotification";
    private static final String ENABLE_RECORDED_ALARM_NOTIFICATION_PROPERTY = ALARM_NOTIFICATION_BLOCK+ "enableRecordedNotification";
    private static final String ALARM_IDLEDOWN_DELAY_PROPERTY = ALARM_NOTIFICATION_BLOCK + "idledownDelay";
    
    private static final String EVR_NOTIFICATION_BLOCK = PROPERTY_PREFIX + "evr.";
    private static final String ENABLE_REALTIME_EVR_NOTIFICATION_PROPERTY = EVR_NOTIFICATION_BLOCK + "enableRealtimeNotification";
    private static final String ENABLE_RECORDED_EVR_NOTIFICATION_PROPERTY = EVR_NOTIFICATION_BLOCK + "enableRecordedNotification";
    private static final String EVR_IDLEDOWN_DELAY_PROPERTY   = EVR_NOTIFICATION_BLOCK+ "idledownDelay";

    private static final String EMAIL_BLOCK = PROPERTY_PREFIX + "email.";
    private static final String EMAIL_HOST_PROPERTY = EMAIL_BLOCK + "host";
    private static final String EMAIL_PORT_PROPERTY = EMAIL_BLOCK + "port";
    private static final String EMAIL_TRANSPORT_TYPE_PROPERTY = EMAIL_BLOCK + "transportType";
    private static final String EMAIL_RETURN_ADDRESS_PROPERTY = EMAIL_BLOCK + "returnAddress";
    private static final String EMAIL_MAX_ATTEMPTS_PROPERTY   = EMAIL_BLOCK + "maxAttempts";
    private static final String EMAIL_SEND_DELAY_PROPERTY     = EMAIL_BLOCK + "sendDelay";
    private static final String EMAIL_FAILURE_DELAY_PROPERTY  = EMAIL_BLOCK + "failureDelay";
    
    private static final String TEXT_MESSAGE_BLOCK = PROPERTY_PREFIX + "textMessage.";
    private static final String TEXT_MESSAGE_PROVIDERS_PROPERTY = TEXT_MESSAGE_BLOCK + "providers";
    private static final String TEXT_MESSAGE_HOST_PROPERTY = TEXT_MESSAGE_BLOCK + "host";
    
    private static final String DEFAULT_RETURN_ADDRESS = "ampcs_help@list.jpl.nasa.gov";
    private static final String DEFAULT_HOST = "mail.jpl.nasa.gov";
    private static final String DEFAULT_PORT = "465";
    private static final String DEFAULT_TRANSPORT_TYPE = "smtps";

    private boolean realtimeAlarmEnabled = true;
    private boolean recordedAlarmEnabled = true;

    private boolean realtimeEvrEnabled = true;
    private boolean recordedEvrEnabled = true;

    private String emailHost = DEFAULT_HOST;
    private String emailReturnAddress = DEFAULT_RETURN_ADDRESS;
    private UnsignedInteger emailPort = UnsignedInteger.valueOf(DEFAULT_PORT);
    private String emailTransportType = DEFAULT_TRANSPORT_TYPE;
    
    private List<String> textMessageProviders;

    private static final int  EMAIL_MAX_ATTEMPTS_DEFAULT   =                 5;
    private static final long EMAIL_SEND_DELAY_DEFAULT     =        3L * 1000L;
    private static final long EMAIL_FAILURE_DELAY_DEFAULT  =       10L * 1000L;
    private static final long ALARM_IDLEDOWN_DELAY_DEFAULT = 10L * 60L * 1000L;
    private static final long EVR_IDLEDOWN_DELAY_DEFAULT   = 10L * 60L * 1000L;

    private int  emailMaxAttempts   = EMAIL_MAX_ATTEMPTS_DEFAULT;
    private long emailSendDelay     = EMAIL_SEND_DELAY_DEFAULT;
    private long emailFailureDelay  = EMAIL_FAILURE_DELAY_DEFAULT;
    private long alarmIdledownDelay = ALARM_IDLEDOWN_DELAY_DEFAULT;
    private long evrIdledownDelay   = EVR_IDLEDOWN_DELAY_DEFAULT;

    private final Tracer        log;

    /**
     * Test constructor
     */
    public NotificationProperties() {
        this(new SseContextFlag(), TraceManager.getTracer(Loggers.NOTIFIER));
    }

    /**
     * Default constructor. Reads the default properties file.
     * 
     * @param sseFlag
     *            The SSE context flag
     * @param log
     *            Tracer to log with
     */
    public NotificationProperties(final SseContextFlag sseFlag, final Tracer log)
    {
        super(PROPERTY_FILE, sseFlag);
        this.textMessageProviders = new ArrayList<>(16);
        this.log = log;
        loadDefaultConfig();
    }


    /**
     * Load all the values from the default configuration file.
     */
    private void loadDefaultConfig()
    {
        this.realtimeAlarmEnabled = getBooleanProperty(ENABLE_REALTIME_ALARM_NOTIFICATION_PROPERTY, true);
        this.recordedAlarmEnabled = getBooleanProperty(ENABLE_RECORDED_ALARM_NOTIFICATION_PROPERTY, true);

        this.realtimeEvrEnabled = getBooleanProperty(ENABLE_REALTIME_EVR_NOTIFICATION_PROPERTY, true);
        this.recordedEvrEnabled = getBooleanProperty(ENABLE_RECORDED_EVR_NOTIFICATION_PROPERTY, true);

        this.emailHost = getProperty(EMAIL_HOST_PROPERTY,DEFAULT_HOST);
        this.emailPort = UnsignedInteger.valueOf(getProperty(EMAIL_PORT_PROPERTY, DEFAULT_PORT));
        this.emailTransportType = getProperty(EMAIL_TRANSPORT_TYPE_PROPERTY, DEFAULT_TRANSPORT_TYPE);
        
        /*
         * MPCS_4665 - 3/30/15. Default return address to the AMPCS help
         * address, which was already read into emailReturnAddress by the
         * constructor.
         */
        this.emailReturnAddress = getProperty(EMAIL_RETURN_ADDRESS_PROPERTY, this.emailReturnAddress);
        this.textMessageProviders = getListProperty(TEXT_MESSAGE_PROVIDERS_PROPERTY, null, ",");
       
        emailMaxAttempts   = getIntProperty( EMAIL_MAX_ATTEMPTS_PROPERTY,   EMAIL_MAX_ATTEMPTS_DEFAULT);
        emailSendDelay     = getLongProperty(EMAIL_SEND_DELAY_PROPERTY,     EMAIL_SEND_DELAY_DEFAULT);
        emailFailureDelay  = getLongProperty(EMAIL_FAILURE_DELAY_PROPERTY,  EMAIL_FAILURE_DELAY_DEFAULT);
        alarmIdledownDelay = getLongProperty(ALARM_IDLEDOWN_DELAY_PROPERTY, ALARM_IDLEDOWN_DELAY_DEFAULT);
        evrIdledownDelay   = getLongProperty(EVR_IDLEDOWN_DELAY_PROPERTY,   EVR_IDLEDOWN_DELAY_DEFAULT);
    }


    /**
     * Get host for given provider.
     *
     * @param provider Provider
     *
     * @return Host
     */
    public String getHostForTextMessageProvider(final String provider)
    {
        if (! this.textMessageProviders.contains(provider.toLowerCase()))
        {
            return null;
        }

        return getProperty(TEXT_MESSAGE_HOST_PROPERTY + "." + provider);
    }


    /**
     * Get Email host.
     *
     * @return the emailHost
     */
    public String getEmailHost()
    {
        return emailHost;
    }


    /**
     * Get Email return address.
     *
     * @return the emailReturnAddress
     */
    public String getEmailReturnAddress()
    {
        return emailReturnAddress;
    }


    /**
     * Get text message providers.
     *
     * @return the textMessageProviders
     */
    public synchronized List<String> getTextMessageProviders()
    {
        return textMessageProviders;
    }


    /**
     * Get state of R/T alarm notification.
     *
     * @return State
     */
    public boolean isRealtimeAlarmNotificationEnabled()
    {
        return this.realtimeAlarmEnabled;
    }


    /**
     * Get state of recorded alarm notification.
     *
     * @return State
     */
    public boolean isRecordedAlarmNotificationEnabled()
    {
        return this.recordedAlarmEnabled;
    }

    /**
     * Get state of R/T EVR notification.
     *
     * @return State
     */
    public boolean isRealtimeEvrNotificationEnabled()
    {
        return this.realtimeEvrEnabled;
    }


    /**
     * Get state of recorded EVR notification.
     *
     * @return State
     */
    public boolean isRecordedEvrNotificationEnabled()
    {
        return this.recordedEvrEnabled;
    }


    /**
     * Get an Email center.
     *
     * @return Email center
     */
    public EmailCenter getEmailCenter()
    {
        return(new EmailCenter(getEmailHost(),
        		               emailPort,
        		               emailTransportType,
                               emailMaxAttempts,
                               emailSendDelay,
                                emailFailureDelay, log));
    }


    /**
     * Get alarm idle-down delay time.
     *
     * @return Delay in milliseconds
     *
     */
    public long getAlarmIdledownDelay()
    {
        return alarmIdledownDelay;
    }


    /**
     * Get EVR idle-down delay time.
     *
     * @return Delay in milliseconds
     *
     */
    public long getEvrIdledownDelay()
    {
        return evrIdledownDelay;
    }
    
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }
}
