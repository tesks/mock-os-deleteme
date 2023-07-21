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
package jpl.gds.shared.process;

import java.io.IOException;

/**
 * Standard out line handler which prints to the System.out
 * 
 *
 */
public class StdoutLineHandler implements LineHandler {

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.process.LineHandler#handleLine(java.lang.String)
     */
    public void handleLine(String line) throws IOException {
        System.out.println(line);
        System.out.flush();
    }

}
