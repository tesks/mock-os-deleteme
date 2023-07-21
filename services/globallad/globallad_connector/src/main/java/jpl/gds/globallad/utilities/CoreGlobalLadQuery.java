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
package jpl.gds.globallad.utilities;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.context.ApplicationContext;

import com.google.common.collect.Lists;

import jpl.gds.common.config.types.VenueType;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.eha.api.message.IEhaMessageFactory;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.message.IEvrMessage;
import jpl.gds.globallad.GlobalLadClientConversionException;
import jpl.gds.globallad.GlobalLadException;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.EhaGlobalLadData;
import jpl.gds.globallad.data.EvrGlobalLadData;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.DataSource;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.RecordedState;
import jpl.gds.globallad.data.utilities.BaseGlobalLadQuery;
import jpl.gds.globallad.data.utilities.GlobalLadQueryParams;
import jpl.gds.globallad.data.utilities.GlobalLadQueryParams.GlobalLadQueryParamsBuilder;
import jpl.gds.shared.log.Tracer;

/**
 * Core implementation of the global lad query.  Includes the legacy global lad query algorithms.
 * Please note: using the methods in this class will attempt to use the global 
 * EvrDefinitionTable instances to reconstruct Evrs. Global instances should
 * be loaded with the intended dictionary.
 */
