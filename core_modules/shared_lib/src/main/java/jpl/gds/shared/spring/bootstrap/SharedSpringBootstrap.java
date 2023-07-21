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
package jpl.gds.shared.spring.bootstrap;

import jpl.gds.shared.config.WebGuiProperties;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.shared.algorithm.AlgorithmConfig;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.app.ICommandLineApp;
import jpl.gds.shared.cli.cmdline.AliasingApacheCommandLineParser;
import jpl.gds.shared.cli.cmdline.ICommandLineParser;
import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.log.ExternalTraceNotifier;
import jpl.gds.shared.log.GuiNotifier;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.Slf4jTracer;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.log.config.LoggingProperties;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.GenericPublicationBus;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.PublishableLogMessage;
import jpl.gds.shared.message.RegisteredMessageConfiguration;
import jpl.gds.shared.metadata.context.ContextKey;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.performance.PerformanceSummaryPublisher;
import jpl.gds.shared.spring.AnnotatedBeanLocator;
import jpl.gds.shared.spring.BeanUtil;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * Spring bootstrap configuration for shared library. Contains configuration for
 * (at minimum) shared configuration and properties beans, an internal message
 * bus bean, logging-related beans, and a performance publisher bean. Note that
 * there is a separate bootstrap for shared time-related beans.
 * 
 *
 * @since R8
 */
@Configuration
public class SharedSpringBootstrap {
    /**
     * Bean name for internal message publication bus.
     */
    public static final String PUBLICATION_BUS             = "PUBLICATION_BUS";

    /**
     * Bean name for performance properties object.
     */
    public static final String PERFORMANCE_PROPERTIES      = "PERFORMANCE_PROPERTIES";

    /**
     * Bean name for logging properties
     */
    public static final String LOGGING_PROPERTIES          = "LOGGING_PROPERTIES";

    /**
     * Bean name for performance summary publisher;
     */
    public static final String PERFORMANCE_PUBLISHER       = "PERFORMANCE_PUBLISHER";

    /**
     * Bean name for algorithm configuration.
     * 
     */
    public static final String ALGORITHM_CONFIG            = "ALGORITHM_CONFIG";

    /**
     * Bean name for context key.
     * 
     */
    public static final String CONTEXT_KEY                 = "CONTEXT_KEY";

    /**
     * Bean name for the GUI log message notifier
     */
    public static final String GUI_TRACE_NOTIFIER          = "GuiNotifier";

    /**
     * Bean name for the EXTERNAL log message notifier
     */
    public static final String EXTERNAL_TRACE_NOTIFIER     = "ExternalTraceNotifier";

    /**
     * Bean name for the command options bean
     */
    public static final String COMMAND_OPTIONS             = "CommandOptions";

    /**
     * Bean name for the command line parser bean
     */
    public static final String COMMAND_LINE_PARSER         = "CliParser";

    /**
     * Bean name for the BeanUtil helper bean
     */
    public static final String BEAN_UTIL               = "BeanUtil";

    /**
     * Bean name for the Tracer bean
     */
    public static final String TRACER                  = "Tracer";

    /**
     * Bean name for the SSE Context Flag
     */
    public static final String SSE_CONTEXT_FLAG        = "SseContextFlag";

    /**
     * Bean name for the Bean Locator
     */
    public static final String BEAN_LOCATOR            = "BeanLocator";

    /**
     * Bean name for the Web GUI Properties object 
     */
    public static final String WEB_GUI_PROPERTIES = "WEB_GUI_PROPERTIES";

    @Autowired
    private ApplicationContext appContext;

