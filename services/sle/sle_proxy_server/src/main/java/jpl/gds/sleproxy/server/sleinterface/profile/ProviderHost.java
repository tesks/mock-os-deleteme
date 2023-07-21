/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.sleproxy.server.sleinterface.profile;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Represents a provider's hostname and port pair
 *
 */
public class ProviderHost {
    private final String hostName;
    private final int    port;

    /**
     * Constructor
     *
     * @param hostName provider host name
     * @param port provider port
     */
    public ProviderHost(final String hostName, final String port) {
        if (hostName == null || hostName.isEmpty()) {
            throw new IllegalArgumentException("Host name cannot be null or empty: " + hostName);
        }
        this.hostName = hostName;

        try {
            this.port = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port specified for host " + hostName + ": " + port, e);
        }
    }

    /**
     * Get the provider's port
     * @return provider's port
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Get the provider's hostname
     * @return provider's hostname
     */
    public String getHostName() {
        return this.hostName;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof ProviderHost)) {
            return false;
        }

        final ProviderHost other = (ProviderHost) o;

        return new EqualsBuilder()
                .append(hostName, other.hostName)
                .append(port, other.port)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(19, 29)
                .append(hostName)
                .append(port)
                .toHashCode();
    }

    @Override
    public String toString() {
        return hostName + ":" + port;
    }
}
