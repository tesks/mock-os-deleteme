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

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.GlobalLadUserDatatypeConverter;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.DataSource;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.QueryType;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.RecordedState;
import jpl.gds.globallad.rest.RestUtils;
import jpl.gds.globallad.rest.resources.ResourceUris;
import jpl.gds.shared.log.Tracer;

/**
 * Global lad REST resource class that is used to set and retrieve the user data type data depth.  This is the max number of 
 * samples for the ring buffers to hold before trimming old data.
 */
@RestController
@Scope("request")
@Api
public class DepthResource extends AbstractGlobalLadMVCResource {

	@Autowired
	GlobalLadProperties config;
	
	/**
	 * @param depth new depth
	 * @param udts If null sets the default data type deal.
	 * @return Response calling get depth with the input user data types as the entity body.
	 * 
	 * @throws IOException 
	 * @throws JsonProcessingException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	private ResponseEntity<Object> setDataDepth(int depth, Collection<Byte> udts) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException {
		Tracer tracer = GlobalLadProperties.getTracer();

		if (udts == null || udts.isEmpty()) {
			config.setDefaultDepth(depth);
			tracer.info("Updated the default data depth to " , depth);
		} else {
			// For specific type.
			for (byte udt : udts) {
				config.setDataDepth(udt, depth);
			}
			
			tracer.info("Updated the data depth for user data types " , udts , " to " , depth);
		}
		
		return getDepth(udts);
	}
	
	/**
	 * If udts is empty returns default depth.  If udts has values returns the types for those udts.  If 
	 * udts is null returns everything.
	 * 
	 * @param udts
	 * @return Response user data type depths requested as the entity body.
	 * @throws IOException 
	 * @throws JsonProcessingException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	private ResponseEntity<Object> getDepth(Collection<Byte> udts) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		
		if (udts == null) {
				builder.add("defaultDepth", config.getDefaultDataDepth());
		} else if (udts.isEmpty()) {
			// Add any of the user data type specific depths.
			Iterator<Entry<Byte, Integer>> it = config.getDepthSetIterator();
			
			while (it.hasNext()) {
				Entry<Byte, Integer> entry = it.next();
				builder.add(entry.getKey().toString(), entry.getValue());
			}
		} else {
			// Only get the ones requested.
			for (Byte udt : udts) {
				builder.add(udt.toString(), config.getDataDepth(udt));
			}
		}
		
		return ok(RestUtils.jsonObjToPrettyString(builder));
	}

	/**
	 * Gets the default depth.
	 * @return Response with the default depth as the entity body.
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	@GetMapping(path="depth", produces=MediaType.APPLICATION_JSON)
	@ApiOperation(value="Get the default depth, which is the depth for all user data types that do not have a specific depth configured.",
		notes="Example output: {\"defaultDepth\" : 100}"
	)
	public ResponseEntity<Object> getDefaultDepth() throws JsonParseException, JsonMappingException, JsonProcessingException, IOException  {
		return getDepth(null);
	}
	
	/**
	 * Sets the default depth.
	 * @param depth new depth
	 * @return Response with the new default depth as the entity body.
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	@PutMapping(path="depth", produces=MediaType.APPLICATION_JSON, consumes=MediaType.TEXT_PLAIN)
	@ApiOperation(value="Set the default depth.", 
		notes="Returns the default depth after setting.  Example output:  {\"defaultDepth\" : 100}")
	public ResponseEntity<Object> setDefaultDepth(
			@ApiParam(value="The new depth to set", required=true, allowableValues="range[1,infinity]")
			@RequestBody String depth) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException  {
		return setDataDepth(Integer.parseInt(depth), null);
	}
	
	/**
	 * Set the data depth for the queryType.
	 * 
	 * @param queryType
	 * @param depth new depth
	 * @return Response with the new depth for the user data types mapped to queryType as the entity body.
	 * @throws IOException 
	 * @throws JsonProcessingException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	@PutMapping(path=ResourceUris.allDepthURI, produces=MediaType.APPLICATION_JSON, consumes=MediaType.TEXT_PLAIN)
	@ApiOperation(value="Set the depth for all user data types associated with queryType.", 
		notes="Returns the new depth for all user data associated with ueryType.  Example output with query type \"eha\" and depth set to 20:  { \"0\" : 20, \"1\" : 20, \"2\" : 20, \"3\" : 20, \"4\" : 20}"
	)
	public ResponseEntity<Object> setDepth(
			@ApiParam(value="The query type", allowableValues=ResourceUris.queryTypeSwagger, required=true) 
			@PathVariable(ResourceUris.queryTypeQP) String queryType, 
			@ApiParam(value="The new depth to set", required=true, allowableValues="range[1,infinity]")
			@RequestBody String depth) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException {
		return setDataDepth(Integer.parseInt(depth),
				GlobalLadUserDatatypeConverter.lookupUserDataTypes(
						QueryType.valueOf(queryType),
						DataSource.all, 
						RecordedState.both));
	}
	
	/**
	 * Get the data depth for the queryType.
	 * 
	 * @param queryType
	 * @return Response with the depth for the user data types mapped to queryType as the entity body.
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	@GetMapping(path=ResourceUris.allDepthURI, produces=MediaType.APPLICATION_JSON)
	@ApiOperation(value="Gets the depth for all user data types within queryType.", 
		notes="Returns the depth for all user data types within queryType.  Example output with query type \"eha\":  { \"0\" : 20, \"1\" : 20, \"2\" : 20, \"3\" : 20, \"4\" : 20}"
	)
	public ResponseEntity<Object> getDepthQueryType(
			@ApiParam(value="The query type", allowableValues=ResourceUris.queryTypeSwagger, required=true) 
			@PathVariable(ResourceUris.queryTypeQP) String queryType) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException {
		return getDepth(GlobalLadUserDatatypeConverter.lookupUserDataTypes(
					QueryType.valueOf(queryType),
					DataSource.all, 
					RecordedState.both));
	}

	/**
	 * Sets the data depth for the user data types that are mapped to the queryType, source and recordedState.
	 * 
	 * @param queryType
	 * @param source
	 * @param recordedState
	 * @param depth new depth
	 * @return Response with the new depth for the user data types mapped to queryType, source and recordedState as the entity body.
	 * @throws IOException 
	 * @throws JsonProcessingException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	@PutMapping(path=ResourceUris.specificDepthURI, produces=MediaType.APPLICATION_JSON, consumes=MediaType.TEXT_PLAIN)
	@ApiOperation(value="Sets the depth for all user data types associated with queryType, source and recordedState. the depth for all user data types within queryType.", 
		notes="Returns the new depth for all user data types within queryType.  Example output with query type \"eha\", source \"fsw\" and source \"both\":  { \"0\" : 20, \"1\" : 20}"
	)
	public ResponseEntity<Object> setSpecificDepth(
			@ApiParam(value="The query type", allowableValues=ResourceUris.queryTypeSwagger, required=true) 
			@PathVariable(ResourceUris.queryTypeQP) String queryType,
			@ApiParam(value="The data source", allowableValues=ResourceUris.sourceSwagger, required=true) 
			@PathVariable(ResourceUris.sourceQP) String source, 
			@ApiParam(value="The recorded state", allowableValues=ResourceUris.recordedStatSwagger, required=true) 
			@PathVariable(ResourceUris.recordedStateQP) String recordedState, 
			@ApiParam(value="The new depth to set", required=true, allowableValues="range[1,infinity]")
			@RequestBody String depth
			) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException {
		return setDataDepth(Integer.parseInt(depth),
				GlobalLadUserDatatypeConverter.lookupUserDataTypes(queryType, source, recordedState));
	}
	
	/**
	 * Gets the data depth for the user data types that are mapped to the queryType, source and recordedState.
	 * @param queryType
	 * @param source
	 * @param recordedState
	 * @return Response with the depth for the user data types mapped to queryType, source and recordedState as the entity body.
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	@GetMapping(path=ResourceUris.specificDepthURI, produces=MediaType.APPLICATION_JSON)
	@ApiOperation(value="Gets the depth for all user data types associated with queryType, source and recordedState.", 
		notes="Returns the depth for all user data types within queryType.  Example output with query type \"eha\", source \"fsw\" and source \"both\":  { \"0\" : 20, \"1\" : 20}"
	)
	public ResponseEntity<Object> getSpecificDepth(
			@ApiParam(value="The query type", allowableValues=ResourceUris.queryTypeSwagger, required=true) 
			@PathVariable(ResourceUris.queryTypeQP) String queryType,
			@ApiParam(value="The data source", allowableValues=ResourceUris.sourceSwagger, required=true) 
			@PathVariable(ResourceUris.sourceQP) String source, 
			@ApiParam(value="The recorded state", allowableValues=ResourceUris.recordedStatSwagger, required=true) 
			@PathVariable(ResourceUris.recordedStateQP) String recordedState
			) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException {
		return getDepth(GlobalLadUserDatatypeConverter.lookupUserDataTypes(queryType, source, recordedState));
	}
}
