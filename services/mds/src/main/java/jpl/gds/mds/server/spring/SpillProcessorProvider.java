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

import jpl.gds.mds.server.config.MdsProperties;
import jpl.gds.mds.server.tcp.MessageListContainer;
import jpl.gds.message.api.spill.ISpillProcessor;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Queue;

/**
 * MDS spill processor provider. Spring-aware. Uses the application context to instantiate, the bean does not lend
 * itself to create via an object provider.
 */
public class SpillProcessorProvider {

    private final MdsProperties mdsProperties;

    @Autowired
    private ApplicationContext appContext;

    /**
     * Constructor
     *
     * @param mdsProperties
     */
    public SpillProcessorProvider(final MdsProperties mdsProperties) {
        this.mdsProperties = mdsProperties;
    }

    /**
     * Get an MDS spill processor
     *
     * @param clientQueue
     * @param quota
     * @param timeout
     * @param logger
     * @return
     */
    public ISpillProcessor<MessageListContainer> getSpillProcessor(final Queue<MessageListContainer> clientQueue,
                                                                   final int quota,
                                                                   final long timeout,
                                                                   final Tracer logger) {
        return appContext.getBean(ISpillProcessor.class,
                GdsSystemProperties.getUserConfigDir(),
                clientQueue,
                quota, // quota
                mdsProperties.isEnableSpill(), // isSpillEnabled
                MessageListContainer.class,
                "Channel Sample Counter",
                false, // isKeepSpillFilesEnabled
                timeout,
                logger,
                new SseContextFlag(false)
        );
    }
}
