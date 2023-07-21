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

import jpl.gds.mds.server.sfdu.ISfduValidator;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import org.springframework.integration.core.GenericSelector;
import org.springframework.messaging.Message;

/**
 * MON-0158 Filter
 */
public class Monitor0158Filter implements GenericSelector<Message> {

    private static final Tracer         LOG = TraceManager.getTracer(Loggers.MDS);
    private final        ISfduValidator validator;

    /**
     * Constructor
     *
     * @param validator
     */
    public Monitor0158Filter(final ISfduValidator validator) {

        this.validator = validator;
    }

    @Override
    public boolean accept(final Message message) {

        final boolean valid = validator.validate((byte[]) message.getPayload());
        if (!valid) {
            LOG.warn("Received invalid MON-0158 data, discarding.");
        }
        return valid;
    }
}
