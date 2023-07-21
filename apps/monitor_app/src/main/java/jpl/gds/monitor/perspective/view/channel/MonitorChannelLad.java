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
package jpl.gds.monitor.perspective.view.channel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.utilities.LadConsumer;
import jpl.gds.monitor.config.MonitorGuiProperties;
import jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.types.Pair;


/**
 * The MonitorChannelLad class is used to represent the local LAD in a running
 * chill_monitor instance. It maintains the last N values for all channels
 * received by the monitor, and keeps the latest channel timestamp fields. Separate
 * LAD histories are kept for realtime and recorded samples. For monitor channels,
 * separate LAD history is kept by station.
 * 
 * This class uses a singleton pattern.
 */
public final class MonitorChannelLad {

	/* The single static instance of the LAD */
//	private static MonitorChannelLad instance;


	private boolean gotLad = false;
	private Timer ladTimer;
	
	// Station registration/de-registration.
	// registeredStations is a map of the object (GUI view composite)
	// registering and the station number for which it is registering.
	private final Map<Object, Integer> registeredStations;

	private final boolean globalLadEnabled;
	
	private boolean monitorIsGGui;

	/*
	 * The tables of vectors/lists of channel samples, keyed by channel ID and station. Station
	 * will be 0 for non-monitor channels.  Note that monitor channels go into the realtime table.
	 * We could have another table, but all the code gets a lot more complex if we do.
	 */
	private final Map<Pair<String, Integer>,List<MonitorChannelSample>> realtimeLadTable = new HashMap<Pair<String, Integer>,List<MonitorChannelSample>>();
	private final Map<Pair<String, Integer>,List<MonitorChannelSample>> recordedLadTable = new HashMap<Pair<String, Integer>,List<MonitorChannelSample>>();

	/*
	 * The depth of the LAD.
	 */
	private final int depth;

	/*
	 * Latest time trackers for recorded, realtime, and combined LAD tables.
	 */
	private final LatestChannelTimes recordedTimes = new LatestChannelTimes();
	private final LatestChannelTimes realtimeTimes = new LatestChannelTimes();
	private final LatestChannelTimes allTimes = new LatestChannelTimes();
	
	/*
	 * Object for synchronizing the tables.
	 */
	private final Object ladLock = new Object();
	private final ApplicationContext appContext;
	private final TimeComparisonStrategyContextFlag timeStrategy;
	private final IChannelDefinitionProvider chanDefs;


	/**
     * Creates an instance of MonitorChannelLad.
     * 
     * @param appContext
     *            the current application context
     */
	public MonitorChannelLad(final ApplicationContext appContext)
	{
		this.appContext = appContext;
		timeStrategy = appContext.getBean(TimeComparisonStrategyContextFlag.class);
		this.chanDefs = this.appContext.getBean(IChannelDefinitionProvider.class);
		

		globalLadEnabled = GlobalLadProperties.getGlobalInstance().isEnabled();


		depth = appContext.getBean(MonitorGuiProperties.class).getLadDepth();
		registeredStations = new HashMap<Object, Integer>();
	}
	
	/**
	 * Sets the "monitor is GUI" flag.  If this is not set, no LAD fetches will be
	 * performed.
	 * 
	 * @param isGui true or false
	 */
	public void setMonitorIsGui(final boolean isGui) {
	    this.monitorIsGGui = isGui;
	}

