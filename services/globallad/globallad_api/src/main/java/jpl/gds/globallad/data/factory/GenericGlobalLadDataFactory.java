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
package jpl.gds.globallad.data.factory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.AlarmHistoryGlobalLadData;
import jpl.gds.globallad.data.EhaGlobalLadData;
import jpl.gds.globallad.data.EvrGlobalLadData;
import jpl.gds.globallad.data.GlobalLadDataException;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.serialization.globallad.data.Proto3GlobalLadTransport;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * Generic implementation of the AbstractGlobalLadDataFactory for the core code base that handles
 * the the basic global lad data types for all types of EHA and EVRs.
 * 
 * This class is responsible for mapping the user data types.  To update either change this class or create a 
 * new implementation and update the user data types in it.  The converter class used for queries will create a new 
 * instance of the factory class configured in the global lad properties file.
 */
public class GenericGlobalLadDataFactory implements IGlobalLadDataFactory {
	/**
	 * Constants for user data types.  
	 */
	public static final byte FSW_REALTIME_EHA = 0;
	public static final byte FSW_RECORDED_EHA = 1;
	public static final byte HEADER_EHA = 2;
	public static final byte MONITOR_EHA = 3;
	public static final byte SSE_EHA = 4;
	public static final byte FSW_REALTIME_EVR = 5;
	public static final byte FSW_RECORDED_EVR = 6;
	public static final byte SSE_EVR = 7;

	public static final Collection<Byte> fswEhaRealtime = Arrays.asList(FSW_REALTIME_EHA);
	public static final Collection<Byte> fswEhaRecorded = Arrays.asList(FSW_RECORDED_EHA);
	public static final Collection<Byte> headerEha = Arrays.asList(HEADER_EHA);
	public static final Collection<Byte> monitorEha = Arrays.asList(MONITOR_EHA);
	public static final Collection<Byte> sseEha = Arrays.asList(SSE_EHA);
	public static final Collection<Byte> fswEvrRealtime = Arrays.asList(FSW_REALTIME_EVR);
	public static final Collection<Byte> fswEvrRecorded = Arrays.asList(FSW_RECORDED_EVR);
	public static final Collection<Byte> sseEvrs = Arrays.asList(SSE_EVR);

	public static final Collection<Byte> fswEha = Arrays.asList(FSW_REALTIME_EHA, FSW_RECORDED_EHA);
	public static final Collection<Byte> fswEvr = Arrays.asList(FSW_REALTIME_EVR, FSW_RECORDED_EVR);
	
	public static final Collection<Byte> realtimeEvr = Arrays.asList(FSW_REALTIME_EVR, SSE_EVR);
	public static final Collection<Byte> realtimeEha = Arrays.asList(FSW_REALTIME_EHA, HEADER_EHA, MONITOR_EHA, SSE_EHA);

	public static final Collection<Byte> allEhas = Arrays.asList(FSW_REALTIME_EHA, FSW_RECORDED_EHA, HEADER_EHA, MONITOR_EHA, SSE_EHA);
	
	/**
	 * recorded evr was not part of the list but the recorded eha was.  This caused a lot of issues.
	 */
	public static final Collection<Byte> allEvrs = Arrays.asList(FSW_REALTIME_EVR, FSW_RECORDED_EVR, SSE_EVR);
	
    private static final Tracer          log              = TraceManager.getTracer(Loggers.GLAD);
	

