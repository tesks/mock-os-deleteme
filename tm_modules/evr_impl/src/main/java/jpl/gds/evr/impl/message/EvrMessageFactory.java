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
package jpl.gds.evr.impl.message;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.message.IEvrMessage;
import jpl.gds.evr.api.message.IEvrMessageFactory;

/**
 * A factory for creating messages in the EVR modules.
 * 
 *
 * @since R8
 */
public class EvrMessageFactory implements IEvrMessageFactory {

    private final MissionProperties missionProperties;

    public EvrMessageFactory(final MissionProperties missionProperties) {
        this.missionProperties = missionProperties;
    }
    /**
     * @{inheritDoc}
     * @see jpl.gds.evr.api.message.IEvrMessageFactory#createEvrMessage(jpl.gds.evr.api.IEvr)
     */
    @Override
    public IEvrMessage createEvrMessage(final IEvr evr) {
        return new EvrMessage(evr, missionProperties);
    }
}
