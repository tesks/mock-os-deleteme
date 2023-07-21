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
package jpl.gds.sleproxy.server.resources;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;

import jpl.gds.sleproxy.common.resources.IMessagesResource;
import jpl.gds.sleproxy.server.messages.MessagesAppender;
import jpl.gds.sleproxy.server.messages.config.MessagesConfigManager;
import jpl.gds.sleproxy.server.time.DateTimeFormattingUtil;

/**
 * Restlet resource for the "messages" API.
 * 
 */
public class MessagesServerResource extends ServerResource implements IMessagesResource {

	/* (non-Javadoc)
	 * @see jpl.gds.sle.proxy_common.resources.IMessagesResource#toJson()
	 */
	@Override
	public final Representation toJson() {
		String fromIndexStr = getQuery().getValues("from-index");
		String fromTimeStr = getQuery().getValues("from-time");

		List<Map<String, String>> messagesList = null;
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
		Configuration loggerContextConfiguration = loggerContext.getConfiguration();
		Appender appender = loggerContextConfiguration
				.getAppender(MessagesConfigManager.INSTANCE.getMessagesLog4JAppenderName());
		MessagesAppender messagesAppender = (MessagesAppender) appender;

		if (fromIndexStr != null) {
			// Return messages from the specified index

			try {
				long fromIndex = Long.valueOf(fromIndexStr);
				messagesList = messagesAppender.getMessages(fromIndex);
			} catch (NumberFormatException nfe) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
						"'from-index' query value must be a positive integer");
				return null;
			}

		} else if (fromTimeStr != null) {
			// Return messages from the specified time

			try {
				ZonedDateTime fromTime = ZonedDateTime.ofInstant(
						LocalDateTime.parse(fromTimeStr, DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter())
								.toInstant(ZoneOffset.UTC),
						ZoneOffset.UTC);
				messagesList = messagesAppender.getMessages(fromTime);
			} catch (DateTimeParseException dtpe) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
						"'from-time' query value is not properly formatted (proper format: yyyy-DDDTHH:mm:ss[.SSS]). Error: "
								+ dtpe.getMessage());
				return null;
			}

		} else {
			// Return all messages
			messagesList = messagesAppender.getMessages();
		}

		return new JacksonRepresentation<List<Map<String, String>>>(messagesList);
	}

}