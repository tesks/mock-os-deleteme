/*
 * Copyright 2006-2020. California Institute of Technology.
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

package jpl.gds.eha.api.message.aggregation;

import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;

/**
 * Interface for alarm change state messages
 *
 * @since R8.5
 */
public interface IAlarmChangeMessage extends IAlarmedChannelValueMessage {
    /**
     * Enum for change states
     */
    enum AlarmChangeState {
        ENTERED("Entered alarm"),
        EXITED("Exited alarm"),
        STILL_IN("Still in alarm"),
        NOT_IN("Not in alarm");

        private String text;

        AlarmChangeState(String text) {
            this.text = text;
        }

        /**
         * Get the text description of the alarm change state
         *
         * @return
         */
        public String getText() {
            return text;
        }
    }

    /**
     * Return alarm change state
     *
     * @return
     */
    AlarmChangeState getAlarmChangeState();

    /**
     * Return original alarmed channel value message
     *
     * @return
     */
    IAlarmedChannelValueMessage getAlarmedChannelValueMessage();

}
