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

package jpl.gds.eha.impl.service.channel.alarm;

import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.dictionary.api.alarm.AlarmType;
import jpl.gds.dictionary.api.alarm.IAlarmDefinitionProvider;
import jpl.gds.dictionary.api.alarm.IAlarmReloadListener;
import jpl.gds.eha.api.channel.IAlarmValue;
import jpl.gds.eha.api.channel.IAlarmValueSet;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.channel.alarm.IAlarmFactory;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider;
import jpl.gds.eha.api.channel.alarm.IChannelAlarm;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.eha.api.message.IChannelValueMessage;
import jpl.gds.eha.api.message.IEhaMessageFactory;
import jpl.gds.eha.api.message.aggregation.IAlarmChangeMessage;
import jpl.gds.eha.api.service.alarm.IAlarmPublisherService;
import jpl.gds.eha.impl.alarm.AlarmValueSet;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.types.Pair;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AlarmPublisherService is responsible for listening to internal ChannelValueMessages, and if alarm processing is
 * enabled, calculating the alarms on the received values. Whether or not alarm processing is enabled,
 * AlarmPublisherService publishes an AlarmedChannelValueMessage for each incoming InternalChannelValueMessage.
 *
 */
public class AlarmPublisherService implements IAlarmPublisherService, IAlarmReloadListener {

    private final Tracer tracer;


    private       AlarmSubscriber          subscriber;
    private final AlarmTable               table;
    private       boolean                  enabled;
    private       boolean                  started               = false;
    private final IMessagePublicationBus   messageBus;
    private final IAlarmHistoryProvider    alarmHistory;
    private final IAlarmDefinitionProvider alarmDefProvider;
    private final AtomicBoolean            needsDefinitionReload = new AtomicBoolean(false);
    private final IEhaMessageFactory       ehaMessageFactory;
    private final Set<String>              channelsInAlarm;

