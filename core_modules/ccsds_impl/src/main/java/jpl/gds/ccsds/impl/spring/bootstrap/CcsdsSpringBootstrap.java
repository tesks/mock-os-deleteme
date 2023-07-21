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
package jpl.gds.ccsds.impl.spring.bootstrap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.ccsds.api.CcsdsApiBeans;
import jpl.gds.ccsds.api.cfdp.ICfdpPduFactory;
import jpl.gds.ccsds.api.config.CcsdsProperties;
import jpl.gds.ccsds.api.tm.packet.ISecondaryHeaderExtractorFactory;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderLookup;
import jpl.gds.ccsds.impl.cfdp.CfdpPduFactory;
import jpl.gds.ccsds.impl.tm.packet.SecondaryHeaderExtractorFactory;
import jpl.gds.ccsds.impl.tm.packet.SecondaryPacketHeaderLookup;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.algorithm.AlgorithmConfig;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.TimeProperties;

/**
 * Spring bootstrap configuration class for beans in the CCSDS project.
 * 
 * @since R8
 *
 */
@Configuration
public class CcsdsSpringBootstrap {
    
    @Autowired
    ApplicationContext appContext;
     
    /**
     * Gets the singleton CCSDS properties bean.
     * 
     * @param sseFlag
     *            The SSE context flag
     * 
     * @return CcdsProperties bean
     */
    @Bean(name=CcsdsApiBeans.CCSDS_PROPERTIES) 
    @Scope("singleton")
    @Lazy(value = true)
    public CcsdsProperties getCcsdsProperties(final SseContextFlag sseFlag) {
        return new CcsdsProperties(sseFlag);
    }

    /**
     * Gets the singleton secondary packet header lookup bean. Autowiring will
     * cause the algorithm configuration, and APID dictionary
     * beans to be created and loaded the first time this is called. This
     * method will tolerate lack of an apid dictionary entirely, but not an APID
     * dictionary that cannot be loaded. The TimeProperties singleton will
     * also be loaded.
     * 
     * @param algoConfig the current AlgorithmConfig bean, autowired
     * 
     * @return SecondaryPacketHeaderLookup bean
     */
    @Bean(name=CcsdsApiBeans.SECONDARY_PACKET_HEADER_LOOKUP) 
    @Scope("singleton")
    @Lazy(value = true)
    public ISecondaryPacketHeaderLookup getSecondaryPacketHeaderLookup(final AlgorithmConfig algoConfig) {
        
        final DictionaryProperties dictConfig = appContext.getBean(DictionaryProperties.class);
        IApidDefinitionProvider apidDefs = null;
        try {
            dictConfig.findFileForSystemMission(DictionaryType.APID);
            apidDefs = appContext.getBean(IApidDefinitionProvider.class);
        } catch (final DictionaryException e) {
            TraceManager.getTracer(appContext, Loggers.DEFAULT).debug(
                    "APID dictionary file was not found in CcsdsSpringBootstrap.getSecondaryPacketHeaderLookup().");
            TraceManager.getTracer(appContext, Loggers.DEFAULT)
                    .debug("Default secondary header format assumed for telemetry packets.");
        }
        
    	return new SecondaryPacketHeaderLookup(TimeProperties.getInstance(), algoConfig, apidDefs);
    	
    }
    
    /**
     * Gets the singleton CFDP PDU Factory.
     * 
     * @return ICfdpPduFactory instance
     */
    @Bean(name=CcsdsApiBeans.CFDP_PDU_FACTORY) 
    @Scope("singleton")
    @Lazy(value = true)
    public ICfdpPduFactory getCfdpPduFactory() {
        return new CfdpPduFactory();
    }
    
    /**
     * Gets the singleton Secondary Header Extractor Factory.
     * 
     * @return ISecondaryHeaderExtractorFactory instance
     */
    @Bean(name=CcsdsApiBeans.SECONDARY_HEADER_EXTRACTOR_FACTORY) 
    @Scope("singleton")
    @Lazy(value = true)
    public ISecondaryHeaderExtractorFactory getSecondaryHeaderExtractorFactory() {
        return new SecondaryHeaderExtractorFactory();
    }
    
    
}
