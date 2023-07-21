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
package jpl.gds.globallad.rest;

import java.io.IOException;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestUtils {
	private static final ObjectMapper mapper = new ObjectMapper();
	
	/**
	 * @param builder
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public static final String jsonObjToPrettyString (JsonObjectBuilder builder) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException {
		return jsonObjToPrettyString(builder.build());
	}
	
	/**
	 * Conv method to convert a json object to a pretty string.
	 * @param obj
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public static final String jsonObjToPrettyString (JsonObject obj) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException {
		return mapper.writerWithDefaultPrettyPrinter()
				.writeValueAsString(mapper.readValue(obj.toString(), Object.class));
	}

}
