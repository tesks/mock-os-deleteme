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
package jpl.gds.telem.common.app;

import org.apache.commons.cli.ParseException;
import org.springframework.boot.ExitCodeGenerator;

import jpl.gds.common.error.ErrorCode;
import jpl.gds.shared.cli.app.ICommandLineApp;

/**
 * Class ExitCodeHandler
 */
public class ExitCodeHandler implements ExitCodeGenerator {
    private final ICommandLineApp app;
    private final Throwable    t;

    /**
     * @param app
     *            the AMPCS app being run
     */
    public ExitCodeHandler(final ICommandLineApp app) {
        this(app, null);
    }

    /**
     * @param t
     *            the Exception thrown from the execution of the AMPCS app
     */
    public ExitCodeHandler(final Throwable t) {
        this(null, t);
    }

    /**
     * @param app
     *            the AMPCS app being run
     * @param t
     *            the Exception thrown from the execution of the AMPCS app
     */
    public ExitCodeHandler(final ICommandLineApp app, final Throwable t) {
        super();
        this.app = app;
        this.t = t;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getExitCode() {
        final int errorCode;
        if (t != null) {
            if (t instanceof Error) {
                errorCode = DownlinkErrorManager.getErrorCode((Error) t);
            }
            else if (t instanceof ParseException) {
                errorCode = DownlinkErrorManager.getDownlinkParseErrorCode((ParseException) t);
            }
            else {
                errorCode = DownlinkErrorManager.getSessionErrorCode((Exception) t);
            }
        }
        else if (app != null) {
            errorCode = app.getErrorCode();
        }
        else {
            errorCode = ErrorCode.UNKNOWN_ERROR_CODE.getNumber();
        }
        return errorCode;
    }
}
