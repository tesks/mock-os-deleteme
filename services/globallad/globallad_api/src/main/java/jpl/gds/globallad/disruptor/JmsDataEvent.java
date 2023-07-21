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
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;

import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.message.api.external.IExternalMessage;

/**
 * Event implementation used by the disruptor responsible for distributing new global lad objects to be inserted into the global lad.
 */
public class JmsDataEvent {

    /**
     * Static event factory to create global lad data events.  Used by the disruptor.
     */
    public static final EventFactory<JmsDataEvent> DATA_EVENT_FACTORY = new EventFactory<JmsDataEvent>() {

        @Override
        public JmsDataEvent newInstance() {
            return new JmsDataEvent();
        }
    };

    /**
     * Static event translator to set the data in the data events from the disruptor.
     */
    public static final EventTranslatorOneArg<JmsDataEvent, IExternalMessage> DATA_TRANSLATOR = new EventTranslatorOneArg<JmsDataEvent, IExternalMessage>() {

        /* (non-Javadoc)
         * @see com.lmax.disruptor.EventTranslatorOneArg#translateTo(java.lang.Object, long, java.lang.Object)
         */
        @Override
        public void translateTo(final JmsDataEvent event, final long sequence, final IExternalMessage data) {
            event.setData(data);
        }
    };

    public IExternalMessage data;
    public long timestamp;

    /**
     * @param data the new value of data
     */
    public void setData(final IExternalMessage data) {
        this.data = data;
        this.timestamp = System.nanoTime();
    }

    public void clear() {
        data = null;
    }

}
