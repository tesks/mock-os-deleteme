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
package jpl.gds.cfdp.processor.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * {@code CfdpProcessorControllersConfig} is a Spring Web MVC configurer, to apply certain non-default settings to
 * the CFDP Processor controllers.
 *
 * @since 8.2
 */
@Configuration
// MPCS-11189  - 09/13/19 : chill_cfdp_processor should host the chill_cfdp Web GUI
// Removed annotation @EnableWebMvc so we can have the CFDP processor host the GUI code
// This annotation does not generally mix well with Spring Boot. It disables the
// Spring Boot auto configuration and in most cases is not necessary. The non-default
// settings configured here should work without this annotation.
public class CfdpProcessorControllersConfig extends WebMvcConfigurerAdapter {

    @Override
    public void configureContentNegotiation(final ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(false).
                favorParameter(false).
                ignoreAcceptHeader(false).
                defaultContentType(APPLICATION_JSON);
    }

}
