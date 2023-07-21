/**
 * 
 */
package jpl.gds.eha.api.channel.alarm;

import jpl.gds.alarm.serialization.Proto3AlarmHistory;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.shared.log.Tracer;

/**
 * Interface IAlarmHistoryFactory
 */
public interface IAlarmHistoryFactory {
	
	/**
	 * @return new empty alarm history
	 */
	public IAlarmHistoryProvider createAlarmHistory();

	/**
	 * Create an alarm history from proto
	 * @param proto
	 * @param chanDict
	 * @param log
	 * @return a new initialized alarm history
	 */
	IAlarmHistoryProvider createAlarmHistory(Proto3AlarmHistory proto, IChannelDefinitionProvider chanDict, Tracer log);

}
