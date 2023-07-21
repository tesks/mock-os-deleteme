/*
 * Copyright 2006-2020. California Institute of Technology.
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

package jpl.gds.product.automation.hibernate;

import jpl.gds.shared.log.Tracer;

/**
 * Interface to use to Spring-inject the PDPP logger
 */
public interface IAutomationLogger {
    @Deprecated
    void fatal(String message);

    void error(String message);

    void warn(String message);

    void info(String message);

    void debug(String message);

    void trace(String message);

    @Deprecated
    void fatal(String message, Long processorId);

    void error(String message, Long processorId);

    void warn(String message, Long processorId);

    void info(String message, Long processorId);

    void debug(String message, Long processorId);

    void trace(String message, Long processorId);

    @Deprecated
    void fatal(String message, Long processorId, Long productId);

    void error(String message, Long processorId, Long productId);

    void warn(String message, Long processorId, Long productId);

    void info(String message, Long processorId, Long productId);

    void debug(String message, Long processorId, Long productId);

    void trace(String message, Long processorId, Long productId);

    Tracer getTracer();
}
