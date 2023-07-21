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
package jpl.gds.telem.process.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

/**
 * Swagger Auto-Documenation Configuration
 * 
 */
@Configuration
@EnableSwagger2
public class ProcessSwaggerConfiguration {
    /**
     * @return the Swagger Docket object
     */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2).apiInfo(apiInfo()).select()
                                                      .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfo("AMPCS Telemetry Processor with RESTful Monitor & Control",
                           "The Telemetry Processor RESTful API allows runtime monitoring and control of the AMPCS Downlink Process (chill_telem_process)",
                           "API TOS",
                           "Copyright 2006-2018. California Institute of Technology. ALL RIGHTS RESERVED U.S. Government sponsorship acknowledged.",
                           new Contact("MGSS for AMPCS Questions",
                                       "https://ammos.jpl.nasa.gov/files/ammos_catalog/AMMOS_Catalog-V5_1_Public_Release.pdf",
                                       "ampcs-questions@jpl.nasa.gov"),
                           "California Institute of Technology/Jet Propulsion Laboratory", "https://jpl.nasa.gov",
                           Collections.emptyList());
    }
}
