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
package jpl.gds.globallad.workers;

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.GlobalLadReapSettings;
import jpl.gds.globallad.IGlobalLadReapable;
import jpl.gds.globallad.memory.IMemoryThresholdChecker;
import jpl.gds.shared.log.Tracer;

/**
 * Runnable that will reap all stale data from a container.
 *
 * Updated the reaper to also keep track of memory usage and to
 * attempt to remove data from the glad data tree so an out of memory issue does not occur. 
 */
public class GlobalLadReaper implements Runnable {

    private final Tracer                  log;
    private final IGlobalLadReapable      reapTarget;
    private final int                     maxNumberReaps;
    private final GlobalLadProperties     config;
    private final IMemoryThresholdChecker memoryChecker;

    private GlobalLadReapSettings         lastReapSettings;

    /**
     * The reap target will never get trimmed.
     * 
     * @param reapTarget
     *            the reap target
     * @param log
     *            GlobalLad Tracer
     * @param memoryCheck
     *            IMemoryThresholdChecker
     */
    public GlobalLadReaper(final IGlobalLadReapable reapTarget, final Tracer log,
            final IMemoryThresholdChecker memoryCheck) {
        this.reapTarget = reapTarget;
        this.log = log;
        this.memoryChecker = memoryCheck;
        this.lastReapSettings = GlobalLadReapSettings.NORMAL;
        this.maxNumberReaps = GlobalLadReapSettings.values().length;
        this.config = GlobalLadProperties.getGlobalInstance();
    }

    /**
     * Calculates the current reap setting based on the last reap setting and the current state of
     * the memory.
     * 
     * @return GlobalLadReapSettings
     */
    public GlobalLadReapSettings checkAndCalculateReapSettings() {
        GlobalLadReapSettings currentReapSettings;

        if (!memoryChecker.isOverMemoryThreshold()) {
            currentReapSettings = GlobalLadReapSettings.NORMAL;
        }
        else {
            switch (lastReapSettings) {
                case NORMAL:
                    currentReapSettings = GlobalLadReapSettings.MEM_NORMAL;
                    break;
                case MEM_NORMAL:
                    currentReapSettings = GlobalLadReapSettings.REDUCED_TOUCH_TIME_25;
                    break;
                case REDUCED_TOUCH_TIME_25:
                    currentReapSettings = GlobalLadReapSettings.REDUCED_TOUCH_TIME_50;
                    break;
                case REDUCED_TOUCH_TIME_50:
                    currentReapSettings = GlobalLadReapSettings.REDUCED_TOUCH_TIME_75;
                    break;
                case REDUCED_TOUCH_TIME_75:
                    currentReapSettings = GlobalLadReapSettings.REDUCED_TOUCH_TIME_90;
                    break;
                case REDUCED_TOUCH_TIME_90:
                    currentReapSettings = GlobalLadReapSettings.IGNORE_LEVEL_RESTRICTIONS;
                    break;
                case IGNORE_LEVEL_RESTRICTIONS:
                    currentReapSettings = GlobalLadReapSettings.REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_25;
                    break;
                case REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_25:
                    currentReapSettings = GlobalLadReapSettings.REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_50;
                    break;
                case REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_50:
                    currentReapSettings = GlobalLadReapSettings.REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_75;
                    break;
                case REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_75:
                    currentReapSettings = GlobalLadReapSettings.REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_90;
                    break;
                case REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_90:
                    currentReapSettings = GlobalLadReapSettings.REDUCE_DATA_DEPTHS_10;
                    break;
                case REDUCE_DATA_DEPTHS_10:
                    currentReapSettings = GlobalLadReapSettings.REDUCE_DATA_DEPTHS_25;
                    break;
                case REDUCE_DATA_DEPTHS_25:
                    currentReapSettings = GlobalLadReapSettings.REDUCE_DATA_DEPTHS_50;
                    break;
                case REDUCE_DATA_DEPTHS_50:
                    currentReapSettings = GlobalLadReapSettings.REDUCE_DATA_DEPTHS_75;
                    break;
                case REDUCE_DATA_DEPTHS_75:
                    currentReapSettings = GlobalLadReapSettings.REDUCE_DATA_DEPTHS_90;
                    break;
                case REDUCE_DATA_DEPTHS_90:
                    currentReapSettings = GlobalLadReapSettings.REDUCE_DATA_DEPTHS_99;
                    break;
                case REDUCE_DATA_DEPTHS_99:
                case REDUCE_DATA_DEPTHS_PERM:
                    currentReapSettings = GlobalLadReapSettings.REDUCE_DATA_DEPTHS_PERM;
                    break;
                default:
                    currentReapSettings = GlobalLadReapSettings.NORMAL;
            }
        }

        lastReapSettings = currentReapSettings;
        return currentReapSettings;
    }

