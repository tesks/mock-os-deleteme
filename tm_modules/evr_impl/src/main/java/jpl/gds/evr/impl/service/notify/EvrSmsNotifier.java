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

import jpl.gds.common.notify.NotificationProperties;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.shared.util.HostPortUtility;

/**
 * A text notifier for EVR conditions.
 * 
 *
 * @since R8
 */
public class EvrSmsNotifier extends EvrEmailNotifier
{
    /**
     * Default template for text output.
     */
	public static final String TEXT_MESSAGE_TEMPLATE = "sms" + TemplateManager.EXTENSION;
	
    /**
     * Constructor.
     * 
     * @param notifyProps
     *            current notification properties object
     * @param number
     *            phone number to send text to
     * @param provider
     *            telephone communications provider
     * @param host
     *            current host name
     */
	public EvrSmsNotifier(final NotificationProperties notifyProps, final String number, final String provider, final String host) {
		super(notifyProps, number + "@" + (notifyProps.getHostForTextMessageProvider(provider) != null ?
		        notifyProps.getHostForTextMessageProvider(provider) : HostPortUtility.LOCALHOST),
		        TEXT_MESSAGE_TEMPLATE, host);
	}
}
