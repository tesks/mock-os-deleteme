/*
 * Copyright 2006-2020. California Institute of Technology.
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
package jpl.gds.globallad.spring.mvc.resources;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jpl.gds.globallad.GlobalLadException;
import jpl.gds.globallad.data.GlobalLadDataException;
import jpl.gds.globallad.data.GlobalLadUserDatatypeConverter;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.container.GlobalLadUtilities;
import jpl.gds.globallad.data.container.IGlobalLadDeltaQueryable.DeltaQueryStatus;
import jpl.gds.globallad.data.container.search.query.BasicQuerySearchAlgorithm.BasicQuerySearchAlgorithmBuilder;
import jpl.gds.globallad.data.container.search.query.EhaQuerySearchAlgorithm;
import jpl.gds.globallad.data.container.search.query.EvrQuerySearchAlgorithm;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.DataSource;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.QueryType;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.RecordedState;
import jpl.gds.globallad.data.utilities.ConvertUtils;
import jpl.gds.globallad.data.utilities.JsonUtilities;
import jpl.gds.globallad.message.handler.IGlobalLad;
import jpl.gds.globallad.rest.resources.QueryOutputFormat;
import jpl.gds.globallad.rest.resources.ResourceUris;
import jpl.gds.shared.gdr.GDR;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Query the lad. 
 */
@RestController
@Scope("request")
@Api
public class LadQueryResource extends AbstractGlobalLadMVCResource {
	@Autowired
	IGlobalLad glad;

