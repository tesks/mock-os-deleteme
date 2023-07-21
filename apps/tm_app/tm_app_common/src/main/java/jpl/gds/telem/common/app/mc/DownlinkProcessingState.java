package jpl.gds.telem.common.app.mc;

/**
 * Enum describing the different possible processing states of downlink.
 * Attempts to be in compliance with SLE processing states.
 *
 *
 */
public enum DownlinkProcessingState {
    // @formatter: off
    /** Downlink disconnected to telemetry input source */
    UNBOUND,

    /** Downlink connected to telemetry input source, but not processing */
    BOUND,

    /** Downlink connected to telemetry input source and processing */
    STARTED,

    /** Downlink connected to telemetry input source and temporarily paused */
    PAUSED,

    /** This should never be returned, however if it is, we are truly in an unexpected state */
    UNKNOWN;
    // @formatter: on
}