	/**
	 * Add a new (most recent) sample for a channel
	 * 
	 * @param val The new sample to add to the table
	 */
	public void addNewValue(final MonitorChannelSample val)
	{
		if (val == null)
		{
			throw new IllegalArgumentException("Null channel value passed in");
		}
		final ChannelDefinitionType chanType = getChannelType(val.getChanId());

		/*
		 * Reject header or monitor channels with non-realtime status.
		 * @ToDo("Consider whether header channels should always be realtime or not.")
		 */
		if (!val.isRealtime() && (chanType == ChannelDefinitionType.H ||
				chanType == ChannelDefinitionType.M)) {
			throw new IllegalArgumentException("This LAD does not accept recorded M or H channels");
		}

		/*
		 * Get the map ID, which will be the key in the LAD table, for this channel.
		 */
		final Pair<String, Integer> mapId = getMapIdFromChannelId(val.getChanId(), val.getDssId());

		synchronized(this.ladLock) {

			/*
			 * Get the history vector we currently have for this channel, either from the
			 * realtime LAD table or the recorded LAD table.
			 */		
			List<MonitorChannelSample> historyVector = val.isRealtime() ? this.realtimeLadTable.get(mapId) :
				this.recordedLadTable.get(mapId);

			/*
			 * This channel doesn't exist in the LAD table, so we have to create the history vector
			 */
			if (historyVector == null)
			{
				historyVector = new ArrayList<MonitorChannelSample>(this.depth);
			}

			final MonitorChannelSample lastSample = historyVector.isEmpty() ? null: historyVector.get(0);
			if (lastSample != null) {
				/*
				 * Determine whether the new value has a later timestamp than
				 * the value currently in the LAD. If it DOES NOT, it is not
				 * added to the LAD, but the UTC is updated in the overall time
				 * tracking objects to indicate that some data flow was seen.
				 */
				if(!ChannelSampleUtility.timestampIsLater(timeStrategy, lastSample, val)) {

					if (val.isRealtime()) {
						this.realtimeTimes.updateTimestamp();
					} else {
						this.recordedTimes.updateTimestamp();
					}
					this.allTimes.updateTimestamp();
					return;
				}
				/*
				 * Determine whether the new
				 * value already exists in the LAD. This situation can occur 
				 * when the same channel value is added from the global LAD 
				 * and the message service.
				 */
				else if(ChannelSampleUtility.sameChannelValue(chanType, lastSample, val)) {
					return;
				}
			}

			/*
			 * If the history vector has reached maximum depth, remove the oldest value (the last value in the list)
			 */
			if(historyVector.size() == this.depth)
			{
				historyVector.remove(this.depth - 1); 
			}

			/*
			 * Add the new value at the front of the history vector
			 */
			historyVector.add(0,val);

			/*
			 * Put the history vector back into the LAD table it belongs in,
			 * and update the latest channel timestamps
			 */
			final ChannelDefinitionType type = getChannelType(val.getChanId());
			if (val.isRealtime()) {
				this.realtimeLadTable.put(mapId, historyVector);
				this.realtimeTimes.updateFromChannelValue(val, type);
			} else {
				this.recordedLadTable.put(mapId, historyVector);
				this.recordedTimes.updateFromChannelValue(val, type);
			}
			this.allTimes.updateFromChannelValue(val, type);
		}
	}

	/**
	 * Gets the definition type of a channel.
	 * 
	 * @param chanId the channel ID
	 * @return the channel definition type
	 */
	private ChannelDefinitionType getChannelType(final String chanId) {
		final IChannelDefinition def = this.chanDefs.getDefinitionFromChannelId(chanId);
		// If no channel definition assume FSW type. This happens when displays
		// contain out-of-date channels or are mismatched with current dictionary
		return def == null ? ChannelDefinitionType.FSW : def.getDefinitionType();
	}

