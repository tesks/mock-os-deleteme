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
package jpl.gds.globallad.disruptor;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventTranslatorOneArg;

import jpl.gds.globallad.data.IGlobalLADData;

/**
 * Event implementation used by the disruptor responsible for distributing new global lad objects to be inserted into the global lad.
 */
public class GlobalLadDataEvent {
	
	/**
	 * Static event factory to create global lad data events.  Used by the disruptor.
	 */
	public static final EventFactory<GlobalLadDataEvent> DATA_EVENT_FACTORY = new EventFactory<GlobalLadDataEvent>() {

		@Override
		public GlobalLadDataEvent newInstance() {
			return new GlobalLadDataEvent();
		}
	};

	/**
	 * Static event translator to set the data in the data events from the disruptor.
	 */
	public static final EventTranslatorOneArg<GlobalLadDataEvent, IGlobalLADData> DATA_TRANSLATOR = new EventTranslatorOneArg<GlobalLadDataEvent, IGlobalLADData>() {

		/* (non-Javadoc)
		 * @see com.lmax.disruptor.EventTranslatorOneArg#translateTo(java.lang.Object, long, java.lang.Object)
		 */
		@Override
		public void translateTo(GlobalLadDataEvent event, long sequence, IGlobalLADData data) {
			event.setData(data);
		}
	};
	
	public IGlobalLADData data;
	
	/**
	 * @param data the new value of data
	 */
	public void setData(final IGlobalLADData data) {
		this.data = data;
	}
}
