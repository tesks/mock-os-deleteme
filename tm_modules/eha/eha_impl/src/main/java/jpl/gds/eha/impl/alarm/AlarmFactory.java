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
package jpl.gds.eha.impl.alarm;

import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.dictionary.api.alarm.AlarmType;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.dictionary.api.alarm.ICombinationAlarmDefinition;
import jpl.gds.dictionary.api.alarm.ICompoundAlarmDefinition;
import jpl.gds.eha.api.channel.alarm.IAlarmFactory;
import jpl.gds.eha.api.channel.alarm.IChannelAlarm;
import jpl.gds.eha.api.channel.alarm.ICombinationAlarm;
import jpl.gds.shared.log.Tracer;

/**
 * Class AlarmFactory
 */

public final class AlarmFactory implements IAlarmFactory {

    private final Tracer log;

    /**
     * Alarm factory constructor
     * 
     * @param log
     *            Tracer to log with
     */
    public AlarmFactory(final Tracer log) {
        this.log = log;
    }


	@Override
	public IChannelAlarm createAlarm(final IAlarmDefinition def, final TimeComparisonStrategyContextFlag timeStrategy) {
		if (def == null) {
            throw new IllegalArgumentException("alarm definition cannot be null");
		}

		final AlarmType type = def.getAlarmType();
		switch (type) {

		case HIGH_VALUE_COMPARE:
			return new HighValueCompareAlarm(def, timeStrategy);
		case LOW_VALUE_COMPARE:
			return new LowValueCompareAlarm(def, timeStrategy);
		case STATE_COMPARE:
			return new StateCompareAlarm(def, timeStrategy);
		case VALUE_CHANGE:
			return new ValueChangeAlarm(def, timeStrategy);
		case VALUE_DELTA:
			return new ValueDeltaAlarm(def, timeStrategy);
		case EXCLUSIVE_COMPARE:
			return new ExclusiveCompareAlarm(def, timeStrategy);
		case INCLUSIVE_COMPARE:
			return new InclusiveCompareAlarm(def, timeStrategy);
		case DIGITAL_COMPARE:
			return new DigitalCompareAlarm(def, timeStrategy);
		case MASK_COMPARE:
			return new MaskCompareAlarm(def, timeStrategy);
		case COMPOUND:
			return new CompoundAlarm((ICompoundAlarmDefinition)def, timeStrategy, this);
		default:
			throw new IllegalArgumentException("AlarmType " + type
					+ " is not supported");

		}
	}

	@Override
    public ICombinationAlarm createCombinationAlarm(final ICombinationAlarmDefinition def,
                                                    final TimeComparisonStrategyContextFlag timeStrategy) {
		return new CombinationAlarm(def, timeStrategy, this);
	}

}
