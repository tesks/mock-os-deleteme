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

package jpl.gds.tc.mps.impl.spring.bootstrap;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.impl.ContextIdentification;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.time.SclkScetConverter;
import jpl.gds.tc.api.*;
import jpl.gds.tc.api.cltu.ITcCltuBuilder;
import jpl.gds.tc.api.config.CltuProperties;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.PlopProperties;
import jpl.gds.tc.api.exception.CommandFileParseException;
import jpl.gds.tc.api.frame.ITcTransferFrameBuilder;
import jpl.gds.tc.api.frame.ITcTransferFrameParser;
import jpl.gds.tc.api.frame.ITcTransferFrameSerializer;
import jpl.gds.tc.api.frame.ITcTransferFramesBuilder;
import jpl.gds.tc.api.through.ITcThroughBuilder;
import jpl.gds.tc.impl.cltu.parsers.CltuBuilder;
import jpl.gds.tc.mps.impl.MpsTewUtility;
import jpl.gds.tc.mps.impl.cltu.parsers.IMpsCltuParser;
import jpl.gds.tc.mps.impl.cltu.parsers.MpsCltuParser;
import jpl.gds.tc.mps.impl.cltu.serializers.MpsTcCltuBuilder;
import jpl.gds.tc.mps.impl.cmd.MpsTcCommandReverser;
import jpl.gds.tc.mps.impl.ctt.CommandTranslationTable;
import jpl.gds.tc.mps.impl.frame.MpsTcTransferFramesBuilder;
import jpl.gds.tc.mps.impl.frame.parsers.MpsTcTransferFrameBuilder;
import jpl.gds.tc.mps.impl.frame.parsers.MpsTcTransferFrameParser;
import jpl.gds.tc.mps.impl.frame.serializers.MpsTcTransferFrameSerializer;
import jpl.gds.tc.mps.impl.properties.MpsTcProperties;
import jpl.gds.tc.mps.impl.scmf.MpsTcScmfWriter;
import jpl.gds.tc.mps.impl.through.MpsTcThroughBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

import static jpl.gds.tc.api.TcApiBeans.*;
import static jpl.gds.tc.mps.impl.TcMpsImplBeans.MPS_TC_PROPERTIES;
import static jpl.gds.tc.mps.impl.TcMpsImplBeans.TC_THROUGH_BUILDER;

/**
 * Spring bootstrap for MPS TEW utility.
 *
 */
@Configuration
public class MpsTewSpringBootstrap {

    public static final String COMMAND_TRANSLATION_TABLE = "COMMAND_TRANSLATION_TABLE";

    /**
     * Spring configuration for {@code MpsTcProperties} bean.
     *
     * @return {@code MpsTcProperties} bean
     */
    @Bean(MPS_TC_PROPERTIES)
    @Lazy
    public MpsTcProperties getMpsTcProperties() {
        return new MpsTcProperties();
    }

    /**
     * MPS TEW Utility bean.
     *
     * @param appContext spring application context
     * @return MPS TEW utility bean
     * @throws CommandFileParseException command file parse exception
     * @throws DictionaryException       dictionary exception
     */
    @Bean(TcApiBeans.MPS_TEW_UTILITY)
    @Lazy
    @Primary
    public ITewUtility getMpsUtility(final ApplicationContext appContext) throws
            CommandFileParseException,
            DictionaryException {
        return new MpsTewUtility.Builder(appContext).build();
    }

    @Bean(COMMAND_TRANSLATION_TABLE)
    @Lazy
    public CommandTranslationTable getCommandTranslationTable(final ApplicationContext appContext,
                                                              final DictionaryProperties dictProp,
                                                              final IContextConfiguration contextConfig) throws
            CommandFileParseException,
            DictionaryException {
        final String cmdDictPath  = dictProp.findFileForSystemMission(DictionaryType.COMMAND);
        final int    scid         = contextConfig.getContextId().getSpacecraftId();
        final String sclkscetPath = SclkScetConverter.getSclkScetFilePath(scid);

        return new CommandTranslationTable(cmdDictPath, sclkscetPath, scid, appContext);
    }

