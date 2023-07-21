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
package jpl.gds.globallad.data.utilities;

import java.util.Collection;
import java.util.Map;

import jpl.gds.globallad.GlobalLadException;
import jpl.gds.globallad.data.GlobalLadDataIterator;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.container.IGlobalLadDeltaQueryable.DeltaQueryStatus;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.QueryType;
import jpl.gds.globallad.data.utilities.GlobalLadQueryParams.GlobalLadQueryParamsBuilder;

/**
 * public interface to query the global lad from within MPCS.  
 */
public interface IGlobalLadQuery {
	/**
	 * Does a lad query using the values set in builder.
	 * 
	 * @param builder
	 * @return collection of query results.
	 * @throws GlobalLadException
	 */
	public Collection<IGlobalLADData> ladQuery(GlobalLadQueryParamsBuilder builder) throws GlobalLadException;
	
	/**
	 * Ensures the query type is set properly and calls ladQuery.
	 * 
	 * @param builder
	 * @return collection of eha query results.
	 * @throws GlobalLadException
	 */
	public default Collection<IGlobalLADData> ehaLadQuery(GlobalLadQueryParamsBuilder builder) throws GlobalLadException {
		return ladQuery(builder.setQueryType(QueryType.eha));
	}
	
	/**
	 * Ensures the query type is set properly and calls ladQuery.
	 * 
	 * @param builder
	 * @return collection of evr query results.
	 * @throws GlobalLadException
	 */
	public default Collection<IGlobalLADData> evrLadQuery(GlobalLadQueryParamsBuilder builder) throws GlobalLadException {
		return ladQuery(builder.setQueryType(QueryType.evr));
	}
	
	/**
	 * Does a verified lad query using the values in builder.  This is synonomous with delta queries.
	 * 
	 * The status is one of the following:
	 * <p>
	 * <b>complete</b> - All of the data requested is present.
	 * <p>
	 * <b>incomplete</b> - It is possible the collection of data requested is complete but it is not certain.  This happens when the 
	 * lower bound time falls on or after the last value that was trimmed from a data buffer.
	 * <p>
	 * <b>unknown</b> - No lower bound time was defined.  There is no way to know if the data is complete or incomplete.
	 * 
	 * @param builder
	 * @return map of collections of query results keyed by the query status.
	 * @throws GlobalLadException 
	 */
	public Map<DeltaQueryStatus, Collection<IGlobalLADData>> ladQueryVerified(GlobalLadQueryParamsBuilder builder) throws GlobalLadException;
	
	/**
	 * Ensures the query type is set properly and does a verified lad query  using the values in builder.  
	 * This is synonomous with delta queries.
	 * <p>
	 * The status is one of the following:
	 * <p>
	 * <b>complete</b> - All of the data requested is present.
	 * <p>
	 * <b>incomplete</b> - It is possible the collection of data requested is complete but it is not certain.  This happens when the 
	 * lower bound time falls on or after the last value that was trimmed from a data buffer.
	 * <p>
	 * <b>unknown</b> - No lower bound time was defined.  There is no way to know if the data is complete or incomplete.
	 * 
	 * @param builder
	 * @return map of collections of query results keyed by the query status.
	 * @throws GlobalLadException 
	 */
	public default Map<DeltaQueryStatus, Collection<IGlobalLADData>> ehaLadQueryVerified(GlobalLadQueryParamsBuilder builder) throws GlobalLadException {
		return ladQueryVerified(builder.setQueryType(QueryType.eha));
	}
	
	/**
	 * Ensures the query type is set properly and does a verified lad query  using the values in builder.  
	 * This is synonomous with delta queries.
	 * <p>
	 * The status is one of the following:
	 * <p>
	 * <b>complete</b> - All of the data requested is present.
	 * <p>
	 * <b>incomplete</b> - It is possible the collection of data requested is complete but it is not certain.  This happens when the 
	 * lower bound time falls on or after the last value that was trimmed from a data buffer.
	 * <p>
	 * <b>unknown</b> - No lower bound time was defined.  There is no way to know if the data is complete or incomplete.
	 * 
	 * @param builder
	 * @return map of collections o
	*/
	public default Map<DeltaQueryStatus, Collection<IGlobalLADData>> evrLadQueryVerified(GlobalLadQueryParamsBuilder builder) throws GlobalLadException {
		return ladQueryVerified(builder.setQueryType(QueryType.evr));
	}
	
	/**
	 * Sets the binary response flag and will query the global lad and return a data iterator from the response.
	 * @param builder the query builder
	 * @param isVerified if this is a verified query.
	 * @return glad data iterator
	 */
	public GlobalLadDataIterator getLadQueryIterator(GlobalLadQueryParamsBuilder builder, boolean isVerified);

	/**
	 * Sets the binary response flag and will query the global lad and return a data iterator from the response.
	 * 
	 * @param path the path 
	 * @param builder the query builder
	 * @param isVerified if this is a verified query.
	 * @return glad data iterator
	 */
	public GlobalLadDataIterator getLadQueryIterator(String path, GlobalLadQueryParamsBuilder builder, boolean isVerified);
}