	/**
	 * Returns the history vector for the channel with the given map key. Which history
	 * vector is returned is determined by the realtime/recorded filter type and the
	 * timestamp of the latest sample in each vector. 
	 * 
	 * @param mapId the map key for the channel to get history for
	 * @param filterType the RealtimeRecordedFilterType enum value indicating which type of data to get
	 * @return List of Channel Samples representing the sample history, latest sample
	 * at the front of the list; may return null if no history found
	 */
	private List<MonitorChannelSample> getHistoryVector(final Pair<String, Integer> mapId, final RealtimeRecordedFilterType filterType) {
		List<MonitorChannelSample> historyVector = null;

		switch(filterType) {
		case REALTIME:
			historyVector = this.realtimeLadTable.get(mapId);
			break;
		case RECORDED:
			historyVector = this.recordedLadTable.get(mapId);
			break;
		case BOTH:
			/*
			 * If both recorded and realtime data are requested, we want to 
			 * return the history vector with the latest sample in it.
			 */
			final List<MonitorChannelSample> historyVector1 = this.realtimeLadTable.get(mapId);
			final List<MonitorChannelSample> historyVector2 = this.recordedLadTable.get(mapId);
			if (historyVector1 == null || historyVector1.isEmpty()) {
				historyVector = historyVector2;
			} else if (historyVector2 == null || historyVector2.isEmpty()) {
				historyVector = historyVector1;
			} else {
				final MonitorChannelSample val1 = historyVector1.get(0);
				final MonitorChannelSample val2 = historyVector2.get(0);
				

				if (ChannelSampleUtility.timestampIsLater(timeStrategy, val1, val2)) {
					historyVector = historyVector2;
				} else {
					historyVector = historyVector1;
				}
			}
			break;
		default:
		    throw new IllegalArgumentException("Unsupported value for realtime/recorded filter type: " + filterType);
		}

		return historyVector;
	}

	/**
	 * Get the most recent sample for a channel.
	 * 
	 * @param chanId The channel ID of the channel whose most recent value is wanted
	 * @param  filterType the RealtimeRecordedFilterType enum value indicating which type of data to get
	 * @param dssId the station ID for monitor channels
	 * @return The most recent sample for the channel, or null if none found
	 */
	public MonitorChannelSample getMostRecentValue(final String chanId, final RealtimeRecordedFilterType filterType, final int dssId)
	{
		if(chanId == null)
		{
			throw new IllegalArgumentException("Null input channel ID");
		}

		/*
		 * Need the map ID for the requested channel to key into the LAD.
		 */
		final Pair<String,Integer> mapId = getMapIdFromChannelId(chanId, dssId);

		synchronized (this.ladLock) {

			/*
			 * Get the history vector with the right rt/recorded type and the
			 * latest sample.
			 */
			final List<MonitorChannelSample> historyVector = getHistoryVector(mapId, filterType);

			/*
			 * If this channel doesn't exist in the LAD, return null
			 */
			if (historyVector == null || historyVector.isEmpty())
			{
				return null;
			}

			/*
			 * Otherwise, return most recent value
			 */
			return historyVector.get(0);           
		}
	}

	/**
	 * Returns the map key for the given channel ID. The map key is used
	 * as key into the LAD tables. 
	 * 
	 * @param chanId the channel ID
	 * @param dssId the station ID (for monitor channels)
	 * @return Pair representing the map key
	 */
	private Pair<String, Integer> getMapIdFromChannelId(final String chanId, final int dssId) {
		if (getChannelType(chanId) == ChannelDefinitionType.M) {
			return new Pair<String, Integer>(chanId, dssId);
		} else {
		    return new Pair<String, Integer>(chanId, 0);
		}
	}

	/**
	 * Get the most recent samples for a list of channels.
	 * 
	 * @param channelIds The IDs of the channels whose most recent values are wanted
	 * @param filterType the RealtimeRecordedFilterType enum value indicating which type of data to get
	 * @param dssId the station ID for monitor channels
	 * @return List of the most recent samples for each channel, in the same order as the
	 * channelIds given as input. Note that an entry in the list may be returned as null if
	 * no sample was found for the requested channel. 
	 */
	public List<MonitorChannelSample> getLatestValues(final List<String> channelIds, final RealtimeRecordedFilterType filterType, final int dssId) {
		/*
		 * No channels requested.
		 */
		if (channelIds == null || channelIds.isEmpty()) {
			return null;
		}

		final List<MonitorChannelSample> result = new ArrayList<MonitorChannelSample>(channelIds.size());

		synchronized(this.ladLock) {
			for (final String id: channelIds) {
				final MonitorChannelSample data = getMostRecentValue(id, filterType, dssId);
				/*
				 * Note this will add nulls if no value found. This is very deliberate
				 * and is expected by the caller.
				 */
				result.add(data); 
			}
		}
		return result;
	}

