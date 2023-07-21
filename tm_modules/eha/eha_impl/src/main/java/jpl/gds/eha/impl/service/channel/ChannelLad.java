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
package jpl.gds.eha.impl.service.channel;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.eha.api.channel.IChannelLad;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.impl.ChannelTimeComparator;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.types.Pair;

/**
 * 
 *         This class is responsible for keeping track of the most recent
 *         value for all the channels in the downlink. This class registers 
 *         for channel messages and automatically maintains the LAD as 
 *         channels are processed. For each channel, the most recent value for 
 *         the specified time type is stored. Note that channel are separated 
 *         by their realtime/recorded classification in this table. Monitor 
 *         channels are stored in the realtime map along with their station; 
 *         all other channels store station 0.<br>
 *         This class is also a singleton.
 *
 *
 * 10/17/13 entire class modified for station
 * segregation in chill_down LAD
 *         
 */
public class ChannelLad implements IChannelLad 
{
	/** 
	 * Table of lists of realtime channel values.
	 */
	private final Map <Pair <String, Integer>, IServiceChannelValue> realtimeLadMap;
	/** 
     * Table of lists of realtime channel values.
     */
	private final Map <Pair <String, Integer>, IServiceChannelValue> recordedLadMap;


	/*
	 * The table for accessing the channel definitions from the dictionaries.
	 */
	private IChannelDefinitionProvider channelTable;
	private final ChannelTimeComparator timeCompare;


    /**
     * Tracks the last time the LAd changed.
     */
	private long lastAddTime = 0;
	
	    /**
     * Creates an instance of ChannelLad.
     * 
     * @param defProvider
     *            channel definition provider
     * @param timeStrategy
     *            current time comparison strategy
     */
	public ChannelLad(final IChannelDefinitionProvider defProvider, final TimeComparisonStrategyContextFlag timeStrategy)
	{
		realtimeLadMap = new HashMap<Pair <String, Integer>, IServiceChannelValue>();
		recordedLadMap = new HashMap<Pair <String, Integer>, IServiceChannelValue>();

		this.channelTable = defProvider;
		
		this.timeCompare = new ChannelTimeComparator(timeStrategy);
	}

	
	@Override
    public IChannelDefinitionProvider getDefinitionProvider() {
	    return this.channelTable;
	    
	}
	
	@Override
    public void setDefinitionProvider(final IChannelDefinitionProvider provider) {
	    this.channelTable = provider;	    
	}
	

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.IChannelLad#addNewValue(IServiceChannelValue)
     */
	@Override
    public synchronized void addNewValue(final IServiceChannelValue val)
	{
		if(val == null)
		{
			throw new IllegalArgumentException("Null value passed in");
		}
		else if(val.getChanId() == null)
		{
			throw new IllegalArgumentException("The value passed in has a " +
					"null Channel ID");
		}

		if (val.getRct() != null) {
			lastAddTime = val.getRct().getTime();
		}

		/*
		 * Use channel definition to get type
		 */
		final boolean isMonitor = getChannelType(val).equals(
				ChannelDefinitionType.M);
		final boolean isHeader = getChannelType(val).equals(
				ChannelDefinitionType.H);
		
		/*
		 * Reject header or monitor channels with non-realtime status.
		 * @ToDo("Consider whether header channels should always be realtime 
		 * or not.")
		 */
		if (!val.isRealtime() && (isHeader || isMonitor)) {
			throw new IllegalArgumentException("This LAD does not accept " +
					"recorded M or H channels");
		}
		
		final Integer station = isMonitor ? 
				val.getDssId() : StationIdHolder.UNSPECIFIED_VALUE;
		
		final Pair<String, Integer> mapId = 
				new Pair<String, Integer>(val.getChanId(), station);
		
		if (val.isRealtime()) {
			
			final IServiceChannelValue oldVal = realtimeLadMap.get(mapId);
			if(oldVal == null || timeCompare.timestampIsLater(oldVal, val))
			{
				realtimeLadMap.put(mapId, val);
			}
		} else {
			final IServiceChannelValue oldVal = recordedLadMap.get(mapId);
			if(oldVal == null || timeCompare.timestampIsLater(oldVal, val))
			{
				recordedLadMap.put(mapId, val);
			}
		}
	}
	
