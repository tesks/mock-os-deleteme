/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.cfdp.clt.mib;

import static jpl.gds.cfdp.clt.ENonActionCommandType.MIB;
import static jpl.gds.cfdp.clt.mib.MibClt.EMibEntityType.LOCAL;
import static jpl.gds.cfdp.clt.mib.MibClt.EMibEntityType.REMOTE;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.io.PrintWriter;
import java.net.URI;
import java.util.Properties;

import org.apache.commons.cli.ParseException;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jpl.gds.cfdp.clt.AGetSetClt;
import jpl.gds.cfdp.clt.ENonActionCommandType;
import jpl.gds.cfdp.common.GenericPropertiesMapResponse;
import jpl.gds.cfdp.common.GenericPropertiesResponse;
import jpl.gds.cfdp.common.GenericPropertiesSetRequest;
import jpl.gds.cfdp.common.mib.ELocalEntityMibPropertyKey;
import jpl.gds.cfdp.common.mib.ERemoteEntityMibPropertyKey;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.config.GdsSystemProperties;

public class MibClt extends AGetSetClt {

	private static final String mibUriRoot = ENonActionCommandType.MIB.getRelativeUri() + "/";

	private EMibEntityType entityType;
	private String entityId;
	private String getKey;
	private Properties propertiesToUpdate;

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#createOptions()
	 */
	@Override
	public BaseCommandOptions createOptions() {
		
	    if (optionsCreated.get()) {
            return options;
        }

		getOption = new StringOption(GET_SHORT, GET_LONG, "{local|remote}[:entity-id[:key]]",
				"MIB properties to look up", false);
		setOption = new StringOption(SET_SHORT, SET_LONG, "{local|remote}:entity-id:key-values",
				"comma-separated key-value pairs of MIB properties to update, commas inside quotes are ignored (e.g. mykey=myvalue or mykey1=my\\ value\\ 1,mykey2=\\\"val1,val2,val3\\\")",
				false);
		return super.createOptions();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * jpl.gds.shared.cli.app.AbstractCommandLineApp#configure(jpl.gds.shared.cli.
	 * cmdline.ICommandLine)
	 */
	@Override
	public void configure(final ICommandLine commandLine) throws ParseException {
		super.configure(commandLine);

		if (getParam != null && setParam != null) {
			throw new ParseException("Only one of " + getOption + " and " + setOption + " is accepted, not both");
		}

		if (getParam != null) {
			parseGetParameter(getParam);
		} else if (setParam != null) {
			parseSetParameter(setParam);
		}

	}

	void parseGetParameter(final String getParam) throws ParseException {
		final String[] splitParams = getParam.split(":", 3);

		try {
			entityType = EMibEntityType.valueOf(splitParams[0].toUpperCase());
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new ParseException(e.getLocalizedMessage());
		}

		if (splitParams.length > 1) {
			entityId = splitParams[1];
		}

		if (splitParams.length > 2) {
			getKey = splitParams[2];
		}

	}

