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
package jpl.gds.globallad.spring.mvc.resources;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jpl.gds.globallad.data.AlarmHistoryGlobalLadData;
import jpl.gds.globallad.data.GlobalLadUserDatatypeConverter;
import jpl.gds.globallad.data.container.GlobalLadContainerFactory;
import jpl.gds.globallad.data.container.search.query.BasicQuerySearchAlgorithm;
import jpl.gds.globallad.data.container.search.query.BasicQuerySearchAlgorithm.BasicQuerySearchAlgorithmBuilder;
import jpl.gds.globallad.data.container.search.query.EhaQuerySearchAlgorithm;
import jpl.gds.globallad.data.container.search.query.EvrQuerySearchAlgorithm;
import jpl.gds.globallad.data.storage.DataInsertionManager;
import jpl.gds.globallad.data.utilities.ConvertUtils;
import jpl.gds.globallad.io.socket.GlobalLadSocketServer;
import jpl.gds.globallad.io.IGlobalLadDataSource;
import jpl.gds.globallad.message.handler.IGlobalLad;
import jpl.gds.globallad.rest.RestUtils;
import jpl.gds.globallad.rest.resources.ResourceUris;
import jpl.gds.globallad.spring.beans.GlobalLadDataSourceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

/**
 * REST resource to get LAD statistics.
 */
@RestController
@Scope("request")
//@Api(tags={"metadata", "stats"})
@Api
public class LadMetadataAndControlResource extends AbstractGlobalLadMVCResource {

	@Autowired
	private IGlobalLad glad;
	
	@Autowired
	private GlobalLadDataSourceProvider dataSourceProvider;

	@Autowired
	private DataInsertionManager manager;
	
	/**
	 * Queries the lad to get either the metadata or the summary.  
	 * 
	 * @param queryType
	 * @param source
	 * @param recordedState
	 * @param channelId
	 * @param evrLevel
	 * @param evrName
	 * @param sessionId
	 * @param scid
	 * @param host
	 * @param venue
	 * @param dssId
	 * @param vcid
	 * @param isSummary
	 * @return Response with the metadata of the matching containers set as the entity body.
	 * 
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 * @throws IOException
	 * @throws Exception
	 */
	private ResponseEntity<Object> getMetadata(
			String queryType,
			String source, 
			String recordedState,
			String channelId,
			String evrLevel,
			String evrName,
			Long sessionId,
			Integer scid,
			String host,
			String venue,
			Byte dssId,
			Byte vcid,
			boolean isSummary
			) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException, Exception {
		BasicQuerySearchAlgorithmBuilder builder;
		
		if (queryType == null) {
			/**
			 * Get all types.
			 */
			builder = BasicQuerySearchAlgorithm.createBuilder();
		} else if (queryType.equals("evr")) {
			builder = EvrQuerySearchAlgorithm
					.createBuilder()
					.setName(evrName)
					.setEvrLevel(evrLevel);
		} else if (queryType.equals("eha")) {
			builder = EhaQuerySearchAlgorithm
					.createBuilder()
					.setChannelId(channelId);
		} else if (queryType.equals("alarm")) {
			/**
			 * Filter on the user data type of the alarm history.
			 */
			builder = BasicQuerySearchAlgorithm
					.createBuilder()
					.setUserDataType(AlarmHistoryGlobalLadData.ALARM_HISTORY_GLAD_USER_DATA_TYPE)
					;
		} else {
			throw new Exception("Bogus query type");
		}

		/**
		 * Set all the general values.
		 */
		builder
			.setMatchOnNullOrEmpty(false) // We do not want to match on null because we want to stop in the middle of the tree at times.
			.setHost(host)
			.setVenue(venue)
			.setSessionNumber(sessionId)
			.setVcid(vcid)
			.setScid(scid)
			.setDssId(dssId)
			.setTimeType("ert");

		/**
		 * Check the combination of the query type, source and recorded state to see if 
		 * this is a filter on user data type.  
		 */
		if (queryType != null && source != null && recordedState != null) {
			builder.setUserDataTypes(GlobalLadUserDatatypeConverter
					.lookupUserDataTypes(queryType, source, recordedState));
		}
		
		JsonObject statsOrSummary = isSummary ?
				glad.getSummary(builder.build()) :
				glad.getMetadata(builder.build());
		
		return ok(RestUtils.jsonObjToPrettyString(statsOrSummary));
	}

