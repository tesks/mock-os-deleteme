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
package jpl.gds.shared.cli.app;

import org.apache.commons.cli.ParseException;

/**
 * A special version of Apache's CLI ParseException that simply indicates that help or version information was
 * displayed. It allows AMPCS command line parsing to work in a spring-boot or spring-mvc environment.
 * 
 */
public class HelpOrVersionDisplayedException extends ParseException {
    private static final long serialVersionUID = -2098537847225241129L;

    private final boolean     helpDisplayed;
    private final boolean     versionDisplayed;
    private final Throwable   t;

    /**
     * This exception has no message.
     * 
     * @param helpDisplayed
     *            if true, then the command line parser displayed the help message, if false, it did not
     * @param versionDisplayed
     *            if true, then the command line parser displayed the version message, if false, it did not
     * @param t
     *            the Throwable that caused help or or version information to be displayed (if not explicitly asked for)
     */
    public HelpOrVersionDisplayedException(final boolean helpDisplayed, final boolean versionDisplayed,
            final Throwable t) {
        super("");
        this.helpDisplayed = helpDisplayed;
        this.versionDisplayed = versionDisplayed;
        this.t = t;
    }

    @Override
    public Throwable getCause() {
        return t;
    }

    /**
     * @return true if the command line parser displayed the help message, false if not
     */
    public boolean isHelpDisplayed() {
        return helpDisplayed;
    }

    /**
     * @return true if the command line parser displayed the version message, false if not
     */
    public boolean isVersionDisplayed() {
        return versionDisplayed;
    }
}
