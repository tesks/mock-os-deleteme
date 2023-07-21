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
package jpl.gds.message.impl.handlers;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventTranslatorOneArg;

import jpl.gds.message.api.external.IExternalMessage;

/**
 * Message event implementation used by the disruptor-based queuing message handler.
 */
public class MessageReceiptEvent {
	
	/**
	 * Static event factory to create message receipt events.  Used by the disruptor.
	 */
	public static final EventFactory<MessageReceiptEvent> DATA_EVENT_FACTORY = new EventFactory<MessageReceiptEvent>() {

		@Override
		public MessageReceiptEvent newInstance() {
			return new MessageReceiptEvent();
		}
	};

	/**
	 * Static event translator to set the data in the data events from the disruptor.
	 */
	public static final EventTranslatorOneArg<MessageReceiptEvent, IExternalMessage> DATA_TRANSLATOR = new EventTranslatorOneArg<MessageReceiptEvent, IExternalMessage>() {

		@Override
		public void translateTo(final MessageReceiptEvent event, final long sequence, final IExternalMessage data) {
			event.setData(data);
		}
	};
	
	/** The actual event data */
	private IExternalMessage data;
	
	/**
	 * Sets the event data value.
	 * 
	 * @param data the new value of data
	 */
	public void setData(final IExternalMessage data) {
		this.data = data;
	}
	
	/**
     * Gets the event data value.
     * 
     * @return the value of data
     */
	public IExternalMessage getData() {
	    return this.data;
	}
}
