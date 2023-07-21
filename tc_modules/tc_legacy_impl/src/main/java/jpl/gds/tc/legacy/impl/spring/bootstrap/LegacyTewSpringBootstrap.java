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

package jpl.gds.tc.legacy.impl.spring.bootstrap;

import jpl.gds.tc.api.ITewUtility;
import jpl.gds.tc.api.TcApiBeans;
import jpl.gds.tc.api.cltu.ICltuFactory;
import jpl.gds.tc.api.command.IFlightCommandTranslator;
import jpl.gds.tc.api.frame.ITcTransferFrameFactory;
import jpl.gds.tc.api.frame.ITcTransferFrameSerializer;
import jpl.gds.tc.api.scmf.IScmfBuilder;
import jpl.gds.tc.api.scmf.IScmfSerializer;
import jpl.gds.tc.api.session.ISessionBuilder;
import jpl.gds.tc.legacy.impl.LegacyTewUtility;
import jpl.gds.tc.legacy.impl.cltu.LegacyCltuFactory;
import jpl.gds.tc.legacy.impl.command.LegacyFlightCommandTranslator;
import jpl.gds.tc.legacy.impl.frame.LegacyTcTransferFrameFactory;
import jpl.gds.tc.legacy.impl.frame.LegacyTcTransferFrameSerializer;
import jpl.gds.tc.legacy.impl.scmf.LegacyScmfSerializer;
import jpl.gds.tc.legacy.impl.scmf.parsers.LegacyScmfBuilder;
import jpl.gds.tc.legacy.impl.session.SessionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

/**
 * Spring bootstrap for legacy (AMPCS) TEW implementation
 *
 */
@Configuration
public class LegacyTewSpringBootstrap {


    @Bean(TcApiBeans.LEGACY_COMMAND_TRANSLATOR)
    @Lazy
    @Primary
    public IFlightCommandTranslator getFlightCommandTranslator(final ApplicationContext appContext) {
        return new LegacyFlightCommandTranslator(appContext);
    }

    @Bean(name = TcApiBeans.TELECOMMAND_FRAME_FACTORY)
    @Lazy
    public ITcTransferFrameFactory getTelecommandFrameFactory(final ApplicationContext appContext) {
        return new LegacyTcTransferFrameFactory(appContext);
    }

    @Bean(name = TcApiBeans.LEGACY_COMMAND_FRAME_SERIALIZER)
    @Scope("singleton")
    @Lazy
    public ITcTransferFrameSerializer getTcTransferFrameSerializer(final ApplicationContext appContext) {
        return new LegacyTcTransferFrameSerializer(appContext);
    }

    @Bean(TcApiBeans.SCMF_SFDU_HEADER_SERIALIZER)
    @Lazy
    public IScmfSerializer getScmfSfduHeaderSerializer(final ApplicationContext appContext) {
        return new LegacyScmfSerializer(appContext);
    }

    @Bean(name = TcApiBeans.SESSION_BUILDER)
    @Lazy
    @Scope("prototype")
    public ISessionBuilder getSessionBuilder(final ApplicationContext appContext) {
        return new SessionBuilder(appContext);
    }

    @Bean(TcApiBeans.LEGACY_SCMF_BUILDER)
    @Scope("prototype")
    @Lazy
    public IScmfBuilder getScmfBuilder(final ApplicationContext appContext) {
        return new LegacyScmfBuilder(appContext);
    }

    @Bean(TcApiBeans.LEGACY_TEW_UTILITY)
    @Lazy
    public ITewUtility getTewUtility(final ApplicationContext appContext) {
        return new LegacyTewUtility(appContext);
    }

    @Bean(TcApiBeans.LEGACY_CLTU_FACTORY)
    @Lazy
    public ICltuFactory getLegacyCltuFactory(final ApplicationContext appContext) {
        return new LegacyCltuFactory(appContext);
    }

}
