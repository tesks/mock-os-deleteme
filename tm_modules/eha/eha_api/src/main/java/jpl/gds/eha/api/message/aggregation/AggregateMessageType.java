package jpl.gds.eha.api.message.aggregation;

import jpl.gds.shared.message.IMessageType;

public enum AggregateMessageType implements IMessageType {
	AggregateHeaderChannel,
	AggregateAlarmedEhaChannel,
	AggregateSseChannel,
	AggregateMonitorChannel;

	@Override
	public String getSubscriptionTag() {
		return name();
	}
}