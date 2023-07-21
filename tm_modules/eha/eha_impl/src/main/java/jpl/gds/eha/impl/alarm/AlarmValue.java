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

import jpl.gds.alarm.serialization.AlarmValue.Proto3AlarmValue;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.alarm.AlarmType;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.eha.api.channel.IAlarmValue;
import jpl.gds.serialization.primitives.alarm.Proto3AlarmLevel;
import jpl.gds.serialization.primitives.alarm.Proto3AlarmType;

/**
 * AlarmValue represents an the state of a single alarm on a single channel value.
 *
 */
public class AlarmValue implements IAlarmValue
{
	private boolean inAlarm;
	private AlarmLevel level = AlarmLevel.NONE; 
	private AlarmType type = AlarmType.NO_TYPE;
	private String state = "";
	private boolean onEu = false;
	private String alarmId;

	/**
	 * Creates an empty instance of AlarmValue.
	 * 
	 */
    public AlarmValue() {
		super();
	}
    
    /**
     * Initializes from the proto buffer object.
     * @param av
     */
    public AlarmValue(Proto3AlarmValue av) {
    	deserialize(av);
    }

	/**
	 * Creates an instance of AlarmValue.
	 * 
	 * @param type the definition type of the alarm
	 * @param level the level of the alarm
	 * @param onEu true if alarm is on channel EU, false if on DN
	 * @param inAlarm the alarm trigger state; true if triggered, false if not
	 * @param state the state string to display for a triggered alarm
     * @param alarmId the alarm id
     */
    public AlarmValue(final AlarmType type, final AlarmLevel level, final boolean onEu, final boolean inAlarm,
            final String state, String alarmId) {
		super();
		this.type = type;
		this.level = level;
		this.inAlarm = inAlarm;
		this.state = state;
		this.onEu = onEu;
		this.alarmId = alarmId;
	}

	/**
	 * Creates an instance of AlarmValue.
	 * 
	 * @param alarmDef the Alarm definition associated with this alarm.
	 * @param inAlarm the alarm trigger state; true if triggered, false if not
	 * @param state the state string to display for a triggered alarm
	 * 
	 */
    public AlarmValue(final IAlarmDefinition alarmDef, final boolean inAlarm, final String state) {
		super();
		type = alarmDef.getAlarmType();
		level = alarmDef.getAlarmLevel();
		this.inAlarm = inAlarm;
		this.state = state;
		onEu = alarmDef.isCheckOnEu();
		this.alarmId = alarmDef.getAlarmId();
	}

	@Override
	public void setLevel(final AlarmLevel level) {
		this.level = level;
	}

	@Override
	public void setType(final AlarmType type) {
		this.type = type;
	}

	@Override
	public void setOnEu(final boolean onEu) {
		this.onEu = onEu;
	}

	@Override
	public boolean isInAlarm()
	{
		return inAlarm;
	}

	@Override
	public void setInAlarm(final boolean inAlarm)
	{
		this.inAlarm = inAlarm;
	}

	@Override
	public String getState() {
		return state;
	}

	@Override
	public AlarmLevel getLevel() {
		return level;
	}

	@Override
	public AlarmType getType() {
		return type;
	}

	@Override
	public void setState(final String state) {
		this.state = state;
	}

	@Override
	public boolean isOnEu() {
		return onEu;
	}

	@Override
	public String getAlarmId() {
		return alarmId;
	}

	@Override
	public void setAlarmId(String id) {
		this.alarmId = id;
	}

	@Override
	public Proto3AlarmValue getProto() {
		return Proto3AlarmValue.newBuilder()
			.setIsInAlarm(isInAlarm())
			.setIsOnEu(isOnEu())
			.setState(getState())
			.setAlarmId(getAlarmId())
			.setType(Proto3AlarmType.valueOf(getType().toString()))
			.setLevel(Proto3AlarmLevel.valueOf(getLevel().toString()))
			.build();
	}

	@Override
	public void deserialize(Proto3AlarmValue value) {
		setInAlarm(value.getIsInAlarm());
		setOnEu(value.getIsOnEu());
		setState(value.getState());
		setAlarmId(value.getAlarmId());

		setType(AlarmType.valueOf(value.getType().toString()));
		setLevel(AlarmLevel.valueOf(value.getLevel().toString()));

		
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alarmId == null) ? 0 : alarmId.hashCode());
		result = prime * result + (inAlarm ? 1231 : 1237);
		result = prime * result + ((level == null) ? 0 : level.hashCode());
		result = prime * result + (onEu ? 1231 : 1237);
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AlarmValue other = (AlarmValue) obj;
		if (alarmId == null) {
			if (other.alarmId != null)
				return false;
		} else if (!alarmId.equals(other.alarmId))
			return false;
		if (inAlarm != other.inAlarm)
			return false;
		if (level != other.level)
			return false;
		if (onEu != other.onEu)
			return false;
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
}
