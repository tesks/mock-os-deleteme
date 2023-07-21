/*
 * Copyright 2006-2020. California Institute of Technology.
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
package jpl.gds.product.automation.context.spring.bootstrap;


import jpl.gds.common.service.telem.ITelemetryFeatureManager;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.mapper.IFswToDictionaryMapper;
import jpl.gds.dictionary.mapper.ReferenceFswToDictionaryMapper;
import jpl.gds.file.PdppReferenceProductFilenameBuilderFactory;
import jpl.gds.product.PdppApiBeans;
import jpl.gds.product.api.file.IProductFilenameBuilderFactory;
import jpl.gds.product.api.file.IProductMetadataBuilder;
import jpl.gds.product.automation.*;
import jpl.gds.product.automation.checkers.ReferencePdppMnemonic;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationProductDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationUserDAO;
import jpl.gds.product.automation.hibernate.gui.AncestorMap;
import jpl.gds.product.context.*;
import jpl.gds.product.processors.*;
import jpl.gds.product.utilities.file.ReferenceProductMetadataBuilder;
import jpl.gds.shared.exceptions.ExceptionTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import java.io.File;
import java.util.Arrays;

/**
 * Class PdppArbiterSpringBootstrap
 *
 */
@Configuration
public class PdppArbiterSpringBootstrap {

    @Autowired
	private ApplicationContext appContext;

    @Bean(name=PdppApiBeans.ANCESTOR_MAP)
    @Scope("singleton")
    @Lazy(value = true)
    public AncestorMap getAncestorMap(final ProductAutomationProductDAO productDao, final ProductAutomationUserDAO userDao) {
    	return new AncestorMap(productDao, userDao);
    }

    @Bean(name=PdppApiBeans.PDPP_CONTEXT_CACHE)
    @Scope("singleton")
    @Lazy(value = true)
    public IPdppContextCache getPdppContextCache() {
    	return new PdppContextCache(appContext);
    }

    @Bean(name=PdppApiBeans.SESSION_FETCHER)
    @Scope("singleton")
    @Lazy(value = true)
    public IPdppSessionFetcher getPdppSessionFetcher() {
        return new PdppSessionFetcher(appContext);
    }

    @Bean(name=PdppApiBeans.AUTOMATION_PROCESS_CACHE)
    @Scope("singleton")
    @Lazy(value = true)
    public IProductAutomationProcessCache getProcessCache(final IContextIdentification id) {
    	return new ProductAutomationProcessCache(id.getHost(), appContext);
    }

    @Bean(name= PdppApiBeans.AUTOMATION_DOWNLINK_SERVICE)
    @Lazy
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public IAutomationDownlinkService getAutomationDownlinkService() {
        return new AutomationDownlinkService(appContext);
    }

    @Bean(name=PdppApiBeans.DICTIONARY_MAPPER)
    @Scope("singleton")
    @Lazy(value = true)
    public IFswToDictionaryMapper getFswToDictionaryMapper(final DictionaryProperties dictConfig) throws DictionaryException {

        try{
            String filePath = null;

            try{
                filePath = dictConfig.findFileForSystemMission(DictionaryType.MAPPER);
            }
            catch(final DictionaryException e){
                // Don't care, the mapper will use the default.
            }

            final String f = filePath == null ? null : new File(filePath).getAbsolutePath();

            return new ReferenceFswToDictionaryMapper(f);

        } catch (final Exception e) {
            e.printStackTrace();
            throw new DictionaryException("Cannot create FSW to dictionary mapper adaptation: " + e.toString());
        }
    }

    @Bean(name = PdppApiBeans.PDPP_CONTEXT_CONTAINER_CREATOR)
    @Lazy(value = true)
    public IPdppContextContainerCreator getPdppContextContainerCreator() {
        return new ReferencePdppContextContainerCreator(appContext);
    }

    @Bean(name = PdppApiBeans.PDPP_PRODUCT_AUTOMATION_PRODUCT_ADDER)
    @Lazy(value = true)
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public IProductAutomationProductAdder getProductAutomationProductAdder() {
        return new ReferenceProductAutomationProductAdder(appContext, false);
    }

    @Bean
    @Lazy
    @Scope("singleton")
    public IPostDownlinkProductProcessor getPdppProcessor(final PostDownlinkProductProcessorOptions options,
                                                          final String mnemonic) {
        ProductAutomationProperties config = appContext.getBean(ProductAutomationProperties.class);
        if (!config.hasMappedMnemonic(mnemonic)) {
            throw new IllegalStateException(mnemonic + " is not one of the configured checkers: " + Arrays
                    .toString(config.getCheckOrder().toArray()));
        }
        try {
            if (ReferencePdppMnemonic.LOGGER.toString().equalsIgnoreCase(mnemonic)) {
                return new ConsoleLoggerProcessor(options, appContext);
            } else {
                throw new IllegalStateException("No defined processor for " + mnemonic);
            }
        } catch (Exception e) {
            throw new IllegalStateException(ExceptionTools.getMessage(e));
        }
    }

    @Bean(name = PdppApiBeans.AUTOMATION_FEATURE_MANAGER)
    @Scope("singleton")
    @Lazy
    public ITelemetryFeatureManager getAutomationFeatureManager() {
        return new AutomationFeatureManager();

    }

    /**
     * Creates the singleton product filename builder factory bean specific to PDPP.
     *
     * @return product filename builder factory bean
     */
    @Bean(name=PdppApiBeans.PDPP_PRODUCT_FILENAME_BUILDER_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IProductFilenameBuilderFactory createProductFilenameBuilderFactory() {
        return new PdppReferenceProductFilenameBuilderFactory(appContext);
    }

    /**
     * Creates the singleton product metadata builder bean.
     *
     * @return product metadata builder bean
     */
    @Bean(name=PdppApiBeans.PDPP_PRODUCT_METADATA_BUILDER)
    @Scope("singleton")
    @Lazy(value = true)
    public IProductMetadataBuilder createProductMetadataBuilder() {
        return new ReferenceProductMetadataBuilder(appContext);
    }
}
