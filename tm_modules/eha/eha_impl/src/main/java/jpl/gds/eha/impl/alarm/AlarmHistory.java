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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jpl.gds.alarm.serialization.AlarmStatistics.Proto3AlarmStatisticsMap;
import jpl.gds.alarm.serialization.AlarmStatistics.Proto3MonitorAlarmStatisticsMap;
import jpl.gds.alarm.serialization.AlarmStatistics.Proto3MonitorAlarmStatisticsStationMap;
import jpl.gds.alarm.serialization.Proto3AlarmHistory;
import jpl.gds.alarm.serialization.Proto3ChannelValueTable;
import jpl.gds.alarm.serialization.Proto3LastValueTable;
import jpl.gds.alarm.serialization.Proto3MonitorChannelValueTable;
import jpl.gds.alarm.serialization.Proto3MonitorLastValueTable;
import jpl.gds.alarm.serialization.Proto3MonitorLastValueTableMap;
import jpl.gds.alarm.serialization.Proto3MonitorValueTableMap;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider;
import jpl.gds.eha.api.channel.alarm.IAlarmStatistics;
import jpl.gds.eha.api.channel.alarm.IChannelAlarm;
import jpl.gds.eha.api.channel.serialization.Proto3ChannelValue;
import jpl.gds.eha.api.channel.serialization.Proto3ServiceCollection;
import jpl.gds.eha.impl.channel.ChannelValueFactory;
import jpl.gds.shared.log.Tracer;

/**
 * This class is used by alarm processing to keep a history of channel values
 * needed to calculate alarms, indexed by the alarm definition object. An alarm
 * definition can add as many values as it needs to for a specific channel in
 * order to perform alarm calculations. Values in excess of the history limit
 * are removed as new values are added.
 *
 * Updated for alarm dictionary re-factoring, now
 * uses AbstractAlarm objects rather than alarm definition objects
 * 
 */
public class AlarmHistory implements IAlarmHistoryProvider
{
	/** The table of lists of channel values...indexed by owning alarm definition's ID
	 * 
	 *  NOTE: This should be a linked list, not an array list because we're
	 *	constantly adding values to the front and removing from the end (brn)
	 */
	protected final Map<String, LinkedList<IServiceChannelValue>> rtChanValTable;
	protected final Map<String, IAlarmStatistics> rtStatsTable;
	/*
	 * 01/16/2014 - New table to save just the last
	 * channel value used in calculating hysteresis. Decided to create a
	 * separate table rather than rely on rtChanValTable because the purpose of
	 * rtChanValTable is not for holding hysteresis history, but for holding the
	 * history of those alarms that by itself require history. Which means that
	 * most alarms do not even have entries in the rtChanValTable. Rather than
	 * making the across-the-board change of having all alarms now insert
	 * history into rtChanValTable, chose the minimal-risk and probably more
	 * memory efficient approach (under the assumption that alarms with
	 * hysteresis will make up the minority, not the majority of alarms) of
	 * using this separate table.
	 */
	protected final Map<String, IServiceChannelValue> rtLastValueTable;

	protected final Map<String, LinkedList<IServiceChannelValue>> recChanValTable;
	protected final Map<String, IAlarmStatistics> recStatsTable;
	/*
	 * New table to save just the last
	 * channel value used in calculating hysteresis. Decided to create a
	 * separate table rather than rely on recChanValTable because the purpose of
	 * recChanValTable is not for holding hysteresis history, but for holding
	 * the history of those alarms that by itself require history. Which means
	 * that most alarms do not even have entries in the recChanValTable. Rather
	 * than making the across-the-board change of having all alarms now insert
	 * history into recChanValTable, chose the minimal-risk and probably more
	 * memory efficient approach (under the assumption that alarms with
	 * hysteresis will make up the minority, not the majority of alarms) of
	 * using this separate table.
	 */
	protected final Map<String, IServiceChannelValue> recLastValueTable;

