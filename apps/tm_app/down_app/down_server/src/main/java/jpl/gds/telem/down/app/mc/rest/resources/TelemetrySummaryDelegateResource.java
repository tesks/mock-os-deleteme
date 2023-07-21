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
package jpl.gds.telem.down.app.mc.rest.resources;

import jpl.gds.common.service.telem.IDownlinkSummary;
import jpl.gds.common.service.telem.ITelemetrySummary;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.telem.common.app.mc.DownlinkProcessingState;
import jpl.gds.telem.down.IDownlinkApp;

/**
 * A POJO delegate to deliver Telemetry Summary information to RESTful M&C Service
 * 
 */
public class TelemetrySummaryDelegateResource {
    private final IDownlinkApp             app;
    private static final IDownlinkSummary EMPTY_TM_SUMMARY = new EmptyDelegate();
    private static final String            UNKNOWN_TIME     = "UNKNOWN TIME";

    /** Status: Telemetry Summary Object */
    private IDownlinkSummary              tmSummary;

    /** Status: Total number of frames processed. */
    private long                           numFrames;

    /** Status: Total number of out of sync frames processed. */
    private long                           outOfSyncFrames;

    /** Status: Current bitrate */
    private double                         bitrate;

    /** Status: Total number of valid packets processed. */
    private long                           numValidPackets;

    /** Status: Total number of invalid packets processed. */
    private long                           numInvalidPackets;

    /** Status: Total number of fill packets processed. */
    private long                           numFillPackets;

    /** Status: Total number of repeated frames encountered. */
    private long                           numFrameRepeats;

    /** Status: Total number of frame gaps encountered. */
    private long                           numFrameGaps;

    /** Status: Total number of station packets processed. */
    private long                           numStationPackets;

    /** Status: Total number of CFDP packets processed. */
    private long                          numCfdpPackets;

    /**
     * @param app
     *            the IDownlinkApp that is running
     */
    public TelemetrySummaryDelegateResource(final IDownlinkApp app) {
        super();
        this.app = app;
        setTelemetrySummary(EMPTY_TM_SUMMARY);
    }

