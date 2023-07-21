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
package jpl.gds.globallad.data.container.buffer;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.GlobalLadReapSettings;
import jpl.gds.globallad.data.GlobalLadContainerException;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.container.GlobalLadContainerFactory;
import jpl.gds.globallad.data.container.GlobalLadUtilities;
import jpl.gds.globallad.data.container.IGlobalLadContainer;
import jpl.gds.globallad.data.container.IGlobalLadDepthNotifiable;
import jpl.gds.globallad.data.container.IGlobalLadSearchAlgorithm;
import jpl.gds.globallad.data.container.IGlobalLadSerializable;
import jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm;
import jpl.gds.globallad.data.container.search.IGlobalLadDataSearchAlgorithm;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory;
import jpl.gds.globallad.data.json.ObjectWithTypeDeserializer;
import jpl.gds.globallad.data.json.ObjectWithTypeSerializer;
import jpl.gds.globallad.data.json.views.GlobalLadSerializationViews;
import jpl.gds.shared.log.Tracer;

/**
 * Implementation of the global lad ring buffer.  This uses three navigable sets to store the last N data values based on inser time,
 * ERT and SCET.  The data depth is set at start up and implements IGlobalLadDepthNotifiable so it can adjust this depth when there is
 * a change.
 */
@SuppressWarnings("deprecation")
public class SortedSetGlobalLadDataBuffer implements IGlobalLadContainer, IGlobalLadSerializable, IGlobalLadDepthNotifiable {
	/**
	 * IGlobalLadSerializable interface defines methods to serialize the entire lad structure
	 * as JSON strings.  This turned out to be too brittle as well as not efficient enough for our needs
	 * so it was deprecated, but not removed because there may be a need in the future to use this.
	 */
	public static final Tracer log = GlobalLadProperties.getTracer();
	public static final boolean debug = GlobalLadProperties.getGlobalInstance().isDebug();

	private final AtomicLong insertNumber = new AtomicLong();

	/**
	 * Added trim numbers which will be used when trimming
	 * the data buffers.
	 */
	private final AtomicLong eventTrimNumber = new AtomicLong();
	private final AtomicLong ertTrimNumber = new AtomicLong();
	private final AtomicLong scetTrimNumber = new AtomicLong();

	@JsonSerialize(using = ObjectWithTypeSerializer.class)
	@JsonDeserialize(using = ObjectWithTypeDeserializer.class)
	private final Object identifier;
	private final String containerType;
	private long lastInsert;
	private long lastTouch;

	private long totalInsertTime;
	private long totalQueryGetTime;
	private long queryGetCount;

	@JsonIgnore
	private int maxSize;

	@JsonProperty("userDataType")
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	byte userDataType;

	/**
	 * Holding three buffers to support three different query types.  One is insert order,
	 * one is scet ordered and one is ert ordered.
	 */
	@JsonIgnore
	private final NavigableSet<IGlobalLADData> insertBuffer;

	@JsonIgnore
	private final NavigableSet<IGlobalLADData> scetBuffer;

	@JsonIgnore
	private final NavigableSet<IGlobalLADData> ertBuffer;

	AtomicLong lastTrimErt;
	AtomicLong lastTrimScet;
	AtomicLong lastTrimEvent;

	private final boolean storeErt;
	private final boolean storeScet;
	private final boolean storeEvent;

