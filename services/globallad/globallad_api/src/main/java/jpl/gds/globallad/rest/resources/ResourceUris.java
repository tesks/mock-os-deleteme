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
package jpl.gds.globallad.rest.resources;

import java.lang.reflect.Field;

import jpl.gds.globallad.GlobalLadProperties;

/**
 * All of the URIs for the global lad rest server are defined and built in this class.  All of the resource annotations
 * use these URIs.
 */
public class ResourceUris {
	public static final String alarmHistoryURI = "alarm/history";

	/**
	 * Constants for swagger annotations.
	 */
	public static final String queryTypeSwagger = "eha, evr";
	public static final String sourceSwagger = "fsw, sse, header, monitor, all";
	public static final String recordedStatSwagger = "realtime, recorded, both";
	public static final String timeTypeSwagger = "ert, scet, event, sclk, lst, all";
	public static final String venueTypeSwagger = "TESTSET, TESTBED, ATLO, CRUISE, SURFACE, OPS";
	
	public static final String queryType = "{queryType:eha|evr|alarm}";
	public static final String source = "{source:fsw|sse|header|monitor|all}";
	public static final String recordedState = "{recordedState:realtime|recorded|both}";
	public static final String timeType = "{timeType:ert|scet|event|sclk|lst|all}";
	
	// These specify the data identifiers.
	public static final String channelId = "channelId/{channelId}";
	public static final String evrLevel = "level/{level}";
	
	public static final String slash = "";
//	public static final String slash = "{slash:[/]}";

	/**
	 * This is the least that must be defined.  This should be used to work with any query type.
	 */
	public static final String userDataTypeURI = queryType + "/" + source + "/" + recordedState;
	
	public static final String allDepthURI = queryType + "/depth";
	public static final String specificDepthURI = userDataTypeURI + "/depth";
	
	public static final String minimalQueryURI = userDataTypeURI;
	public static final String statsURI = minimalQueryURI + slash;
	public static final String minimalQuerySummaryURI = minimalQueryURI + "/summary";
	
	/**
	 * Need to call these out since we use a path parameter to specify either a channel ID or a level.  For 
	 * any other more involved query path parameters can be used and we can use the general URI for the data agnostic queries.
	 */
	public static final String dataAgnosticTimeTypeMinimal = userDataTypeURI + "/" + timeType;
	
	public static final String ehaMinimal = "eha/" + source + "/" + recordedState + "/" + timeType;
	public static final String evrMinimal = "evr/" + source + "/" + recordedState + "/" + timeType;

	/**
	 * Insert URIs. 
	 */
	public static final String ehaInsertURI = "eha/" + source + "/" + recordedState + "/insert";
	public static final String evrInsertURI = "evr/" + source + "/" + recordedState + "/insert";
	
	/**
	 * Server URIs. 
	 */
	public static final String serverURI = "server";
	public static final String serverStatsURI = serverURI + slash;

	
	/**
	 * Specify the string values for query params.
	 */
	public static final String queryTypeQP = "queryType";
	public static final String sourceQP = "source";
	public static final String recordedStateQP = "recordedState";
	public static final String timeTypeQP = "timeType";

	
	public static final String sessionIdQP = "sessionNumber";
	public static final String channelIdQP = "channelId";
	/**
	 * Adding evr IDs.
	 */
	public static final String evrIdQP = "evrId";
	public static final String evrLevelQP = "evrLevel";
	public static final String evrNameQP = "evrName";
	public static final String evrMessageQP = "evrMessage";
	public static final String hostQP = "host";
	public static final String venueQP = "venue";
	public static final String dssIdQP = "dssId";
	public static final String vcidQP = "vcid";
	public static final String scidQP = "scid";
	public static final String maxResultsQP = "maxResults";
	public static final String lowerBoundTimeQP = "lowerBoundTime";
	public static final String upperBoundTimeQP = "upperBoundTime";
	public static final String verifiedQP = "verified";
	public static final String binaryResponseQP = "binaryResponse";
	public static final String outputFormatQP = "outputFormat";
	public static final String showColHeadersQP = "showColumnHeaders";

	private static String buildUri(String uriBase, String uriName, String uri) {
		return String.format("%-25s: %s%s", uriName, uriBase, uri);
	}
	
	public static void main(String args[]) throws IllegalArgumentException, IllegalAccessException {
		String base = String.format(GlobalLadProperties.getGlobalInstance().getRestURIBase().replace("%d", "%s"), "<HOST>", "<PORT>");
		Field[] f = ResourceUris.class.getDeclaredFields();
		
		StringBuilder uris = new StringBuilder();
		StringBuilder params = new StringBuilder();
		
		for (Field ff : f) {
			String name = ff.getName();
			
			if (name.endsWith("URI")) {
				uris.append(buildUri(base, name, ff.get(null).toString()));
				uris.append("\n");
			} else if (name.endsWith("QP")) {
				params.append(name + ": " + ff.get(null));
				params.append("\n");
			}
		}
		
		System.out.println("URIS:");
		System.out.println(uris.toString());
		
		System.out.println("\nQuery Params and names:");
		System.out.println(params.toString());

		
		
//		Arrays.asList(f).stream().forEach(System.out::println);
		
	}
	
}
