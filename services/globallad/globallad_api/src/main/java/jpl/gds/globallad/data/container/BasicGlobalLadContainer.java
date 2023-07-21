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
/**
 * 
 */
package jpl.gds.globallad.data.container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.PatternSyntaxException;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

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
import jpl.gds.globallad.data.GlobalLadSearchAlgorithmException;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm;
import jpl.gds.globallad.data.container.search.query.InsertSearchAlgorithm;
import jpl.gds.globallad.data.json.MapKeyDeserializer;
import jpl.gds.globallad.data.json.MapKeySerializer;
import jpl.gds.globallad.data.json.ObjectWithTypeDeserializer;
import jpl.gds.globallad.data.json.ObjectWithTypeSerializer;
import jpl.gds.globallad.data.json.views.GlobalLadSerializationViews;
/**
 * Basic container used to store other containers.  Stores the data in a HashMap with an object as a key.
 */
@SuppressWarnings("deprecation")
public class BasicGlobalLadContainer extends AbstractGlobalLadContainer {
	/**
	 * There may be a need to change this but once we get the sweat spot it should not need to be changed again.
	 */
	public static final int INITIAL_CAPACITY = 16;
	public static final float LOAD_FACTOR = (float) 0.9;
	public static final int CONCURRENCY = 4;
	
	@JsonSerialize(using = ObjectWithTypeSerializer.class)
	@JsonDeserialize(using = ObjectWithTypeDeserializer.class)
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	private final Object containerIdentifier;
	
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	private final String containerType;
	
	@JsonDeserialize(as = ConcurrentHashMap.class, keyUsing = MapKeyDeserializer.class)
	@JsonSerialize(keyUsing = MapKeySerializer.class)
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	private ConcurrentHashMap<Object, IGlobalLadContainer> containers;
	
	/**
	 * Numbers used for stats and to keep track of how the global lad is performing.
	 */
	private long totalInsertTime;
	private long totalQueryGetTime;
	private long totalInsertGetTime;
	private long numInsertGets;
	private long numQueryGets;

	private final AtomicLong numInserts;

