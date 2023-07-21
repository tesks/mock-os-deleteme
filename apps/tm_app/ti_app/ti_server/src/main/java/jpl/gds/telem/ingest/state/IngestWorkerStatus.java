/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.telem.ingest.state;

/**
 * This class is used to report the processing state/status
 * of the TI Worker. Reports Connection, Flow and Sync states.
 *
 */
public class IngestWorkerStatus implements IIngestWorkerStatus {
    private ConnectionState connectionState;
    private FlowState flowState;
    private SyncState syncState;

    /**
     * Constructor for creating a TelemetryIngestWorker status
     * @param isConnected whether or not the ingest worker is connected
     * @param isFlowing whether or not the ingest worker is flowing
     * @param isInSync whether or not the ingest worker is In Sync
     */
    public IngestWorkerStatus(final boolean isConnected, final boolean isFlowing, final boolean isInSync) {
        this(isConnected ? ConnectionState.CONNECTED : ConnectionState.NOT_CONNECTED,
             isFlowing ? FlowState.FLOWING : FlowState.NOT_FLOWING,
                isInSync ? SyncState.IN_SYNC: SyncState.OUT_OF_SYNC);
    }

    private IngestWorkerStatus(final ConnectionState connectionState, final FlowState flowState, final SyncState syncState) {
        this.connectionState = connectionState;
        this.flowState = flowState;
        this.syncState = syncState;
    }


    @Override
    public boolean isWorking() {
        return isConnected() && isFlowing();
    }

    @Override
    public boolean isConnected() {
        return this.connectionState == ConnectionState.CONNECTED;
    }

    @Override
    public boolean isFlowing() {
        return this.flowState == FlowState.FLOWING;
    }

    @Override
    public boolean isInSync() {
        return this.syncState == SyncState.IN_SYNC;
    }


    private enum ConnectionState {
        CONNECTED,
        NOT_CONNECTED
    }

    private enum FlowState {
        FLOWING,
        NOT_FLOWING
    }

    private enum SyncState {
        IN_SYNC,
        OUT_OF_SYNC
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append(getClass().getSimpleName())
          .append(" is ")
          .append(connectionState.toString())
          .append(" and ")
          .append(flowState.toString())
          .append(" and ")
          .append(syncState.toString());

        return sb.toString();
    }

}
