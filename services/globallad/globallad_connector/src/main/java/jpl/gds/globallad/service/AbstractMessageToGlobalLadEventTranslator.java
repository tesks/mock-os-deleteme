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
package jpl.gds.globallad.service;

import com.lmax.disruptor.EventTranslatorOneArg;

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.disruptor.GlobalLadDataEvent;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;

/**
 * Abstract class used in the global lad downlink service message.  This implements the lmax Disruptor EventTranslatorOneArg interface.
 * A translator is given to a disruptor instance and is used to convert an IMessage event into a GlobalLadDataEvent which will be stored 
 * within the disruptors ring buffer.
 */
public abstract class AbstractMessageToGlobalLadEventTranslator implements EventTranslatorOneArg<GlobalLadDataEvent, IMessage> {
	private static final Tracer log = GlobalLadProperties.getTracer();
	
	/**
	 * Converts the message to global lad data object.
	 * 
	 * @param message
	 * @return
	 */
	public abstract IGlobalLADData convertTo(IMessage message) throws GlobalLadConversionException;
	
	/* (non-Javadoc)
	 * @see com.lmax.disruptor.EventTranslatorOneArg#translateTo(java.lang.Object, long, java.lang.Object)
	 */
	@Override
	public void translateTo(GlobalLadDataEvent event, long sequence, IMessage message) {
		try {
			event.setData(convertTo(message));
		} catch (GlobalLadConversionException e) {
			log.error(String.format("Failed to translate message with type %s to Global LAD data word due to a conversion error: %s", 
					message.getType(), e.getMessage()), e.getCause());
		}
	}
}