	/**
	 * @param containerType - Used to identify what type of data this container is holding.  Child containers
	 * are created with reflection based on the mapping in the GDS configuration.
	 * @param identifier - Object identifier for this container.  Used when inserting or querying global lad data.
	 *
	 */
	@JsonCreator
	public BasicGlobalLadContainer(@JsonProperty("containerType") final String containerType, 
			@JsonProperty("containerIdentifier") final Object identifier) {
		super();
		this.containerIdentifier = identifier;
		this.containerType = containerType;
		this.totalInsertTime = 0;
		this.totalInsertGetTime = 0;
		this.totalQueryGetTime = 0;
		this.numInserts = new AtomicLong();
		this.numInsertGets = 0;
		this.numQueryGets = 0;

		this.containers = new ConcurrentHashMap<Object, IGlobalLadContainer>(INITIAL_CAPACITY, LOAD_FACTOR, CONCURRENCY);
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#remove(jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm)
	 */
	@Override
	public boolean remove(final IGlobalLadContainerSearchAlgorithm matcher) throws Exception {
		
		if (matcher.isChildMatchNeeded(this)) {
			/**
			 * Must match child containers.  
			 */
			for (final IGlobalLadContainer container : search(matcher)) {
				if (container.remove(matcher)) {
					log.info("Removing container with type " + container.getContainerType() + " and identifier " + container.getContainerIdentifier());
					containers.remove(container.getContainerIdentifier());
				}
			}
			
			/**
			 * Check to see if all of the children have been removed.  If this is empty we can also 
			 * be removed.
			 */
			return isEmpty();
		} else {
			/**
			 * This container has been matched and can be removed.
			 */
			return true;
		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#clear()
	 */
	@Override
	public void clear() {
		containers.clear();
	}

	/**
	 * The generator must be closed by the caller to be sure that all of the serialization data has been written.
	 * {@inheritDoc}
	 * @see jpl.gds.globallad.data.container.IGlobalLadSerializable#dehydrate(com.fasterxml.jackson.core.JsonGenerator, java.lang.Object[])
	 */
	@Override
	public void dehydrate(final JsonGenerator generator, final Object... identifiers) throws IOException {
		if (identifiers.length == 0) {
			generator.writeStartArray();
		}
		
		final Object id = new StringBuilder()
			.append(getContainerIdentifier())
			.append(":")
			.append(getContainerIdentifier().getClass().getCanonicalName())
			.append(",")
			.append(this.getContainerType())
			.toString();
		
		final List<Object> _ids = new ArrayList<Object>(Arrays.asList(identifiers));
		_ids.add(id);
		final Object[] ids = _ids.toArray(); 

		for (final IGlobalLadContainer child : this.containers.values()) {
			if (child instanceof IGlobalLadSerializable) {
				
				((IGlobalLadSerializable) child).dehydrate(generator, ids);
			}
		}
		
		if (identifiers.length == 0) {
			generator.writeEndArray();
		}
	}
	
	/**
	 * 
	 * {@inheritDoc}
	 * @see jpl.gds.globallad.data.container.IGlobalLadSerializable#pull(jpl.gds.globallad.data.container.IGlobalLadContainer, java.util.List, java.util.List)
	 */
	@Override
	public void rehydrate(final IGlobalLadContainer container,
			final List<String> childContainerTypes,
			final List<Object> childContainerIdentifiers) throws GlobalLadContainerException {
		if (	childContainerTypes.size() != childContainerIdentifiers.size()) {
			throw new GlobalLadContainerException("Either empty or not the same size.");
		}

		if (childContainerTypes.isEmpty() || childContainerIdentifiers.isEmpty()) {
			// Should be a data buffer. 
			this.add(container);
		} else {
			final String ctype = childContainerTypes.remove(0);
			final Object cid = childContainerIdentifiers.remove(0);
	
			// See if there is already a child container.
			IGlobalLadContainer childContainer;
			
			if (containers.containsKey(cid)) {
				childContainer = containers.get(cid);
			} else {
				childContainer = GlobalLadContainerFactory.createChildContainer(ctype, cid);
				add(childContainer);
			}
			
			// Pass the stuff along.
			((IGlobalLadSerializable) childContainer).rehydrate(container, childContainerTypes, childContainerIdentifiers);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * If child is reapable removes it from the internal store. 
	 * If all children are reapable or there are no children this returns true.
	 * 
	 * @see jpl.gds.globallad.IGlobalLadReapable#reap()
	 */
	@Override
	@JsonIgnore
	public boolean reap(final GlobalLadReapSettings reapSettings, final long checkTimeMilliseconds, final boolean parentWasReapable, final long parentTimeToLive) {
		/**
		 * Fixing issues to make this work properly.
		 */
		
		/**
		 * This is reapable, meaning it can be reaped not that it should be reaped, if the type is 
		 * configured to be reapable or one of our ancestors was reapable.
		 */
		final boolean reapable = parentWasReapable || GlobalLadProperties.getGlobalInstance()
				.isReapable(containerType, containerIdentifier.toString());

		/**
		 * This value will be passed to all reap calls to children, so seed it with the passed in value.
		 */
		long ttl = parentTimeToLive;
		
		// This value will be used to determine if this container can be reaped.
		boolean expired = false;
		
		if (reapable) {
			/**
			 * Get the max and update ttl if it has been set.
			 */
			final long configuredMax = GlobalLadProperties.getGlobalInstance().getReapingMilliseconds(containerType, containerIdentifier.toString());
			ttl = configuredMax > 0 ? configuredMax : ttl;

			expired = ttl < checkTimeMilliseconds - lastInsert;
		}
		
		/**
		 * Check if this is the reap level.
		 * If it is check the children to see if they are reapable and delete them if they are.
		 * Otherwise, we just call reap on them and see if they are all reapable to send up to our parent.
		 */
		final boolean isReapLevel = GlobalLadProperties.getGlobalInstance().isReapingLevel(containerType);

		if (isReapLevel) {
			final Iterator<IGlobalLadContainer> iterator = containers.values().iterator();

			while (iterator.hasNext()) {
				final IGlobalLadContainer container = iterator.next();
				
				final boolean childIsReapable = container.reap(reapSettings, checkTimeMilliseconds, reapable, ttl);
				
				if (childIsReapable) {
					if (container.logableReap()) {
						log.info(String.format("Container with type %s and identifier %s has been reaped.", container.getContainerType(), container.getContainerIdentifier().toString()));
					}
					iterator.remove();
				}
				
				expired = expired && childIsReapable;
			}
		} else { 

			final Iterator<IGlobalLadContainer> iterator = containers.values().iterator();

			while (iterator.hasNext()) {
				final IGlobalLadContainer container = iterator.next();
				// Don't delete anything, just figuring out if we are reapable.
				expired = container.reap(reapSettings, checkTimeMilliseconds, reapable, ttl) && expired;
			}
		}
		
		/**
		 * We are reapable if all of our children were reapable or we hold nothing.
		 */
		return expired || containers.isEmpty();
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
		return this.totalInsertGetTime;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#getTotalGetTimeNS()
	 */
	@Override
	public long getTotalQueryGetTimeNS() {
		return totalQueryGetTime;
	}

	/**
	 * @return the containers
	 */
	@JsonProperty("childContainers")
	public Map<Object, IGlobalLadContainer> getContainers() {
		return containers;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#getChildren()
	 */
	@Override
	public Collection<IGlobalLadContainer> getChildren() {
		return containers.values();
	}

	/**
	 * @param containers the containers to set
	 */
	@JsonProperty("childContainers")
	public void setContainers(final ConcurrentHashMap<Object, IGlobalLadContainer> containers) {
		this.containers = containers;
	}

	@Override
	protected Collection<IGlobalLadContainer> search(final IGlobalLadContainerSearchAlgorithm matcher) {
		try {
			return matcher.getMatchedChildren(this);
		} catch (final GlobalLadSearchAlgorithmException e) {
			log.error(String.format("Error trying to run getMatchedChildren for container %s: %s" + this, e.getMessage()), e.getCause());
			return new ArrayList<IGlobalLadContainer>();
		}
	}
		
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#generateSearchAlgorithm(jpl.gds.globallad.data.IGlobalLADData)
	 */
	@Override
	public IGlobalLadContainerSearchAlgorithm generateInsertSearchAlgorithm(final IGlobalLADData data) {
		return InsertSearchAlgorithm.createBuilder()
				.fromData(data)
				.build();
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#insert(jpl.gds.globallad.data.IGlobalLADData)
	 */
	@Override
	public boolean insert(final IGlobalLADData data) throws GlobalLadContainerException {
		return insert(data, generateInsertSearchAlgorithm(data));
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#insert(jpl.gds.globallad.data.IGlobalLADData, jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm)
	 */
	@Override
    public boolean insert(final IGlobalLADData data, final IGlobalLadContainerSearchAlgorithm matcher) throws GlobalLadContainerException {
		final long start = System.nanoTime();
		numInserts.incrementAndGet();
		
		try {
			/**
			 * We expect there to be one or zero matches since we are working in a tree like structure.  However, 
			 * this will insert to all the matches that are found.
			 * 
			 * Call the search that will return the first match.
			 */
		
			Collection<IGlobalLadContainer> containers = search(matcher);
			this.totalInsertGetTime += System.nanoTime() - start;
			
			/**
			 * If there are no containers add one. 
			 */
			if (containers.isEmpty()) {
				final IGlobalLadContainer container = insertNewContainer(data);
				
				if (container == null) {
					
					/**
					 * Because there are multiple inserters pushing data into this container there is a 
					 * race condition where the search comes up empty and a new container is set to be inserted.
					 * During this time a different insert could have already added a new container and the insert
					 * of the new container will fail.
					 * 
					 * If we get here try the search again.  If something is returned move on.  If it is 
					 * empty it is a real failure and we should error out.
					 */
					 containers = search(matcher);
					 
					 if (containers.isEmpty()) {
						log.error("Container could not be added to the data store: " + data);
						return false;
					 }
				} else {
					containers.add(container);
				}
			}
			boolean allAdded = true;
			
			for (final IGlobalLadContainer container : containers) {
				allAdded = allAdded && container.insert(data, matcher);
			}
			
			return allAdded;
		} finally {
			this.totalInsertTime += System.nanoTime() - start;
			this.numInsertGets++;
			this.lastInsert = System.currentTimeMillis();
		}
	}
	
	/**
	 * Creates a new container and adds it to the internal map by calling the add method. 
	 * 
	 * @param data
	 * @return The new container if was was created and added successfully, else null.
	 * @throws GlobalLadContainerException 
	 */
	protected IGlobalLadContainer insertNewContainer(final IGlobalLADData data) throws GlobalLadContainerException {
		/**
		 * This is a very tricky spot in the code. Especially when a client first connects the multiple inserters
		 * will be doing work at the exact same time and there will be race conditions. All of them will try
		 * to insert a new buffer but only one will succeed. The important thing is that we need to make sure
		 * that the container that actually gets inserted into the map is the one that is returned.
		 */
		final IGlobalLadContainer container = generateContainer(data);
		containers.putIfAbsent(container.getContainerIdentifier(), container);
		
		/**
		 * No matter what happens above we want to return the container that is actually in the data map 
		 * so do a get and return that container.
		 */
		return containers.get(container.getContainerIdentifier());
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#getChildrenWithRegex(java.lang.String)
	 */
	@Override
	public Collection<IGlobalLadContainer> getChildrenWithRegex(final String rx) {
		Collection<IGlobalLadContainer> matched = null;
		for (final Object key : containers.keySet()) {
			try {
				if (key.toString().matches(rx)) {
					if (matched == null) {
						matched = new ArrayList<IGlobalLadContainer>();
					}
					matched.add(containers.get(key));
				}
			} catch (final PatternSyntaxException e) {
				log.warn("Bad regex for global lad search: " + e.getMessage(), e.getCause());
			}
		}
		return matched == null ? Collections.<IGlobalLadContainer>emptyList() : matched;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#getChild(java.lang.Object)
	 */
	@Override
	public IGlobalLadContainer getChild(final Object identifier) {
		return containers.get(identifier);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#get(jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm, int)
	 */
	@Override
	public Map<Object, Collection<IGlobalLADData>> get(final IGlobalLadSearchAlgorithm matcher, final int numRecords) {
		return getImpl(matcher, numRecords);
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#get(jpl.gds.globallad.data.IGlobalLADData)
	 */
	@Override
	public Map<Object, Collection<IGlobalLADData>> getAll(final IGlobalLadSearchAlgorithm matcher) {
		return getImpl(matcher, -1);
	}
	
	/**
	 * Gets the number of records specified.  If num records is <= 0 gets all records.
	 * @param matcher
	 * @param numRecords
	 * @return
	 */
	@JsonIgnore
	protected Map<Object, Collection<IGlobalLADData>> getImpl(final IGlobalLadSearchAlgorithm matcher, final int numRecords) {
		final long start = System.nanoTime();
		try {
			final Collection<IGlobalLadContainer> matched = search(matcher);
			
			if (matched.isEmpty()) {
				return Collections.<Object, Collection<IGlobalLADData>>emptyMap();
			} else {
				Map<Object, Collection<IGlobalLADData>> dataMap = null;
				
				for (final IGlobalLadContainer container : matched) {
					final Map<Object, Collection<IGlobalLADData>> m = numRecords > 0 ? 
							container.get(matcher, numRecords) :
							container.getAll(matcher);

					/**
					 * If the map comes back empty it is more than likely an empty map from the collections
					 * emtpyMap call.  You can not use this map to add things to but we want to use this 
					 * to create empty maps whenever possible to cut down on resource use.  So we will 
					 * only assign to dataMap when the returned map is not empty.
					 */
					if (m.isEmpty()) {
						// Do nothing.
					} else if (dataMap == null) {
						dataMap = m;
					} else {
						GlobalLadUtilities.mergeMap(matcher.getTimeType(), m, dataMap, numRecords);
					}
				}
				
				return dataMap == null ? 
						Collections.<Object, Collection<IGlobalLADData>>emptyMap(): 
						dataMap;
			}
		} finally {
			this.totalQueryGetTime += System.nanoTime() - start;
			this.numQueryGets++;
			this.lastInsert = System.currentTimeMillis();
		}
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadDeltaQueryable#deltaQuery(jpl.gds.globallad.data.container.IGlobalLadSearchAlgorithm, java.util.Map)
	 */
	@Override
	public void deltaQuery(final IGlobalLadSearchAlgorithm matcher, final Map<DeltaQueryStatus, Map<Object, Collection<IGlobalLADData>>> resultMap) {
		final Collection<IGlobalLadContainer> matched = search(matcher);
		
		for (final IGlobalLadContainer container : matched) {
			container.deltaQuery(matcher, resultMap);
		}
	}

	/**
	 * This method is only used by the rehydrate method and is not really needed anymore.  If this is really 
	 * needed it should be updated to make more sense. 
	 * 
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#add(jpl.gds.globallad.data.container.IGlobalLadContainer)
	 */
	@Override
	public boolean add(final IGlobalLadContainer container) {
		containers.putIfAbsent(container.getContainerIdentifier(), container);
		return true;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#getContainerIdentifier()
	 */
	@Override
	public Object getContainerIdentifier() {
		return containerIdentifier;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#getContainerType()
	 */
	@Override
	public String getContainerType() {
		return containerType;
	}
	
	/**
	 * Used for the stats JSON, not the serialization JSON.  
	 * Is a string representation of the container type and the container identifier.
	 * Example where type is "host" and identifier is "ampcsdev1", this would return
	 * "host - ampcsdev1".
	 * 
	 * @return
	 */
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadStatable#getJsonId()
	 */
	@Override
	public String getJsonId() {
		return new StringBuilder(containerType)
			.append(" - ")
			.append(containerIdentifier)
			.toString();
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.AbstractGlobalLadContainer#generateContainer(jpl.gds.globallad.data.IGlobalLADData)
	 */
	@Override
    protected IGlobalLadContainer generateContainer(final IGlobalLADData data) throws GlobalLadContainerException {
		return GlobalLadContainerFactory.createChildContainer(this.getContainerType(), data);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadContainer#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return containers.isEmpty();
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadStatable#getSize()
	 */
	@Override
	public int getChildCount() {
		return containers.size();
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadStatable#getInsertDataCount()
	 */
	@Override
	public long getInsertDataCount() {
		long count = 0;
		for (final IGlobalLadContainer container : this.containers.values()) {
			count += container.getInsertDataCount();
		}
		
		return count;
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadStatable#getErtDataCount()
	 */
	@Override
	public long getErtDataCount() {
		long count = 0;
		for (final IGlobalLadContainer container : this.containers.values()) {
			count += container.getErtDataCount();
		}
		
		return count;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadStatable#getScetDataCount()
	 */
	@Override
	public long getScetDataCount() {
		long count = 0;
		for (final IGlobalLadContainer container : this.containers.values()) {
			count += container.getScetDataCount();
		}
		
		return count;
	}

	public double avgInsertTimeMS() {
		if (numInserts.get() == 0) {
			return 0;
		} else {
			return (totalInsertTime / numInserts.get()) / 1E6;
		}
	}
	
	public double avgQueryGetTimeMS() {
		if (numQueryGets == 0) {
			return 0;
		} else {
			return (totalQueryGetTime / numQueryGets) / 1E6;
		}
	}
	
	public long insertsPerSecond() {
		if (numInserts.get() == 0) {
			return 0;
		} else {
			final double avgNano = totalInsertTime / this.numInserts.get();
			return (long) (1 / (avgNano / 1E9));
		}
	}
	
	private JsonObjectBuilder getObjectBuilder() {
		return Json.createObjectBuilder()
			    .add(IGlobalLadContainer.CONTAINER_IDENTIFIER_KEY, this.getContainerIdentifier().toString())
			    .add(IGlobalLadContainer.CONTAINER_TYPE_KEY, this.getContainerType())
				.add("numInserts", this.numInserts.get())
				.add("insertsPerSecond", this.insertsPerSecond())
				.add("avgInsertMS", this.avgInsertTimeMS())
				.add("insertTimeNS", this.totalInsertTime)
				.add("insertGetTimeNS", this.totalInsertGetTime)
				.add("numInsertGets", this.numInsertGets)
				.add("insertGetTimePercent", totalInsertTime > 0 ? ((double)(totalInsertGetTime)/totalInsertTime)*100 : 0)
				.add("queryGetTimeNS", this.totalQueryGetTime)
				.add("numQueryGets", this.numQueryGets)
				.add("avgQueryGetTimeMS", this.avgQueryGetTimeMS())
				.add("childCount", this.getChildCount())
				.add("scetDataCount", this.getScetDataCount())
				.add("ertDataCount", this.getErtDataCount())
				.add("eventDataCount", this.getInsertDataCount());
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadStatable#getStats()
	 */
	@Override
	public JsonObject getStats() {
		return getObjectBuilder().build();
	}


	/**
	 * @return
	 */
	protected JsonObject buildSummary() {
		final JsonObjectBuilder builder = Json.createObjectBuilder()
				.add(CONTAINER_TYPE_KEY, this.containerType)
				.add(CONTAINER_IDENTIFIER_KEY, getContainerIdentifier().toString());
		
		final JsonArrayBuilder ab = Json.createArrayBuilder();
		
		for (final IGlobalLadContainer container : this.containers.values()) {
			ab.add(
					Json.createObjectBuilder()
						.add(CONTAINER_TYPE_KEY, container.getContainerType())
						.add(CONTAINER_IDENTIFIER_KEY, container.getContainerIdentifier().toString())		
					);
		}
		
		builder.add("children", ab);
		return builder.build();
	}
	
	/**
	 * Uses the matcher to find the first match and will return the stats object from 
	 * getStats.
	 * 
	 * @param matcher
	 * @return
	 */
	@Override 
	public JsonObject getMetadata(final IGlobalLadContainerSearchAlgorithm matcher) {
		try {
			return getSummaryOrMetadata(matcher, false);
		} catch (final GlobalLadSearchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return Json.createObjectBuilder().build();
	}
	
	@Override
    public JsonObject getSummary(final IGlobalLadContainerSearchAlgorithm matcher) {
		try {
			return getSummaryOrMetadata(matcher, true);
		} catch (final GlobalLadSearchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return Json.createObjectBuilder().build();
	}

	/**
	 * @param matcher
	 * @param isSummary - If true gets summary else get metadata.
	 * @return
	 * @throws GlobalLadSearchAlgorithmException 
	 */
	protected JsonObject getSummaryOrMetadata(final IGlobalLadContainerSearchAlgorithm matcher, final boolean isSummary) throws GlobalLadSearchAlgorithmException {
		/**
		 * Updating the last insert time when checking metadata.
		 */
		try {
			if (matcher.isChildMatchNeeded(this)) {
				final Collection<IGlobalLadContainer> matched = search(matcher);
				
				if (matched.isEmpty()) {
					/**
					 * No matches in children, return empty json.
					 */
					return Json.createObjectBuilder().build();
				} else {
					/**
					 * Matched some children.
					 */
					if (matched.size() > 1) {
						final String type = isSummary ? "summary": "metadata";
						log.warn("Multiple results were found for " + type + "query.  Using first match and skipping others.");
					}
					
					if (isSummary) {
						final IGlobalLadContainer c = matched.iterator().next();
						return c.getSummary(matcher);
					} else {
						final IGlobalLadContainer c = matched.iterator().next();
						return c.getMetadata(matcher);
					}
				}
				
			} else if (GlobalLadContainerFactory.isMasterContainer(this) || matcher.isContainerMatch(this)) {
				// End of the line and this matched.  Return the result JSON.
				return isSummary ? buildSummary() : getStats();
			} else {
				/**
				 * Was supposed to match self and did not match. 
				 */
				return Json.createObjectBuilder().build();
			}
		} finally {
			this.lastInsert = System.currentTimeMillis();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof BasicGlobalLadContainer)) {
			return false;
		} else if (this == obj) {
			return true;
		} else {
			final BasicGlobalLadContainer o = (BasicGlobalLadContainer) obj;
			if (o.getChildCount() == getChildCount() &&
				containerIdentifier.equals(o.getContainerIdentifier()) &&
				containerType.equals(o.getContainerType()) &&
				containers.keySet().containsAll(o.getContainers().keySet()) &&
				containers.entrySet().containsAll(o.getContainers().entrySet())
				) 
			{
				return true;
			} else {
				return false;
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("Container[");

		if (containerType != null) {
			builder.append("containerType=");
			builder.append(containerType);
			builder.append(", ");
		}
		if (containerIdentifier != null) {
			builder.append("identifier=");
			builder.append(containerIdentifier);
			builder.append(", ");
		}
		if (containers != null) {
			builder.append("containers=");
			builder.append(containers);
		}
		builder.append("]");
		return builder.toString();
	}
}
