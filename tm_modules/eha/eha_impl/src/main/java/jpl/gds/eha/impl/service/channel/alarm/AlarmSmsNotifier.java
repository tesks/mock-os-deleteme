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

import jpl.gds.common.notify.NotificationProperties;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.shared.util.HostPortUtility;

/**
 * A text (SMS/message) notifier for channel alarms.
 * 
 * @since R8
 */
public class AlarmSmsNotifier extends AlarmEmailNotifier
{
    /** Name of template used for formatting text */
	public static final String TEXT_MESSAGE_TEMPLATE = "sms" + TemplateManager.EXTENSION;
	
    /**
     * Constructor.
     * 
     * @param props
     *            notification properties object
     * @param number
     *            phone number
     * @param provider
     *            phone service provider
     * @param host
     *            current host name
     */
	public AlarmSmsNotifier(final NotificationProperties props, final String number, final String provider, final String host) {
		super(props, number + "@" + (props.getHostForTextMessageProvider(provider) != null ?
		        props.getHostForTextMessageProvider(provider) : HostPortUtility.LOCALHOST),
		        TEXT_MESSAGE_TEMPLATE, host);
	}
}
