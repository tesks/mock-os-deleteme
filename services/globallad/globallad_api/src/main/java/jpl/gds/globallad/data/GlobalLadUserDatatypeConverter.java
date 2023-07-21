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
package jpl.gds.globallad.data;

import java.util.Collection;

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.factory.GenericGlobalLadDataFactory;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.DataSource;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.QueryType;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.RecordedState;

/**
 * This class turned into a proxy for the IGlobalLadDataFactory.  This will create a static instance that 
 * will be used to look up the user data types given the input enums.  This allows for the decoupling of the 
 * actual values from any production code other than the implementation of the data factory.
 */
public class GlobalLadUserDatatypeConverter {
	private static IGlobalLadDataFactory factory;
	
	static {
		try {
			factory = new GenericGlobalLadDataFactory();
		} catch (Exception e) {
			GlobalLadProperties.getTracer().error("Failed to create data factory for user data conversion: " + e.getMessage(), e.getCause());
		}
	}
	
	/**
	 * Looks up the user data type from the data factory.
	 * 
	 * @param data
	 * @return Mapped user data type of data.
	 */
	public static byte lookupUserDataType(IGlobalLADData data) {
		return factory.lookupUserDataType(data);
	}
	
	/**
	 * Calls getUserDataType on the object and sets the returned value in data. 
	 * 
	 * @param data
	 */
	public static void setUserDataTypeFromData(IGlobalLADData data) {
		data.setUserDataType(lookupUserDataType(data));
	}

	
	/**
	 * Looks up the user data type for the given parameters.
	 * 
	 * @param queryType
	 * @param dataSource
	 * @param recordedState
	 * 
	 * @return all user data types associated with the given paramters.
	 */
	public static Collection<Byte> lookupUserDataTypes(String queryType, String dataSource, String recordedState) {
		return factory.lookupUserDataTypes(queryType, dataSource, recordedState);
	}
	
	/**
	 * Looks up the user data type for the given parameters.
	 * 
	 * @param queryType 
	 * @param dataSource - fsw|header|sse|monitor
	 * @param recordedState - realtime|recorded.  If null 
	 * 
	 * @return all user data types associated with the given paramters.
	 */
	public static Collection<Byte> lookupUserDataTypes(QueryType queryType, DataSource dataSource, RecordedState recordedState) {
		return factory.lookupUserDataTypes(queryType, dataSource, recordedState);
	}
	
}
