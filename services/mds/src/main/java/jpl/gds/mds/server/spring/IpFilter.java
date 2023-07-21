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

package jpl.gds.mds.server.spring;

import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import org.springframework.integration.core.GenericSelector;
import org.springframework.messaging.Message;

import java.util.Set;

/**
 * IP Filter
 */
public class IpFilter implements GenericSelector<Message> {

    private static final String IP_HEADER = "ip_address";
    private static final Tracer LOG       = TraceManager.getTracer(Loggers.MDS);

    private final Set<String> ipSet;

    /**
     * Constructor
     *
     * @param ipSet
     */
    public IpFilter(final Set<String> ipSet) {

        this.ipSet = ipSet;
    }

    @Override
    public boolean accept(final Message message) {
        final boolean accepted = ipSet.contains(message.getHeaders().get(IP_HEADER, String.class));
        if (!accepted) {
            LOG.warn("UDP sender IP [", message.getHeaders().get(IP_HEADER, String.class),
                    "] not in allowed list, discarding message.");
        }
        return accepted;
    }
}