	/**
	 * Get the entire LAD history for a particular channel.
	 * 
	 * @param chanId The ID of the channel whose value history is wanted
	 * @param filterType the RealtimeRecordedFilterType enum value indicating which type of data to get
	 * @param dssId the station ID for monitor channels
	 * @return non-modifiable list of values for the channel.  The first (0th) index of the
	 * List is the most recent value and the last (N-1) index of the List is
	 * the oldest value. Null is returned if there are no matching values in the LAD.
	 */
	public List<MonitorChannelSample> getValueHistory(final String chanId, final RealtimeRecordedFilterType filterType, final int dssId)
	{
		if(chanId == null) {
			throw new IllegalArgumentException("Null input channel ID");
		}

		/*
		 * Get the map ID for this channel, for use as key into the LAD tables
		 */
		final Pair<String, Integer> mapId = getMapIdFromChannelId(chanId, dssId);

		List<MonitorChannelSample> list = null;

		synchronized(this.ladLock) {

			if (filterType == RealtimeRecordedFilterType.REALTIME) {
				list = this.realtimeLadTable.get(mapId);
			} else if (filterType == RealtimeRecordedFilterType.RECORDED) {
				list = this.recordedLadTable.get(mapId);
			} else {
				/*
				 * If both recorded and realtime data is requested, the
				 * two history vectors must be combined.
				 */
				final List<MonitorChannelSample> historyVector1 = this.realtimeLadTable.get(mapId);
				final List<MonitorChannelSample> historyVector2 = this.recordedLadTable.get(mapId);
				list = combineSampleLists(chanId, historyVector1, historyVector2);
			}
			
			/*
			 * Copies the contents of list to a local list
			 * in order to allow iteration through it without the danger of a ConcurrentModificationException.
			 * 
			 * The snapshot copy of this list for return does not need synchronization because we are
			 * still within the synchronization on this.ladLock.
			 */
			return (null == list) ? null : Collections.unmodifiableList(new ArrayList<MonitorChannelSample>(list));
		}
	}

	/**
	 * Combines two channel history vectors into one, sorted according to the
	 * primary timestamp configured for the Global LAD, with duplicates removed.
	 * 
	 * @param chanId the channel ID
	 * @param rtList first history vector (realtime)
	 * @param recList second history vector (recorded)
	 * @return combined history vector, as list of channel samples; may be null if both input
	 * lists are null or empty
	 */
	private List<MonitorChannelSample> combineSampleLists(final String chanId, final List<MonitorChannelSample> rtList, final List<MonitorChannelSample> recList) {
		if (rtList == null || rtList.isEmpty()) {
			return recList;
		}
		if (recList == null || recList.isEmpty()) {
			return rtList;
		}
		/*
		 * Start with all the realtime samples in the result list.
		 */
		final List<MonitorChannelSample> result = new ArrayList<MonitorChannelSample>(rtList);
		/*
		 * Go through the recorded list. Insert recorded items into the result list
		 * based upon timestamp.
		 */
		for (final MonitorChannelSample sampleToCheck: recList) {
			int resultIndex = 0;
			boolean found = false;
			for (final MonitorChannelSample sampleInResult: result) {
				/*
				 * This will insert the recorded sample into the result list 
				 * before the realtime sample, if the recorded sample has a
				 * a later timestamp.  If the recorded sample has an equal 
				 * timestamp, it will end up being inserted after the realtime 
				 * sample.
                 */
				if (ChannelSampleUtility.timestampIsLater(timeStrategy, sampleInResult, sampleToCheck)) {
					result.add(resultIndex, sampleToCheck);
					found = true;
					break;
				}
				resultIndex++;
			}
			if (!found) {
				result.add(sampleToCheck);
			}
		}

		/*
		 * The process above still leaves duplicates in the result list for cases in
		 * which both realtime and recorded samples may have been received. Note
		 * that this does not apply to either monitor or header channels, which have
		 * only realtime equivalents. So we skip the duplicate removal step.
		 */
		final ChannelDefinitionType type = getChannelType(chanId);
		if (type != ChannelDefinitionType.M && type != ChannelDefinitionType.H) {

			final List<MonitorChannelSample> toRemove = new LinkedList<MonitorChannelSample>();
			/*
			 * Must weed out samples with duplicate SCLKs. If realtime and recorded
			 * samples with the same SCLK are found, they are considered duplicates.
			 */
            final SortedSet<ISclk> uniqueSclks = new TreeSet<ISclk>();
			for (final MonitorChannelSample sample: result) {
				if (!uniqueSclks.contains(sample.getSclk())) {
					uniqueSclks.add(sample.getSclk());
				} else {
					toRemove.add(sample);
				}
			}
			result.removeAll(toRemove);
		}
		return result;
	}