	/**
	 * Base method to get either the summary or metadata for containers matching the input parameters.  
	 * 
	 * Metadata example:
	 * 
	 * 
<pre>
{
  "containerIdentifier" : "master",
  "containerType" : "master",
  "numInserts" : 178441,
  "insertsPerSecond" : 64901,
  "avgInsertMS" : 0.015408,
  "insertTimeNS" : 2749543889,
  "insertGetTimeNS" : 96760708,
  "numInsertGets" : 178431,
  "insertGetTimePercent" : 3.519154881909943,
  "queryGetTimeNS" : 0,
  "numQueryGets" : 0,
  "avgQueryGetTimeMS" : 0.0,
  "childCount" : 1,
  "scetDataCount" : 158116,
  "ertDataCount" : 158116,
  "eventDataCount" : 158116
}
</pre>
	 * <p>  
	 *  Summary example:
<pre>
{
  "containerType" : "master",
  "containerIdentifier" : "master",
  "children" : [ {
    "containerType" : "host",
    "containerIdentifier" : "LMC-047512"
  } ]
}
</pre>
	 *  
	 * @param queryType
	 * @param source
	 * @param recordedState
	 * @param channelId
	 * @param evrLevel
	 * @param evrName
	 * @param sessionId
	 * @param host
	 * @param venue
	 * @param dssId
	 * @param vcid
	 * @param isSummary
	 * @return Response with the metadata or summary of the matching containers set as the entity body.
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 * @throws IOException
	 * @throws Exception
	 */
	@GetMapping(path=ResourceUris.slash, produces=MediaType.APPLICATION_JSON)
	@ApiOperation(value="Returns metadata or summary of the matching containers", 
		notes="If isSummary is false returns a JSON object with basic statistics about the"
				+ " matching container.  If isSummary is true returns a JSON object with "
				+ "information regarding the matching containers children")
	public ResponseEntity<Object> getStatsBase(
			@ApiParam(value="The query type", allowableValues=ResourceUris.queryTypeSwagger, required=false) 
			@RequestParam(name=ResourceUris.queryTypeQP, required=false) String queryType,

			@ApiParam(value="The data source", allowableValues=ResourceUris.sourceSwagger, required=false) 
			@RequestParam(name=ResourceUris.sourceQP, required=false) String source, 

			@ApiParam(value="The recorded state", allowableValues=ResourceUris.recordedStatSwagger, required=false) 
			@RequestParam(name=ResourceUris.recordedStateQP, required=false) String recordedState,

			@ApiParam(value="Channel ID filter", required=false, defaultValue="", allowMultiple=false, example="A-1234 or A-12*")
			@RequestParam(name=ResourceUris.channelIdQP, required=false) String channelId,

			@ApiParam(value="EVR Level filter", required=false, defaultValue="", allowMultiple=false, example="WARNING_HI")
			@RequestParam(name=ResourceUris.evrLevelQP, required=false) String evrLevel,

			@ApiParam(value="Evr Name filter", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.evrNameQP, required=false) String evrName,

			@ApiParam(value="Session Identifier", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.sessionIdQP, required=false) Long sessionId,

			@ApiParam(value="Spacecraft Identifier", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.scidQP, required=false) Integer scid,

			@ApiParam(value="Session Host", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.hostQP, required=false) String host,

			@ApiParam(value="Session Venue", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.venueQP, required=false) String venue, 

			@ApiParam(value="Session DSS ID.", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.dssIdQP, required=false) Byte dssId, 

			@ApiParam(value="Virtual Channel Identifier.", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.vcidQP, required=false) Byte vcid, 

			@ApiParam(value="Output Type", required=false, defaultValue="false", allowMultiple=false)
			@RequestParam(name="isSummary", required=false, defaultValue="false") boolean isSummary
			) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException, Exception {
		return getMetadata(queryType, source, recordedState, 
				channelId, evrLevel, evrName, sessionId, scid, host, venue, dssId, vcid, isSummary);
	}
	