    /**
     * Updates the global lad config with a permanent change to the default depth.
     */
    private void changePermSettings() {
        switch (lastReapSettings) {
            case REDUCE_DATA_DEPTHS_PERM:
                /**
                 * Adjust the default depth permanently based on the depth ratio.
                 */
            	if(config.getDefaultDataDepth() > 2) {
    				final int newMaxSize = (int) (lastReapSettings.depthRatio * config.getDefaultDataDepth());
    				config.setDefaultDepth(newMaxSize > 2 ? newMaxSize : 2);
    			}
                break;
            case IGNORE_LEVEL_RESTRICTIONS:
            case NORMAL:
            case MEM_NORMAL:
            case REDUCED_TOUCH_TIME_25:
            case REDUCED_TOUCH_TIME_50:
            case REDUCED_TOUCH_TIME_75:
            case REDUCED_TOUCH_TIME_90:
            case REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_25:
            case REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_50:
            case REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_75:
            case REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_90:
            case REDUCE_DATA_DEPTHS_10:
            case REDUCE_DATA_DEPTHS_25:
            case REDUCE_DATA_DEPTHS_50:
            case REDUCE_DATA_DEPTHS_75:
            case REDUCE_DATA_DEPTHS_90:
            case REDUCE_DATA_DEPTHS_99:
                break;
        }
    }

    @Override
    public void run() {
        // System.out.println("REAPING");
        synchronized (reapTarget) {
            int inlineReaps = this.maxNumberReaps;
            boolean wasReaped = false;
            boolean normalReap = false;

            do {
                checkAndCalculateReapSettings();

                /**
                 * Must do proper logging of these in the event that things are getting removed. Must add a switch here
                 * because
                 * this is where the actual work is going to happen.
                 */
                if (lastReapSettings == GlobalLadReapSettings.NORMAL) {
                    normalReap = true;
                }
                else {
                    log.warn("Memory condition has been detected and corrective actions are being taken:  ",
                                   lastReapSettings);
                }

                switch (lastReapSettings) {
                    case REDUCE_DATA_DEPTHS_PERM:
                        changePermSettings();
                        // If we change the perm settings break out and let the glad handle this before doing any more
                        // work.
                        inlineReaps = -1;
                        break;
                    case NORMAL:
                    case MEM_NORMAL:
                    case IGNORE_LEVEL_RESTRICTIONS:
                    case REDUCED_TOUCH_TIME_25:
                    case REDUCED_TOUCH_TIME_50:
                    case REDUCED_TOUCH_TIME_75:
                    case REDUCED_TOUCH_TIME_90:
                    case REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_25:
                    case REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_50:
                    case REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_75:
                    case REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_90:
                    case REDUCE_DATA_DEPTHS_10:
                    case REDUCE_DATA_DEPTHS_25:
                    case REDUCE_DATA_DEPTHS_50:
                    case REDUCE_DATA_DEPTHS_75:
                    case REDUCE_DATA_DEPTHS_90:
                    case REDUCE_DATA_DEPTHS_99:
                        /**
                         * All other cases we want to do a normal reap.
                         */
                        wasReaped = reapTarget.reap(lastReapSettings, System.currentTimeMillis(), false, -1);
                        break;
                }

                inlineReaps--;

                /**
                 * If something was reaped, give the JVM time to get rid of it so don't do any more work now, wait for
                 * the next reap cycle.
                 */
            } while (!normalReap && !wasReaped && inlineReaps > 0);

            this.lastReapSettings = GlobalLadReapSettings.NORMAL;
        }
    }
}
