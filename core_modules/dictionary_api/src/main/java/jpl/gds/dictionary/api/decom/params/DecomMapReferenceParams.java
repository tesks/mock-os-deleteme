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
package jpl.gds.dictionary.api.decom.params;

import java.util.HashMap;
import java.util.Map;

import jpl.gds.dictionary.api.decom.IDecomStatement;
import jpl.gds.dictionary.api.decom.types.IDecomMapReference;

/**
 * Parameter builder class for creating {@link IDecomMapReference} instances.
 * Mutable and reusable.
 * 
 * All values set for an instance of this class will be applied to the {@link IDecomStatement}
 * instance it will be used to create. For more information for each of the parameters,
 * see the interface this parameter object corresponds to.
 */
public class DecomMapReferenceParams implements IDecomDefinitionParams {

	private Map<String, String> nameToChannelMap = new HashMap<>();
	private String mapId;

	/**
	 * @see IDecomMapReference#getNameToChannelMap()
	 * @return the mapping of decom field names to channel IDs
	 */
	public Map<String, String> getNameToChannelMap() {
		return nameToChannelMap;
	}

	/**
	 * Add a mapping between a decom map field and a channel
	 * @param fieldName the field within the referenced decom map
	 *        Must not be null.
	 * @param channelId the ID to map the field to
	 * 		  Must not be null.
	 * @throws IllegalArgumentException if either argument is null.
	 */
	public void addMapping(String fieldName, String channelId) {
		if (fieldName == null) {
			throw new IllegalArgumentException("The field name for a channel mapping "
					+ " is null");
		}
		if (channelId == null) {
			throw new IllegalArgumentException("The channel ID for a channel mapping is null");
		}
		nameToChannelMap.put(fieldName, channelId);
	}

	/**
	 * @see IDecomMapReference#getMapId() 
	 * @return the string ID of the map being referenced
	 */
	public String getMapId() {
		return mapId;
	}

	/**
	 * Set the map id of the map reference
	 * @param mapId the simple string mapId identifying a referenced map
	 * 		  Must not be null.
	 * @throws IllegalArgumentException if the argument is null
	 */
	public void setMapId(String mapId) {
		if (mapId == null) {
			throw new IllegalArgumentException("The decom map ID can not be null for a map reference");
		}
		this.mapId = mapId;
	}
	
	@Override
	public void reset() {
		nameToChannelMap = new HashMap<>();
		mapId = "";
	}
}
