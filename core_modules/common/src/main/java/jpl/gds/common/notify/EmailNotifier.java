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

import java.io.File;
import java.util.HashMap;

import javax.mail.MessagingException;

import org.apache.velocity.Template;

import jpl.gds.common.types.IRealtimeRecordedSupport;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.email.EmailCenter;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;

/**
 * A notifier that sends e-mail for notification services.
 * 
 *
 */
public class EmailNotifier implements INotifier {
	private static final Tracer trace = TraceManager.getDefaultTracer();

	/**
	 * E-mail address to send notifications to.
	 */
	protected String address;	
	/**
	 * Template style name for e-mail content.
	 */
	protected String style;	
	/**
	 * Template manager for generating e-mail text.
	 */
	protected final TemplateManager tm;	
	/**
	 * The EmailCEnter object used to actually send e-mail.
	 */
	protected final EmailCenter ec;
	/**
	 * Return address for e-mails.
	 */
	protected final String returnAddress;	
	/**
	 * The template to be used for formatting e-mail content.
	 */
	protected Template t;	
	/**
	 * The name of the parent notification service.
	 */
    protected String notificationName;
    /**
     * The name of the template sub-directory in the template structure.
     */
    protected String templateDir;    
	/**
	 * Subject line for e-mails related to realtime data.
	 */
	protected final String realtimeEmailSubject;
	/**
	 * Subject line for e-mails related to recorded data.
	 */
	protected final String recordedEmailSubject;

	/**
	 * Constructor.
	 * 
	 * @param notifyProps
	 *            NotificationProperties object containing current configuration
	 * @param address
	 *            e-mail address to notify
	 * @param style
	 *            template style name for e-mail context
	 * @param templateSubdir
	 *            template subdirectory for e-mail templates
	 * @param notifyName
	 *            name of the parent notification service, for error messages
	 * @param realtimeSubjectPrefix
	 *            e-mail subject prefix for notifications related to realtime
	 *            data
	 * @param recordedSubjectPrefix
	 *            e-mail subject prefix for notifications related to recorded
	 *            data
	 * @param host host from which the e-mail is being issued
	 */
	protected EmailNotifier(final NotificationProperties notifyProps, final String address,
			final String style, final String templateSubdir, final String notifyName,
			final String realtimeSubjectPrefix, final String recordedSubjectPrefix,
			final String host) {
		super();

		this.t = null;
		this.address = address;
		this.style = style;
		this.notificationName = notifyName;
		this.templateDir = templateSubdir;

        // TODO: find a way to get the sse context flag here and use that for checking SSE

        String mission = GdsSystemProperties.getSystemMission();
		this.tm = new TemplateManager(mission);
		this.ec = notifyProps.getEmailCenter();
		this.returnAddress = notifyProps.getEmailReturnAddress();

		mission = mission.toUpperCase();

		final String suffix = ": " + mission + " [" + host + "]";

		this.realtimeEmailSubject = realtimeSubjectPrefix + suffix;

		this.recordedEmailSubject = recordedSubjectPrefix + suffix;

	}


	/**
	 * Gets the e-mail address in use by this notifier.
	 * 
	 * @return e-mail address
	 */
	public String getAddress() {
		return (this.address);
	}

	/**
	 * Gets the template style name for e-mail content.
	 * 
	 * @return style name
	 */
	public String getStyle() {
		return (this.style);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.common.notify.INotifier#notify(jpl.gds.shared.message.IMessage)
	 */
	@Override
	public boolean notify(final IMessage message)
    {
		return notify(message,
                      ((IRealtimeRecordedSupport)message).isRealtime() ? realtimeEmailSubject
                                           : recordedEmailSubject,
                      this.templateDir,
                      this.notificationName);
	}

	private boolean notify(final IMessage message, final String subject,
			final String templateSubDir, final String notificationName) {
		final HashMap<String, Object> context = new HashMap<String, Object>();
		message.setTemplateContext(context);

		if (this.t == null) {
			try {
				t = tm.getTemplate("email" + File.separator + templateSubDir
						+ File.separator + style);

			} catch (final TemplateException e) {
				trace.error("Could not format " + notificationName + " email: "
						+ e.getMessage());
				return (false);
			}
		}

		final String text = TemplateManager.createText(t, context);
		try {
			this.ec.sendEmail("", "", address, this.returnAddress,
					subject, text);
		} catch (final MessagingException e) {
			trace.error(
					"Could not send " + notificationName + " email: "
							+ e.getMessage());
			return (false);
		}

		return (true);
	}

}
