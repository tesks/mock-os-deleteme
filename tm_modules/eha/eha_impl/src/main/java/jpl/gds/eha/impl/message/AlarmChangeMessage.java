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

package jpl.gds.eha.impl.message;

import com.google.protobuf.InvalidProtocolBufferException;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.eha.api.channel.serialization.Proto3AlarmChangeMessage;
import jpl.gds.eha.api.channel.serialization.Proto3AlarmedChannelValueMessage;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.eha.api.message.aggregation.IAlarmChangeMessage;
import jpl.gds.eha.impl.channel.ClientChannelValue;
import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.shared.message.BaseBinaryMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Alarm messages encapsulate Alarmed Channel Value messages and denote changes in alarm state
 *
 * @since 8.5
 */
public class AlarmChangeMessage extends AbstractChannelMessage implements IAlarmChangeMessage {

    private final IAlarmedChannelValueMessage          message;
    private final IAlarmChangeMessage.AlarmChangeState alarmChangeState;

    /**
     * Create an Alarm Message from a channel value message and previous/current alarm state
     *
     * @param message           original alarmed channel value message
     * @param previouslyInAlarm if the channel was previously in alarm
     * @param currentlyInAlarm  if the channel is currently in alarm
     * @param missionProperties mission properties
     */
    public AlarmChangeMessage(IAlarmedChannelValueMessage message, boolean previouslyInAlarm, boolean currentlyInAlarm,
                              MissionProperties missionProperties) {
        super(EhaMessageType.AlarmChange, System.currentTimeMillis(), missionProperties);

        this.message = message;
        if (previouslyInAlarm && currentlyInAlarm) {
            this.alarmChangeState = AlarmChangeState.STILL_IN;
        } else if (!previouslyInAlarm && currentlyInAlarm) {
            this.alarmChangeState = AlarmChangeState.ENTERED;
        } else if (previouslyInAlarm && !currentlyInAlarm) {
            this.alarmChangeState = AlarmChangeState.EXITED;
        } else {
            // should never get here
            this.alarmChangeState = AlarmChangeState.NOT_IN;
        }
    }

    /**
     * Create an Alarm Message from a serialized Protobuf
     *
     * @param msg               protobuf message
     * @param missionProperties mission properties
     * @throws InvalidProtocolBufferException
     */
    public AlarmChangeMessage(final Proto3AlarmChangeMessage msg, MissionProperties missionProperties) throws
                                                                                                       InvalidProtocolBufferException {
        super(EhaMessageType.AlarmChange, msg.getSuper(), missionProperties);
        ClientChannelValue channelValue = new ClientChannelValue(msg.getMessage().getChanVal());
        this.message = new AlarmedChannelValueMessage(msg.getMessage(), channelValue, missionProperties);
        this.alarmChangeState = AlarmChangeState.valueOf(msg.getAlarmChange().name());
    }

    @Override
    public IClientChannelValue getChannelValue() {
        return message.getChannelValue();
    }

    @Override
    public String getEscapedCsv() {
        return this.alarmChangeState.getText() + CSV_SEPARATOR + message.getEscapedCsv();
    }

    @Override
    public byte[] toBinary() {
        return this.build().toByteArray();
    }

    @Override
    public String getOneLineSummary() {
        return message.getOneLineSummary() + " Alarm Change: " + alarmChangeState.getText();
    }

    @Override
    public Proto3AlarmChangeMessage build() {
        final Proto3AlarmChangeMessage.Builder retVal = Proto3AlarmChangeMessage.newBuilder();
        retVal.setSuper((Proto3AbstractMessage) super.build());
        retVal.setMessage((Proto3AlarmedChannelValueMessage) message.build());
        retVal.setAlarmChangeValue(alarmChangeState.ordinal());
        return retVal.build();
    }

    @Override
    public String toString() {
        return "AlarmMessage[Change=" + alarmChangeState.name() + " [" + message.toString() + "]]";
    }

    @Override
    public String getXmlRootName() {
        return null;
    }

    @Override
    public int getMtakFieldCount() {
        return 0;
    }

    @Override
    public AlarmChangeState getAlarmChangeState() {
        return alarmChangeState;
    }

    @Override
    public IAlarmedChannelValueMessage getAlarmedChannelValueMessage() {
        return message;
    }

    @Override
    public boolean isRealtime() {
        return message.isRealtime();
    }

    @Override
    public synchronized void setTemplateContext(Map<String, Object> map) {
        message.setTemplateContext(map);
        map.put("alarmChange", alarmChangeState.getText());
    }

    /**
     * BinaryParseHandler is the message-specific SAX parse handler for creating this Message from its binary
     * representation.
     *
     */
    public static class BinaryParseHandler extends BaseBinaryMessageParseHandler {

        private final MissionProperties missionProperties;

        /**
         * constructor
         *
         * @param appContext
         */
        public BinaryParseHandler(ApplicationContext appContext) {
            this.missionProperties = appContext.getBean(MissionProperties.class);
        }

        @Override
        public IMessage[] parse(List<byte[]> content) throws IOException {
            for (final byte[] msgBytes : content) {
                final Proto3AlarmChangeMessage msg     = Proto3AlarmChangeMessage.parseFrom(msgBytes);
                final AlarmChangeMessage       message = new AlarmChangeMessage(msg, missionProperties);
                addMessage(message);
            }
            return getMessages();
        }
    }
}