	/**
	 * Does a query with all filter options as query parameters.
	 * 
	 * @param queryType
	 * @param source
	 * @param recordedState
	 * @param timeType
	 * @param channelIdRegexes
	 * @param evrLevelRegexes
	 * @param evrNameRegexes
	 * @param messageRegexes
	 * @param sessionIds
	 * @param hostRegexes
	 * @param venueRegexes
	 * @param dssIds
	 * @param vcids
	 * @param maxResults
	 * @param lowerBoundTimeString
	 * @param upperBoundTimeString
	 * @param verified - If this is true and there is a lower bound time this will perform a verified delta query.  The 
	 * @param outputFormat One of the following. [json, csv].  The default value is json.
	 * output will be grouped in a JSON of complete and incomplete results.  If we can not guarantee that the results are 
	 * complete it is incomplete.
	 * 
	 * MPCS-7918 triviski 1/29/2016 - Adding options and updates to allow for csv output.
	 * 
	 * @return Response with the requested data as an octet stream as the entity body.
	 * @throws Exception
	 */
	@GetMapping(path=ResourceUris.dataAgnosticTimeTypeMinimal, 
			produces={MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_PLAIN})
	@ApiOperation(value="Query the global lad.",
	response=IGlobalLADData.class,
	responseContainer="Map")
	public StreamingResponseBody getDataWithQueryParams(
			@ApiParam(value="The query type", allowableValues=ResourceUris.queryTypeSwagger, required=true) 
			@PathVariable(ResourceUris.queryTypeQP) final QueryType queryType,

			@ApiParam(value="The data source", allowableValues=ResourceUris.sourceSwagger, required=true) 
			@PathVariable(ResourceUris.sourceQP) final DataSource source, 

			@ApiParam(value="The recorded state", allowableValues=ResourceUris.recordedStatSwagger, required=true) 
			@PathVariable(ResourceUris.recordedStateQP) final RecordedState recordedState,

			@ApiParam(value="The time type of the query", allowableValues=ResourceUris.timeTypeSwagger, required=true) 
			@PathVariable(ResourceUris.timeTypeQP) final String timeType,

			@ApiParam(value="Channel ID regular expressions", required=false, defaultValue="", allowMultiple=true)
			@RequestParam(name=ResourceUris.channelIdQP, required=false) final List<String> channelIdRegexes,

			@ApiParam(value="EVR identifiers", required=false, defaultValue="", allowMultiple=true)
			@RequestParam(name=ResourceUris.evrIdQP, required=false) final List<String> evrIds,

			@ApiParam(value="EVR level regular expressions", required=false, defaultValue="", allowMultiple=true)
			@RequestParam(name=ResourceUris.evrLevelQP, required=false) final List<String> evrLevelRegexes,

			@ApiParam(value="EVR name regular expressions", required=false, defaultValue="", allowMultiple=true)
			@RequestParam(name=ResourceUris.evrNameQP, required=false) final List<String> evrNameRegexes,

			@ApiParam(value="EVR message regular expressions", required=false, defaultValue="", allowMultiple=true)
			@RequestParam(name=ResourceUris.evrMessageQP, required=false) final List<String> messageRegexes,

			@ApiParam(value="Session IDs", required=false, defaultValue="", allowMultiple=true)
			@RequestParam(name=ResourceUris.sessionIdQP, required=false) final List<String> sessionIds,

			@ApiParam(value="Session hosts", required=false, defaultValue="", allowMultiple=true)
			@RequestParam(name=ResourceUris.hostQP, required=false) final List<String> hostRegexes,

			@ApiParam(value="Session venues", required=false, defaultValue="", allowMultiple=true)
			@RequestParam(name=ResourceUris.venueQP, required=false) final List<String> venueRegexes, 

			@ApiParam(value="Session DSS Identifier", required=false, defaultValue="", allowMultiple=true)
			@RequestParam(name=ResourceUris.dssIdQP, required=false) final List<String> dssIds, 

			@ApiParam(value="Virtual channel identifiers", required=false, defaultValue="", allowMultiple=true)
			@RequestParam(name=ResourceUris.vcidQP, required=false) final List<String> vcids,

			@ApiParam(value="Spacecraft Identifier", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.scidQP, required=false) final Integer scid,

			@ApiParam(value="Maximum query results for each matching identifier", required=false, defaultValue="-1", allowMultiple=false)
			@RequestParam(name=ResourceUris.maxResultsQP, required=false, defaultValue="-1") final int maxResults,

			@ApiParam(value="The lower bound time for a time box query.", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.lowerBoundTimeQP, required=false) final String lowerBoundTimeString,

			@ApiParam(value="The upper bound time for a time box query.", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.upperBoundTimeQP, required=false) final String upperBoundTimeString,

			@ApiParam(value = "If true and a lower bound time is given this will verify a query, meaning it will group the outputs based "
					+ "on verified or unverified.  Verified means that the last trimmed data point was before the lower bound time, "
					+ "meaning there is no fear that expected data was trimmed.  Unverified means this condition can not be guarenteed.", 
					required = false, defaultValue = "false", allowMultiple = false)			
			@RequestParam(name=ResourceUris.verifiedQP, required=false, defaultValue="false") final boolean verified, 

			@ApiParam(value="Used to stream query results from the global lad as byte data.", hidden=true, required=false, defaultValue="false", allowMultiple=false)
			@RequestParam(name=ResourceUris.binaryResponseQP, required=false, defaultValue="false") final boolean binaryResponse,

			@ApiParam(value="Specify the output format", required=false, allowMultiple=false, allowableValues="json, csv")
			@RequestParam(name=ResourceUris.outputFormatQP, defaultValue="json") final QueryOutputFormat outputFormat,

			@ApiParam(value="Only valid when outputFormat is \"csv\".  This will include the column headers in the output.", required=false, defaultValue="false", allowMultiple=false)
			@RequestParam(name=ResourceUris.showColHeadersQP, defaultValue="false") final boolean showColHeaders) throws Exception {

		BasicQuerySearchAlgorithmBuilder builder;

		/**
		 * MPCS-7918 triviski 1/29/2016 - Updated to automatically convert the enums.
		 */
		switch (queryType) {
		case eha:
			builder = EhaQuerySearchAlgorithm
			.createBuilder()
			.setChannelIdWildCards(convertUrlRegexToRegex(channelIdRegexes));
			break;
		case evr:
			/**
			 * MPCS-8025 triviski 2/29/2016 - Add the evr ids.  Expected to be a csv list
			 * of single entries and / or ranges.
			 */
			builder = EvrQuerySearchAlgorithm
			.createBuilder()
			.setEvrIds(ConvertUtils.convertRange(evrIds))
			.setEvrLevelWildCards(convertUrlRegexToRegex(evrLevelRegexes))
			.setNameWildCards(convertUrlRegexToRegex(evrNameRegexes))
			.setMessageWildCards(convertUrlRegexToRegex(messageRegexes));
			break;
		case alarm:
			/**
			 * MPCS-11770 - enable an all-channels-in-alarm query
			 */
			builder = EhaQuerySearchAlgorithm
					.createBuilder()
					.setAlarmQuery(true);

			if (channelIdRegexes != null) {
				((EhaQuerySearchAlgorithm.EhaSearchAlgorithmBuilder) builder)
						.setChannelIdWildCards(convertUrlRegexToRegex(channelIdRegexes));
			}

			break;
		default:
			throw new Exception("Bogus query type");
		}

		/**
		 * Checks the validity of the time type string.  If all is good set and convert the data.  No need
		 * to worry about checking the time strings the conversion and set methods handle the null checks
		 * for us.
		 */
		final GlobalLadPrimaryTime type = getTimeType(timeType);

		builder.setTimeType(timeType)
		.setLowerBound(convertTimeString(lowerBoundTimeString, type, scid))
		.setUpperBound(convertTimeString(upperBoundTimeString, type, scid));

		/*
		 * MPCS-8069 triviski 3/29/2016 - Scid was never added to the builder, so adding it.
		 */
		builder.setSessionNumbers(ConvertUtils.convertRange(sessionIds))
		.setHostWildCards(convertUrlRegexToRegex(hostRegexes))
		.setVenueWildCards(convertUrlRegexToRegex(venueRegexes))
		.setDssIds(ConvertUtils.convertRangeBytes(dssIds))
		.setVcids(ConvertUtils.convertRangeBytes(vcids))
		.setScid(scid)
		.setUserDataTypes(GlobalLadUserDatatypeConverter.lookupUserDataTypes(queryType, source, recordedState));

		if (verified) {
			final Map<DeltaQueryStatus, Map<Object, Collection<IGlobalLADData>>> resultMap = 
					new HashMap<DeltaQueryStatus, Map<Object,Collection<IGlobalLADData>>>();

			glad.getMasterContainer().deltaQuery(builder.build(), resultMap);

			return binaryResponse ? 
					createDeltaBinaryQueryStreamingResponse(resultMap) :
					createDeltaQueryStreamingResponse(resultMap, queryType, outputFormat, showColHeaders);
		} else {
			final Map<Object, Collection<IGlobalLADData>> results = glad.get(builder.build(), maxResults);

			return binaryResponse ? 
					createBinaryQueryStreamingResponse(results) :
					createQueryStreamingResponse(results, type, queryType, outputFormat, showColHeaders);

		}
	}