	/*
	 * New tables needed for station monitor
	 * data segregation. Note that this is a real-time only table.
	 */
	protected final Map<String, Map<Integer, LinkedList<IServiceChannelValue>>> monChanValTable;
	protected final Map<String, Map<Integer, IAlarmStatistics>> monStatsTable;
	/*
	 * 01/16/2014 - New table to save just the last
	 * channel value used in calculating hysteresis. Decided to create a
	 * separate table rather than rely on monChanValTable because the purpose of
	 * monChanValTable is not for holding hysteresis history, but for holding
	 * the history of those alarms that by itself require history. Which means
	 * that most alarms do not even have entries in the monChanValTable. Rather
	 * than making the across-the-board change of having all alarms now insert
	 * history into monChanValTable, chose the minimal-risk and probably more
	 * memory efficient approach (under the assumption that alarms with
	 * hysteresis will make up the minority, not the majority of alarms) of
	 * using this separate table.
	 */
	protected final Map<String, Map<Integer, IServiceChannelValue>> monLastValueTable;


	/**
	 * Creates an instance of AlarmHistory.
	 */
	public AlarmHistory()
	{

		rtChanValTable = new HashMap<String, LinkedList<IServiceChannelValue>>();
		rtStatsTable = new HashMap<String, IAlarmStatistics>();
		rtLastValueTable = new HashMap<String, IServiceChannelValue>();
		recChanValTable = new HashMap<String, LinkedList<IServiceChannelValue>>();
		recStatsTable = new HashMap<String, IAlarmStatistics>();
		recLastValueTable = new HashMap<String, IServiceChannelValue>();

		/*
		 * Initialize new tables for station monitor data segregation.
		 */
		monChanValTable = new HashMap<String, Map<Integer, LinkedList<IServiceChannelValue>>>();
		monStatsTable = new HashMap<String, Map<Integer, IAlarmStatistics>>();
		monLastValueTable = new HashMap<String, Map<Integer, IServiceChannelValue>>();
	}

