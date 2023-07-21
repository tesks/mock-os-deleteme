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
package jpl.gds.dictionary.api.decom.types;
/**
 * Represents any data that may be channelzied from a decom map.
 * 
 *
 */
public interface IChannelizableDataDefinition extends IDecomDataDefinition {
	
	/**
	 * Get the string channel ID that the data should be published with
	 * if channelization is performed.
	 * @return the string channel ID, or the empty string if none was defined.
	 */
	public String getChannelId();
	
	/**
	 * Determine whether the data should be published as a channel value.
	 * @return true if the data should be channelized, otherwise false.
	 */
	public boolean shouldChannelize();

}