	/**
	 * Gets a list of all channel IDs in the LAD that meet the given filtering criteria.
	 * 
	 * @param filterType the RealtimeRecordedFilterType enum value indicating which type of channel IDs to get
	 * @param dssId the station ID for monitor channels
	 * 
	 * @return List of string channel IDs
	 */
	private List<String> getAllChannelIds(final RealtimeRecordedFilterType filterType, final int dssId) {
		Set<Pair<String, Integer>> mapIds = null;

		if (filterType == RealtimeRecordedFilterType.REALTIME) {
			mapIds = this.realtimeLadTable.keySet();
		} else if (filterType == RealtimeRecordedFilterType.RECORDED) {
			mapIds = this.recordedLadTable.keySet();
		} else {
			/*
			 * If both recorded and realtime data is requested, the keys
			 * from both LADs must be combined. The TreeSet will
			 * discard duplicates that appear in both tables.
			 */
			mapIds = new HashSet<Pair<String,Integer>>(this.realtimeLadTable.keySet());
			mapIds.addAll(this.recordedLadTable.keySet());
		}

		final List<String> result = new ArrayList<String> ();
		/*
		 * The result list is constructed from the Set, but monitor channel IDs
		 * that do not match the requested station are not included in the result list.
		 */
		for (final Pair<String,Integer> mapId: mapIds) {
			final Integer station = mapId.getTwo();
			final String chanId = mapId.getOne();

			if (station == null || station.intValue() == dssId || 
					!getChannelType(chanId).equals(ChannelDefinitionType.M)) {
				result.add(mapId.getOne());
			}
		}
		return result;
	}

	/**
	 * Gets latest samples of channels in alarm which either match the given alarm filter or are on
	 * the given list of additional channel IDs
	 *  
	 * @param alarmFilter AlarmFilter used to filter return list
	 * @param filterType the RealtimeRecordedFilterType enum value indicating which type of channel IDs to get
	 * @param dssId the station ID for monitor channels
	 * @param additionalChannels list of additional channel IDs required, beyond the filter; may be null
	 * @return List of the most recent samples that match either the given alarm filter or a channel ID
	 * in the list of additional IDs, or null if no channels matched
	 */
	public List<MonitorChannelSample> getLatestValues(final AlarmFilter alarmFilter, 
			final RealtimeRecordedFilterType filterType, final int dssId, final List<String> additionalChannels) {

		final List<MonitorChannelSample> result = new ArrayList<MonitorChannelSample>();

		synchronized(this.ladLock) {

			/*
			 * First get all the channels IDs in the LAD that match our realtime/recorded 
			 * and station criteria.
			 */

			final List<String> allChans = getAllChannelIds(filterType, dssId);
			/*
			 * No channels matched.
			 */
			if (allChans == null) {
				return null;
			}

			/*
			 * For channels that did match, now get their most recent value, if they match the alarm
			 * filtering criteria OR are on the list of additional channels requested.
			 */
			for (final String channel : allChans) {
			    final MonitorChannelSample data = getMostRecentValue(channel, filterType, dssId);
			    if (data != null && alarmFilter.accept(data) || (additionalChannels!= null && additionalChannels.contains(channel))) {
			        result.add(data);

			    }
			}
		}

		// If no channels matched, return null
		if (result.isEmpty()) {
			return null;
		} else {
			return result;
		}
	}

