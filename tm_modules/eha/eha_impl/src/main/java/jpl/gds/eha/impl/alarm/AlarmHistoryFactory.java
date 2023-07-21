package jpl.gds.eha.impl.alarm;

import jpl.gds.alarm.serialization.Proto3AlarmHistory;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryFactory;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider;
import jpl.gds.shared.log.Tracer;

public class AlarmHistoryFactory implements IAlarmHistoryFactory {

	@Override
	public IAlarmHistoryProvider createAlarmHistory() {
		return new AlarmHistory();
	}

	@Override
	public IAlarmHistoryProvider createAlarmHistory(Proto3AlarmHistory proto, IChannelDefinitionProvider chanDict, Tracer log) {
		return new AlarmHistory(proto, chanDict, log);
	}
}
