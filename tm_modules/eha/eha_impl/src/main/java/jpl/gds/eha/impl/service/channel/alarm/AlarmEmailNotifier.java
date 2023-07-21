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
package jpl.gds.eha.impl.service.channel.alarm;

import jpl.gds.common.notify.EmailNotifier;
import jpl.gds.common.notify.NotificationProperties;

/**
 * An email notifier for channel alarms.
 * 
 * @since R8
 *
 */
public class AlarmEmailNotifier extends EmailNotifier {

    private static final String REALTIME_ALARM_EMAIL_SUBJECT_PREFIX =
            "MPCS Realtime Alarm Notification";
private static final String RECORDED_ALARM_EMAIL_SUBJECT_PREFIX =
            "MPCS Recorded Alarm Notification";

      private static final String TEMPLATE_DIR = "alarm";
      private static final String NOTIFIER_NAME = "Alarm notification";


    /**
     * Constructor.
     * 
     * @param notifyProps
     *            notification properties object
     * @param email
     *            email address
     * @param style
     *            template style for email text
     * @param host
     *            current host name
     */
    public AlarmEmailNotifier(final NotificationProperties notifyProps,
    		  final String email, final String style, final String host) {
          super(notifyProps, email, style, TEMPLATE_DIR, NOTIFIER_NAME, REALTIME_ALARM_EMAIL_SUBJECT_PREFIX, RECORDED_ALARM_EMAIL_SUBJECT_PREFIX, host);
      }

}
