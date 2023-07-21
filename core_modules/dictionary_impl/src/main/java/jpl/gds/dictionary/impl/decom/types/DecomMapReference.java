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
package jpl.gds.dictionary.impl.decom.types;

import java.util.Collections;
import java.util.Map;

import jpl.gds.dictionary.api.decom.params.DecomMapReferenceParams;
import jpl.gds.dictionary.api.decom.types.IDecomMapReference;

/**
 * Implementation class for decom map references within generic decom maps.
 *
 */
public class DecomMapReference implements IDecomMapReference {

	private final String id;
	private final Map<String, String> channelMapping;

	/**
	 * Create an instance initialied from the given parameter object.
	 * @param params
	 */
	public DecomMapReference(DecomMapReferenceParams params) {
		id = params.getMapId();
		channelMapping = params.getNameToChannelMap();
	}

	@Override
	public String getMapId() {
		return id;
	}

	@Override
	public Map<String, String> getNameToChannelMap() {
		return Collections.unmodifiableMap(channelMapping);
	}

}
