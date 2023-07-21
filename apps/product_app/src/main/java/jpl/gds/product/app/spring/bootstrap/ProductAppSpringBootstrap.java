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
package jpl.gds.product.app.spring.bootstrap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.product.api.ProductApiBeans;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.decom.IProductDecomUpdater;
import jpl.gds.product.api.decom.receng.IRecordedEngProductDecom;
import jpl.gds.product.app.decom.ReferenceProductDecom;
import jpl.gds.product.app.decom.receng.MultimissionRecordedEngineeringDecom;
import jpl.gds.shared.log.TraceManager;

/**
 * 
 * Spring bootstrap configuration class for the product modules.
 *
 */
@Configuration
public class ProductAppSpringBootstrap {

    @Autowired
    private ApplicationContext appContext;

    /**
     * Creates a prototype product decom bean.
     * 
     * @return the product decom bean.
     */
    @Bean(name = ProductApiBeans.PRODUCT_DECOM)
    @Scope("prototype")
    @Lazy(value = true)
    public IProductDecomUpdater createProductDecom() {
        return new ReferenceProductDecom(appContext);
    }
    
    /**
     * Creates a prototype recorded engineering product decom bean.
     * 
     * @return recorded product properties bean
     *
     * @param useDatabase
     *            whether or not to use the database
     * @param useGlobalLad
     *            whether or not to use globallad
     * @param useJms
     *            whether or not to use jms
     * @param dictDir
     *            the dictinary directory
     * @param dictVer
     *            the dictionary version
     */
   @Bean(name=ProductApiBeans.RECORDED_ENG_PRODUCT_DECOM) 
   @Scope("prototype")
   @Lazy(value = true)
   public IRecordedEngProductDecom createRecordedEngProductDecom(final boolean useDatabase,
           final boolean useGlobalLad, final boolean useJms, final String dictDir, final String dictVer) {
       try {
        return new MultimissionRecordedEngineeringDecom(appContext, useDatabase, useGlobalLad, useJms, dictDir, dictVer);
    } catch (final ProductException e) {
            TraceManager.getDefaultTracer(appContext).error("Unable to create IRecordedEngProductDecom bean");
        return null;
    }
   }
}