    /**
     * Constructor.
     */
    public SharedSpringBootstrap() {
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CommonMessageType.PerformanceSummary, 
                PublishableLogMessage.XmlParseHandler.class.getName(), null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CommonMessageType.Log,
                PublishableLogMessage.XmlParseHandler.class.getName(), null));
    }

    /**
     * Creates or returns the internal message publication bus bean. The
     * instance created here has no assigned header context.
     * 
     * @param key
     *            the current IContextKey object
     * 
     * @return IMessagePublicationBus bean
     */
    @Bean(name=PUBLICATION_BUS)
    @Scope("singleton")
    @Lazy(value = true)
    public IMessagePublicationBus getPublicationBus(final IContextKey key) {
        return new GenericPublicationBus(key, TraceManager.getTracer(appContext, Loggers.BUS));
    } 
    
    /**
     * Creates or returns the algorithm configuration properties bean.
     * When created, the underlying configuration resources are loaded.
     * 
     * @param sseFlag
     *            The current SSE context flag
     *
     * @return AlgorithmConfig bean
     */
    @Bean(name=ALGORITHM_CONFIG)
    @Scope("singleton")
    @Lazy(value = true)
    public AlgorithmConfig getAlgorithmConfig(final SseContextFlag sseFlag) {
        return new AlgorithmConfig(sseFlag, TraceManager.getTracer(appContext, Loggers.CONFIG));
    } 
    

    /**
     * Creates or returns the performance properties bean.
     * When created, the underlying configuration resources are loaded.
     * 
     * @param sseFlag
     *            The SSE context flag
     *
     * @return PerformanceProperties bean
     */
    @Bean(name=PERFORMANCE_PROPERTIES)
    @Scope("singleton")
    @Lazy(value = true)
    public PerformanceProperties getPerformanceProperties(final SseContextFlag sseFlag) {
        return new PerformanceProperties(sseFlag);
    }  
    
    /**
     * Creates or returns the logging properties bean.
     * 
     * No longer a lazy bean
     * 
     * @param sseFlag
     *            The SSE context flag
     * 
     * @return LoggingProperties bean
     */
    @Bean(name = LOGGING_PROPERTIES)
    @Scope("singleton")
    @Lazy(value = false)
    public LoggingProperties getLoggingProperties(final SseContextFlag sseFlag) {
        return new LoggingProperties(sseFlag);
    }

    /**
     * Creates or returns the singleton performance properties bean. When
     * created, the underlying configuration resources are loaded.
     * 
     * @param appContext
     *            The current application context
     * 
     * @return PerformanceProperties bean
     */
    @Bean(name=PERFORMANCE_PUBLISHER)
    @Scope("singleton")
    @Lazy(value = true)
    public PerformanceSummaryPublisher getPerformancePublisher(final ApplicationContext appContext) {
        return new PerformanceSummaryPublisher(appContext);
    }  

    /**
     * Gets a unique AnnotatedBeanLocator bean.
     * 
     * @param appContext
     *            The current application context
     * 
     * @return AnnotatedBeanLocator bean
     */
    @Bean(name = BEAN_LOCATOR)
    @Scope("prototype")
    @Lazy(value=true)
    public AnnotatedBeanLocator createBeanLocator(final ApplicationContext appContext) {
        return new AnnotatedBeanLocator(appContext, false);
    }
    
    
    /**
     * Gets the singleton IContextKey bean, which is the identifier for the current context.
     * 
     * @return IContextKey bean
     */
    @Bean(name=CONTEXT_KEY)
    @Scope("singleton")
    @Lazy(value=true)
    public IContextKey getContextKey() {
        return new ContextKey();
    }

    /**
     * Gets the GUI Notifier bean; applications must register this to route log
     * messages to the GUI
     * 
     * @return GUI Notifier
     */
    @Bean(name = GUI_TRACE_NOTIFIER)
    @Scope("singleton")
    @Lazy(value = true)
    public GuiNotifier getGuiTraceNotifier() {
        return new GuiNotifier();
    }

    /**
     * Gets the External Trace Notifier bean; applications must register this to
     * route log messages externally
     * 
     * @return External Trace Notifier
     */
    @Bean(name = EXTERNAL_TRACE_NOTIFIER)
    @Scope("singleton")
    @Lazy(value = true)
    public ExternalTraceNotifier getExternalTraceNotifier() {
        return new ExternalTraceNotifier();
    }
    
    /**
     * Gets the BaseCommandOptions bean. Note this is a prototype bean
     * because it has an argument. Use common sense. You only need one
     * command options object per application. Do not call this over and
     * over. Get the bean and save it.
     * 
     * @param app
     *            the ICommandLineApp the options are for
     * 
     * @return BaseCommandOptions
     * 
     */
    @Bean(name = COMMAND_OPTIONS)
    @Scope("prototype")
    @Lazy(value = true)
    @DependsOn(COMMAND_LINE_PARSER)
    public BaseCommandOptions getCommandOptions(final ICommandLineApp app) {
        return new BaseCommandOptions(app, true, appContext.getBean(ICommandLineParser.class));
    }
    
    /**
     * Gets the command line parser bean
     * 
     * @return CLI parser bean
     * 
     */
    @Bean(name = COMMAND_LINE_PARSER)
    @Scope("singleton")
    @Lazy(value = true)
    public ICommandLineParser getCliParser() {
        return new AliasingApacheCommandLineParser();
    }

    /**
     * Bean Utility helper for interacting with the application context
     * 
     * @return BeanUtil
     */
    @Bean(name = BEAN_UTIL)
    @Scope("singleton")
    @Lazy(value = false)
    public BeanUtil getBeanUtil() {
        return new BeanUtil();
    }

    /**
     * Tracer bean for Slf4j logging implementation
     * 
     * @param name
     *            <Loggers> Tracer to log with
     * @return <Tracer>
     */
    @Bean(name = TRACER)
    @Scope("prototype")
    @Lazy(value = true)
    public Tracer getTracer(final Loggers name) {
        return new Slf4jTracer(LoggerFactory.getLogger(name.toString()));
    }

    /**
     * Gets the current SSE context flag - whether or not an application is SSE
     * 
     * @return <SseContextFlag>
     */
    @Bean(name = SSE_CONTEXT_FLAG)
    @Scope("singleton")
    @Lazy(value = false)
    public SseContextFlag getSseContextFlag() {
        return new SseContextFlag();
    }


    /**
     * Gets the WebGuiProperties object
     *
     * @param sseFlag The SSE Context flag
     * @return WebGuiProperties object
     */
    @Bean(name = WEB_GUI_PROPERTIES)
    @Scope("singleton")
    @Lazy
    public WebGuiProperties getWebGuiProperties(SseContextFlag sseFlag) {
        return new WebGuiProperties(sseFlag);
    }

}