	/**
	 * Creates a new ring buffer.
	 *
	 * @param containerType the container type.
	 * @param identifier the identifier of the data.
	 * @param userDataType userDataType of all data stored in this buffer.
	 */
	@JsonCreator
	public SortedSetGlobalLadDataBuffer(
			@JsonProperty("containerType") final String containerType,
			@JsonProperty("containerIdentifier") final Object identifier,
			@JsonProperty("userDataType") final byte userDataType
			)
	{
		this.identifier = identifier;
		this.containerType = containerType;
		this.userDataType = userDataType;

		this.lastTrimErt = new AtomicLong(-1);
		this.lastTrimScet = new AtomicLong(-1);
		this.lastTrimEvent = new AtomicLong(-1);

		this.lastInsert = System.currentTimeMillis();
		this.lastTouch = System.currentTimeMillis();

		/**
		 * NOTE:  These buffers store the data backward, meaning the values that are the most recent will
		 * be at the front of the buffer.
		 */
		insertBuffer = new ConcurrentSkipListSet<IGlobalLADData>(IGlobalLadDataFactory.eventComparator);
		scetBuffer = new ConcurrentSkipListSet<IGlobalLADData>(IGlobalLadDataFactory.scetComparator);
		ertBuffer = new ConcurrentSkipListSet<IGlobalLADData>(IGlobalLadDataFactory.ertComparator);

		/**
		 * This is getting the default depth but it should get the depth of the user data type this container is.
		 */
		maxSize = GlobalLadProperties.getGlobalInstance().getDataDepth(userDataType);

		queryGetCount = 0;
		totalQueryGetTime = 0;
		totalInsertTime = 0;

		/**
		 * Add this to the notifiables list in the configuration.
		 */
		GlobalLadProperties.getGlobalInstance().addDepthListener(this);

		storeEvent = GlobalLadProperties.getGlobalInstance().storeEvent(userDataType);
		storeErt = GlobalLadProperties.getGlobalInstance().storeErt(userDataType);
		storeScet = GlobalLadProperties.getGlobalInstance().storeScet(userDataType);
	}

	/**
	 * Used for JSON marshalling.
	 *
	 * @param ert the ert time to set.
	 */
	@JsonProperty("lastTrimErt")
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	public void setLastTrimErt(final long ert) {
 		this.lastTrimErt.set(ert);
	}

	/**
	 * Used for JSON marshalling.
	 * @return ert of the last data object that got trimmed.  Will return -1 if nothing has been trimmed.
	 */
	@JsonProperty("lastTrimErt")
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	public long getLastTrimErt() {
		long last = lastTrimErt.get();

		if (last < 0 && !ertBuffer.isEmpty()) {
			last = ertBuffer.last().getErtMilliseconds();
			lastTrimErt.set(last);
		}

		return last;
	}

	/**
	 * Used for JSON marshalling.
	 * @param scet the time of the last scet data object to get trimmed.
	 */
	@JsonProperty("lastTrimScet")
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	public void setLastTrimScet(final long scet) {
		this.lastTrimScet.set(scet);
	}

	/**
	 * Used for JSON marshalling.
	 * @return scet of the last data object that got trimmed.  Will return -1 if nothing has been trimmed.
	 */
	@JsonProperty("lastTrimScet")
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	public long getLastTrimScet() {
		long last = lastTrimScet.get();

		if (last < 0 && !scetBuffer.isEmpty()) {
			last = scetBuffer.last().getScetMilliseconds();
			lastTrimScet.set(last);
		}

		return last;
	}

	/**
	 * Used for JSON marshalling.
	 * @param evnet the time of the last event time data object to get trimmed.
	 */
	@JsonProperty("lastTrimEvent")
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	public void setLastTrimEvent(final long event) {
		this.lastTrimEvent.set(event);
	}

	/**
	 * Used for JSON marshalling.
	 * @return lastTrimEvent time of the last data object that got trimmed.  Will return -1 if nothing has been trimmed.
	 */
	@JsonProperty("lastTrimEvent")
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	public long getLastTrimEvent() {
		long last = lastTrimEvent.get();

		if (last < 0 && !insertBuffer.isEmpty()) {
			last = insertBuffer.last().getEventTime();
			lastTrimEvent.set(last);
		}

		return last;
	}

	/**
	 * Used for serialization.  This will return a sorted set that contains all of the data
	 * containers merged from all of the internal buffers.
	 *
	 * @return a sorted set that contains all of the data containers merged from all of the internal buffers.
	 */
	@JsonProperty("mergedSet")
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	public NavigableSet<IGlobalLADData> getGlobalDataCollection() {
		final NavigableSet<IGlobalLADData> mergedSet = new TreeSet<IGlobalLADData>(insertBuffer);
		mergedSet.addAll(ertBuffer);
		mergedSet.addAll(scetBuffer);

		return mergedSet;
	}