	/**
	 * Gets either the metadata or summary of the containers that match the input.
	 * Metadata example:
	 * 
	 * 
<pre>
{
  "containerIdentifier" : "master",
  "containerType" : "master",
  "numInserts" : 178441,
  "insertsPerSecond" : 64901,
  "avgInsertMS" : 0.015408,
  "insertTimeNS" : 2749543889,
  "insertGetTimeNS" : 96760708,
  "numInsertGets" : 178431,
  "insertGetTimePercent" : 3.519154881909943,
  "queryGetTimeNS" : 0,
  "numQueryGets" : 0,
  "avgQueryGetTimeMS" : 0.0,
  "childCount" : 1,
  "scetDataCount" : 158116,
  "ertDataCount" : 158116,
  "eventDataCount" : 158116
}
</pre>
	 * <p>  
	 *  Summary example:
<pre>
{
  "containerType" : "master",
  "containerIdentifier" : "master",
  "children" : [ {
    "containerType" : "host",
    "containerIdentifier" : "LMC-047512"
  } ]
}
</pre>

	 * 
	 * @param queryType
	 * @param source
	 * @param recordedState
	 * @param channelId
	 * @param evrLevel
	 * @param evrName
	 * @param sessionId
	 * @param scid
	 * @param host
	 * @param venue
	 * @param dssId
	 * @param vcid
	 * @param isSummary
	 * @return Response with the metadata of the matching containers set as the entity body.
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 * @throws IOException
	 * @throws Exception
	 */
	@GetMapping(path=ResourceUris.statsURI, produces=MediaType.APPLICATION_JSON)
	@ApiOperation(value="Returns metadata or summary of the matching containers", 
		notes="If isSummary is false returns a JSON object with basic statistics "
				+ "about the matching container.  If isSummary is true returns a JSON "
				+ "object with information regarding the matching containers children")
	public ResponseEntity<Object> getStats(
			@ApiParam(value="The query type", allowableValues=ResourceUris.queryTypeSwagger, required=true) 
			@PathVariable(ResourceUris.queryTypeQP) String queryType,

			@ApiParam(value="The data source", allowableValues=ResourceUris.sourceSwagger, required=true) 
			@PathVariable(ResourceUris.sourceQP) String source, 

			@ApiParam(value="The recorded state", allowableValues=ResourceUris.recordedStatSwagger, required=true) 
			@PathVariable(ResourceUris.recordedStateQP) String recordedState,

			@ApiParam(value="Channel ID filter", required=false, defaultValue="", allowMultiple=false, example="A-1234 or A-12*")
			@RequestParam(name=ResourceUris.channelIdQP, required=false) String channelId,

			@ApiParam(value="EVR Level filter", required=false, defaultValue="", allowMultiple=false, example="WARNING_HI")
			@RequestParam(name=ResourceUris.evrLevelQP, required=false) String evrLevel,

			@ApiParam(value="Evr Name filter", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.evrNameQP, required=false) String evrName,

			@ApiParam(value="Session Identifier", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.sessionIdQP, required=false) Long sessionId,

			@ApiParam(value="Spacecraft Identifier", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.scidQP, required=false) Integer scid,

			@ApiParam(value="Session Host", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.hostQP, required=false) String host,

			@ApiParam(value="Session Venue", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.venueQP, required=false) String venue, 

			@ApiParam(value="Session DSS ID.", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.dssIdQP, required=false) Byte dssId, 

			@ApiParam(value="Virtual Channel Identifier.", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.vcidQP, required=false) Byte vcid, 

			@ApiParam(value="Output Type", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name="isSummary", required=false) boolean isSummary

			) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException, Exception {
		return getMetadata(queryType, source, recordedState, 
				channelId, evrLevel, evrName, sessionId, scid, host, venue, dssId, vcid, isSummary);
	}
	