	void parseSetParameter(final String setParam) throws ParseException {
		final String[] splitParams = setParam.split(":", 3);

		if (splitParams.length < 3) {
			throw new ParseException(
					"set parameter requires colon-delimited entity type (\"local\" or \"remote\"), entity ID, and one or more key-value pairs");
		}

		try {
			entityType = EMibEntityType.valueOf(splitParams[0].toUpperCase());
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new ParseException(e.getLocalizedMessage());
		}

		entityId = splitParams[1];

		/*-
		 * The split pattern string below will split at each comma, which may or may not
		 * be preceded and/or followed by spaces, but will not split if the comma is
		 * inside double-quotes.
		 *
		 * For example, if the command-line parameter is:
		 *
		 * mykey1=my\ value\ 1,mykey2=\"val1,val2,val3\"
		 *
		 * it will be read as:
		 *
		 * mykey1=my value 1,mykey2="val1,val2,val3"
		 *
		 * and then tokenized as:
		 *
		 * mykey1=my value 1
		 * mykey2="val1,val2,val3"
		 */
		final String[] rawKeyValues = splitParams[2].trim().split("\\s*,(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)\\s*", -1);

		propertiesToUpdate = new Properties();

		for (final String kv : rawKeyValues) {
			final String[] kvPairArray = kv.split("\\s*=\\s*", 2);
			propertiesToUpdate.setProperty(kvPairArray[0], kvPairArray[1]);
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#showHelp()
	 */
	@Override
	public void showHelp() {

		if (helpDisplayed.getAndSet(true)) {
			return;
		}

		final OptionSet options = createOptions().getOptions();
		final PrintWriter pw = new PrintWriter(System.out);
		pw.println("Usage: " + ApplicationConfiguration.getApplicationName() + " " + MIB.getCltCommandStr()
				+ " [options]");
		pw.println();
		options.printOptions(pw);
		printTemplateStylesAndDirectories(pw);
		pw.flush();

		showLocalEntityMibKeys();
		showRemoteEntityMibKeys();
	}

	private void showLocalEntityMibKeys() {
		final PrintWriter pw = new PrintWriter(System.out);
		pw.println("\nAvailable MIB keys for local entities are:");
		ELocalEntityMibPropertyKey.getAllKeyStrings().stream().forEach(s -> pw.println(
				"   " + s + (ELocalEntityMibPropertyKey.getAllRequiredKeyStrings().contains(s) ? " (required)" : "")));
		pw.flush();
	}

	private void showRemoteEntityMibKeys() {
		final PrintWriter pw = new PrintWriter(System.out);
		pw.println("\nAvailable MIB keys for remote entities are:");
		ERemoteEntityMibPropertyKey.getAllKeyStrings().stream().forEach(s -> pw.println(
				"   " + s + (ERemoteEntityMibPropertyKey.getAllRequiredKeyStrings().contains(s) ? " (required)" : "")));
		pw.flush();
	}

	@Override
	public void run() {

		try {

			if (getParam == null && setParam == null) {
				System.out.println("Querying local entity MIB");
				fetchAndPrintAllEntitiesMib(LOCAL, true);
				System.out.println();
				System.out.println("Querying remote entity MIB");
				fetchAndPrintAllEntitiesMib(REMOTE, true);
			} else if (getParam != null) {

				if (entityId == null) {
					fetchAndPrintAllEntitiesMib(entityType);
				} else {
					String subUri = entityId;

					if (getKey != null) {
						subUri += "/" + getKey;
					}

					fetchAndPrintSingleEntityMib(subUri, entityType, entityId);
				}

			} else {
				setAndPrintMib(entityType, entityId);
			}

		} catch (final RestClientException rce) {
			System.err.println(rce);
			if (headers == null) {
				System.err.println("Server may require authentication. Please supply \""+this.accessOptions.LOGIN_METHOD_NON_GUI.getLongOrShort()+"\" option.");
			}
        
		}

	}

	private void setAndPrintMib(final EMibEntityType entityType, final String entityId) throws RestClientException {
		final RestTemplate restTemplate = new RestTemplate();
		final GenericPropertiesSetRequest req = new GenericPropertiesSetRequest();
        req.setRequesterId(GdsSystemProperties.getSystemUserName());
		req.setPropertiesToSet(propertiesToUpdate);
		final String entityTypeStr = entityType.toString().toLowerCase();
		final String absoluteUri = getAbsoluteUri(mibUriRoot + entityTypeStr + "/" + entityId);
		final RequestEntity<GenericPropertiesSetRequest> requestEntity = new RequestEntity<>(req, headers, PUT,
				URI.create(absoluteUri));

		trace.debug("PUT " + absoluteUri);

		try {
			final ResponseEntity<GenericPropertiesResponse> resp = restTemplate.exchange(requestEntity,
					GenericPropertiesResponse.class);
			resp.getBody().printToSystemOut();
		} catch (final HttpClientErrorException hcee) {
			System.err.println("HTTP Status Code " + hcee.getStatusCode() + ": " + hcee.getResponseBodyAsString()
					+ " [PUT: " + absoluteUri + "]");
		}

	}

	private void fetchAndPrintAllEntitiesMib(final EMibEntityType entityType) throws RestClientException {
		fetchAndPrintAllEntitiesMib(entityType, false);
	}

	private void fetchAndPrintAllEntitiesMib(final EMibEntityType entityType, final boolean suppressNotFound)
			throws RestClientException {
		final RestTemplate restTemplate = new RestTemplate();
		final String entityTypeStr = entityType.toString().toLowerCase();
		final String absoluteUri = getAbsoluteUri(mibUriRoot + entityTypeStr);

		trace.debug("GET " + absoluteUri);
		ResponseEntity<GenericPropertiesMapResponse> resp = null;

		try {
			resp = restTemplate.exchange(new RequestEntity<>(headers, HttpMethod.GET, URI.create(absoluteUri)), 
					GenericPropertiesMapResponse.class);
			resp.getBody().printToSystemOut();

		} catch (final HttpClientErrorException hcee) {

			if (hcee.getStatusCode() != NOT_FOUND || !suppressNotFound) {
				System.err.println("HTTP Status Code " + hcee.getStatusCode() + ": " + hcee.getResponseBodyAsString()
						+ " [GET: " + absoluteUri + "]");
			}

		}

	}

	private void fetchAndPrintSingleEntityMib(final String subUri, final EMibEntityType entityType, final String entityId)
			throws RestClientException {
		final RestTemplate restTemplate = new RestTemplate();
		final String entityTypeStr = entityType.toString().toLowerCase();
		final String absoluteUri = getAbsoluteUri(mibUriRoot + entityTypeStr) + "/" + subUri;

		trace.debug("GET " + absoluteUri);

		try {
			final ResponseEntity<GenericPropertiesResponse> resp = restTemplate.exchange(new RequestEntity<>(headers, HttpMethod.GET, URI.create(absoluteUri)), 
					GenericPropertiesResponse.class);
			resp.getBody().printToSystemOut();
		} catch (final HttpClientErrorException hcee) {
			System.err.println("HTTP Status Code " + hcee.getStatusCode() + ": " + hcee.getResponseBodyAsString()
					+ " [GET: " + absoluteUri + "]");
		}

	}

	static enum EMibEntityType {
		LOCAL, REMOTE;
	}

}