	/**
	 * Clears all channel values and latest times from the LAD.
	 */
	public void clear() {
	    
	    synchronized (this.ladLock) {
	        this.realtimeLadTable.clear();
	        this.recordedLadTable.clear();
	        this.recordedTimes.clear();
	        this.realtimeTimes.clear();
	        this.allTimes.clear();
	    }
	}

	/**
	 * Gets the latest times for recorded channels. The latest times object
	 * tracks the latest ERT, SCET, SCLK, LST, and UTC for the entire LAD.
	 * 
	 * @return LatestTimes object for recorded channel samples only
	 */
	public LatestChannelTimes getLatestRecordedTimes() {
	    synchronized (this.ladLock) {
	        return new LatestChannelTimes(this.recordedTimes);
	    }
	}

	/**
	 * Gets the latest times for realtime channels. The latest times object
	 * tracks the latest ERT, SCET, SCLK, LST, and UTC for the entire LAD.
	 * 
	 * @return LatestTimes object for realtime channel samples only
	 */
	public LatestChannelTimes getLatestRealtimeTimes() {
	    synchronized (this.ladLock) {
	        return new LatestChannelTimes(this.realtimeTimes);
	    }
	}

	/**
	 * Gets the latest times for all channels. The latest times object
	 * tracks the latest ERT, SCET, SCLK, LST, and UTC for the entire LAD.
	 * 
	 * @return LatestTimes object for all channel samples
	 */
	public LatestChannelTimes getLatestTimes() {
		synchronized (this.ladLock) {
		    return new LatestChannelTimes(this.allTimes);
		}
	}
	
	/**
	 * Starts the process of fetching the global LAD, which is a REST request
	 * over the network. The fetch will be triggered periodically thereafter
	 * until successful. Additional calls will have no effect until the initial
	 * request succeeds.
	 */
	public void triggerLadFetch() {

		if (globalLadEnabled) {
			triggerLadFetchInternal(); // call actual synchronized method only if the Global LAD is enabled d
		}
	}