	/**
	 * Used for serialization.  Adds all of the values in mergedSet to the internal buffers.  This method turns
	 * out to be more efficient than trying to keep track of which values are in which buffer.  This must be done because
	 * jackson will create new instances for objects that should be equal but just referenced in multiple buffers.
	 *
	 * @param mergedSet All data to be added to the internal buffers.
	 */
	@JsonProperty("mergedSet")
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	public void setGlobalDataCollection(final Collection<IGlobalLADData> mergedSet) {
		for (final IGlobalLADData container : mergedSet) {
			_insert(container, false);
		}
	}

	/**
	 * Returns the buffer for the given time type.
	 *
	 * @param timeType
	 * @return An iterator over the buffer based on timeType.
	 */
	@JsonIgnore
	public Iterator<IGlobalLADData> getBufferIterator(final GlobalLadPrimaryTime timeType) {
		Set<IGlobalLADData> buffer;

		switch(timeType) {
		case ERT:
			buffer = ertBuffer;
			break;
		case EVENT:
			buffer = insertBuffer;
			break;
		case SCET:
		case LST:
		case SCLK:
		default:
			buffer = scetBuffer;
			break;
		}

		return buffer.iterator();
	}

	/**
	 * Stubbed method.  Always returns null.
	 * @return null.  This container never has children, it is always the last stop on the train.
	 */
	@Override
	public Collection<IGlobalLadContainer> getChildren() {
		return null;
	}

	/**
	 * Stubbed method.  Always returns null.
	 * @return null.  This container never has children, it is always the last stop on the train.
	 */
	@Override
	public IGlobalLadContainer getChild(final Object identifier) {
		return null;
	}


	/**
	 * Stubbed method.  Always returns null.
	 * @return null.  This container never has children, it is always the last stop on the train.
	 */
	@Override
	public Collection<IGlobalLadContainer> getChildrenWithRegex(final String rx) {
		return null;
	}

	/**
	 * This will always be null for this class.
	 * @return null.  This container never has children, it is always the last stop on the train.
	 */
	@Override
	public IGlobalLadContainerSearchAlgorithm generateInsertSearchAlgorithm(final IGlobalLADData data) {
		return null;
	}

	/**
	 * Stubbed method.  Always returns null.
	 * @return false, this container is always empty because it never has children, it is always the last stop on the train.
	 */
	@Override
	public boolean isEmpty() {
		return false;
	}

	/**
	 * @return the insertNumber
	 */
	@JsonProperty("insertNumber")
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	public long getInsertNumber() {
		return insertNumber.get();
	}

