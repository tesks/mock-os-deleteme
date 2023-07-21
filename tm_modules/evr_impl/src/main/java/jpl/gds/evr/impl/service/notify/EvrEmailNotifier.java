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
package jpl.gds.evr.impl.service.notify;

import jpl.gds.common.notify.EmailNotifier;
import jpl.gds.common.notify.NotificationProperties;

/**
 * An e-mail notification class for EVR conditions.
 * 
 * @since R8
 */
public class EvrEmailNotifier extends EmailNotifier {
    
	private static final String REALTIME_EVR_EMAIL_SUBJECT_PREFIX = "MPCS Realtime EVR Notification";
	private static final String RECORDED_EVR_EMAIL_SUBJECT_PREFIX = "MPCS Recorded EVR Notification";
	private static final String TEMPLATE_DIR = "evr";
	private static final String NOTIFIER_NAME = "EVR notification";

    /**
     * Constructor.
     * 
     * @param notifyProps
     *            current notification properties bean
     * @param address
     *            e-mail address
     * @param style
     *            template style for e-mail text
     * @param host
     *            current host name
     */
    public EvrEmailNotifier(final NotificationProperties notifyProps, final String address, final String style, final String host) {
        super(notifyProps, address, style, TEMPLATE_DIR, NOTIFIER_NAME, REALTIME_EVR_EMAIL_SUBJECT_PREFIX, RECORDED_EVR_EMAIL_SUBJECT_PREFIX, host);
    }

}
