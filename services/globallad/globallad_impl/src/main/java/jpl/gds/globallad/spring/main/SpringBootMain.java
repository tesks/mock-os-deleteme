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
package jpl.gds.globallad.spring.main;

import jpl.gds.common.error.ErrorCode;
import jpl.gds.shared.exceptions.ExceptionTools;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Main configuration with main method.
 */
@SpringBootApplication(exclude = { ErrorMvcAutoConfiguration.class})
@EnableSwagger2
@ComponentScan(basePackages = { "jpl.gds.globallad",
        "jpl.gds.security.spring.bootstrap",
        "jpl.gds.shared.spring.bootstrap",
        "jpl.gds.jms.spring.bootstrap",
        "jpl.gds.common.spring.bootstrap",
        "jpl.gds.context.**.spring.bootstrap",
        "jpl.gds.eha.**.spring.bootstrap",
        "jpl.gds.evr.**.spring.bootstrap",
        "jpl.gds.dictionary.**.spring.bootstrap",
        "jpl.gds.message.**.spring.bootstrap"}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE))
public class SpringBootMain {
    /**
     * @param args
     *            the command line arguments
     */
	public static void main(final String[] args) {
        try {
            final SpringApplication app = new SpringApplication(SpringBootMain.class);
            app.setBannerMode(Banner.Mode.OFF);
            // MPCS-11493 - Allow bean override
            app.setAllowBeanDefinitionOverriding(true);
            final ConfigurableApplicationContext ctx = app.run(args);
            ctx.registerShutdownHook();

        }
        catch (final Throwable t) {
            ExceptionTools.handleSpringBootStartupError(t);
            System.exit(ErrorCode.UNKNOWN_ERROR_CODE.getNumber());
        }
	}

    /**
     * Error page
     * @return WebServerFactoryCustomizer
     */
	@Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory>  exceptionHandling() {
        return container -> container.addErrorPages(new ErrorPage("/error.html"));
    }
}