    @Bean(name = TcApiBeans.MPS_COMMAND_FRAME_SERIALIZER)
    @Scope("singleton")
    @Lazy
    @Primary
    public ITcTransferFrameSerializer getTcTransferFrameSerializer() {
        return new MpsTcTransferFrameSerializer();
    }

    /**
     * CLTU parser bean
     *
     * @param missionProperties mission properties
     * @param plopProperties    PLOP properties
     * @param cltuProperties    CLTU properties
     * @return cltu parser implementation
     */
    @Bean(TcApiBeans.MPS_CLTU_PARSER)
    @Lazy
    @Primary
    public IMpsCltuParser getCltuParser(final MissionProperties missionProperties,
                                        final PlopProperties plopProperties, final CltuProperties cltuProperties) {
        CltuBuilder.setSequences(cltuProperties, plopProperties);
        return new MpsCltuParser(missionProperties.getDefaultScid(), cltuProperties.getStartSequence(), cltuProperties.getTailSequence());
    }



    /**
     * Spring configuration for {@code ITcThroughBuilder} bean.
     *
     * @return {@code ITcThroughBuilder} bean implemented by this module
     */
    @Bean(TC_THROUGH_BUILDER)
    @Scope("prototype")
    @Lazy
    @Primary
    public ITcThroughBuilder getTcThroughBuilder(final ApplicationContext appContext, final CommandTranslationTable commandTranslationTable) {
        return new MpsTcThroughBuilder(appContext).setCommandTranslationTable(commandTranslationTable);
    }

    /**
     * Spring configuration for {@code ITcTransferFramesBuilder} bean.
     *
     * @return {@code ITcTransferFramesBuilder} bean implemented by this module
     */
    @Bean(TC_TRANSFER_FRAMES_BUILDER)
    @Scope("prototype")
    @Lazy
    @Primary
    public ITcTransferFramesBuilder getTcTransferFramesBuilder() {
        return new MpsTcTransferFramesBuilder();
    }

    @Bean(MPS_TC_TRANSFER_FRAME_BUILDER)
    @Scope("prototype")
    @Lazy
    @Primary
    public ITcTransferFrameBuilder getTcTransferFrameBuilder(final ApplicationContext appContext) {
        return new MpsTcTransferFrameBuilder().setFrameConfig(appContext.getBean(CommandFrameProperties.class));
    }

    /**
     * Spring configuration for {@code ITcScmfWriter} bean.
     *
     * @return {@code ITcScmfWriter} bean implemented by this module
     */
    @Bean(TC_SCMF_WRITER)
    @Scope("prototype")
    @Lazy
    public ITcScmfWriter getTcScmfWriter(final ApplicationContext appContext) {
        return new MpsTcScmfWriter(appContext);
    }

    /**
     * Spring configuration for {@code ITcCommandReverser} bean.
     *
     * @return {@code ITcCommandReverser} bean implemented by this module
     */
    @Bean(TC_COMMAND_REVERSER)
    @Scope("prototype")
    @Lazy
    public ITcCommandReverser getTcCommandReverser(final ApplicationContext appContext) throws CommandFileParseException {
        return new MpsTcCommandReverser(appContext);
    }


    @Bean("CLTU_BUILDER")
    @Scope("prototype")
    @Lazy
    public ITcCltuBuilder getCltuBuilder(final ContextIdentification contextId) {
        return new MpsTcCltuBuilder(contextId.getSpacecraftId());
    }

    @Bean(MPS_COMMAND_FRAME_PARSER)
    @Scope("prototype")
    @Lazy
    public ITcTransferFrameParser getTcParser(final MissionProperties missionProperties,
                                              final CommandFrameProperties commandFrameProperties) {
        final MpsTcTransferFrameParser parser = new MpsTcTransferFrameParser(missionProperties.getDefaultScid());
        parser.setFecLength(commandFrameProperties.getFecfLength());

        return parser;
    }

}