	/**
	 * Creates a streaming output response object to be returned by a REST query.  This knows how to serialize the 
	 * output for a delta query.
	 * 
	 * @param resultMap - The map that will be used to add the matching data.
	 * @return Response with the requested data as an octet stream.
	 * @throws Exception
	 */
	public StreamingResponseBody createDeltaQueryStreamingResponse(final Map<DeltaQueryStatus, Map<Object, Collection<IGlobalLADData>>> resultMap,
			final QueryType queryType, final QueryOutputFormat outputFormat, final boolean showColumnHeaders) throws Exception {
		final StreamingResponseBody stream = new StreamingResponseBody() {

			@Override
			public void writeTo(final OutputStream output) throws IOException {
				/**
				 * Using a switch so it will be easier to expand in the future.
				 */
				switch(outputFormat) {
				case csv:
					try {
						writeCsv(output);
					} catch (final GlobalLadException e) {
						throw new WebApplicationException(e);
					}
					break;
				case json:
				default:
					writeJson(output);
					break;
				}
			}

			private void writeCsv(final OutputStream output) throws IOException, WebApplicationException, GlobalLadException {
				for (final Entry<DeltaQueryStatus, Map<Object, Collection<IGlobalLADData>>> entry : resultMap.entrySet()) {
					for (final Collection<IGlobalLADData> results : entry.getValue().values()) {
						doCsvOutput(queryType, true, entry.getKey(), showColumnHeaders, output, results);
					}
				}
			}

			private void writeJson(final OutputStream output) throws IOException, WebApplicationException {
				/**
				 * MPCS-7918 - triviski 1/30/2016 - Adding support for csv output.  If csv do not need to do any 
				 * of the fancy generation stuff, just get the csv mapper and do your thing....
				 */
				final JsonGenerator generator = JsonUtilities.getRequestMapper()
						.getFactory()
						.createGenerator(output);

				generator.useDefaultPrettyPrinter();

				// The start of the object.  This will section the outputs based on the query status.
				generator.writeStartObject();

				for(final Entry<DeltaQueryStatus, Map<Object, Collection<IGlobalLADData>>> statusEntry : resultMap.entrySet()) {
					// Start of the status entry object. 
					generator.writeObjectFieldStart(statusEntry.getKey().toString()); 

					for (final Entry<Object, Collection<IGlobalLADData>> entry : statusEntry.getValue().entrySet()) {
						// Start of the collections entry which would be keyed as the identifier, ie channel id or evr level.
						generator.writeArrayFieldStart(entry.getKey().toString());

						for (final Object pojo : entry.getValue()) {
							generator.writeObject(pojo);
						}

						// end of the data collection.
						generator.writeEndArray();
					}

					// End of the status entry object.
					generator.writeEndObject(); 
				}

				generator.writeEndObject();
				generator.flush();
				generator.close();
			}

		};

		return stream;
	}


