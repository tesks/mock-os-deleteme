/*
 * Copyright 2006-2017. California Institute of Technology.
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
package jpl.gds.common.config.bootstrap;

import java.util.Collection;
import java.util.Collections;

/**
 * This class encapsulates the ChannelLAD bootstrap configuration information.
 * 
 * @since R8
 *
 */
public class ChannelLadBootstrapConfiguration {

    private boolean                bootstrapLad = false;
    private Collection<Long> bootstrapIds = Collections.emptyList();


    /**
     * Gets the ChannelLAD bootstrap flag
     * 
     * @return bootstrap ChannelLAD flag
     */
    public boolean getLadBootstrapFlag() {
        return bootstrapLad;
    }

    /**
     * Sets the ChannelLAD bootstrap flag
     * 
     * @param bootstrap
     *            true if bootstrapping Channel LAD
     * 
     */
    public void setLadBootstrapFlag(final boolean bootstrap) {
        this.bootstrapLad = bootstrap;
    }

    /**
     * Gets a Collection of session id's to fetch the ChannelLAD for
     * 
     * @return Collection<Long> session id's
     */
    public Collection<Long> getLadBootstrapIds() {
        return bootstrapIds;
    }

    /**
     * Set the session id's to fetch the ChannelLAD for
     * 
     * @param fetchIds
     *            Collection<Long> of session id's to fetch the ChannelLAD for
     */
    public void setLadBootstrapIds(final Collection<Long> fetchIds) {
    	if(fetchIds != null) {
    		bootstrapIds = fetchIds;
    	}
    	else {
    		bootstrapIds = Collections.emptyList();
    	}
    }

}
