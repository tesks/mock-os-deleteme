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
package jpl.gds.monitor.perspective.view;

import java.util.List;

/**
 * This interface is to be implemented by views that contain channel IDs.
 * 
 */
public interface ChannelViewConfiguration {
    /**
     * Retrieves the channel ID associated with the field.
     * 
     * @return Channel ID string
     */
    public List<String> getReferencedChannelIds();

    /**
     * Retrieves whether or not the channel ID is defined for the associated
     * field.
     * 
     * @return true if the the channel id is null, false otherwise
     */
    public boolean containsNullChannelIds();
}