    /**
     * Constructor.
     *
     * @param serviceContext the current application context
     */
    public AlarmPublisherService(final ApplicationContext serviceContext) {
        this.messageBus = serviceContext.getBean(IMessagePublicationBus.class);
        this.alarmHistory = serviceContext.getBean(IAlarmHistoryProvider.class);
        this.alarmDefProvider = serviceContext.getBean(IAlarmDefinitionProvider.class);

        this.table = new AlarmTable(
                serviceContext.getBean(IAlarmFactory.class),
                serviceContext.getBean(TimeComparisonStrategyContextFlag.class));

        this.table.populateFromDefinitionProvider(alarmDefProvider);
        this.alarmDefProvider.addReloadListener(this);
        this.ehaMessageFactory = serviceContext.getBean(IEhaMessageFactory.class);
        this.tracer = TraceManager.getTracer(serviceContext, Loggers.ALARM);
        this.channelsInAlarm = new HashSet<>();
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.shared.interfaces.IService#startService()
     */
    @Override
    public boolean startService() {
        subscriber = new AlarmSubscriber();
        tracer.info(Markers.NOTIFY, "Alarm processing is " + (enabled ? "enabled" : "disabled"));
        started = true;
        return started;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.service.alarm.IAlarmPublisherService#enableCalculation(boolean)
     */
    @Override
    public void enableCalculation(final boolean isEnable) {
        enabled = isEnable;
    }

    private IAlarmValueSet checkAlarms(final IAlarmedChannelValueMessage message, final boolean targetAlarmMode) {
        final String              id       = message.getChannelValue().getChanId();
        final List<IChannelAlarm> alarmSet = table.getAlarmsFromChannelId(id);
        /*
         * Do not create an alarm value set unless
         * there really is an alarm. Leave it null.
         */
        IAlarmValueSet alarmVals = null;

        if (alarmSet == null || alarmSet.size() == 0) {
            if (tracer.isEnabledFor(TraceSeverity.TRACE)) {
                tracer.trace("No alarms defined for channel " + id);
            }
        } else {
            final Iterator<IChannelAlarm> it = alarmSet.iterator();
            while (it.hasNext()) {
                final IChannelAlarm def = it.next();

                if (targetAlarmMode == (def.getDefinition().getAlarmType() == AlarmType.COMBINATION_TARGET)) {

                    final IAlarmValue alarmVal = def
                            .check(alarmHistory, (IServiceChannelValue) message.getChannelValue());
                    if (alarmVal != null) {
                        /*
                         * IF there was an actual
                         * alarm, create an alarm value set if not already
                         * created.
                         */
                        if (alarmVals == null) {
                            alarmVals = new AlarmValueSet();
                        }
                        alarmVals.addAlarm(alarmVal);
                    }
                }
            }
        }

        return (alarmVals);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.shared.interfaces.IService#stopService()
     */
    @Override
    public void stopService() {
        tracer.debug("Shutting down alarm publisher");
        messageBus.unsubscribeAll(subscriber);
        started = false;
        alarmDefProvider.removeReloadListener(this);
        tracer.debug("Alarm publisher stopped");
    }

    /**
     * AlarmSubscriber is the private subscription class for internal ChannelValueMessages.
     *
     */
    private class AlarmSubscriber implements MessageSubscriber {

        private final List<IAlarmedChannelValueMessage> heldMessages;
        private final Set<Pair<String, Boolean>>        publishedTargetChannels;

        public AlarmSubscriber() {
            heldMessages = new ArrayList<IAlarmedChannelValueMessage>(16);
            publishedTargetChannels = new HashSet<Pair<String, Boolean>>(16);
            messageBus.subscribe(EhaMessageType.ChannelValue, this);
            messageBus.subscribe(EhaMessageType.EndChannelProcessing, this);
        }


        @Override
        public void handleMessage(final IMessage message) {

            /*
             * R8 Refactor - reload alarm table now if definitions have
             * changed.
             */
            if (needsDefinitionReload.get()) {
                table.populateFromDefinitionProvider(alarmDefProvider);
                needsDefinitionReload.set(false);
            }

            if (message.isType(EhaMessageType.ChannelValue)) {
                // always create the outgoing EHA channel message
                final IAlarmedChannelValueMessage outMessage = ehaMessageFactory
                        .createAlarmedChannelMessage((IChannelValueMessage) message);


                /*
                 * combined conditional logic into
                 * nested ifs. Fixed explicit equality testing of boolean with
                 * boolean constants.
                 */
                if (enabled) {
                    IAlarmValueSet alarmSet = checkAlarms(outMessage, false);
                    ((IServiceChannelValue) outMessage.getChannelValue()).setAlarms(alarmSet);

                    // updated to generate alarm change messages
                    calculateAlarmChange(outMessage);

                    if (table.getCombinationAlarmTable()
                            .hasCombinationAlarmsTargettingOnChannel(outMessage.getChannelValue().getChanId())) {
                        heldMessages.add(outMessage);
                        return;
                    }
                }
                messageBus.publish(outMessage);
            }

            else if (enabled && message.isType(EhaMessageType.EndChannelProcessing)) {
                for (final IAlarmedChannelValueMessage ecm : heldMessages) {
                    // continue from where we left off
                    final IAlarmValueSet alarmVals = ((IServiceChannelValue) ecm.getChannelValue()).getAlarms();

                    final IAlarmValueSet targetAlarmVals = checkAlarms(ecm, true);

                    /*
                     *
                     * After changes made the checkAlarms function
                     * above may return null if there are no alarms on the
                     * channel. When these alarms are attempted to be added, a
                     * null pointer exception occurred. Additionally, if there
                     * have been no alarms the first set of alarms will also
                     * cause a null pointer exception since they will attempt
                     * to be added to null.
                     */
                    if (targetAlarmVals != null) {

                        if (alarmVals == null) {
                            ((IServiceChannelValue) ecm.getChannelValue()).setAlarms(targetAlarmVals);
                        } else {
                            alarmVals.addAlarmSet(targetAlarmVals);

                            ((IServiceChannelValue) ecm.getChannelValue()).setAlarms(alarmVals);
                        }
                    }

                    messageBus.publish(ecm);

                    publishedTargetChannels.add(new Pair<String, Boolean>(ecm.getChannelValue().getChanId(),
                            Boolean.valueOf(ecm.getChannelValue().isRealtime())));
                }

                heldMessages.clear();

                /*
                 * Combination Alarms
                 *
                 * It was decided in April 2013 that the old combination alarms
                 * approach of producing "faux" channel values to reflect the
                 * updated combination alarm conditions to the user/system is
                 * not the desired behavior. The policy agreed upon is: No
                 * target channel => combination alarm does not get evaluated.
                 *
                 * Hence the lines of code below are now removed:
                 *
                 * // now find and publish artificial target channels
                 * Set<Pair<String, Boolean>> targetsToArtificiallyPublish =
                 * CombinationAlarmTable.getInstance().
                 * getAffectedChannelsToPublish();
                 * targetsToArtificiallyPublish.removeAll(
                 * publishedTargetChannels); publishedTargetChannels.clear(); //
                 * manufacture an AlarmedChannelValueMessage for ChannelId,
                 * publish for (Pair<String, Boolean> cidBoolPair :
                 * targetsToArtificiallyPublish) { // TODO the cid being
                 * republished here will have had their alarm status changed...
                 * republishChannel(cidBoolPair.getOne(),
                 * cidBoolPair.getTwo().booleanValue()); }
                 * CombinationAlarmTable.getInstance().clearAllFlags();
                 */

            }

        }

        /**
         * Perform checks for alarm changes
         *
         * @param outMessage alarmed eha message
         */
        protected void calculateAlarmChange(IAlarmedChannelValueMessage outMessage) {
            IAlarmValueSet alarmSet           = outMessage.getChannelValue().getAlarms();
            String         channelId          = outMessage.getChannelValue().getChanId();
            boolean        channelWasInAlarm  = channelsInAlarm.contains(channelId);
            boolean        currentlyInAlarm   = alarmSet != null && alarmSet.inAlarm();
            boolean        createAlarmMessage = false;

            if (alarmSet != null && alarmSet.inAlarm()) {
                createAlarmMessage = true;
                if (!channelWasInAlarm) {
                    // case 1) channel entered alarm, create message saying "entered alarm"
                    channelsInAlarm.add(channelId);
                    tracer.debug("ALARM ENTERED: ", channelId);
                } else {
                    // case 2) channel value was already in alarm, create message saying "still in alarm"
                    tracer.debug("ALARM MAINTAINED: ", channelId);
                }
            } else if (channelWasInAlarm) {
                // case 3) channel value was in alarm, but isn't any longer. create message saying "exited alarm"
                createAlarmMessage = true;
                channelsInAlarm.remove(channelId);
                tracer.debug("ALARM EXITED: ", channelId);
            }
            if (createAlarmMessage) {
                IAlarmChangeMessage alarmMessage = ehaMessageFactory
                        .createAlarmChangeMessage(outMessage, channelWasInAlarm, currentlyInAlarm);
                messageBus.publish(alarmMessage);
            }
        }

    }

    /**
     * Indicates if the alarm publisher service has been started.
     *
     * @return true if started, false if not
     */
    public boolean isStarted() {
        return started;
    }

    @Override
    public void alarmsReloaded() {
        this.needsDefinitionReload.set(true);

    }
}
