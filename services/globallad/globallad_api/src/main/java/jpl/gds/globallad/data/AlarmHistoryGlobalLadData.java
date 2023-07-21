/**
 * 
 */
package jpl.gds.globallad.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jpl.gds.alarm.serialization.Proto3AlarmHistory;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider;
import jpl.gds.serialization.globallad.data.Proto3AlarmHistoryGladData;
import jpl.gds.serialization.globallad.data.Proto3GlobalLadTransport;

public class AlarmHistoryGlobalLadData extends AbstractGlobalLadData {
	/**
	 * New data type for alarm history.
	 */
	public static final byte ALARM_HISTORY_GLAD_USER_DATA_TYPE = 8;

    /** The alarm history to use */
	protected final Proto3AlarmHistory alarmHistory;

	private final String historyId;

	/**
     * Constructor for creating GLAD alarm history from a provider <IAlarmHistoryProvider>
     * 
     * @param history
     *            <IAlarmHistoryProvider>
     * @param c
     *            <IContextConfiguration>
     */
	public AlarmHistoryGlobalLadData(final IAlarmHistoryProvider history, final IContextConfiguration c) {
		super();
		final IContextIdentification id = c.getContextId();
		final IVenueConfiguration vc = c.getVenueConfiguration();

		setSessionNumber(id.getNumber());
		setHost(id.getHost());
		setVenue(vc.getVenueType().toString());
		setUserDataType(ALARM_HISTORY_GLAD_USER_DATA_TYPE);
		
		final long et = System.currentTimeMillis();
		
		// Set all the times the same.
		setEventTime(et);
		setErtMilliseconds(et);
		setErtNanoseconds(0);
		setScetMilliseconds(et);
		setScetNanoseconds(0);
		setSclkCoarse(et);
		setSclkFine(0);


		this.alarmHistory = history.build();

		historyId = String.format("host-%s-venue-%s-session-%d", host, venue, sessionNumber);
	}
	
	
    /**
     * Constructor for creating GLAD AlarmHistory from a <Proto3AlarmHistoryGladData> binary message
     * 
     * @param proto
     *            <Proto3AlarmHistoryGladData>
     * @throws GlobalLadDataException
     *             if an error occurs parsing the provided <Proto3AlarmHistoryGladData>
     */
	public AlarmHistoryGlobalLadData(final Proto3AlarmHistoryGladData proto) throws GlobalLadDataException {
		super(proto.getBase());
		
		alarmHistory = proto.getHistory();
		historyId = String.format("host-%s-venue-%s-session-%d", host, venue, sessionNumber);
	}

	@Override
	public Object getIdentifier() {
		return historyId;
	}

	/**
	 * @return the alarmHistory
	 */
	public Proto3AlarmHistory getAlarmHistory() {
		return alarmHistory;
	}

	@Override
	@JsonIgnore
	public Proto3GlobalLadTransport serialize() {
		return Proto3GlobalLadTransport.newBuilder()
				.setHistory(Proto3AlarmHistoryGladData.newBuilder()
						.setBase(serializeBase())
						.setHistory(alarmHistory)
						.build())
				.build();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((alarmHistory == null) ? 0 : alarmHistory.hashCode());
		result = prime * result + ((historyId == null) ? 0 : historyId.hashCode());
		return result;
	}


	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final AlarmHistoryGlobalLadData other = (AlarmHistoryGlobalLadData) obj;
		if (alarmHistory == null) {
			if (other.alarmHistory != null)
				return false;
		} else if (!alarmHistory.equals(other.alarmHistory))
			return false;
		if (historyId == null) {
			if (other.historyId != null)
				return false;
		} else if (!historyId.equals(other.historyId))
			return false;
		return true;
	}

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("AlarmHistoryGlobalLadData [");

        builder.append("historyId=");
        builder.append(historyId);
        builder.append(", alarmHistory=");
        builder.append(alarmHistory);
        builder.append(" \n\t");

        builder.append(super.toString());

        builder.append("]");
        return builder.toString();
    }
}