	/**
	 * Takes the given collection and will stream the output.  This is for a normal
	 * REST query set of data.
	 * 
	 * @param queryType Required for csv output, ignored for json.
	 * @param outputFormat defines the type of output.
	 * @param showColumnHeaders if the output format is csv, includes the column headers.
	 * @param results - Map of matching data keyed by the identifier.
	 * @return Response with the requested data as an octet stream.
	 * @throws Exception
	 * @param timeType used for csv output to pick the sort order of the flattened map.
	 * @return
	 * @throws Exception
	 */
	public StreamingResponseBody createQueryStreamingResponse(final Map<Object, Collection<IGlobalLADData>> results,
			final GlobalLadPrimaryTime timeType,
			final QueryType queryType, final QueryOutputFormat outputFormat, final boolean showColumnHeaders) throws Exception {
		final StreamingResponseBody stream = new StreamingResponseBody() {

			@Override
			public void writeTo(final OutputStream output) throws IOException {
				/**
				 * Using a switch so it will be easier to expand in the future.
				 */
				switch(outputFormat) {
				case csv:
					try {
						writeCsv(output);
					} catch (final GlobalLadException e) {
						throw new WebApplicationException(e);
					}
					break;
				case lm_csv:
					/**
					 * MPCS-8513 triviski 10/7/2016 - Created a special csv output for use with chill_check_channel.  This 
					 * has a separate property to set the columns, and will only quote string is it is required due to special characters.
					 */
					// Special case used for chill check channel mainly.
					try {
						doLmCsvOutput(output, GlobalLadUtilities.flattenMap(timeType, results, false));
					} catch (final GlobalLadException e) {
						throw new WebApplicationException(e);
					}
					break;
				case json:
				default:
					writeJson(output);
					break;
				}
			} 

			/**
			 * Used for csv output
			 * @param output stream to write the data to.
			 * @throws IOException 
			 * @throws GlobalLadException 
			 * @throws JsonMappingException 
			 * @throws JsonGenerationException 
			 */
			private void writeCsv(final OutputStream output) throws JsonGenerationException, JsonMappingException, GlobalLadException, IOException {
				/**
				 * MPCS-8042 triviski 5/4/2016 - The order was not correct so changing the input map to 
				 * be an already sorted collection.  Only need to output the single collection now.
				 */
				doCsvOutput(queryType, false, null, showColumnHeaders, output, GlobalLadUtilities.flattenMap(timeType, results, false));
			}

			/**
			 * Used for json output format.
			 * 
			 * @param output
			 * @throws IOException
			 */
			private void writeJson(final OutputStream output) throws IOException {
				final JsonGenerator generator = JsonUtilities.getRequestMapper()
						.getFactory()
						.createGenerator(output);
				generator.useDefaultPrettyPrinter();

				generator.writeStartObject();

				/**
				 * The map is an identifier ie evr level or channel id with a list of results.
				 */
				for (final Entry<Object, Collection<IGlobalLADData>> entry : results.entrySet()) {
					generator.writeArrayFieldStart(entry.getKey().toString());

					for (final Object pojo : entry.getValue()) {
						generator.writeObject(pojo);
					}

					generator.writeEndArray();
				}

				generator.writeEndObject();

				generator.flush();
				generator.close();
			}
		};

		return stream;
	}