	/**
	 * Creates an instance of AlarmHistory.
	 */
	public AlarmHistory(final Proto3AlarmHistory proto, final IChannelDefinitionProvider chanDict, final Tracer log)
	{
		this();
		
		/**
		 * This is a long set of code since we have to unpack all the values in the alarm history proto and fill up nine maps.  
		 * Each map needs a block of code.
		 */

		ChannelValueFactory factory = new ChannelValueFactory();
		
		/**
		 * Load the real time channel tables.
		 */
		loadValues(true, proto.getRtChanValTable(), factory, chanDict, log);
		loadLastValue(true, proto.getRtLastValueTable(), factory, chanDict, log);
		loadStats(true, proto.getRtStatsTable());

		/**
		 * Load the recorded channel tables.
		 */
		loadValues(false, proto.getRecChanValTable(), factory, chanDict, log);
		loadLastValue(false, proto.getRecLastValueTable(), factory, chanDict, log);
		loadStats(false, proto.getRecStatsTable());
		
		/**
		 * Build the monitor tables.
		 */
		load(proto.getMonChanValTable(), factory, chanDict, log);
		loadStats(proto.getMonStatsTable());
		loadLastValue(proto.getMonLastValueTable(), factory, chanDict, log);
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider#merge(jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider)
	 */
	@Override
	public void merge(IAlarmHistoryProvider history) {
		/**
		 * Can only work with the AlarmHistory directly.
		 */
		if (history instanceof AlarmHistory) {
			AlarmHistory h = (AlarmHistory) history;

			rtChanValTable.putAll(h.rtChanValTable);
			rtLastValueTable.putAll(h.rtLastValueTable);
			rtStatsTable.putAll(h.rtStatsTable);

			recChanValTable.putAll(h.recChanValTable);
			recLastValueTable.putAll(h.recLastValueTable);
			recStatsTable.putAll(h.recStatsTable);
			
			monChanValTable.putAll(h.monChanValTable);
			monLastValueTable.putAll(h.monLastValueTable);
			monStatsTable.putAll(h.monStatsTable);
			
		} else {
			System.out.println("Not a know type of alarm history provider.");
			
		}
	}

	/**
	 * Loads the monitor station stats map.
	 * @param isRealtime
	 * @param table
	 */
	private void loadLastValue(Proto3MonitorLastValueTable table, 
			ChannelValueFactory factory, IChannelDefinitionProvider chanDict, final Tracer log) {
		table.getLastValuesMap().entrySet().stream()
		
		.forEach(monitorEs -> {
			Map<Integer, IServiceChannelValue> stationMap = new HashMap<>();

			monitorEs.getValue().getLastValuesMap().entrySet()
			/**
			 * These are the stations values keyed by station id.
			 */
			.forEach(stationEs -> {
				Proto3ChannelValue channelProto = stationEs.getValue();
				IChannelDefinition def = chanDict.getDefinitionFromChannelId(channelProto.getChannelId());

				if (def == null) {
					log.warn("Channel definition not found for channel ID " + channelProto.getChannelId());
				} else {
					IServiceChannelValue cv = factory.createServiceChannelValue(def);
					cv.load(channelProto);

					stationMap.put(stationEs.getKey(), cv);
				}
			});
			
			/**
			 * Once the station map is done, add to the main map.
			 */
			monLastValueTable.put(monitorEs.getKey(), stationMap);
		});
	}
	
	/**
	 * Load the last value table for realtime or recorded.
	 * @param isRealtime
	 * @param table
	 */
	private void loadLastValue(boolean isRealtime, Proto3LastValueTable table, 
			ChannelValueFactory factory, IChannelDefinitionProvider chanDict, final Tracer log) {
		Map<String, IServiceChannelValue> target = isRealtime ? rtLastValueTable : recLastValueTable;
		table.getLastValuesMap().entrySet().stream()
		
		.forEach(chanEs -> {
			Proto3ChannelValue channelProto = chanEs.getValue();
			IChannelDefinition def = chanDict.getDefinitionFromChannelId(channelProto.getChannelId());

			if (def == null) {
				log.warn("Channel definition not found for channel ID " + channelProto.getChannelId());
			} else {
				IServiceChannelValue cv = factory.createServiceChannelValue(def);
				cv.load(channelProto);
				
				target.put(chanEs.getKey(), cv);
			}
		});
	}

	/**
	 * Loads the monitor station stats map.
	 * @param isRealtime
	 * @param table
	 */
	private void loadStats(Proto3MonitorAlarmStatisticsMap table) {
		table.getStatsMapMap().entrySet().stream()
		
		.forEach(monitorEs -> {
			Map<Integer, IAlarmStatistics> stationMap = new HashMap<>();

			monitorEs.getValue().getIntegerMapMap().entrySet()
			/**
			 * These are the stations values keyed by station id.
			 */
			.forEach(stationEs -> {
				stationMap.put(stationEs.getKey(), new AlarmStatistics(stationEs.getValue()));
			});
			
			/**
			 * Once the station map is done, add to the main map.
			 */
			this.monStatsTable.put(monitorEs.getKey(), stationMap);
		});
	}
	
	/**
	 * Load the stats table for realtime or recorded.
	 * @param isRealtime
	 * @param table
	 */
	private void loadStats(boolean isRealtime, Proto3AlarmStatisticsMap table) {
		Map<String, IAlarmStatistics> target = isRealtime ? rtStatsTable : recStatsTable;
		table.getStatsMapMap().entrySet().stream()
		
		.forEach(statsEs -> {
			target.put(statsEs.getKey(), new AlarmStatistics(statsEs.getValue()));
		});
	}
	
	/**
	 * Used to fill the rt and rec tables.  This is an attempt to reuse as much code as possible
	 * @param isRealtime 
	 * @param table
	 * @param chanDict
	 * @param log
	 */
	private void loadValues(boolean isRealtime, Proto3ChannelValueTable table, ChannelValueFactory factory, IChannelDefinitionProvider chanDict, final Tracer log) {
		Map<String, LinkedList<IServiceChannelValue>> target = isRealtime ? this.rtChanValTable : recChanValTable;
		table.getValuesMap().entrySet().stream()
		/**
		 * The entry set is channelId, list of data.  These need to be converted to service channels 
		 * and added to a linked list in the order they are encountered.
		 */
		.forEach(es -> {
			LinkedList<IServiceChannelValue> chans = new LinkedList<>();

			/**
			 * The value is the list of channels that go into the linked list.
			 */
			es.getValue().getChannelsList().stream().forEachOrdered(channelProto -> {
				IChannelDefinition def = chanDict.getDefinitionFromChannelId(channelProto.getChannelId());

				if (def == null) {
					log.warn("Channel definition not found for channel ID " + channelProto.getChannelId());
				} else {
					IServiceChannelValue cv = factory.createServiceChannelValue(def);
					cv.load(channelProto);

					chans.add(cv);
				}

			});
			
			/**
			 * Once the list is created, add it to the main map.
			 */
			target.put(es.getKey(), chans);
		});
	}	
	
	/**
	 * Used to fill the rt and rec tables.  This is an attempt to reuse as much code as possible
	 * @param isRealtime 
	 * @param monTable
	 * @param chanDict
	 * @param log
	 */
	private void load(Proto3MonitorChannelValueTable monTable, ChannelValueFactory factory, IChannelDefinitionProvider chanDict, final Tracer log) {
		monTable.getValuesMap().entrySet().stream()
		/**
		 * The entry set is channelId, to an integer to list of data map.  These need to be converted to service channels 
		 * and added to a linked list in the order they are encountered.
		 */
		.forEach(es -> {
			/**
			 * The data in this will be another map keyed by the station id.
			 */
			Map<Integer, LinkedList<IServiceChannelValue>> stationMap = new HashMap<>();

			es.getValue().getValuesMap().entrySet().stream()
			.forEach(stationMapEs -> {
				LinkedList<IServiceChannelValue> chans = new LinkedList<>();

				/**
				 * The value is the list of channels that go into the linked list.
				 */
				stationMapEs.getValue().getChannelsList().stream().forEachOrdered(channelProto -> {
					IChannelDefinition def = chanDict.getDefinitionFromChannelId(channelProto.getChannelId());

					if (def == null) {
						log.warn("Channel definition not found for channel ID " + channelProto.getChannelId());
					} else {
						IServiceChannelValue cv = factory.createServiceChannelValue(def);
						cv.load(channelProto);

						chans.add(cv);
					}
				});

				/**
				 * Once the list is created, add it to the sub map.
				 */
				stationMap.put(stationMapEs.getKey(), chans);
			});
			
			/**
			 * Once the station ID map is done, add to the main monitor table.
			 */
			monChanValTable.put(es.getKey(), stationMap);
		});
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider#addNewValue(jpl.gds.eha.api.channel.alarm.IChannelAlarm, jpl.gds.eha.api.channel.IServiceChannelValue)
     */
	@Override
    public synchronized void addNewValue(final IChannelAlarm alarm, final IServiceChannelValue val)
	{
		if (alarm == null) {
			throw new IllegalArgumentException("Null alarm passed in");
		} else if (val == null) {
			throw new IllegalArgumentException("Null value passed in");
		}

		// get the vector of values
		LinkedList<IServiceChannelValue> history = null;
		final String alarmId = alarm.getDefinition().getAlarmId();

		/*
		 * If the channel value is a
		 * monitor data, use the new monitor data specific table for keeping
		 * track.
		 */
		if (val.getDefinitionType() == ChannelDefinitionType.M) {
			Map<Integer, LinkedList<IServiceChannelValue>> stationHistoryTable = monChanValTable
					.get(alarmId);

			if (stationHistoryTable == null) {
				stationHistoryTable = new HashMap<Integer, LinkedList<IServiceChannelValue>>(
						1);
				monChanValTable.put(alarmId, stationHistoryTable);
			}

			history = stationHistoryTable.get(val.getDssId());
		} else if (val.isRealtime()) {
			history = rtChanValTable.get(alarmId);
		} else {
			history = recChanValTable.get(alarmId);
		}
		// this channel doesn't exist in the table we have to create the list
		if (history == null)
		{
			history = new LinkedList<IServiceChannelValue>();
		}

		// if the list is longer than the requested length, remove the oldest value(s) 
		// (the last value(s) in the list)
		while(history.size() > 0 && history.size() >= alarm.getDefinition().getHistoryCount())
		{
			history.removeLast();
		}

		// add this new value at the front of the list and put it back in the table
		history.addFirst(val);

		/*
		 * If the channel value is a
		 * monitor data, use the new monitor data specific table for keeping
		 * track.
		 */
		if (val.getDefinitionType() == ChannelDefinitionType.M) {
			monChanValTable.get(alarmId)
			.put(val.getDssId(), history);
		} else if (val.isRealtime()) {
			rtChanValTable.put(alarmId, history);
		} else {
			recChanValTable.put(alarmId, history);
		}

	}


	    /**
     * {@inheritDoc}
     */
	@Override
    public synchronized void updateStatistics(
			final IChannelAlarm alarm,
			final IAlarmStatistics stats, final boolean isRealtime,
			final boolean isMonitor, final Integer dss) {

		final String id = alarm.getDefinition().getAlarmId();

		/*
		 * Added updating of station monitor data statistics table.
		 */
		if (isMonitor) {

			if (dss == null) {
				throw new IllegalArgumentException(
						"dss cannot be null when updating monitor data statistics");
			}

			Map<Integer, IAlarmStatistics> stationStatsTable = monStatsTable
					.get(id);

			if (stationStatsTable == null) {
				stationStatsTable = new HashMap<Integer, IAlarmStatistics>(1);
				monStatsTable.put(id, stationStatsTable);
			}

			stationStatsTable.put(dss, stats);

		} else if (isRealtime) {
			rtStatsTable.put(id, stats);
		} else {
			recStatsTable.put(id, stats);
		}
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider#getStatistics(jpl.gds.eha.api.channel.alarm.IChannelAlarm, boolean, boolean, java.lang.Integer)
     */
	@Override
    public synchronized IAlarmStatistics getStatistics(
			final IChannelAlarm alarm, final boolean isRealtime,
			final boolean isMonitor, final Integer dss)	{
		IAlarmStatistics stats = null;

		final String id = alarm.getDefinition().getAlarmId();

		/*
		 * Added retrieving of station monitor data statistics table.
		 */
		if (isMonitor) {

			if (dss == null) {
				throw new IllegalArgumentException(
						"dss cannot be null when querying monitor data statistics");
			}

			final Map<Integer, IAlarmStatistics> stationStatsTable = monStatsTable
					.get(id);

			if (stationStatsTable != null) {
				stats = stationStatsTable.get(dss);
			}

		} else if (isRealtime) {
			stats = rtStatsTable.get(id);
		} else {
			stats = recStatsTable.get(id);
		}

		if (stats == null) {
			stats = new AlarmStatistics();
		}

		return(stats);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider#getMostRecentValue(jpl.gds.eha.api.channel.alarm.IChannelAlarm, boolean, boolean, java.lang.Integer)
     */
	@Override
    public synchronized IServiceChannelValue getMostRecentValue(
			final IChannelAlarm alarm, final boolean isRealtime,
			final boolean isMonitor, final Integer dss) {

		if (alarm == null) {
			throw new IllegalArgumentException("Null input alarm definition");
		}

		final String id = alarm.getDefinition().getAlarmId();

		// get the history of the element
		LinkedList<IServiceChannelValue> history = null;

		/*
		 * If trying to retrieve station monitor data, use the monitor data
		 * specific table.
		 */
		if (isMonitor) {

			if (dss == null) {
				throw new IllegalArgumentException(
						"dss cannot be null when querying station monitor data");
			}

			final Map<Integer, LinkedList<IServiceChannelValue>> stationHistoryTable = monChanValTable
					.get(id);

			if (stationHistoryTable != null) {
				history = stationHistoryTable.get(dss);
			}

		}
		else if (isRealtime) {
			history = rtChanValTable.get(id);
		} else {
			history = recChanValTable.get(id);
		}

		// if this channel doesn't exist in the value table, return null
		if (history == null) {
			return (null);
		}

		// return the most recent value
		return history.getFirst();
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider#getValueHistory(jpl.gds.eha.api.channel.alarm.IChannelAlarm, boolean, boolean, java.lang.Integer)
     */
	@Override
    public synchronized List<IServiceChannelValue> getValueHistory(
			final IChannelAlarm alarm, final boolean isRealtime,
			final boolean isMonitor, final Integer dss)	{
		if (alarm == null)
		{
			throw new IllegalArgumentException("Null input alarm definition");
		}

		final String id = alarm.getDefinition().getAlarmId();

		/*
		 * If trying to retrieve station
		 * monitor data, use the monitor data specific table.
		 */
		if (isMonitor) {

			if (dss == null) {
				throw new IllegalArgumentException(
						"dss cannot be null when querying station monitor data");
			}

			final Map<Integer, LinkedList<IServiceChannelValue>> stationHistoryTable = monChanValTable
					.get(id);

			if (stationHistoryTable == null) {
				return null;

			} else {
				return stationHistoryTable.get(dss);
			}

		} else if (isRealtime) {
			return rtChanValTable.get(id);
		} else {
			return recChanValTable.get(id);
		}

	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider#clearValues(jpl.gds.eha.api.channel.alarm.IChannelAlarm)
     */
	@Override
    public synchronized void clearValues(final IChannelAlarm alarm) {
		final String id = alarm.getDefinition().getAlarmId();

		rtChanValTable.remove(id);
		rtStatsTable.remove(id);
		rtLastValueTable.remove(id);
		recChanValTable.remove(id);
		recStatsTable.remove(id);
		recLastValueTable.remove(id);

		/*
		 * Clear station monitor data
		 * tables, too. Note that this wipes out data for all DSS.
		 */
		monChanValTable.remove(id);
		monStatsTable.remove(id);
		monLastValueTable.remove(id);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider#clearValues()
     */
	@Override
    public synchronized void clearValues()
	{
		rtChanValTable.clear();
		rtStatsTable.clear();
		rtLastValueTable.clear();
		recChanValTable.clear();
		recStatsTable.clear();
		recLastValueTable.clear();

		/*
		 * Clear station monitor data tables, too.
		 */
		monChanValTable.clear();
		monStatsTable.clear();
		monLastValueTable.clear();
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider#setLastValueInStatistics(jpl.gds.eha.api.channel.alarm.IChannelAlarm, boolean, boolean, java.lang.Integer, jpl.gds.eha.api.channel.IServiceChannelValue)
     */
	@Override
    public void setLastValueInStatistics(
			final IChannelAlarm alarm, final boolean isRealtime,
			final boolean isMonitor, final Integer dss,
			final IServiceChannelValue value) {

		final String id = alarm.getDefinition().getAlarmId();

		/*
		 * Saves the channel value.
		 */
		if (isMonitor) {

			if (dss == null) {
				throw new IllegalArgumentException(
						"dss cannot be null when saving last monitor channel value");
			}

			Map<Integer, IServiceChannelValue> stationLastValueTable = monLastValueTable
					.get(id);

			if (stationLastValueTable == null) {
				stationLastValueTable = new HashMap<Integer, IServiceChannelValue>(1);
				monLastValueTable.put(id, stationLastValueTable);
			}

			stationLastValueTable.put(dss, value);

		} else if (isRealtime) {
			rtLastValueTable.put(id, value);
		} else {
			recLastValueTable.put(id, value);
		}

	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider#getLastValueInStatistics(jpl.gds.eha.api.channel.alarm.IChannelAlarm, boolean, boolean, java.lang.Integer)
     */
	@Override
    public IServiceChannelValue getLastValueInStatistics(
			final IChannelAlarm alarm, final boolean isRealtime,
			final boolean isMonitor, final Integer dss) {
		IServiceChannelValue value = null;

		/*
		 * Retrieves the last channel value.
		 */
		if (isMonitor) {

			if (dss == null) {
				throw new IllegalArgumentException(
						"dss cannot be null when querying last monitor channel value");
			}

			final Map<Integer, IServiceChannelValue> stationLastValueTable = monLastValueTable
					.get(alarm.getDefinition().getAlarmId());

			if (stationLastValueTable != null) {
				value = stationLastValueTable.get(dss);
			}

		} else if (isRealtime) {
			value = rtLastValueTable.get(alarm.getDefinition().getAlarmId());
		} else {
			value = recLastValueTable.get(alarm.getDefinition().getAlarmId());
		}

		return value;
	}
	
	private Proto3ChannelValueTable buildChannelTable(boolean isRealtime) {
		Map<String, LinkedList<IServiceChannelValue>> target = isRealtime ? rtChanValTable : recChanValTable;
		Proto3ChannelValueTable.Builder valueTable = Proto3ChannelValueTable.newBuilder();
		
		target.entrySet().stream().forEach(vals -> {
			Proto3ServiceCollection chanVals = Proto3ServiceCollection.newBuilder().addAllChannels(vals.getValue().stream()
					.map(v -> v.build()).collect(Collectors.toList()))
			.build();

			// Once done, add to the top builder.
			valueTable.putValues(vals.getKey(), chanVals);
		});

		return valueTable.build();
	}


	private Proto3LastValueTable buildLastValueTable(boolean isRealtime) {
		Map<String, IServiceChannelValue> target = isRealtime ? rtLastValueTable : recLastValueTable;
		
		Proto3LastValueTable.Builder builder = Proto3LastValueTable.newBuilder();
		
		target.entrySet().stream().forEach(es -> {
			builder.putLastValues(es.getKey(), es.getValue().build());
		});
		
		return builder.build();
	}

	private Proto3AlarmStatisticsMap buildAlarmsStatsTable(boolean isRealtime) {
		Map<String, IAlarmStatistics> target = isRealtime ? rtStatsTable : recStatsTable;
		Proto3AlarmStatisticsMap.Builder builder = Proto3AlarmStatisticsMap.newBuilder();

		target.entrySet().stream().forEach(es -> {
			builder.putStatsMap(es.getKey(), es.getValue().build());
		});
		
		return builder.build();
	}

	// Monitor build methods.

	private Proto3MonitorChannelValueTable buildMonitorChannelTable() {
		Proto3MonitorChannelValueTable.Builder builder = Proto3MonitorChannelValueTable.newBuilder();

		monChanValTable.entrySet().stream().forEach(monitorEs -> {
			Proto3MonitorValueTableMap.Builder mapBuilder = Proto3MonitorValueTableMap.newBuilder();

			monitorEs.getValue().entrySet().stream().forEach(stationEs -> {
				Proto3ServiceCollection chanVals = Proto3ServiceCollection.newBuilder().addAllChannels(stationEs.getValue().stream()
						.map(v -> v.build()).collect(Collectors.toList()))
						.build();
				/**
				 * Add to the station builder.
				 */
				mapBuilder.putValues(stationEs.getKey(), chanVals);
			});
			
			/**
			 * Add to the main builder.
			 */
			builder.putValues(monitorEs.getKey(), mapBuilder.build());
		});

		return builder.build();
	}

	private Proto3MonitorLastValueTable buildMonitorLastValueTable() {
		Proto3MonitorLastValueTable.Builder builder = Proto3MonitorLastValueTable.newBuilder();
		
		monLastValueTable.entrySet().stream().forEach(monitorEs -> {
			Proto3MonitorLastValueTableMap.Builder lastBuilder = Proto3MonitorLastValueTableMap.newBuilder();
			
			monitorEs.getValue().entrySet().stream().forEach(stationEs -> 
				lastBuilder.putLastValues(stationEs.getKey(), stationEs.getValue().build()));
			
			builder.putLastValues(monitorEs.getKey(), lastBuilder.build());
		});
		
		return builder.build();
	}


	private Proto3MonitorAlarmStatisticsMap buildMonitorAlarmsStatsTable() {
		Proto3MonitorAlarmStatisticsMap.Builder builder = Proto3MonitorAlarmStatisticsMap.newBuilder();
		
		monStatsTable.entrySet().stream().forEach(monitorEs -> {
			Proto3MonitorAlarmStatisticsStationMap.Builder statsBuilder = Proto3MonitorAlarmStatisticsStationMap.newBuilder();
			
			monitorEs.getValue().entrySet().stream().forEach(stationEs -> 
				statsBuilder.putIntegerMap(stationEs.getKey(), stationEs.getValue().build()));
			
			builder.putStatsMap(monitorEs.getKey(), statsBuilder.build());
		});
		
		return builder.build();
	}

	
	@Override
	public Proto3AlarmHistory build() {
		return Proto3AlarmHistory.newBuilder()
				// Tables
				.setRtChanValTable(buildChannelTable(true))
				.setRecChanValTable(buildChannelTable(false))
				.setMonChanValTable(buildMonitorChannelTable())
				
				// last value tables
				.setRtLastValueTable(buildLastValueTable(true))
				.setRecLastValueTable(buildLastValueTable(false))
				.setMonLastValueTable(buildMonitorLastValueTable())
				
				// Stats tables
				.setRtStatsTable(buildAlarmsStatsTable(true))
				.setRecStatsTable(buildAlarmsStatsTable(false))
				.setMonStatsTable(buildMonitorAlarmsStatsTable())
		.build();
	}
}