	private synchronized void triggerLadFetchInternal() {

		
		// Do nothing if there is already a LAD request pending
		if (ladTimer != null) {
			return;
		}
		
		// This flag indicates when we are done
		gotLad = false;
		
		// Start a timer to fetch the LAD
		final long ladFetchInterval = appContext.getBean(MonitorGuiProperties.class).getLadFetchInterval();
		ladTimer = new Timer();
		ladTimer.scheduleAtFixedRate(new TimerTask() {

			/**
			 * @see java.util.TimerTask#run()
			 */
			@Override
			public void run() {
                final Tracer log = TraceManager.getTracer(appContext, Loggers.GLAD);

				log.debug("LAD fetch timer fired");
				
				// We already succeeded
				if (gotLad) {
					return;
				}

                if (appContext.getBean(IContextIdentification.class).getNumber() == null
                        || appContext.getBean(IContextIdentification.class).getNumber() == 0) {
                    log.debug("Cannot get LAD yet; no test number");
                    return;
                }

                // Ok try to get the LAD from the LAD service
                LadConsumer consumer = null;
                Collection<IClientChannelValue> ladValues = null;
                try {
                    log.info("Requesting LAD");
                    consumer = appContext.getBean(LadConsumer.class);

                    ladValues = 
                            consumer.getLadAsChannelValues(
                                    appContext,
                                    new HashSet<Integer>(registeredStations
                                            .values()));
                } catch (final Exception e) {
                    log.error("Error querying Global LAD", ExceptionTools.getMessage(e));
                    log.error(Markers.SUPPRESS, "Error querying Global LAD", ExceptionTools.getMessage(e), e);
                    gotLad = true; // we didn't get it, but no point in continuing to try in this case
                    ladTimer.cancel();
                    return;
				}
                
				if (ladValues == null) {
					log.error("Cannot get LAD for session ID " + appContext.getBean(IContextIdentification.class).getNumber());
					return; // try again next round
				}
				
				// If we got here, we got the LAD. Send it on to the channel distributor
				log.info("Received LAD for session ID " + appContext.getBean(IContextIdentification.class).getNumber());
				sendLad(ladValues);
				
				// Reset things so the user can fetch LAD again
				gotLad = true;
				ladTimer.cancel();     
				ladTimer = null;
			}
		}, ladFetchInterval, ladFetchInterval);		
		
	}
	
    /**
     * Processes the given LAD (latest available data) message, which contains channel samples from
     * the global LAD, translates them to ChannelSample objects, and posts them to the
     * send queue.
     * 
     * @param message the LadMessage to process
     */
	public void sendLad(final Collection<IClientChannelValue> ladValues)
	{

		// If not running in the GUI, we do nothing with the global LAD
		if (this.monitorIsGGui) {
			
		    final LocalLadMessage msg = new LocalLadMessage();
		    
			if (ladValues == null || ladValues.isEmpty()) {
				return;
			}
			
			// Go through all the values
			final Iterator<IClientChannelValue> it = ladValues.iterator();
			while (it.hasNext()) {
				
				// Create a ChannelSample object from the incoming channel value
				final IClientChannelValue value = it.next();
				final MonitorChannelSample data = MonitorChannelSample.create(this.appContext.getBean(IChannelDefinitionProvider.class), value);
				if (data == null) {
					continue;
				}
				
				data.setFromLad(true);

				/*
				 * Global LAD values are only added to the local LAD
				 * if they have a later timestamp, which is now handled by a single addNewValue()
				 * method rather than having a separate method that checks ERT for this case
				 * only.
				 */
				addNewValue(data);

				msg.addSample(data);
				
			}
            appContext.getBean(IMessagePublicationBus.class).publish(msg);
		} 
	}

	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {

		
		cancelLadTimer();
		super.finalize();
	}
	
	/**
	 * Cancels the LAD fetch timer, if active.
	 */
	public synchronized void cancelLadTimer() {


		if (ladTimer != null) {
			ladTimer.cancel();
			ladTimer = null;
		}
		
	}

	/**
	 * Registers a station for fetching monitor LAD. If the object passed is
	 * already registered, this call will have the effect of unregistering and
	 * then registering with the new station number.
	 * 
	 * @param obj
	 *            the object (probably a GUI view composite) registering
	 * @param stationNumber
	 *            number of the station to register
	 */
	public void registerStation(final Object obj, final int stationNumber) {
		


		synchronized (registeredStations) {
			registeredStations.put(obj, stationNumber);
		}
		
		triggerLadFetch();
	}

	/**
	 * Unregisters a station so that monitor LAD will no longer be fetched for
	 * that station if no object remains that has that station registered.
	 * 
	 * @param obj
	 *            the object (probably a GUI view composite) that is now
	 *            unregistering a station
	 */
	public void unregisterStation(final Object obj) {


		synchronized (registeredStations) {
			registeredStations.remove(obj);
		}

		triggerLadFetch();
	}
}