	/**
	 * Default constructor.
	 */
	public GenericGlobalLadDataFactory() {
		super();
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.factory.IGlobalLadDataFactory#lookupUserDataType(jpl.gds.globallad.data.IGlobalLADData)
	 */
	@Override
	public byte lookupUserDataType(final IGlobalLADData data) {
		if (data instanceof EhaGlobalLadData) {
			return getUserDataType((EhaGlobalLadData) data);
		} else if (data instanceof EvrGlobalLadData) {
			return getUserDataType((EvrGlobalLadData) data);
		} else {
			// Unknown type
			return -1;
		}
	}
	
	/**
	 * Knows how to get UDT of eha objects. 
	 * @param eha
	 * @return the user data type of eha
	 */
	private byte getUserDataType(final EhaGlobalLadData eha) {
		if (eha.isFsw()) {
			return eha.isRealTime() ? FSW_REALTIME_EHA : FSW_RECORDED_EHA;
		} else if (eha.isHeader()) {
			return HEADER_EHA;
		} else if (eha.isMonitor()) {
			return MONITOR_EHA;
		} else {
			return SSE_EHA; // SSE
		}
	}
	
	/**
	 * Knows how to get UDT of evr objects. 
	 * 
	 * @param evr
	 * @return the user data type of evr.
	 */
	private byte getUserDataType(final EvrGlobalLadData evr) {
		if (evr.isFsw()) {
			return evr.isRealTime() ? FSW_REALTIME_EVR : FSW_RECORDED_EVR;
		} else {
			return SSE_EVR; // SSE.  No recorded SSE.
		}
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.factory.IGlobalLadDataFactory#lookupUserDataTypes(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public Collection<Byte> lookupUserDataTypes(final String queryType, final String dataSource, final String recordedState) {
		try {
			return lookupUserDataTypes(QueryType.valueOf(queryType),
					DataSource.valueOf(dataSource), 
					RecordedState.valueOf(recordedState));
		} catch (final Exception e) {
			GlobalLadProperties.getTracer().error("Failed to convert values to enum: " + e.getMessage(), e.getCause());
			return Collections.<Byte>emptyList();
		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.factory.IGlobalLadDataFactory#lookupUserDataTypes(jpl.gds.globallad.data.factory.IGlobalLadDataFactory.QueryType, jpl.gds.globallad.data.factory.IGlobalLadDataFactory.DataSource, jpl.gds.globallad.data.factory.IGlobalLadDataFactory.RecordedState)
	 */
	@Override
	public Collection<Byte> lookupUserDataTypes(final QueryType queryType, final DataSource dataSource, final RecordedState recordedState) {
		switch (queryType) {
		case eha:
		case alarm:
			return getEhaUserDataType(dataSource, recordedState);
		case evr:
			return getEvrUserDataType(dataSource, recordedState);
		default:
			return Collections.<Byte>emptyList();
		}
	}
	
	/**
	 * @param dataSource
	 * @param recordedState
	 * @return collection of user data types associated with dataSource and recordedState
	 */
	public static Collection<Byte> getEhaUserDataType(final DataSource dataSource, final RecordedState recordedState) {
		switch (dataSource) {
		case fsw:
			switch(recordedState) {
			case realtime:
				return fswEhaRealtime;
			case recorded:
				return fswEhaRecorded;
			case both:
			default:
				return fswEha;
			}
		case header:
			return headerEha;
		case monitor:
			return monitorEha;
		case sse:
			return sseEha;
		case all:
			switch(recordedState) {
			case realtime:
				return realtimeEha;
			case recorded:
				return fswEhaRecorded;
			case both:
			default:
			}
			
		default:
			return allEhas;
		}
	}
	
	/**
	 * @param dataSource
	 * @param recordedState
	 * @return collection of user data types associated with dataSource and recordedState
	 */
	public static Collection<Byte> getEvrUserDataType(final DataSource dataSource, final RecordedState recordedState) {
		switch (dataSource) {
		case fsw:
			switch(recordedState) {
			case realtime:
				return fswEvrRealtime;
			case recorded:
				return fswEvrRecorded;
			case both:
			default:
				return fswEvr;
			}
		case sse:
			return sseEvrs;
		case header:
		case monitor:
			return Collections.<Byte>emptyList();
		case all:
			switch(recordedState) {
			case realtime:
				return realtimeEvr;
			case recorded:
				return fswEvrRecorded;
			case both:
			default:
			}
		default:
			return allEvrs;
		}
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.globallad.data.factory.IGlobalLadDataFactory#isRealtime(byte)
	 */
	@Override
	public boolean isRealtime(final byte userDataType) {
		return realtimeEha.contains(userDataType) || realtimeEvr.contains(userDataType);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.globallad.data.factory.IGlobalLadDataFactory#isSse(byte)
	 */
	@Override
	public boolean isSse(final byte userDataType) {
		return sseEha.contains(userDataType) || sseEvrs.contains(userDataType);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.globallad.data.factory.IGlobalLadDataFactory#isHeader(byte)
	 */
	@Override
	public boolean isHeader(final byte userDataType) {
		return headerEha.contains(userDataType);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.globallad.data.factory.IGlobalLadDataFactory#isMonitor(byte)
	 */
	@Override
	public boolean isMonitor(final byte userDataType) {
		return monitorEha.contains(userDataType);
	}

	@Override
    public boolean isFsw(final byte userDataType) {
		return fswEha.contains(userDataType) || fswEvr.contains(userDataType);
	}

	@Override
	public IGlobalLADData loadLadData(final Proto3GlobalLadTransport transport) throws GlobalLadDataException {
		IGlobalLADData data = null;
        log.trace("Loading ", transport, " into the GLAD");

		switch (transport.getGladDataCase()) {
		case EHA:
			data = new EhaGlobalLadData(transport.getEha());
			break;
		case EVR:
			data = new EvrGlobalLadData(transport.getEvr());
			break;
		case HISTORY:
			data = new AlarmHistoryGlobalLadData(transport.getHistory());
			break;
		case GLADDATA_NOT_SET:
		default:
			throw new GlobalLadDataException("Unknown transport type for global lad data");
		}
		
		return data;
	}
}