	/**
	 * Creates a binary stream of data described below.
	 * <br>
	 * [<br>
	 * verified message count - 4 bytes unverified message count - 4 bytes | unknown message count - 4 bytes | 
	 *  verified message data   |
	 *  unverified message data | 
	 *  unknown message data
	 * </br>] 
	 * 
	 * @param resultMap
	 * @return Response with the requested data as an octet stream in the entity body.
	 * @throws Exception
	 */
	public StreamingResponseBody createDeltaBinaryQueryStreamingResponse(final Map<DeltaQueryStatus, Map<Object, Collection<IGlobalLADData>>> resultMap) throws Exception {
		final StreamingResponseBody stream = new StreamingResponseBody() {

			/** 
			 * Writes the message count for the given status type.
			 * 
			 * @param status
			 * @param output
			 * 
			 * @throws IOException 
			 */
			private void getCount(final DeltaQueryStatus status, final OutputStream output) throws IOException {
				final Map<Object, Collection<IGlobalLADData>> dataMap = resultMap.containsKey(status) ?
						resultMap.get(status) :
							Collections.<Object, Collection<IGlobalLADData>>emptyMap();

						/**
						 * Must get the counts first.  This is a little lame because we need to loop through 
						 * the collections level of the map twice, but we need to get the count of what 
						 * is being written before any data can be written.
						 */
						int dataCount = 0;

						for (final Collection<IGlobalLADData> dataSet : dataMap.values()) {
							dataCount += dataSet.size();
						}

						/**
						 * Write all the bytes of the integer.
						 */
						final byte[] dataBytes = new byte[4];
						GDR.set_i32(dataBytes, 0, dataCount);
						output.write(dataBytes);
			}

			/**
			 * Writes the data from the map to output.
			 * @param status
			 * @param output
			 * @throws IOException
			 * @throws GlobalLadDataException
			 */
			private void writeData(final DeltaQueryStatus status, final OutputStream output) throws IOException, GlobalLadDataException {
				/**
				 * First get the data map for the given status type.
				 */
				final Map<Object, Collection<IGlobalLADData>> dataMap = resultMap.containsKey(status) ?
						resultMap.get(status) :
							Collections.<Object, Collection<IGlobalLADData>>emptyMap();

						final DataOutputStream dos = new DataOutputStream(output);

						/**
						 * Write the data to the output.
						 */
						for (final Collection<IGlobalLADData> dataSet : dataMap.values()) {
							for (final IGlobalLADData data : dataSet) {
								dos.write(data.toPacketByteArray());
							}
						}
			}

			@Override
			public void writeTo(final OutputStream output) throws IOException {
				try {
					/**
					 * First write the counts for each of the status types.
					 */
					this.getCount(DeltaQueryStatus.complete, output);
					this.getCount(DeltaQueryStatus.incomplete, output);
					this.getCount(DeltaQueryStatus.unknown, output);

					/**
					 * Next write out the data.
					 */
					this.writeData(DeltaQueryStatus.complete, output);
					this.writeData(DeltaQueryStatus.incomplete, output);
					this.writeData(DeltaQueryStatus.unknown, output);
				} catch (final GlobalLadDataException e) {
					throw new WebApplicationException(e);
				}
			}
		};

		return stream;
	}


