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

import jpl.gds.dictionary.api.decom.IDecomStatement;
import jpl.gds.dictionary.api.decom.types.IChannelizableDataDefinition;

/**
 * Parameter builder class for creating {@link IChannelizableDataDefinition} instances.
 * Mutable and reusable.
 * 
 * All values set for an instance of this class will be applied to the {@link IDecomStatement}
 * instance it will be used to create. For more information for each of the parameters,
 * see the interface this parameter object corresponds to.
 */
public abstract class ChannelizableDataParams extends DecomDataParams {
	private String channelId = "";
	private boolean channelize = false;

	/**
	 * @see IChannelizableDataDefinition#getChannelId()
	 * @return the string channel ID for extracted data, or empty string if there is none defined
	 */
	public String getChannelId() {
		return channelId;
	}

	/**
	 * Set the channel ID for this data if and when it is channelized
	 * If the channelId parameter is null, the empty string is used,
	 * meaning data will not be channelized.
	 * @param channelId the string channel ID
	 */
	public void setChannelId(String channelId) {
		if (channelId != null) {
			this.channelId = channelId;
		} else {
			this.channelId = "";
		}
	}

	/**
	 * @see IChannelizableDataDefinition#shouldChannelize()
	 * @return true if the data should be channelized
	 */
	public boolean shouldChannelize() {
		return channelize;
	}

	/**
	 * Set whether the data should be channelized
	 * @param channelize true if data should be channelized, false if it should not be
	 */
	public void setChannelize(boolean channelize) {
		this.channelize = channelize;
	}

	@Override
	public void reset() {
		super.reset();
		this.channelId = "";
		this.channelize = false;
	}

}
