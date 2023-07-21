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
package ammos.datagen.pdu.app.client;

import cfdp.engine.IndicationType;
import cfdp.engine.Subject;
import cfdp.engine.TransStatus;
import cfdp.engine.User;
import cfdp.engine.ampcs.PduLog;
import cfdp.engine.ampcs.RequestResult;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * Implementation of CFDP User interface
 * 
 *
 */
public class CfdpUser implements User {
    private final Tracer statusLogger;

    public CfdpUser() {
        statusLogger = TraceManager.getTracer(Loggers.DATAGEN);
    }

    @Override
    public void indication(final IndicationType indicationType, final TransStatus transStatus,
                           final RequestResult requestResult) {

    }

    @Override
    public boolean ofInterest(final Subject subject) {
        return false;
    }

    @Override
    public void debug(final String text) {
        statusLogger.debug(text);
    }

    @Override
    public void info(final String text) {
        statusLogger.info(text);

    }

    @Override
    public void warning(final String text) {
        statusLogger.warn(text);

    }

    @Override
    public void error(final String text) {
        statusLogger.error(text);
    }

    @Override
    public void pdu(final TransStatus transStatus, final boolean b, final String s, final PduLog pduLog, final long l,
                    final String s1) {


    }
}
