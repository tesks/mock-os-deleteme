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
package jpl.gds.tc.api.frame;

/**
 * {@code FrameErrorControlFieldAlgorithm} is an enumeration of different algorithms used for the Frame Error Control
 * Field in TC transfer frames.
 *
 * @since 8.2.0
 */
public enum FrameErrorControlFieldAlgorithm {

    /**
     * EACSUM55AA algorithm
     */
    EACSUM55AA("EEACSUM55"),

    /**
     * Originated as IBM “SDLC”
     */
    CRC16CCITT("CRC16");

    private String simpleName;

    FrameErrorControlFieldAlgorithm(String simpleName) {
        this.simpleName = simpleName;
    }

    public String getSimpleName() {
        return this.simpleName;
    }

    public static FrameErrorControlFieldAlgorithm getAlgorithmFromPropertyValue(String propVal) {
        for(FrameErrorControlFieldAlgorithm algo : FrameErrorControlFieldAlgorithm.values()) {
            if(propVal.equalsIgnoreCase(algo.simpleName) || propVal.equalsIgnoreCase(algo.toString())) {
                return algo;
            }
        }

        throw new IllegalArgumentException("Provided property String value does is not valid for any FrameErrorControlFieldAlgorithm value");
    }

}