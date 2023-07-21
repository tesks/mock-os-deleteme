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
package jpl.gds.context.api.message;

import jpl.gds.common.config.service.ServiceConfiguration;
import jpl.gds.shared.interfaces.EscapedCsvSupport;
import jpl.gds.shared.template.Templatable;

/**
 * An interface to be implemented by context heartbeat messages.
 * 
 *
 * @since R8
 *
 */
public interface IContextHeartbeatMessage extends IContextMessage, Templatable, EscapedCsvSupport {

    /**
     * Gets the service configuration object from this message.
     * 
     * @return ServiceConfiguration object; may be null
     * 
     */
    public ServiceConfiguration getServiceConfiguration();

    /**
     * Sets the service configuration object in this message.
     * 
     * @param serviceConfig
     *            ServiceConfiguration object to set; may be null
     * 
     */
    public void setServiceConfiguration(final ServiceConfiguration serviceConfig);

}
