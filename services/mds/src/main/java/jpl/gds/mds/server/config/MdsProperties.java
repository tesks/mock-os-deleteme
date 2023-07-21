/*
 * Copyright 2006-2021. California Institute of Technology.
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

package jpl.gds.mds.server.config;

import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration for Monitor Data Service
 */
public class MdsProperties extends GdsHierarchicalProperties {
    private static final String PROPERTY_FILE = "mds.properties";

    private static final String PROPERTY_PREFIX = "mds.";

    private static final String SERVER_SOCKET_PORT  = PROPERTY_PREFIX + "serverSocketPort";
    private static final String CLIENT_UDP_PORT     = PROPERTY_PREFIX + "clientUdpPort";
    private static final String ENABLE_SPILL        = PROPERTY_PREFIX + "enableSpillProcessor";
    private static final String UDP_FORWARD_HOST    = PROPERTY_PREFIX + "udpForwardHost";
    private static final String SECURE_TCP          = PROPERTY_PREFIX + "security.serverSocket.secure";
    private static final String VALIDATE_PACKETS    = PROPERTY_PREFIX + "security.packet.validate";
    private static final String CONTROL_AUTHORITY   = PROPERTY_PREFIX + "security.packet.validate.controlAuthorities";
    private static final String SOURCE_IP_FILTERING = PROPERTY_PREFIX + "security.source.ip.filtering";
    private static final String ALLOWED_SOURCE_IPS  = PROPERTY_PREFIX + "security.source.ip.filtering.allowed";

    /**
     * The configured server socket port
     */
    private int serverSocketPort;

    /**
     * The configured client UDP port
     */
    private int clientUdpPort;

    // enable spill processor for TCP connections
    private boolean enableSpill;

    //UDP forward host
    private String udpForwardHost;

    // enable secured TCP connections
    private boolean secureTcp;

    // enable validation of MON-0158 packets
    private boolean validatePackets;

    // enable source IP filtering
    private boolean sourceIpFiltering;

    // allowed source IPs
    private Set<String> allowedSourceIps;

    // control authorities
    private Set<String> controlAuthorities;

    /**
     * Constructor that loads the default property file, which will be located using the standard configuration search.
     */
    public MdsProperties() {
        super(PROPERTY_FILE, new SseContextFlag());
        load();
    }

    /**
     * Get server socket port
     *
     * @return socket port
     */
    public int getSocketPort() {
        return serverSocketPort;
    }

    /**
     * Set socket TCP port
     *
     * @param port Port
     */
    public void setSocketPort(final int port) {
        serverSocketPort = port;
    }

    /**
     * Get client UDP port
     *
     * @return client port
     */
    public int getClientPort() {
        return clientUdpPort;
    }

    /**
     * Set client UDP port
     *
     * @param port Port
     */
    public void setClientPort(final int port) {
        clientUdpPort = port;
    }

    /**
     * Get spill processor enabled status
     *
     * @return True if spill processor is enabled
     */
    public boolean isEnableSpill() {
        return enableSpill;
    }

    /**
     * Set spill processor enabled status
     *
     * @param enableSpill Boolean
     */
    public void setEnableSpill(final boolean enableSpill) {
        this.enableSpill = enableSpill;
    }

    /**
     * Get UDP forward IP
     * @return IP where UDP is forwarded
     */
    public String getUdpForwardHost() {
        return udpForwardHost;
    }

    /**
     * Set UDP forward IP
     * @param udpForwardHost IP where UDP is forwarded
     */
    public void setUdpForwardHost(final String udpForwardHost) {
        this.udpForwardHost = udpForwardHost;
    }

    /**
     * Get secured TCP enabled status
     *
     * @return
     */
    public boolean isSecureTcp() {
        return this.secureTcp;
    }

    /**
     * Set secured TCP enabled status
     *
     * @param secureTcp boolean
     */
    public void setSecureTcp(final boolean secureTcp) {
        this.secureTcp = secureTcp;
    }

    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

    /**
     * Loads the feature enable/disable flags from the properties.
     */
    private void load() {
        serverSocketPort = getIntProperty(SERVER_SOCKET_PORT, 12349);
        clientUdpPort = getIntProperty(CLIENT_UDP_PORT, 5001);
        enableSpill = getBooleanProperty(ENABLE_SPILL, true);
        udpForwardHost = getProperty(UDP_FORWARD_HOST, "");
        secureTcp = getBooleanProperty(SECURE_TCP, false);
        validatePackets = getBooleanProperty(VALIDATE_PACKETS, false);

        // source ip filtering
        sourceIpFiltering = getBooleanProperty(SOURCE_IP_FILTERING, false);
        final String sourceIps = getProperty(ALLOWED_SOURCE_IPS, "");
        if ((sourceIps == null || sourceIps.isEmpty()) && sourceIpFiltering) {
            allowedSourceIps = Collections.singleton("127.0.0.1");
        } else {
            allowedSourceIps = new HashSet<>(Arrays.asList(StringUtils.split(sourceIps, ',')));
        }

        // set control authorities
        final String controlAuthoritiesProps = getProperty(CONTROL_AUTHORITY, "");
        if (controlAuthoritiesProps != null && !controlAuthoritiesProps.isEmpty()) {
            controlAuthorities = new HashSet<>(Arrays.asList(StringUtils.split(controlAuthoritiesProps, ',')));
        } else {
            controlAuthorities = Collections.singleton("NJPL");
        }
    }

    /**
     * Packet validation enabled
     *
     * @return
     */
    public boolean isValidatePackets() {
        return this.validatePackets;
    }

    /**
     * Get allowed source IP addresses
     *
     * @return
     */
    public Set<String> getAllowedSourceIps() {
        return allowedSourceIps;
    }

    /**
     * Source IP filtering enabled
     *
     * @return
     */
    public boolean isSourceIpFiltering() {
        return sourceIpFiltering;
    }

    /**
     * Get the set of valid Control Authorities
     *
     * @return
     */
    public Set<String> getControlAuthorities() {
        return controlAuthorities;
    }

    /**
     * Set the set of valid Control Authorities
     */
    public void setControlAuthorities(final Set<String> controlAuthorities) {
        this.controlAuthorities = controlAuthorities;
    }
}