	/**
	 * Gets statistics on the socket server and all input clients.
	 * 
	 * @return Response with the metadata of the socket server as the entity body.
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 * @throws IOException
	 * @throws Exception
	 */
	@GetMapping(path=ResourceUris.serverStatsURI, produces=MediaType.APPLICATION_JSON)
	@ApiOperation(value="Get statistics about the data source. In socket server mode, open and closed client information is included. " +
			"In JMS mode, message information is included.")
	public ResponseEntity<Object> getServerStats() throws JsonParseException, JsonMappingException, JsonProcessingException, IOException, Exception {
		IGlobalLadDataSource dataSource = dataSourceProvider.getDataSource();
		JsonObjectBuilder builder = Json.createObjectBuilder()
				.add(dataSource.getJsonId(), dataSource.getStats());
		
		/**
		 * MPCS-8031 3/3/2016 triviski - Merge the stats object for the manager
		 * into the builder because the dashboard is expecting all of those
		 * values to be at the root of the JSON.
		 */
		for (Entry<String, JsonValue> es : manager.getStats().entrySet()) {
			builder.add(es.getKey(), es.getValue());
		}

		return ok(RestUtils.jsonObjToPrettyString(builder.build()));
	}
	
	/**
	 * Tells the socket server to drop all clients.
	 * 
	 * @return Empty response.
	 * @throws IOException
	 * @throws Exception
	 */
	@DeleteMapping(path=ResourceUris.serverURI)
	@ApiOperation(value="Drop all active clients. Only applies when operating in socket server mode.")
	public ResponseEntity<Object> dropAllClients() throws IOException, Exception {
		IGlobalLadDataSource dataSource = dataSourceProvider.getDataSource();
		if (dataSource instanceof GlobalLadSocketServer) {
			((GlobalLadSocketServer)dataSource).dropAllClients();
		}
		return emptyResponse(HttpStatus.NO_CONTENT);
	}
	
	
	/**
	 * Clears the global lad.  
	 * 
	 * @return Response with the status code set.
	 * 
	 * @throws Exception - If there are clients connected to the global lad.
	 */
	@DeleteMapping(path=ResourceUris.slash)
	@ApiOperation(value="Deletes all data from the global lad data store.")
	public ResponseEntity<Object> clearLad() throws Exception {
		glad.clearGlad();
		return ok();
	}
	
