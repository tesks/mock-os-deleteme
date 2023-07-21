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
package jpl.gds.sleproxy.server.messages;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import jpl.gds.sleproxy.server.messages.config.EMessagesBoundingScheme;
import jpl.gds.sleproxy.server.messages.config.MessagesConfigManager;
import jpl.gds.sleproxy.server.time.DateTimeFormattingUtil;
import jpl.gds.sleproxy.server.websocket.MessageDistributor;

/**
 * MessagesAppender is a Log4J2 log appender. Because chill_sle_proxy needs to
 * keep log messages in its memory and provide them as responses to the
 * "messages" REST API calls, this appender is notified of every (or filtered on
 * level) log message generated by chill_sle_proxy. A copy of the message which
 * has been logged can then also be saved in memory. The list of in-memory
 * messages is then provided to RESTful clients using the "messages" API.
 * 
 */
@Plugin(name = "SLEProxyMessagesAppender", category = "Core", elementType = "appender", printObject = true)
public final class MessagesAppender extends AbstractAppender {

	/**
	 * If bounding scheme is by size, a circular buffer, so that we cap the size
	 * of log messages retained in memory. If bounding scheme is by age, a
	 * linked list.
	 */
	private final AbstractCollection<Pair<Long, LogEvent>> messagesQueue;

	/**
	 * Counter for keeping track of the last index value used for each of the
	 * log messages.
	 */
	private AtomicLong lastMessageIndex;

	/**
	 * Construct the appender using the provided name and layout.
	 * 
	 * @param name
	 *            Name of the appender
	 * @param layout
	 *            Layout to configure the appender with (Log4J2 stuff)
	 */
	public MessagesAppender(final String name, final Layout<? extends Serializable> layout) {
		super(name, null, layout);

		if (MessagesConfigManager.INSTANCE.getMessagesBoundingScheme() == EMessagesBoundingScheme.AGE) {
			messagesQueue = new LinkedList<>();
			ScheduledExecutorService queuePurgeScheduler = Executors.newSingleThreadScheduledExecutor();

			queuePurgeScheduler.scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {

					if (messagesQueue != null) {

						synchronized (messagesQueue) {
							long currentTimeMillis = System.currentTimeMillis();
							List<Pair<Long, LogEvent>> messageItemsToPurge = new ArrayList<>();

							for (Pair<Long, LogEvent> messageItem : messagesQueue) {

								if (currentTimeMillis - messageItem.getValue()
										.getTimeMillis() > MessagesConfigManager.INSTANCE.getMessagesPurgeAgeInHours()
												* 3600000) {
									messageItemsToPurge.add(messageItem);
								} else {
									// Finished searching, because the iteration
									// is time-ordered also
									break;
								}

							}

							messagesQueue.removeAll(messageItemsToPurge);
						}

					}

				}

			}, 10, 60, TimeUnit.SECONDS);

		} else {
			messagesQueue = new CircularFifoQueue<>(MessagesConfigManager.INSTANCE.getMessagesQueueSize());
		}

		lastMessageIndex = new AtomicLong(0);
	}

	/**
	 * Creates a <code>MessagesAppender</code>.
	 * 
	 * @param name
	 *            Name of the appender
	 * @param layout
	 *            Layout to configure the appender with (Log4J2 stuff)
	 * @return A new <code>MessagesAppender</code> object
	 */
	@PluginFactory
	public static MessagesAppender createAppender(
			@Required(message = "No name provided for MessagesAppender") @PluginAttribute("name") final String name,
			@PluginElement("Layout") final Layout<? extends Serializable> layout) {
		return new MessagesAppender(Objects.requireNonNull(name), layout);
	}

	/**
	 * Get all messages still in memory.
	 * 
	 * @return All messages still in memory
	 */
	public List<Map<String, String>> getMessages() {
		List<Map<String, String>> messages = null;

		synchronized (messagesQueue) {
			messages = new ArrayList<>();

			for (Pair<Long, LogEvent> messageItem : messagesQueue) {
				messages.add(createMessageItemTuple(messageItem.getKey(), messageItem.getValue()));
			}

		}

		return messages;
	}

	/**
	 * Get all messages starting with the provided message index value.
	 * 
	 * @param fromIndex
	 *            The index of the first message desired. A filtering value
	 * @return List of messages, starting with the specified message index
	 */
	public List<Map<String, String>> getMessages(final long fromIndex) {
		List<Map<String, String>> messages = null;

		synchronized (messagesQueue) {
			messages = new ArrayList<>();

			for (Pair<Long, LogEvent> messageItem : messagesQueue) {

				if (messageItem.getKey() >= fromIndex) {
					messages.add(createMessageItemTuple(messageItem.getKey(), messageItem.getValue()));
				}

			}

		}

		return messages;
	}

	/**
	 * Get all messages on or after the provided message timestamp.
	 * 
	 * @param fromTime
	 *            The timestamp of messages filter. Messages that are on or
	 *            after this timestamp are returned.
	 * @return List of messages that fall after the specified timestamp
	 */
	public List<Map<String, String>> getMessages(final ZonedDateTime fromTime) {
		long fromTimeEpochMilli = fromTime.toInstant().toEpochMilli();
		List<Map<String, String>> messages = null;

		synchronized (messagesQueue) {
			messages = new ArrayList<>();

			for (Pair<Long, LogEvent> messageItem : messagesQueue) {

				if (messageItem.getValue().getTimeMillis() >= fromTimeEpochMilli) {
					messages.add(createMessageItemTuple(messageItem.getKey(), messageItem.getValue()));
				}

			}

		}

		return messages;
	}

	/**
	 * Create a map for a single message entry that includes the message's
	 * metadata. This is a utility method for generating JSONs.
	 * 
	 * @param index
	 *            Index of the message to include in the map
	 * @param logEvent
	 *            Actual message object
	 * @return A map that contains a single log entry's message text and
	 *         metadata
	 */
	private Map<String, String> createMessageItemTuple(final long index, final LogEvent logEvent) {
		Map<String, String> messageItemTuple = new HashMap<>(4);
		// 1: Message index
		messageItemTuple.put(MessagesConfigManager.INSTANCE.getMessagesKeyAbbreviationForIndex(), Long.toString(index));
		// 2: Timestamp
		messageItemTuple.put(MessagesConfigManager.INSTANCE.getMessagesKeyAbbreviationForTime(),
				ZonedDateTime.ofInstant(Instant.ofEpochMilli(logEvent.getTimeMillis()), ZoneOffset.UTC)
						.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()));
		// 3: Level
		messageItemTuple.put(MessagesConfigManager.INSTANCE.getMessagesKeyAbbreviationForLevel(),
				logEvent.getLevel().toString());
		// 4: Message text
		messageItemTuple.put(MessagesConfigManager.INSTANCE.getMessagesKeyAbbreviationForMessage(),
				logEvent.getMessage().getFormattedMessage());
		return messageItemTuple;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.core.Appender#append(org.apache.logging.log4j.
	 * core.LogEvent)
	 */
	@Override
	public void append(final LogEvent event) {

		synchronized (messagesQueue) {
			messagesQueue.add(Pair.of(lastMessageIndex.incrementAndGet(), event));
			MessageDistributor.INSTANCE.eventMessage(createMessageItemTuple(0, event));
		}

	}

}