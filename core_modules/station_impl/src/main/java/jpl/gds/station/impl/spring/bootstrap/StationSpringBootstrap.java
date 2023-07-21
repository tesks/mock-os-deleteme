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
package jpl.gds.station.impl.spring.bootstrap;

import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.RegisteredMessageConfiguration;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.station.api.IStationHeaderFactory;
import jpl.gds.station.api.IStationInfoFactory;
import jpl.gds.station.api.IStationMessageFactory;
import jpl.gds.station.api.StationApiBeans;
import jpl.gds.station.api.StationMessageType;
import jpl.gds.station.api.dsn.chdo.ChdoConfigurationException;
import jpl.gds.station.api.dsn.chdo.IChdoConfiguration;
import jpl.gds.station.api.sle.annotation.ISlePrivateAnnotation;
import jpl.gds.station.impl.StationHeaderFactory;
import jpl.gds.station.impl.StationInfoFactory;
import jpl.gds.station.impl.StationMessageFactory;
import jpl.gds.station.impl.dsn.chdo.ChdoConfiguration;
import jpl.gds.station.impl.dsn.message.DsnMonitorMessage;
import jpl.gds.station.impl.sle.annotation.V3SlePrivateAnnotationHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

/**
 * Spring bootstrap configuration for the station modules.
 *
 * @since R8
 */
@Configuration
public class StationSpringBootstrap {

    @Autowired
    private ApplicationContext appContext;

    /**
     * Constructor.
     */
    public StationSpringBootstrap() {
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(StationMessageType.DsnStationMonitor,
                null, DsnMonitorMessage.BinaryParseHandler.class.getName()));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(StationMessageType.NenStationMonitor,
                null, null));
    }

    /**
     * Creates and loads the singleton CHDO configuration bean.
     *
     * @param sseFlag the SSE context flag
     * @return IChdoConfiguration bean
     * @throws ChdoConfigurationException if there is a problem loading the CHDO definition file
     */
    @Bean(name = StationApiBeans.CHDO_CONFIGURATION)
    @Scope("singleton")
    @Lazy(value = true)
    public IChdoConfiguration getChdoConfiguration(final SseContextFlag sseFlag) throws ChdoConfigurationException {
        return new ChdoConfiguration(sseFlag);
    }

    /**
     * Creates the singleton station header factory bean.
     *
     * @return IStationHeaderFactory bean
     */
    @Bean(name = StationApiBeans.STATION_HEADER_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IStationHeaderFactory getStationHeaderFactory() {
        return new StationHeaderFactory(appContext);
    }

    /**
     * Creates the singleton station message factory bean.
     *
     * @return IStationMessageFactory bean
     */
    @Bean(name = StationApiBeans.STATION_MESSAGE_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IStationMessageFactory getStationMessageFactory() {
        return new StationMessageFactory();
    }

    /**
     * Creates the singleton station information factory bean.
     *
     * @return IStationInfoFactory bean
     */
    @Bean(name = StationApiBeans.STATION_INFO_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IStationInfoFactory getStationInfoFactory() {
        return new StationInfoFactory();
    }

    /**
     * Gets the singleton ISlePrivateAnnotation for handling private annotation
     *
     * @return ISlePrivateAnnotation private annotation handler
     */
    @Bean(name = StationApiBeans.SLE_PRIVATE_ANNOTATION)
    @Scope("prototype")
    @Lazy(value = true)
    public ISlePrivateAnnotation getSlePrivateAnnotationParser() {
        // skipping config at this point. V3 handler does both NO_OP and V3, config feels like
        // it falls into the YAGNI principle. If the DSN updates their spec, or it needs a different override strategy
        // for a mission not using the DSN, that will be the time to decide how to override.
        return new V3SlePrivateAnnotationHandler();
    }
}