	/**
	 * Converts the data to binary words and streams to the output.  
	 * 
	 * @param results
	 * @return Response with the requested data as an octet stream of the entity body.
	 * @throws Exception
	 */
	public StreamingResponseBody createBinaryQueryStreamingResponse(final Map<Object, Collection<IGlobalLADData>> results) throws Exception {
		final StreamingResponseBody stream = new StreamingResponseBody() {
			
			@Override
			public void writeTo(final OutputStream output) throws IOException {
				final DataOutputStream dos = new DataOutputStream(output);
				
				for (final Collection<IGlobalLADData> data : results.values()) {
					for (final IGlobalLADData d : data) {
						dos.write(d.toPacketByteArray());
					}
				}
			}
		};

		return stream;
	}

	/**
	 * Converts result to the special csv for output to chill_check_channel.
	 * MPCS-8513 triviski 10/7/2016 - Uses the special mapper and schema and writes out the results.
	 * @param out
	 * @param result
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonGenerationException
	 * @throws GlobalLadException
	 */
	private void doLmCsvOutput(final OutputStream out, final Collection<IGlobalLADData> results) throws JsonGenerationException, JsonMappingException, IOException, GlobalLadException {
		final CsvMapper mapper = JsonUtilities.createLMCsvMapper();
		final CsvSchema schema = JsonUtilities.createLMCsvSchema();

		/**
		 * Don't close the output stream, let the user handle it.
		 */
		mapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

		/**
		 * This may be a memory issue if we have millions? (dont know the number) of records in the list and we ask jackson to handle
		 * this.  If this turns out to be an issue in the future we should probably work on changing the query interface to give us the
		 * data iterator that works on the byte stream passed from the query server.  For now just going to let jackson handle this.
		 *
		 * triviski - 10/2015
		 */
		mapper.writer(schema).writeValue(out, results);
	}

	/**
	 * Converts results to csv and writes them to out.
	 * 
	 * @param qtype
	 * @param includeVerifiedColumn If true will include the verified column and set the value based on isVerified.
	 * @param verifiedStatus used to get the proper verified mapper.   Only matters if includeVerifiedColumn is true.
	 * @param out
	 * @param results
	 * @throws GlobalLadException
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	private void doCsvOutput(final QueryType qtype, final boolean includeVerifiedColumn, final DeltaQueryStatus verifiedStatus, boolean showColHeaders, final OutputStream out, final Collection<IGlobalLADData> results) throws GlobalLadException, JsonGenerationException, JsonMappingException, IOException {
		CsvMapper mapper;

		if (includeVerifiedColumn) {
			switch(verifiedStatus) {
			case complete:
				mapper = JsonUtilities.createCsvObjectMapperVerified();
				break;
			case incomplete:
				mapper = JsonUtilities.createCsvObjectMapperUnverified();
				break;
			case unknown:
			default:
				mapper = JsonUtilities.createCsvObjectMapperUnknown();
				break;
			}
		} else {
			mapper = JsonUtilities.createCsvObjectMapper();
		}

		CsvSchema schema;

		switch (qtype) {
		case evr:
			schema = JsonUtilities.createEvrCsvSchema(includeVerifiedColumn);
			break;
		case eha:
		case alarm:
		default:
			schema = JsonUtilities.createEhaCsvSchema(includeVerifiedColumn);
			break;
		}

		/**
		 * Don't close the output stream, let the user handle it.
		 */
		mapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

		/**
		 * This may be a memory issue if we have millions? (dont know the number) of records in the list and we ask jackson to handle 
		 * this.  If this turns out to be an issue in the future we should probably work on changing the query interface to give us the
		 * data iterator that works on the byte stream passed from the query server.  For now just going to let jackson handle this.
		 * 
		 * triviski - 10/2015
		 */
		if (showColHeaders) {
			schema = schema.withHeader();
			showColHeaders = false;
		}

		mapper.writer(schema).writeValue(out, results);
	}
}
