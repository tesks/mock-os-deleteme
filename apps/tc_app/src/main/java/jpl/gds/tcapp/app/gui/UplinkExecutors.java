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
package jpl.gds.tcapp.app.gui;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class UplinkExecutors {

    /**
     * Executor to use to perform uplink operations off the UI thread. Single
     * threaded to ensure uplink order and prevent synchronization issues due to
     * heritage of single threaded uplink design
     */
    public static final Executor uplinkExecutor = Executors
    		.newSingleThreadExecutor();
    /** Executor to use to spawn thread for background, non-UI, non-uplink tasks */
    public static final Executor genericExecutor = Executors
    		.newCachedThreadPool();

    public UplinkExecutors() {
        // TODO Auto-generated constructor stub
    }

}