	/**
	 * @param insertNumber the insertNumber to set
	 */
	@JsonProperty("insertNumber")
	public void setInsertNumber(final long insertNumber) {
		this.insertNumber.set(insertNumber);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#getContainerIdentifier()
	 */
	@Override
	public Object getContainerIdentifier() {
		return identifier;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#getContainerType()
	 */
	@Override
	public String getContainerType() {
		return containerType;
	}

	/**
	 * This does nothing.
	 *
	 * @returns false.  This container never has children.
	 *
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#add(jpl.gds.globallad.data.container.IGlobalLadContainer)
	 */
	@Override
	public boolean add(final IGlobalLadContainer container) {
		return false;
	}

	/**
	 * Adds dataContainer to buffer and will trim the oldest value based on inser time if the buffer is larger
	 * than the max size after insert.
	 *
	 * @param dataContainer data to insert.
	 * @param buffer set to add the data to.
	 * @return The data object that got trimmed after this insert.  If nothing was trimmed null.
	 */
	private IGlobalLADData insertBase(final IGlobalLADData dataContainer, final NavigableSet<IGlobalLADData> buffer, final AtomicLong trimCounter) {
		/**
		 * Streamlining the insert process.
		 * The checks to see if the data would be removed were very expensive and much
		 * costlier than just inserting and letting it get trimmed.
		 * Also moving all trimming to the reaper so that nothing gets in the way of inserting.
		 */
		buffer.add(dataContainer);

		return trimBuffer(buffer, trimCounter, maxSize);
	}

	/**
	 * Trims the buffer to the max size.
	 *
	 * @param buffer the buffer to trim
	 * @param trimCounter the trim counter to increment for trims.
	 * @param maximumSize the maximum size of the buffer.
	 * 
	 * @return  The last data object to be trimmed.  If no data was trimmed null.
	 */
	private IGlobalLADData trimBuffer(final NavigableSet<IGlobalLADData> buffer, final AtomicLong trimCounter, final int maximumSize) {
		IGlobalLADData lastTrimmed = null;

		/**
		 * Previously we checked the size of the buffer using
		 * the buffer.size() method.  The java doc for the ConcurrentSkipList states that this
		 * method is inefficient as well as inaccurate at the best of times.  It must traverse the
		 * entire list and touch each node to get the count, and once it is finished another thread
		 * could have inserted or removed a node.  The key to the global lad speed is cutting down on
		 * the insert time.  This change makes the insert faster, cuts down on wasted CPU cycles
		 * and achieves the same effect.
		 */
		while(insertNumber.get() - trimCounter.get() > maximumSize) {
			lastTrimmed = buffer.pollLast();
			trimCounter.incrementAndGet();
		}

		/**
		 * Trickled down changes.  The comparators for the data
		 * were not set up correctly, and there was a lot of mixed logic around.  A lot of the comments
		 * said the data was stored in descending order, but it wasn't and the code reflected that.
		 *
		 * They are now stored in descending order, so when trimming, trim the end of the list, which
		 * is the oldest data.
		 *
		 */
		return lastTrimmed;
	}

	/**
	 * Inserts dataContainer into the insert time buffer.
	 *
	 * @param dataContainer
	 */
	private void insertInsert(final IGlobalLADData dataContainer) {
		if (storeEvent) {
			final IGlobalLADData trimmed = insertBase(dataContainer, insertBuffer, eventTrimNumber);
			if (trimmed != null) {
				lastTrimEvent.set(trimmed.getEventTime());
			}
		}
	}

	/**
	 * Inserts dataContainer into the ert time buffer.
	 * @param dataContainer
	 */
	private void insertErt(final IGlobalLADData dataContainer) {
		if (storeErt) {
			final IGlobalLADData trimmed = insertBase(dataContainer, ertBuffer, ertTrimNumber);
			if (trimmed != null) {
				lastTrimErt.set(trimmed.getErtMilliseconds());
			}
		}
	}


	/**
	 * Inserts dataContainer into the scet time buffer.
	 *
	 * @param dataContainer
	 */
	private void insertScet(final IGlobalLADData dataContainer) {
		if (storeScet) {
			final IGlobalLADData trimmed = insertBase(dataContainer, scetBuffer, scetTrimNumber);
			if (trimmed != null) {
				lastTrimScet.set(trimmed.getScetMilliseconds());
			}
		}
	}

	/**
	 * Inserts dataContainer into all of the enabled time buffers.  Time buffers can be enabled / disabled vial the
	 * global lad properties file.
	 *
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#insert(jpl.gds.globallad.data.IGlobalLADData)
	 */
	@Override
	public boolean insert(final IGlobalLADData dataContainer) {
		return _insert(dataContainer, true);
	}


	/**
	 * Inserts dataContainer into all of the internal time buffers.
	 *
	 * @param dataContainer
	 * @param setInsertNumber
	 * @return if the value was added.
	 */
	private boolean _insert(final IGlobalLADData dataContainer, final boolean setInsertNumber) {
		lastInsert = System.currentTimeMillis();
		final long start = System.nanoTime();

		try {
			if (setInsertNumber) {
				dataContainer.setInsertNumber(insertNumber.incrementAndGet());
			}

			/**
			 * Insert into all of the deals.  If the data was added to any of the buffers
			 * will return true.
			 */
			insertScet(dataContainer);
			insertErt(dataContainer);
			insertInsert(dataContainer);

			/**
			 * Always return true since we will never fail to add anything.
			 */
			return true;
		} finally {
			totalInsertTime += System.nanoTime() - start;
		}
	}

	/**
	 * Inserts dataContainer into all of the enabled time buffers.  This class ignores matcher.
	 * Time buffers can be enabled / disabled vial the global lad properties file.
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#insert(jpl.gds.globallad.data.IGlobalLADData, jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm)
	 */
	@Override
	public boolean insert(final IGlobalLADData data, final IGlobalLadContainerSearchAlgorithm matcher) {
		return insert(data);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadQueryable#get(jpl.gds.globallad.data.container.IGlobalLadSearchAlgorithm, int)
	 */
	@JsonIgnore
	@Override
	public Map<Object, Collection<IGlobalLADData>> get(final IGlobalLadSearchAlgorithm dataMatcher, final int numRecords) {
		final long start = System.nanoTime();

		try {
			if (dataMatcher == null || insertBuffer.isEmpty()) {
				/**
				 * No reason to move on, just return an empty set.
				 */
				return Collections.<Object, Collection<IGlobalLADData>>emptyMap();
			} else {
				Collection<IGlobalLADData> matched;

				NavigableSet<IGlobalLADData> dataBuffer;
				switch(dataMatcher.getTimeType()) {
				case ERT:
					dataBuffer = ertBuffer;
					matched = new TreeSet<IGlobalLADData>(IGlobalLadDataFactory.ertComparator);
					break;
				case EVENT:
					matched = new TreeSet<IGlobalLADData>(IGlobalLadDataFactory.eventComparator);
					dataBuffer = insertBuffer;
					break;
				case ALL:
					/**
					 * Must use the all comparator for both of these
					 * tree sets because all uses the primary time for comparing objects.  The all
					 * comparator sorts such that the objects are in the proper descending order.
					 */
					matched = new TreeSet<IGlobalLADData>(IGlobalLadDataFactory.allComparator);

					/**
					 * In this case we must add them all together.  We need to make sure everything
					 * that is in the buffer is added.
					 */
					dataBuffer = new TreeSet<IGlobalLADData>(IGlobalLadDataFactory.allComparator);

					/**
					 *  Add all the buffers to the new tree set.  These will be sorted and all duplicates removed.
					 */
					dataBuffer.addAll(insertBuffer);
					dataBuffer.addAll(scetBuffer);
					dataBuffer.addAll(ertBuffer);
					break;
				case SCET:
				case LST:
				case SCLK:
				default:
					matched = new TreeSet<IGlobalLADData>(IGlobalLadDataFactory.scetComparator);
					dataBuffer = scetBuffer;
				}

				final boolean isLimited = numRecords > 0;

				/**
				 * Stored in descending order so get the values from the front if there is a size limit.
				 */

				final Iterator<IGlobalLADData> iterator = dataBuffer.iterator();

				while (iterator.hasNext() && (!isLimited || matched.size() < numRecords)) {
					final IGlobalLADData dataContainer = iterator.next();

					if (dataMatcher.isMatched(dataContainer)) {
						matched.add(dataContainer);
					}
				}

				if (matched.isEmpty()) {
					return Collections.<Object, Collection<IGlobalLADData>>emptyMap();
				} else {
					final Map<Object, Collection<IGlobalLADData>> result = new HashMap<Object, Collection<IGlobalLADData>>();
					result.put(getContainerIdentifier(), matched);

					return result;
				}
			}
		} finally {
			queryGetCount++;
			totalQueryGetTime += System.nanoTime() - start;
			lastTouch = System.currentTimeMillis();
		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadQueryable#getAll(jpl.gds.globallad.data.container.IGlobalLadSearchAlgorithm)
	 */
	@Override
	public Map<Object, Collection<IGlobalLADData>> getAll(final IGlobalLadSearchAlgorithm matcher) {
		return get(matcher, -1);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadDeltaQueryable#deltaQuery(jpl.gds.globallad.data.container.IGlobalLadSearchAlgorithm, java.util.Map)
	 */
	@Override
	public void deltaQuery(
			final IGlobalLadSearchAlgorithm matcher, final Map<DeltaQueryStatus, Map<Object, Collection<IGlobalLADData>>> resultMap) {
		final Map<Object, Collection<IGlobalLADData>> results = getAll(matcher);

		if (!results.isEmpty()) {
			final DeltaQueryStatus queryStatus = calculateDeltaQueryStatus(matcher);

			if (resultMap.containsKey(queryStatus)) {
				GlobalLadUtilities.mergeMap(matcher.getTimeType(), results, resultMap.get(queryStatus), -1);
			} else {
				// Nothing there yet, results will be the first.
				resultMap.put(queryStatus, results);
			}
		} else {
			// Nothing to do.
		}
	}

	/**
	 * @param timeType
	 * @return Timestamp for the last data object that got trimmed related to timeType.  Will return -1 if nothing has been trimmed.
	 */
	private long getLastTrimTime(final GlobalLadPrimaryTime timeType) {
		switch (timeType) {
		case ERT:
			return lastTrimErt.get();
		case EVENT:
			return lastTrimEvent.get();
		case SCET:
		case LST:
		case SCLK:
		default:
			return lastTrimScet.get();
		}
	}

	/**
	 * Based on the time value and the time type of matcher figures out the query status.
	 * complete - The lower bound time of the matcher is on or after the last trim time.
	 * incomplete - The lower bound time is before the last trim time.  This does not mean it is incomplete for sure, but we cannot be sure.
	 * unknown - no lower bound time was given.
	 *
	 * @param matcher
	 * @return The delta query status of by checking the lower bound time of matcher and the last trim time of the data buffer associated with the time type of matcher.
	 */
	private DeltaQueryStatus calculateDeltaQueryStatus(final IGlobalLadDataSearchAlgorithm matcher) {
		final long lowerTime = matcher.getLowerBoundMilliseconds();
		final long lastTrim = getLastTrimTime(matcher.getTimeType());

		if (lowerTime < 0) {
			// Was not set or is not a valid time.
			return DeltaQueryStatus.unknown;
		} else if (lowerTime <= lastTrim) {
			/**
			 * Was a less than check before, but it would still be incomplete if the times were equal,
			 * since we could have trimmed something that was expected.
			 */

			// The lower time was before the last trim or equal.  Can not be sure we got everything.
			return DeltaQueryStatus.incomplete;
		} else {
			// Last trim time was before the data so we should have everything.
			return DeltaQueryStatus.complete;
		}
	}



	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#getTotalInsertTimeNS()
	 */
	@Override
	public long getTotalInsertTimeNS() {
		return totalInsertTime;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#getTotalInsertGetTimeNS()
	 */
	@Override
	public long getTotalInsertGetTimeNS() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#getTotalGetTimeNS()
	 */
	@Override
	public long getTotalQueryGetTimeNS() {
		return totalQueryGetTime;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadStatable#lastInsert()
	 */
	@Override
	public long lastInsert() {
		return lastInsert;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadStatable#lastInsertDelta()
	 */
	@Override
	public long lastInsertDelta() {
		return System.currentTimeMillis() - lastInsert;
	}

	/**
	 * @return Calculated inserts per second
	 */
	public long insertsPerSecond() {
		final double avgNano = totalInsertTime / insertNumber.get();
		return (long) (1 / (avgNano / 1E9));
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadStatable#getStats()
	 */
	@Override
	@JsonIgnore
	public JsonObject getStats() {
		return Json.createObjectBuilder()
			    .add(IGlobalLadContainer.CONTAINER_IDENTIFIER_KEY, this.getContainerIdentifier().toString())
			    .add(IGlobalLadContainer.CONTAINER_TYPE_KEY, this.getContainerType())
				.add("numInserts", insertNumber.get())
				.add("insertsPerSecond", insertsPerSecond())
				.add("queryCount", queryGetCount)
				.add("eventBufferCount",  getInsertDataCount())
				.add("ertBufferCount", getErtDataCount())
				.add("scetBufferCount", getScetDataCount())
				.add("lastInsert", lastInsert())
				.add("lastInsertDeltaMS", lastInsertDelta())
				.build();
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadQueryable#getMetadata(jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm)
	 */
	@Override
	public JsonObject getMetadata(final IGlobalLadContainerSearchAlgorithm matcher) {
		try {
			return getStats();
		} finally {
			this.lastTouch = System.currentTimeMillis();
		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.IGlobalLadSummarizableContainer#getSummary(jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm)
	 */
	@Override
	public JsonObject getSummary(final IGlobalLadContainerSearchAlgorithm matcher) {
		try {
			return Json.createObjectBuilder().build();
		} finally {
			this.lastTouch = System.currentTimeMillis();
		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadStatable#getJsonId()
	 */
	@Override
	@JsonIgnore
	public String getJsonId() {
		return (String) getContainerIdentifier();
	}

	/**
	 * @return the lastInsert
	 */
	public long getLastInsert() {
		return lastInsert;
	}

	/**
	 * @param lastInsert the lastInsert to set
	 */
	public void setLastInsert(final long lastInsert) {
		this.lastInsert = lastInsert;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadStatable#getChildCount()
	 */
	@Override
	public int getChildCount() {
		// Ring buffers have no children.
		return 0;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadStatable#getInsertDataCount()
	 */
	@Override
	public long getInsertDataCount() {
		return storeEvent ? insertNumber.get()-eventTrimNumber.get() : 0;
	}


	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadStatable#getErtDataCount()
	 */
	@Override
	public long getErtDataCount() {
		return storeErt ? insertNumber.get()-ertTrimNumber.get() : 0;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadStatable#getScetDataCount()
	 */
	@Override
	public long getScetDataCount() {
		return storeScet ? insertNumber.get()-scetTrimNumber.get() : 0;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadDepthNotifiable#depthUpdated()
	 */
	@Override
	public void depthUpdated() {
		final int newMaxSize = GlobalLadProperties.getGlobalInstance().getDataDepth(this.userDataType);

		if (newMaxSize != maxSize) {
			final boolean mustTrim = newMaxSize < maxSize;
			maxSize = newMaxSize;

			if (mustTrim) {
				trimBuffer(ertBuffer, ertTrimNumber, maxSize);
				trimBuffer(scetBuffer, scetTrimNumber, maxSize);
				trimBuffer(insertBuffer, eventTrimNumber, maxSize);
			}
		}
	}

	@Override
	public boolean logableReap() {
		// Reap of this target should only be logged if in debug mode.
		return debug;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.IGlobalLadReapable#reap()
	 */
	@Override
	public boolean reap(final GlobalLadReapSettings reapSettings, final long checkTimeMilliseconds, boolean parentWasReapable, long parentTimeToLive) {
		/**
		 * Reaping must touch all arms of the tree to find the minimum level to reap
		 * and work back up. Use the parent ttl and see if this has been touched in that time.
		 *
		 * Added a lastTouch variable that is used for gets and status / metadata calls.
		 */
		
		/**
		 * Updated reaping to fight out of memory issues.
		 *
		 * REDUCE_TOUCH_TIME_* - reduces the parent time to live by the stated percentage.  All of the given 
		 * enums use the same code since the deduction rate is in the enum.
		 */
		
		
		switch(reapSettings) {
		case REDUCED_TOUCH_TIME_25:
		case REDUCED_TOUCH_TIME_50:
		case REDUCED_TOUCH_TIME_75:
		case REDUCED_TOUCH_TIME_90:
			/**
			 * Adjust the parent time to live.
			 */
			parentTimeToLive = (long) (parentTimeToLive * reapSettings.touchTimeRatio);
			break;
		case IGNORE_LEVEL_RESTRICTIONS:
			/**
			 * Do normal reaping just set parentWasReapable to true.
			 */
			parentWasReapable = true;
			break;
		case REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_25:
		case REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_50:
		case REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_75:
		case REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_90:
			/**
			 * Set parentWasReapable and adjust the parent time to live.
			 */
			parentTimeToLive = (long) (parentTimeToLive * reapSettings.touchTimeRatio);
			parentWasReapable = true;
			break;
		case REDUCE_DATA_DEPTHS_10:
		case REDUCE_DATA_DEPTHS_25:
		case REDUCE_DATA_DEPTHS_50:
		case REDUCE_DATA_DEPTHS_75:
		case REDUCE_DATA_DEPTHS_90:
		case REDUCE_DATA_DEPTHS_99:
			// Adjust the size 
			int newMaxSize = (int) (reapSettings.depthRatio * maxSize);
			newMaxSize = newMaxSize > 1 ? newMaxSize : 1;
			trimBuffer(ertBuffer, ertTrimNumber, newMaxSize);
			trimBuffer(scetBuffer, scetTrimNumber, newMaxSize);
			trimBuffer(insertBuffer, eventTrimNumber, newMaxSize);
			break;
            case REDUCE_DATA_DEPTHS_PERM:
                // This is not used by the buffers so treat them as a normal reap.
		case NORMAL:
		case MEM_NORMAL:
			break;
		}

		final long localTime = lastTouch > lastInsert ? lastTouch : lastInsert;
		return parentWasReapable && checkTimeMilliseconds - localTime > parentTimeToLive;
	}


	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadSerializable#push(java.lang.Object[][])
	 */
	@Override
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	public void dehydrate(final JsonGenerator generator, final Object... identifiers) throws IOException {
		generator.useDefaultPrettyPrinter();
		generator.writeStartObject();
		generator.writeFieldName(GlobalLadContainerFactory.CONTAINER_PATH);
		generator.writeStartArray();

		for (final Object idd : identifiers) {
			generator.writeObject(idd);
		}

		generator.writeEndArray();

		generator.writeObjectField(GlobalLadContainerFactory.DATA_BUFFER, this);
		generator.writeEndObject();
		generator.flush();
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadSerializable#pull(jpl.gds.globallad.data.container.IGlobalLadContainer, java.util.List, java.util.List)
	 */
	@Override
	public void rehydrate(final IGlobalLadContainer container,
			final List<String> childContainerTypes,
			final List<Object> childContainerIdentifiers)
			throws GlobalLadContainerException {
		throw new GlobalLadContainerException("In ring buffer and should not get here.");

	}

	/**
	 * @return the user data type.
	 */
	public byte getUserDataType() {
		return this.userDataType;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#remove(jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm)
	 */
	@Override
	public boolean remove(final IGlobalLadContainerSearchAlgorithm matcher) throws Exception {
		return matcher.isContainerMatch(this);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#clear()
	 */
	@Override
	public void clear() {
		ertBuffer.clear();
		scetBuffer.clear();
		insertBuffer.clear();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("SortedSetGlobalLadDataBuffer [");
		if (identifier != null) {
			builder.append("identifier=");
			builder.append(identifier);
			builder.append(", ");
		}
		if (containerType != null) {
			builder.append("containerType=");
			builder.append(containerType);
		}
		builder.append("]");
		return builder.toString();
	}

	/**
	 * This equals method is very costly and should not be used very often.  It has to
	 * iterate through all of the internal buffers and check the values in the buffer are in the
	 * same order.  Depending on the depth, this could be a huge time sink.
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof SortedSetGlobalLadDataBuffer)) {
			return false;
		} else {
			final SortedSetGlobalLadDataBuffer other = (SortedSetGlobalLadDataBuffer) obj;

			if (!getContainerIdentifier().equals(other.getContainerIdentifier())) {
				return false;
			} else if (getChildCount() != other.getChildCount()) {
				return false;
			} else if (! containerType.equals(other.getContainerType())) {
				return false;
			} else if (userDataType != other.getUserDataType()) {
				return false;
			} else {
				for (final GlobalLadPrimaryTime timeType: IGlobalLADData.GlobalLadPrimaryTime.values()) {
					final Iterator<IGlobalLADData> thisIterator = getBufferIterator(timeType);
					final Iterator<IGlobalLADData> otherIterator = other.getBufferIterator(timeType);

					while (thisIterator.hasNext() && otherIterator.hasNext()) {
						if (!thisIterator.next().equals(otherIterator.next())) {
							return false;
						}
					}

					// Check if the sizes are not the same.
					final boolean thn = thisIterator.hasNext();
					final boolean ohn = otherIterator.hasNext();

					if (thn || ohn) {
						return false;
					}
				}

				return true;
			}
		}
	}
}
