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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenericPropertiesMapResponse extends AResponse {

	private Map<String, Properties> propertiesMap;

	/**
	 * @return the propertiesMap
	 */
	public Map<String, Properties> getPropertiesMap() {
		return propertiesMap;
	}

	/**
	 * @param propertiesMap
	 *            the propertiesMap to set
	 */
	public void setPropertiesMap(Map<String, Properties> propertiesMap) {
		this.propertiesMap = propertiesMap;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.cfdp.common.AResponse#printToSystemOut()
	 */
	@Override
	public void printToSystemOut() {
		super.printToSystemOut();

		if (getPropertiesMap() != null && !getPropertiesMap().isEmpty()) {

			for (Entry<String, Properties> mapEntry : getPropertiesMap().entrySet()) {
				System.out.println("--------------------------------------------------------------------------------");
				System.out.println(mapEntry.getKey() + ":");

				for (Entry<Object, Object> propertiesEntry : mapEntry.getValue().entrySet()) {
					System.out.println("     " + propertiesEntry.getKey() + "=" + propertiesEntry.getValue());
				}

			}

		} else {
			System.out.println("Nothing to display");
		}

	}

}