    /**
     * @return the current processing state
     */
    public DownlinkProcessingState getProcessingState() {
        return app.getProcessingState();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getDbKey() {
        return tmSummary.getContextKey();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getEhaPackets() {
        return tmSummary.getEhaPackets();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getCfdpPackets() {
        return tmSummary.getCfdpPackets();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getEvrPackets() {
        return tmSummary.getEvrPackets();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public String getFullName() {
        return tmSummary.getFullName();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getInSyncFrames() {
        return tmSummary.getInSyncFrames();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getOutOfSyncData() {
        return tmSummary.getOutOfSyncData();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getPackets() {
        return tmSummary.getPackets();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getStationPackets() {
        return tmSummary.getStationPackets();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getPartialProducts() {
        return tmSummary.getPartialProducts();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getProductDataBytes() {
        return tmSummary.getProductDataBytes();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getProducts() {
        return tmSummary.getProducts();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public String getStartTime() {
        final IAccurateDateTime t = tmSummary.getStartTime();
        return (t != null) ? TimeUtility.getFormatterFromPool().format(t) : UNKNOWN_TIME;
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public String getStopTime() {
        final IAccurateDateTime t = tmSummary.getStopTime();
        return (t != null) ? TimeUtility.getFormatterFromPool().format(t) : UNKNOWN_TIME;
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public String getOutputDirectory() {
        return tmSummary.getOutputDirectory();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getBadFrames() {
        return tmSummary.getBadFrames();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getBadPackets() {
        return tmSummary.getBadPackets();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getDeadFrames() {
        return tmSummary.getDeadFrames();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getFrameGaps() {
        return tmSummary.getFrameGaps();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getFrameRegressions() {
        return tmSummary.getFrameRegressions();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getFrameRepeats() {
        return tmSummary.getFrameRepeats();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getIdleFrames() {
        return tmSummary.getIdleFrames();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getIdlePackets() {
        return tmSummary.getFillPackets();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getProductPackets() {
        return tmSummary.getProductPackets();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getOutOfSyncCount() {
        return tmSummary.getOutOfSyncCount();
    }

    /**
     * @return the current TimeComparisonStrategy
     */
    public String getTimeComparisonStrategy() {
        return app.getTimeComparisonStrategy().getDisplayName();
    }

    /**
     * @param summary
     *            the telemetry summary object to assign as delegate for this class
     */
    public void setTelemetrySummary(final ITelemetrySummary summary) {
        this.tmSummary = (null == summary) ? EMPTY_TM_SUMMARY : (IDownlinkSummary) summary;
    }

    private static class EmptyDelegate implements IDownlinkSummary {
        public EmptyDelegate() {
            super();
        }

        @Override
        public long getContextKey() {
            return 0;
        }

        @Override
        public void setContextKey(long dbKey) { }

        @Override
        public long getEhaPackets() {
            return 0;
        }

        @Override
        public long getEvrPackets() {
            return 0;
        }

        @Override
        public String getFullName() {
            return null;
        }

        @Override
        public void setFullName(String name) { }

        @Override
        public long getInSyncFrames() {
            return 0;
        }

        @Override
        public long getOutOfSyncData() {
            return 0;
        }

        @Override
        public long getPackets() {
            return 0;
        }

        @Override
        public long getStationPackets() {
            return 0;
        }

        @Override
        public long getPartialProducts() {
            return 0;
        }

        @Override
        public long getProductDataBytes() {
            return 0;
        }

        @Override
        public long getProducts() {
            return 0;
        }

        @Override
        public IAccurateDateTime getStartTime() {
            return null;
        }

        @Override
        public IAccurateDateTime getStopTime() {
            return null;
        }

        @Override
        public String getOutputDirectory() {
            return null;
        }

        @Override
        public void setOutputDirectory(String dir) { }

        @Override
        public String toStringNoBlanks(String in) { return ""; }

        @Override
        public long getBadFrames() {
            return 0;
        }

        @Override
        public long getBadPackets() {
            return 0;
        }

        @Override
        public long getDeadFrames() {
            return 0;
        }

        @Override
        public long getFrameGaps() {
            return 0;
        }

        @Override
        public long getFrameRegressions() {
            return 0;
        }

        @Override
        public long getFrameRepeats() {
            return 0;
        }

        @Override
        public long getIdleFrames() {
            return 0;
        }

        @Override
        public long getFillPackets() {
            return 0;
        }

        @Override
        public long getProductPackets() {
            return 0;
        }

        @Override
        public long getOutOfSyncCount() {
            return 0;
        }

        @Override
        public void populateBasicSummary(final IAccurateDateTime start, final IAccurateDateTime end, final String name,
                                         final String outputDir, final Long key) {
            return;
        }

        @Override
        public long getCfdpPackets() {
            return 0;
        }
    }

    /**
     * @return the total number of frames processed
     */
    public long getNumFrames() {
        return numFrames;
    }

    /**
     * @return the number of out of sync frames processed
     */
    public long getOutOfSyncFrames() {
        return outOfSyncFrames;
    }

    /**
     * @return the current bit rate
     */
    public double getBitrate() {
        return bitrate;
    }

    /**
     * @return the number of vaid packets processed
     */
    public long getNumValidPackets() {
        return numValidPackets;
    }

    /**
     * @return the number of invaid packets processed
     */
    public long getNumInvalidPackets() {
        return numInvalidPackets;
    }

    /**
     * @return the number of fill packets processed
     */
    public long getNumFillPackets() {
        return numFillPackets;
    }

    /**
     * @return the number of fill packets processed
     */
    public long getNumCfdpPackets() {
        return numCfdpPackets;
    }

    /**
     * @return the number of repeated frames encountered
     */
    public long getNumFrameRepeats() {
        return numFrameRepeats;
    }

    /**
     * @return the number of frame gaps encountered
     */
    public long getNumFrameGaps() {
        return numFrameGaps;
    }

    /**
     * @return the number of station packets processed
     */
    public long getNumStationPackets() {
        return numStationPackets;
    }

    /**
     * @param numFrames
     *            the total number of frames processed
     */
    public void setNumberOfFrames(final long numFrames) {
        this.numFrames = numFrames;
    }

    /**
     * @param outOfSyncCount
     *            the number of out of sync frames processed
     */
    public void setNumberOfOutOfSyncFrames(final long outOfSyncCount) {
        this.outOfSyncFrames = outOfSyncCount;
    }

    /**
     * @param bitrate
     *            the current bit rate
     */
    public void setBitrate(final double bitrate) {
        this.bitrate = bitrate;
    }

    /**
     * @param numValidPackets
     *            the number of valid packets processed
     */
    public void setNumberOfValidPackets(final long numValidPackets) {
        this.numValidPackets = numValidPackets;
    }

    /**
     * @param numInvalidPackets
     *            the number of invalid packets processed
     */
    public void setNumberOfInvalidPackets(final long numInvalidPackets) {
        this.numInvalidPackets = numInvalidPackets;
    }

    /**
     * @param numFillPackets
     *            the number of fill packets processed
     */
    public void setNumberOfFillPackets(final long numFillPackets) {
        this.numFillPackets = numFillPackets;
    }

    /**
     * @param numFrameRepeats
     *            the number of repeated frames encountered
     */
    public void setNumberOfFrameRepeats(final long numFrameRepeats) {
        this.numFrameRepeats = numFrameRepeats;
    }

    /**
     * @param numFrameGaps
     *            the number of frame gaps encountered
     */
    public void setNumberOfFrameGaps(final long numFrameGaps) {
        this.numFrameGaps = numFrameGaps;
    }

    /**
     * @param numStationPackets
     *            the number of station packets processed
     */
    public void setNumberOfStationPackets(final long numStationPackets) {
        this.numStationPackets = numStationPackets;
    }

    /**
     * Sets the number of CFDP packets processed
     * 
     * @param numCfdpPackets
     *            the number of Cfdp packets processed
     */
    public void setNumberOfCfdpPackets(final long numCfdpPackets) {
        this.numCfdpPackets = numCfdpPackets;
    }

}