	/**
	 * Uses the input values to match containers and delete all matched.
	 * 
	 * @param queryType
	 * @param source
	 * @param recordedState
	 * @param channelIds
	 * @param evrLevels
	 * @param evrNames
	 * @param messageRegex
	 * @param sessionIds
	 * @param scids
	 * @param hosts
	 * @param venues
	 * @return Empty response.
	 * @throws Exception
	 */
	@DeleteMapping(path=ResourceUris.userDataTypeURI)
	@ApiOperation(value="Delete data from the lad that match the input data filters.")
	public ResponseEntity<Object> pruneLad(	
			@ApiParam(value="The query type", allowableValues=ResourceUris.queryTypeSwagger, required=true) 
			@PathVariable(ResourceUris.queryTypeQP) String queryType,
			
			@ApiParam(value="The data source", allowableValues=ResourceUris.sourceSwagger, required=true) 
			@PathVariable(ResourceUris.sourceQP) String source, 
			
			@ApiParam(value="The recorded state", allowableValues=ResourceUris.recordedStatSwagger, required=true) 
			@PathVariable(ResourceUris.recordedStateQP) String recordedState,

			@ApiParam(value="Channel Identifiers", required=false, defaultValue="empty list", allowMultiple=true, example="A-1234 or A-12*")
			@RequestParam(value = ResourceUris.channelIdQP, required = false) List<String> channelIds,

			@ApiParam(value="EVR Levels", required=false, defaultValue="empty list", allowMultiple=true, example="WARNING_HI")
			@RequestParam(value = ResourceUris.evrLevelQP, required = false) List<String> evrLevels,

			@ApiParam(value="Evr Names", required=false, defaultValue="empty list", allowMultiple=true)
			@RequestParam(value = ResourceUris.evrNameQP, required = false) List<String> evrNames,

			@ApiParam(value="Evr Message regular expressions", required=false, defaultValue="empty list", allowMultiple=true)
			@RequestParam(value = ResourceUris.evrMessageQP, required = false) List<String> messageRegex,

			@ApiParam(value="Session Identifiers", required=false, defaultValue="empty list", allowMultiple=true)
			@RequestParam(value = ResourceUris.sessionIdQP, required = false) List<String> sessionIds,

			@ApiParam(value="Spacecraft Identifiers", required=false, defaultValue="empty list", allowMultiple=true)
			@RequestParam(value = ResourceUris.scidQP, required = false) List<String> scids,

			@ApiParam(value="Session Hosts", required=false, defaultValue="empty list", allowMultiple=true)
			@RequestParam(value = ResourceUris.hostQP, required = false) List<String> hosts,

			@ApiParam(value="Session Venues", required=false, defaultValue="empty list", allowMultiple=true)
			@RequestParam(value = ResourceUris.venueQP, required = false) List<String> venues
			) throws Exception {

		BasicQuerySearchAlgorithmBuilder builder;
		
		if (queryType == null) {
			/**
			 * Get all types.
			 */
			builder = BasicQuerySearchAlgorithm.createBuilder();
		} else if (queryType.equals("evr")) {
			builder = EvrQuerySearchAlgorithm
					.createBuilder()
					.setEvrLevelWildCards(evrLevels)
					.setNameWildCards(evrNames)
					.setMessageWildCards(messageRegex);
		} else if (queryType.equals("eha")) {
			builder = EhaQuerySearchAlgorithm
					.createBuilder()
					.setChannelIdWildCards(convertUrlRegexToRegex(channelIds));
		} else {
			throw new Exception("Bogus query type");
		}

		/**
		 * Set all the general values.
		 */
		builder.setSessionNumbers(ConvertUtils.convertRange(sessionIds))
			.setHostWildCards(convertUrlRegexToRegex(hosts))
			.setVenueWildCards(convertUrlRegexToRegex(venues))
			.setUserDataTypes(GlobalLadUserDatatypeConverter.lookupUserDataTypes(queryType, source, recordedState))
			.setScids(ConvertUtils.convertRangeInt(scids));
		
		/**
		 * Remove data from the lad.
		 */
		glad.remove(builder.build());
		
		return emptyResponse(HttpStatus.NO_CONTENT);
	}

	/**
	 * returns a text view of the configured tree structure.  An example would be if we were set to 
	 * store data like "host,scid,venue,sid,udt,identifier" it would return 
	 * 
	 * "host --> scid --> venue --> sid --> udt --> identifier"
	 * 
	 * @return Response with the data level text as the entity body.
	 * @throws JsonProcessingException
	 */
	@GetMapping(path="levels", produces=MediaType.TEXT_PLAIN)
	@ApiOperation(value="Returns a string representation of the configured data levels", 
			notes="Example output: master --> host --> scid --> venue --> sessionNumber --> vcid --> dssId --> userDataType --> identifier")
	public ResponseEntity<Object> showLevels() throws JsonProcessingException {
		StringBuilder builder = new StringBuilder();
		
		String parentType = "master";
		
		do {
			builder.append(parentType);
			parentType = GlobalLadContainerFactory.getChildContainerMap().get(parentType);
			
			if (parentType != null) {
				builder.append(" --> ");
			}
		} while (parentType != null); 
		
		return ok(builder.toString());
	}
}
