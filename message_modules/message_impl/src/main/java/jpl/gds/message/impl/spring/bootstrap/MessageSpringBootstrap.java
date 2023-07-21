package jpl.gds.message.impl.spring.bootstrap;

import jpl.gds.message.api.external.IExternalMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.common.config.GeneralProperties;
import jpl.gds.message.api.IInternalBusPublisher;
import jpl.gds.message.api.MessageApiBeans;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.message.api.handler.IQueuingMessageHandler;
import jpl.gds.message.api.spill.ISpillProcessor;
import jpl.gds.message.api.spill.SpillProcessorException;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.message.api.util.MessageCaptureHandler;
import jpl.gds.message.impl.InternalBusPublisher;
import jpl.gds.message.impl.handlers.DisruptorQueuingMessageHandler;
import jpl.gds.message.impl.spill.SpillProcessor;
import jpl.gds.message.impl.status.ClientHeartbeatMessage;
import jpl.gds.message.impl.status.StatusMessageFactory;
import jpl.gds.shared.config.JndiProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.PublishableLogMessage;
import jpl.gds.shared.message.RegisteredMessageConfiguration;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

import java.io.Serializable;
import java.util.concurrent.BlockingQueue;

/**
 * Spring bootstrap configuration class for the message projects.
 */
@Configuration
public class MessageSpringBootstrap {
    
    @Autowired
    ApplicationContext appContext;
    
    /**
     * Constructor.
     */
    public MessageSpringBootstrap() {
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CommonMessageType.StartOfData,
                PublishableLogMessage.XmlParseHandler.class.getName(), null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CommonMessageType.ClientHeartbeat,
                ClientHeartbeatMessage.XmlParseHandler.class.getName(), null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CommonMessageType.EndOfData,
                PublishableLogMessage.XmlParseHandler.class.getName(), null));
    }
    
    /**
     * Gets the singleton MessageServiceConfiguration bean. Autowiring causes
     * the GeneralProperties object to be created and loaded the first time this
     * is invoked. This method will also attempt to instantiate a JndiProperties
     * bean.  It should get one if an only if the current message service 
     * implementation has defined one in the context.  If a JndiProperties instance
     * cannot be created, then the MessageServiceConfiguration bean will be 
     * created without one.
     * 
     * @param genProps
     *            the current GeneralProperties bean, autowired
     * @return MessageServiceConfiguration bean
     */
    @Bean(name = MessageApiBeans.MESSAGE_SERVICE_CONFIG)
    @Scope("singleton")
    @Lazy(value = true)
    public MessageServiceConfiguration getMessageServiceConfiguration(
            final GeneralProperties genProps) {
        JndiProperties jndiProps = null;
        try {
            jndiProps = appContext.getBean(JndiProperties.class);
        } catch (final Exception e) {
            TraceManager.getTracer(appContext, Loggers.CONFIG)
                    .warn("JndiProperties could not be instantiated in MessageSpringBootstrap");

        }
        return new MessageServiceConfiguration(genProps, jndiProps);
    }

    /**
     * Creates the singleton IStatusMessageFactory bean.
     * 
     * @return IStatusMessageFactory bean
     */
    @Bean(name = MessageApiBeans.STATUS_MESSAGE_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IStatusMessageFactory getStatusMessageFactory() {
        return new StatusMessageFactory();
    }
    
    /**
     * Creates a prototype IQueuingMessageHandler bean.
     * 
     * @param queueLen length of the message handler queue
     * 
     * @return IQueuingMessageHandler bean
     */
    @Bean(name = MessageApiBeans.QUEUING_MESSAGE_HANDLER)
    @Scope("prototype")
    @Lazy(value = true)
    public IQueuingMessageHandler getQueuingMessageHandler(final int queueLen) {
        return new DisruptorQueuingMessageHandler(appContext, queueLen);
    }

    /**
     * Gets the singleton message capture handler utility bean.
     * 
     * @return message capture handler bean
     */
    @Bean(name=MessageApiBeans.MESSAGE_CAPTURE_HANDLER) 
    @Scope("singleton")
    @Lazy(value = true)
    public MessageCaptureHandler getMessageCaptureHandler(){
        return new MessageCaptureHandler(appContext);
    }

    /**
     * Gets prototype SpillProcessor bean
     *
     * @param outputDir
     *            Root output directory for spill files
     * @param targetQueue
     *            Queue of messages to spill from. Used to unspill messages from
     *            the spill file as well as size checks. Extends the
     *            targetQueue's size.
     * @param quota
     *            Quota for target queue Max.
     * @param enable
     *            True to enable spill processing.
     * @param clss
     *            Class type used in spill serialization.
     * @param name
     *            Topic name for spill processing.
     * @param keep
     *            Keep spill files if true.
     * @param timeout
     *            Wait to poll from the queue for up to this time.
     * @param trace
     *            Custom tracer or null for JmsFastTracer.
     * @param sseFlag
     *            The SSE context flag
     * @throws SpillProcessorException
     *             Thrown if an error occurs in constructing the spill processor.
     *
     * @return ISpillProcessor bean
     */

    @Bean(name = MessageApiBeans.SPILL_PROCESSOR)
    @Scope("prototype")
    @Lazy
    public ISpillProcessor<Serializable> getSpillProcessor(final String outputDir,
                                                           final BlockingQueue<Serializable> targetQueue,
                                                           final int quota,
                                                           final boolean enable,
                                                           final Class<Serializable> clss,
                                                           final String name,
                                                           final boolean keep,
                                                           final long timeout,
                                                           final Tracer trace,
                                                           final SseContextFlag sseFlag)
            throws MessageServiceException {
        try{
            return new SpillProcessor<>(outputDir, targetQueue, quota, enable, clss, name, keep, timeout, trace, sseFlag);
        }
        catch (final SpillProcessorException spe) {
            /*
             * This is being mapped to a JMS exception for the sake of higher level handling,
             * but isn't really a JMS error per se. It is important that we know the exact
             * source of the error at this point, so I have added the stack trace.
             */
            TraceManager.getTracer(appContext, Loggers.JMS).error(ExceptionTools.getMessage(spe), spe);
            final MessageServiceException temp = new MessageServiceException("Couldn't start spill processing", spe);
            temp.initCause(spe);
            throw temp;
        }
    }

    /**
     * Gets prototype InternalBusPublisher bean
     *
     * @param spillProc A spill processor bean
     * @param tracer  Tracer for logging
     * @param bus A message publication bus bean
     * @param messageUtil An external message utility bean
     *
     * @return IInternalBusPublisher bean
     */
    @Bean(name = MessageApiBeans.INTERNAL_BUS_PUBLISHER)
    @Scope("prototype")
    @Lazy
    public IInternalBusPublisher getInternalBusPublisher(final ISpillProcessor<IExternalMessage> spillProc,
                                                         final Tracer tracer,
                                                         final IMessagePublicationBus bus,
                                                         final IExternalMessageUtility messageUtil){
        return new InternalBusPublisher(spillProc, tracer, bus, messageUtil);
    }

}