public class CoreGlobalLadQuery extends BaseGlobalLadQuery implements
		ICoreGlobalLadQuery {

	private static final Tracer log = GlobalLadProperties.getTracer();

	final LadChannelValueConverter converter;
	
	/**
	 * @param factory data factory used to recreate global lad objects from byte arrays.
	 */
	public CoreGlobalLadQuery(final ApplicationContext appContext, final IGlobalLadDataFactory factory, final LadChannelValueConverter converter) {
        super(factory, appContext);
		this.converter = converter;
        log.setAppContext(appContext);
	}

	@FunctionalInterface
	private interface LadConverterFunction<T extends IGlobalLADData, R> {
		/** Helper functional interface to leverage the private convert(Eha|Evr) functions
		 *  for returning both message and non-message representations of IGlobalLadData.  
		 * @param ladData - the ladData to be converted.
		 * @return the result of converting ladData to type R.
		 * @throws ParseException if the conversion fails because of an unparseable field of ladData.
		 * @throws DictionaryException if there is a problem restoring dictionary information to the ladData instance.
		 */
		public abstract R convert(T ladData) throws GlobalLadClientConversionException;
		
	}

	/**
	 * Checks to make sure each data point in data is an EHA glad data and converts 
	 * to GladChannelValues.
	 * @param <R>
	 * 
	 * @param data
	 * @param converter the converter function to use.  
	 * @return Collection of converted IChannelValues.
	 * @throws DictionaryException 
	 */
	private <R> Collection<R> convertEha(final Collection<IGlobalLADData> data, final LadConverterFunction<EhaGlobalLadData, R> converter) {
		final Collection<R> values = new ArrayList<R>();
		for (final IGlobalLADData d : data) {
			if (d instanceof EhaGlobalLadData) {
				final EhaGlobalLadData eha = (EhaGlobalLadData) d;
				try {
					values.add(converter.convert(eha));
				} catch (final GlobalLadClientConversionException e) {
					log.warn(e.getMessage() + ". Failed to parse a field of global LAD data: " + eha.toString(), e.getCause());
				}
			} else {
				log.warn("Data object is not eha: " , d);
			}
		}

		return values;
	}
	
	/**
	 * Converts global LAD EHA representations to AMPCS core representations of ChannelValues, filling in as much information as possible.
	 * @param data collection of IGlobalLADData to convert.
	 * @return Collection of IChannelValues corresponding to each IGlobalLADData object in the data collection.
	 * @throws DictionaryException
	 * @throws GlobalLadClientConversionException 
	 */
	public Collection<IClientChannelValue> convertEha(final Collection<IGlobalLADData> data) {
		return convertEha(data, converter::convert);
	}

	/**
	 * Converts global LAD EHA representations to AMPCS core representations of EhaChannelMessages, filling in as much information as possible.
	 * Use this if the original GLAD event time should be kept.
	 * @param data collection of IGlobalLADData to convert.
	 * @return Collection of IChannelValues corresponding to each IGlobalLADData object in the data collection.
	 * @throws DictionaryException
	 * @throws GlobalLadClientConversionException 
	 */
	public Collection<IAlarmedChannelValueMessage> convertEhaToMessages(final Collection<IGlobalLADData> data, final IEhaMessageFactory ehaMessageFactory) {
		return convertEha(data, converter::convertToMessage);
	}


	/**
	 * Checks to make sure each data point in data is an EVR glad data and converts 
	 * to GladEvr.
	 * @param <T>
	 * 
	 * @param data
	 * @return Collection of converted IEvr
	 * @throws DictionaryException 
	 */
	private <R> Collection<R> convertEvrs(final Collection<IGlobalLADData> data, final LadConverterFunction<EvrGlobalLadData, R> converter) {
		final Collection<R> values = new ArrayList<R>();
		for (final IGlobalLADData d : data) {
			if (d instanceof EvrGlobalLadData) {
				final EvrGlobalLadData evr = (EvrGlobalLadData) d;
				try {
					values.add(converter.convert(evr));
				} catch (final GlobalLadClientConversionException e) {
					log.warn(e.getMessage() + ". Failed to parse a field of global LAD data: " + evr.toString(), e.getCause());
				}
			} else {
				log.warn("Data object is not evr: " , d);
			}
		}

		return values;
	}
	
	/**
	 * Converts global LAD Evr representations to AMPCS core representations of Evrs, filling in as much information as possible.
	 * @param data collection of IGlobalLADData to convert.
	 * @return Collection of IEvrs corresponding to each IGlobalLADData object in the data collection.
	 */
	public Collection<IEvr> convertEvrs(final Collection<IGlobalLADData> data) {
		final LadEvrConverter converter = new LadEvrConverter(appContext);
		final Collection<IEvr> evrs = convertEvrs(data, converter::convert);
		return evrs;
	}

	/**
	 * Converts global LAD EVR representations to AMPCS core representations of EvrMessages, filling in as much information as possible.
	 * Use this if the original GLAD event time should be kept.
	 * @param data collection of IGlobalLADData to convert.
	 * @return Collection of IChannelValues corresponding to each IGlobalLADData object in the data collection.
	 */
	public Collection<IEvrMessage> convertEvrsToMessages(final Collection<IGlobalLADData> data) {
		final LadEvrConverter converter = new LadEvrConverter(appContext);
		final Collection<IEvrMessage> evrs = convertEvrs(data, converter::convertToMessage);
		return evrs;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.utilities.ICoreGlobalLadQuery#getChannelHistory(java.lang.String, java.lang.String, int, jpl.gds.config.VenueType, java.lang.String, java.lang.Long, java.lang.Iterable, int)
	 */
	@Override
	public Collection<IClientChannelValue> getChannelHistory(final String channelId,
			final DataSource source , final RecordedState recordedState, final GlobalLadPrimaryTime timeType, 
			final int scid, final VenueType venue,
			final String sessionHost, final Long sessionId,
			final Iterable<Integer> stationIdList, final int maxResults)
			throws GlobalLadException {
		
		if (channelId == null) {
			throw new GlobalLadException("ChannelId must not be null");
		}
		
		/**
		 * Updated to use the builder.
		 */
		final GlobalLadQueryParamsBuilder builder = GlobalLadQueryParams.createBuilder()
				.setSource(source)
				.setRecordedState(recordedState)
				.setTimeType(timeType)
				.setChannelId(channelId)
				.setSessionId(sessionId)
				.setHostRegex(sessionHost)
				.setVenueRegex(venue.toString())
				.setDssIds(Lists.newArrayList(stationIdList))
				.setScid(scid)
				.setMaxResults(maxResults);

        log.debug(builder);
		final Collection<IGlobalLADData> data = ehaLadQuery(builder);
		
		return convertEha(data);
	}
	

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.utilities.ICoreGlobalLadQuery#getLadAsChannelValue(int, jpl.gds.config.VenueType, java.lang.String, java.lang.Long, java.lang.Iterable)
	 */
	@Override
	public Collection<IClientChannelValue> getLadAsChannelValue(final TimeComparisonStrategyContextFlag timeStrategy,
			final int scid,
			final VenueType venue, final String sessionHost, final Long sessionId,
			final Iterable<Integer> stationIdList) throws GlobalLadException {
        /**
         * Map the time strategy to the global lad time value to use for the query.
         * This is redundant in my book and should be done outside this class, but in this case we
         * are trying to mimic the old behavior.
         */
        GlobalLadPrimaryTime timeType;
        
        switch (timeStrategy.getTimeComparisonStrategy()) {
		case ERT:
			timeType = GlobalLadPrimaryTime.ERT;
			break;
		case SCET:
			timeType = GlobalLadPrimaryTime.SCET;
			break;
		case SCLK:
			timeType = GlobalLadPrimaryTime.SCLK;
			break;
		case LAST_RECEIVED:
		default:
			timeType = GlobalLadPrimaryTime.EVENT;
			break;
        }
		
		final GlobalLadQueryParamsBuilder builder = GlobalLadQueryParams.createBuilder()
				.setSource(DataSource.all)
				.setRecordedState(RecordedState.both)
				.setTimeType(timeType)
				.setSessionId(sessionId)
				.setHostRegex(sessionHost)
				.setVenueRegex(venue.toString())
				.setDssIds(Lists.newArrayList(stationIdList))
				.setScid(scid)
				.setMaxResults(1);
				
		final Collection<IGlobalLADData> data = ehaLadQuery(builder);
		
		return convertEha(data);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.utilities.ICoreGlobalLadQuery#getLadAsEvrValue(int, jpl.gds.config.VenueType, java.lang.String, java.lang.Long, java.lang.Iterable)
	 */
	@Override
	public Collection<IEvr> getLadAsEvrValue(final int scid, final VenueType venue,
			final String sessionHost, final Long sessionId, final Iterable<Integer> stationIdList)
			throws GlobalLadException {
		
		final GlobalLadQueryParamsBuilder builder = GlobalLadQueryParams.createBuilder()
				.setSource(DataSource.all)
				.setRecordedState(RecordedState.both)
				.setTimeType(GlobalLadPrimaryTime.ALL)
				.setSessionId(sessionId)
				.setHostRegex(sessionHost)
				.setVenueRegex(venue.toString())
				.setDssIds(Lists.newArrayList(stationIdList))
				.setScid(scid)
				.setMaxResults(1);
		
		final Collection<IGlobalLADData> data = evrLadQuery(builder);
		
		return convertEvrs(data);
	}
	
}
