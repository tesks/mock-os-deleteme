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

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;

import jpl.gds.globallad.IGlobalLadReapable;
import jpl.gds.globallad.IGlobalLadSummarizableContainer;
import jpl.gds.globallad.data.GlobalLadContainerException;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.container.buffer.SortedSetGlobalLadDataBuffer;
import jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm;
import jpl.gds.globallad.data.json.views.GlobalLadSerializationViews;


/**
 * Interface controlling the most basic level of the global lad containers.  It is expected to be a container
 * of containers.  
 * 
 * Includes annotations for the serialization for the basics which are containerIdentifier and containerType.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes({
	@Type(value = BasicGlobalLadContainer.class, name="container"),
	@Type(value = SortedSetGlobalLadDataBuffer.class, name="ringBuffer")
	})

public interface IGlobalLadContainer extends IGlobalLadQueryable, IGlobalLadDeltaQueryable, IGlobalLadStatable, IGlobalLadReapable, IGlobalLadSummarizableContainer {
	public static final String CONTAINER_TYPE_KEY = "containerType";
	public static final String CONTAINER_IDENTIFIER_KEY = "containerIdentifier";
	
	/**
	 * Removes all children from this container.
	 * 
	 */
	@JsonIgnore
	public void clear();
	
	/**
	 * @return The specific identifier for this container. 
	 */
	@JsonProperty("containerIdentifier")
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	public Object getContainerIdentifier();
		
	/**
	 * @return The container type set on container instantiation.
	 */
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	public String getContainerType();
	
	/**
	 * Adds the container to the local storage.
	 * 
	 * @param container new container to add.
	 * 
	 * @return false if the container could not be added.
	 */
	public boolean add(IGlobalLadContainer container);

	/**
	 * Finds the proper place in the data tree to insert the data container using matcher to find its place.
	 * 
	 * @param data
	 * @param matcher
	 * @return True if the data was added successfully.
	 * @throws GlobalLadContainerException 
	 */
	public boolean insert(IGlobalLADData data, IGlobalLadContainerSearchAlgorithm matcher) throws GlobalLadContainerException;

	/**
	 * Finds the proper place in the data tree to insert data.
	 * @param data
	 * 
	 * @return True if the data was added successfully.
	 * @throws GlobalLadContainerException 
	 */
	public boolean insert(IGlobalLADData data) throws GlobalLadContainerException;
	
	/**
	 * Returns the child container that is identified by identifier, or null if none is found.
	 * 
	 * @param identifier
	 * @return The child keyed by identifier, null if not a child.
	 */
	@JsonIgnore
	public IGlobalLadContainer getChild(Object identifier);
	
	/**
	 * @return Collection of all children.
	 */
	@JsonIgnore
	public Collection<IGlobalLadContainer> getChildren();
	
	/**
	 * Finds any children that have an identifier matching tx.  The identifier will 
	 * be converted to string using its toString() method for the check.
	 * 
	 * @param rx regular expression to match child identifiers.
	 * @return Collection of children with identifiers that match rx.
	 */
	@JsonIgnore
	public Collection<IGlobalLadContainer> getChildrenWithRegex(String rx);
	
	/**
	 * Create a container specific search algorithm using data.  Used for inserting.  This algorithm
	 * needs to be very fast when doing the isMatched checks for inserting.
	 * 
	 * @param data
	 * @return Generate a search algorithm to find the proper ring buffer to insert data.
	 */
	@JsonIgnore
	public IGlobalLadContainerSearchAlgorithm generateInsertSearchAlgorithm(IGlobalLADData data);
	
	/**
	 * @return The sum of all time spent "inserting" data in nanoseconds.
	 */
	@JsonIgnore
	public long getTotalInsertTimeNS();

	/**
	 * @return The sum of all time spent "get'ing" data for inserting in nanoseconds.
	 */
	@JsonIgnore
	public long getTotalInsertGetTimeNS();

	
	/**
	 * @return The sum of all time spent "get'ing" data for querying in nanoseconds.
	 */
	@JsonIgnore
	public long getTotalQueryGetTimeNS();
	
	
	/**
	 * @return If there are any children.
	 */
	@JsonIgnore
	public boolean isEmpty();
	
	/**
	 * Remove containers that are matched by matcher.  This should only remove containers that 
	 * do not have children that match.  It should allow a caller to define a path to a set of containers 
	 * to be removed.
	 * 
	 * @param matcher
	 * @return True if this can be removed.
	 * @throws Exception
	 */
	@JsonIgnore
	public boolean remove(IGlobalLadContainerSearchAlgorithm matcher) throws Exception;
}
