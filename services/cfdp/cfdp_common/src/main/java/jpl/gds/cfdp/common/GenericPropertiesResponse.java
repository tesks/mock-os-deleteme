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

package jpl.gds.cfdp.common;

import java.util.Map.Entry;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenericPropertiesResponse extends AResponse {

	private Properties properties;

	/**
	 * @return the properties
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * @param properties
	 *            the properties to set
	 */
	public void setProperties(final Properties properties) {
		this.properties = properties;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.cfdp.common.AResponse#printToSystemOut()
	 */
	@Override
	public void printToSystemOut() {
		super.printToSystemOut();

		if (getProperties() != null && !getProperties().isEmpty()) {
			System.out.println("--------------------------------------------------------------------------------");

			for (final Entry<Object, Object> entry : getProperties().entrySet()) {
				System.out.println(entry.getKey() + "=" + entry.getValue());
			}

		} else {
			System.out.println("Nothing to display");
		}

	}

	@JsonIgnore
	public String getFlattenedString() {
		String flattenedProperties = "";
		String delim = "";

		for (final Entry<Object, Object> entry : properties.entrySet()) {
			flattenedProperties += delim + (entry.getKey() + "=" + entry.getValue()).replaceAll(",", "\\,");
			delim = ",";
		}

		return flattenedProperties;
	}

}
