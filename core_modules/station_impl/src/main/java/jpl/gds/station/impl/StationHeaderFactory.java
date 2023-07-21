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
package jpl.gds.station.impl;

import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationHeaderFactory;
import jpl.gds.station.api.IStationTelemHeaderUpdater;
import jpl.gds.station.api.dsn.chdo.ChdoConfigurationException;
import jpl.gds.station.api.dsn.chdo.IChdoConfiguration;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;
import jpl.gds.station.api.sle.ISleHeader;
import jpl.gds.station.api.sle.annotation.ISlePrivateAnnotation;
import jpl.gds.station.impl.dsn.chdo.ChdoSfdu;
import jpl.gds.station.impl.earth.LeotHeader;
import jpl.gds.station.impl.sle.SleHeader;
import org.springframework.context.ApplicationContext;

/**
 * A factory that creates station headers, including LEOT and CHDO SFDU headers.
 * 
 *
 * @since R8
 *
 */
public class StationHeaderFactory implements IStationHeaderFactory {

    private final ApplicationContext appContext;

    /**
     * Constructor.
     * 
     * @param appContext the current application context
     */
    public StationHeaderFactory(final ApplicationContext appContext) {
        this.appContext = appContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IChdoSfdu createChdoSfdu() throws ChdoConfigurationException {
        return new ChdoSfdu(appContext.getBean(IChdoConfiguration.class));
    } 

    /**
     * {@inheritDoc}
     */
    @Override
    public IStationTelemHeaderUpdater createLeotHeader(final int dataSize, final IAccurateDateTime ert) {
        return new LeotHeader(dataSize, ert);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IStationTelemHeaderUpdater createLeotHeader() {
        return new LeotHeader();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ISleHeader createSleHeader() {
        return new SleHeader(appContext.getBean(ISlePrivateAnnotation.class));
    }

}
