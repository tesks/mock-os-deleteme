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
package jpl.gds.db.api.types;

import jpl.gds.shared.time.IAccurateDateTime;

public interface IDbFrameProvider extends IDbQueryable, IDbRawData {

    /**
     * Get type.
     *
     * @return Type
     */
    String getType();

    /**
     * Get VCFC.
     *
     * @return VCFC
     */
    Integer getVcfc();

    /**
     * Get is-bad status.
     *
     * @return is-bad status
     */
    boolean getIsBad();

    /**
     * Get VCID.
     *
     * @return VCID
     */
    Integer getVcid();

    /**
     * Get RCT.
     *
     * @return RCT
     */
    IAccurateDateTime getRct();

    /**
     * Get fill frame.
     *
     * @return fill frame
     */
    boolean getFillFrame();

    /**
     * Get relay S/C id.
     *
     * @return Relay S/C id
     */
    Integer getRelaySpacecraftId();

    /** Get SLE Metatada
     * @return SLE metadata
     */
    String getSleMetadata();
}