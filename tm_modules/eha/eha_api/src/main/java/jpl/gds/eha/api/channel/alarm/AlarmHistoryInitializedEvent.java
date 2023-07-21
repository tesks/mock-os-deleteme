package jpl.gds.eha.api.channel.alarm;

import org.springframework.context.ApplicationEvent;

/**
 * Event to pass when the alarm history table is initialized and ready to be boot strapped.
 * 
 *
 */
public class AlarmHistoryInitializedEvent extends ApplicationEvent {
	private static final long serialVersionUID = 1232116926992443285L;
	private final IAlarmHistoryProvider alarmHistory;

	public AlarmHistoryInitializedEvent(Object source, IAlarmHistoryProvider alarmHistory) {
		super(source);
		
		this.alarmHistory = alarmHistory;
	}
	
	/**
	 * @return the alarmHistory
	 */
	public IAlarmHistoryProvider getAlarmHistory() {
		return alarmHistory;
	}

}
