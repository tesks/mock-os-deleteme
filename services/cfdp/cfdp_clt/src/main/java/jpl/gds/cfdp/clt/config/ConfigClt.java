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

package jpl.gds.cfdp.clt.config;

import static jpl.gds.cfdp.clt.ENonActionCommandType.CONFIG;

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
import jpl.gds.cfdp.common.GenericPropertiesResponse;
import jpl.gds.cfdp.common.GenericPropertiesSetRequest;
import jpl.gds.cfdp.common.config.EConfigurationPropertyKey;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.config.GdsSystemProperties;

public class ConfigClt extends AGetSetClt {

	private String propertyToGet;
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

		getOption = new StringOption(GET_SHORT, GET_LONG, "key", "configuration property to look up", false);
		setOption = new StringOption(SET_SHORT, SET_LONG, "key-values",
				"comma-separated key-value pairs of configuration properties to update, commas inside quotes are ignored (e.g. mykey=myvalue or mykey1=my\\ value\\ 1,mykey2=\\\"val1,val2,val3\\\")",
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
			propertyToGet = getParam.trim();
		} else if (setParam != null) {
			propertiesToUpdate = parsePropertiesToUpdateFromString(setParam);
		}

	}

	Properties parsePropertiesToUpdateFromString(final String setParam) {

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
		final String[] rawKeyValues = setParam.trim().split("\\s*,(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)\\s*", -1);

		final Properties propertiesToUpdate = new Properties();

		for (final String kv : rawKeyValues) {
			final String[] kvPairArray = kv.split("\\s*=\\s*", 2);
			propertiesToUpdate.setProperty(kvPairArray[0], kvPairArray[1]);
		}

		return propertiesToUpdate;
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
		pw.println("Usage: " + ApplicationConfiguration.getApplicationName() + " " + CONFIG.getCltCommandStr()
				+ " [options]");
		pw.println();
		options.printOptions(pw);
		printTemplateStylesAndDirectories(pw);
		pw.flush();

		showConfigurationPropertyKeys();
	}

	private void showConfigurationPropertyKeys() {
		final PrintWriter pw = new PrintWriter(System.out);
		pw.println("\nAvailable configuration property keys are:");
		EConfigurationPropertyKey.getAllFullKeyStrings().stream().sorted().forEach(s -> pw.println("   " + s));
		pw.flush();
	}

	@Override
	public void run() {
		final RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<GenericPropertiesResponse> resp = null;
		String httpMethod = null;
		String absoluteUri = null;
		RequestEntity<GenericPropertiesSetRequest> requestEntity = null;
		try {

			if (propertyToGet == null && propertiesToUpdate == null) {
				httpMethod = "GET";
				absoluteUri = getAbsoluteUri(CONFIG.getRelativeUri());
				requestEntity = new RequestEntity<>(headers, HttpMethod.resolve(httpMethod), URI.create(absoluteUri));
			} else if (propertyToGet != null) {
				httpMethod = "GET";
				absoluteUri = getAbsoluteUri(CONFIG.getRelativeUri() + "/" + propertyToGet);
				requestEntity = new RequestEntity<>(headers, HttpMethod.resolve(httpMethod), URI.create(absoluteUri));
				
			} else {
				final GenericPropertiesSetRequest req = new GenericPropertiesSetRequest();
                req.setRequesterId(GdsSystemProperties.getSystemUserName());
				req.setPropertiesToSet(propertiesToUpdate);
				httpMethod = "PUT";
				absoluteUri = getAbsoluteUri("config");
				requestEntity = new RequestEntity<>(req, headers, HttpMethod.resolve(httpMethod), URI.create(absoluteUri));
			}
			resp = restTemplate.exchange(requestEntity, GenericPropertiesResponse.class);
			resp.getBody().printToSystemOut();

		} catch (final HttpClientErrorException hcee) {
			System.err.println("HTTP Status Code " + hcee.getStatusCode() + ": " + hcee.getResponseBodyAsString() + " ["
					+ httpMethod + ": " + absoluteUri + "]");
		} catch (final RestClientException rce) {
			System.err.println(rce);
			if (headers == null) {
				System.err.println("Server may require authentication. Please supply \""+this.accessOptions.LOGIN_METHOD_NON_GUI.getLongOrShort()+"\" option.");
			}  
		}

	}

}