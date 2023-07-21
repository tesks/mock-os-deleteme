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
package jpl.gds.shared.spring.context.flag;

/**
 * SSE Context flag meant to replace the GdsSytemProperties.isApplicationSse() system property lookup,
 * 
 *
 */
public class SseContextFlag {

    private Boolean isSse;

    /**
     * Default SSE Context flag constructor
     */
    public SseContextFlag() {
        this(false);
    }

    /**
     * SSE Context Flag constructor with initialization value
     * 
     * @param isSse
     *            whether or not the application is SSE
     */
    public SseContextFlag(final boolean isSse) {
        this.isSse = Boolean.valueOf(isSse);
    }


    /**
     * Returns whether or not the application is SSE
     * 
     * @return isSse
     */
    public boolean isApplicationSse() {
        return isSse;
    }

    /**
     * Sets the SSE context flag
     * 
     * @param enable
     *            SSE flag to set
     */
    public void setApplicationIsSse(final boolean enable) {
        isSse = enable;
    }

}
