/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */
package jpl.gds.tc.mps.impl.cmd;

import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tc.api.ITcCommandReverser;
import jpl.gds.tc.api.ITewUtility;
import jpl.gds.tc.api.exception.CommandFileParseException;
import jpl.gds.tc.api.exception.CommandParseException;
import jpl.gds.tc.mps.impl.MpsTewUtility;
import jpl.gds.tc.mps.impl.ctt.CommandTranslationTable;
import jpl.gds.tc.mps.impl.session.ReverseTranslationMpsSession;
import org.springframework.context.ApplicationContext;

/**
 * <p>{@code MpsTcCommandReverser} is the MPS implementation for the {@code ITcCommandReverser}.</p>
 *
 * @since 8.2.0
 */
public class MpsTcCommandReverser implements ITcCommandReverser {

    private final Tracer      log;
    private final CommandTranslationTable ctt;

    /**
     * Constructor
     *
     * @param appContext spring application context
     */
    public MpsTcCommandReverser(final ApplicationContext appContext) throws CommandFileParseException {
        this(TraceManager.getTracer(appContext, Loggers.UPLINK), appContext.getBean(MpsTewUtility.class).getCtt());
    }

    /**
     * Constructor
     *
     * @param tracer     log tracer
     * @param ctt command translation table
     */
    public MpsTcCommandReverser(final Tracer tracer, final CommandTranslationTable ctt) {
        this.log = tracer;
        this.ctt = ctt;
    }

    @Override
    public String reverse(final String commandBytesHex) throws CommandParseException {

        if (ctt == null) {
            throw new CommandParseException("Command reversal: Command translation table must be set first");
        }

        if (commandBytesHex == null) {
            throw new CommandParseException("Command reversal: Command bytes (hex string) cannot be null");
        }

        if (commandBytesHex.length() < 2) {
            throw new CommandParseException(
                    "Command reversal: Command bytes (hex string) too small. Length is " + commandBytesHex.length());
        }

        try {
            final ReverseTranslationMpsSession reverseSession = new ReverseTranslationMpsSession(ctt);

            return reverseSession.reverseCommandBytesHex(commandBytesHex);

        } catch (final Exception e) {
            throw new CommandParseException(e);
        }

    }

}