	/*  station segregation in chill_down LAD
	 * (added station parameter to fetch method) */
	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.channel.api.ILatestSampleProvider#getMostRecentValue(java.lang.String, boolean, int)
     */
	@Override
    public synchronized IServiceChannelValue getMostRecentValue(final String id, 
			final boolean realtime, int station)
	{
		if(id == null)
		{
			throw new IllegalArgumentException("Null input ID");
		}
		/*
		 * use channel definition to get type
		 */
		station = getChannelType(id).equals(ChannelDefinitionType.M) ? station : 0;
		
		final Pair<String, Integer> mapId = new Pair<String, Integer>(id, station);
		
		if(realtime) {
			return realtimeLadMap.get(mapId);
		}
		else {
			return recordedLadMap.get(mapId);
	
		}
	}
	

	/**
	 * Gets the definition type of a channel.
	 * 
	 * @param val service channel value
	 * @return the channel definition type
	 */
	private ChannelDefinitionType getChannelType(final IServiceChannelValue val) {
		final ChannelDefinitionType defType = val.getDefinitionType();
		// If no channel definition assume FSW type. This happens when displays
		// contain out-of-date channels or are mismatched with current dictionary
		return defType == null ? ChannelDefinitionType.FSW : defType;
	}
	
	private ChannelDefinitionType getChannelType(final String id) {
        final IChannelDefinition def = channelTable.getDefinitionFromChannelId(id);
        // If no channel definition assume FSW type. This happens when displays
        // contain out-of-date channels or are mismatched with current dictionary
        return def == null ? ChannelDefinitionType.FSW : def.getDefinitionType();
    }

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.IChannelLad#getAllChannelIds(boolean)
     */
	@Override
    public synchronized List<String> getAllChannelIds(final boolean realtime) {
		
		final Set<Pair<String, Integer>> ids = realtime ? realtimeLadMap.keySet() : 
			recordedLadMap.keySet();
		

		final List<String> result = new ArrayList<String> ();
		
		for(final Pair<String, Integer> mapId : ids) {
			result.add(mapId.getOne());
		}
		
		return result;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.IChannelLad#getAllChannelIdAndStationPairs(boolean)
     */
	@Override
    public synchronized List<Pair<String, Integer>> getAllChannelIdAndStationPairs(
			final boolean realtime) {
		
		final Set<Pair<String, Integer>> ids = realtime ? realtimeLadMap.keySet() : 
			recordedLadMap.keySet();
		

		final List<Pair<String, Integer>> result = 
				new ArrayList<Pair<String, Integer>>();
		

		result.addAll(ids);
		
		return result;
	}
	
	/**
     * Add method to get all IInternalChannelValues
     * @see jpl.gds.eha.api.channel.IChannelLad#getChannelValueList()
     */
    @Override
    public synchronized List<IServiceChannelValue> getChannelValueList() {
		final Collection<IServiceChannelValue> channelList1 = realtimeLadMap.values();

		final Collection<IServiceChannelValue> channelList2 = recordedLadMap.values();
		
		final List<IServiceChannelValue> list = new ArrayList<IServiceChannelValue>(channelList1.size() + channelList2.size());
		
		list.addAll(channelList1);
		list.addAll(channelList2);

		return list;
	}
 	

	@Override
    public synchronized long clearAll() {
		realtimeLadMap.clear();
		recordedLadMap.clear();
		
		// R8 Refactor TODO - We just cannot have this going on here. An instance
		// of the LAD should not be doing anything with another EHA service, nor
		// globally accessing the alarm history.
		// I am putting this in AbstractDownlinkApp.

//		new AlarmHistory().clearValues();
//		if(alarmNotifier != null) {
//			alarmNotifier.clearCache();
//		}
//		
		final long temp = lastAddTime;
		setLastAddTime(0);
		return temp;
	}

	private synchronized void setLastAddTime(final long time) {
	    lastAddTime = time;
	}
	
	/* removed
	 * clearChannelUpToTime(clearTime) and clearChannelUpToTime(id, clearTime) 
	 * methods. They were only used by LadClearResource.java which will be 
	 * going away as part of the LADKeeper Removal. Also removed 
	 * removeValuesOlderThan(val, time) private method. It was only used 
	 * internally by the aforementioned methods. */

	@Override
    public synchronized boolean writeCsv(final Writer writer) {

	    try {
	        final BufferedWriter bufferedWriter = new BufferedWriter(writer);

	        bufferedWriter.write("channelId,dn,eu,rct,ert,scet,sclk,dssId,vcid," +
	                "dnAlarmState,euAlarmState,realtime\n");

	        final StringBuilder sb = new StringBuilder();

	        final List<IServiceChannelValue> channelValues = new LinkedList<IServiceChannelValue>(getChannelValueList());
	        Collections.sort(channelValues, new Comparator<IServiceChannelValue>() {
	            @Override
	            public int compare(final IServiceChannelValue cv1, final IServiceChannelValue cv2) {
	                return cv1.getChanId().compareTo(cv2.getChanId());
	            }
	        });

	        /* This debug code is similar to
	         * DatabaseChannelSample which should be refactored in the future  
	         * so that both classes can use it without causing architectural 
	         * coupling between the chill_down GUI and the database (from code 
	         * review)
	         */
	        for (final IServiceChannelValue cv : channelValues) {

	            sb.append(cv.getChanId());
	            sb.append(",");
	            sb.append(cv.getDn());
	            sb.append(",");
	            /* Use hasEu() on channel value rather than on definition. */
	            sb.append((cv.hasEu()) ? cv.getEu() : "");
	            sb.append(",");
	            sb.append(TimeUtility.formatDOY(cv.getRct()));
	            sb.append(",");
	            sb.append(cv.getErt().getFormattedErt(true));
	            sb.append(",");
	            sb.append(cv.getScet().getFormattedScet(true));
	            sb.append(",");
	            sb.append(cv.getSclk().toString());
	            sb.append(",");
	            sb.append(cv.getDssId());
	            sb.append(",");
	            sb.append(cv.getVcid() == null ? "0" : cv.getVcid());
	            sb.append(",");
	            
	            /* R8 Refactor - VERIFY these next two lines to get
	             * alarm level behaves like the old code below.
	             */
	            sb.append(cv.getDnAlarmLevel());	            
	            sb.append(",");
	            sb.append(cv.getEuAlarmLevel());
	            
//	            IAlarmValueSet alarms = cv.getAlarms();
//
//	            if (alarms != null) {
//	                final IAlarmValueSet dnSet = alarms.getAlarmSet(false);
//
//	                sb.append(dnSet.getWorstLevel().toString());
//	                sb.append(",");
//
//	                final IAlarmValueSet euSet = alarms.getAlarmSet(true);
//
//	                sb.append(euSet.getWorstLevel().toString());
//	            } else {
//	                sb.append(AlarmLevel.NONE.toString());
//	                sb.append(",");
//	                sb.append(AlarmLevel.NONE.toString());
//	            }
	            sb.append(",");
	            sb.append(cv.isRealtime());
	            sb.append("\n");

	            bufferedWriter.write(sb.toString());

	            sb.setLength(0);
	        }
	        bufferedWriter.close();
	    } catch (final IOException e) {
	        e.printStackTrace();
	        TraceManager.getDefaultTracer().error("Error saving LAD: " + e.toString(), e);

	        return false;
	    }
	    return true;  
	}
}
