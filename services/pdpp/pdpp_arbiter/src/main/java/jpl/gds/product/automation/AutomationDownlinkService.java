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
package jpl.gds.product.automation;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jpl.gds.product.automation.hibernate.IAutomationLogger;
import org.springframework.context.ApplicationContext;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.message.IPartialProductMessage;
import jpl.gds.product.api.message.IProductAssembledMessage;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.product.automation.disruptor.IAutomationDisruptorProducer;
import jpl.gds.product.automation.disruptor.ProductMetadataEvent;
import jpl.gds.product.automation.disruptor.ProductMetadataEventFactory;
import jpl.gds.product.automation.disruptor.ProductMetadataEventProducerWithTranslator;
import jpl.gds.session.message.EndOfSessionMessage;
import jpl.gds.session.message.SessionMessageType;
import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.performance.IPerformanceData;
import jpl.gds.shared.performance.IPerformanceProvider;
import jpl.gds.shared.performance.PerformanceSummaryPublisher;
import jpl.gds.shared.performance.QueuePerformanceData;

/**
 *
 * AutomationDownlinkService gets all product assembled and partial product messages and attempts to add them to the automation
 * database.  
 * 
 * MPCS-8179 - 06/07/16 - Added to AMPCS, updated from original version in MPCS for MSL G9.
 * MPCS-8295 - 11/28/16 - Changed the ProductAutomationProductAdder from a blocking queue to a Disruptor and added performance reporting.
 */

public class AutomationDownlinkService implements IAutomationDownlinkService {
    private final IAutomationLogger log;

	
	private MessageSubscriber subscriber;
	// MPCS-8295 - 11/28/16 - removed productQueue, renamed adderThread to
	// adderHandler and adderThreadClassName to adderHandlerClassName
	private IProductAutomationProductAdder adderHandler = null;
	
	// MPCS-8295 - 11/28/16 - added disruptor, executor, producer, exceptionHandler, disruptor performance, and perfReporter
	private Disruptor<ProductMetadataEvent> disruptor = null;
	private ExecutorService executor = null;
	private IAutomationDisruptorProducer<IProductMetadataProvider> producer = null;
	private final ApplicationContext context;
	private final IMessagePublicationBus bus;
	
	ExceptionHandler<ProductMetadataEvent> exceptionHandler = new ExceptionHandler<ProductMetadataEvent>(){
		
		// if any exception has bubbled up from an event handler the service needs to be shut down
		@Override
		public void handleEventException(final Throwable ex, final long sequence, final ProductMetadataEvent event) {
			log.error(ex.getMessage());
			
			/*
			 * MPCS-8568 01/04/17 - If the adder threw an exception, then
			 * everything just needs to quit.
			 */
			forceStop();
			
		}

		@Override
		public void handleOnStartException(final Throwable ex) {
			log.error("Failed to start ProductAutomationProductAdder. " + ex);
		}

		@Override
		public void handleOnShutdownException(final Throwable ex) {
			log.warn("Failed to stop ProductAutomationProductAdder. " + ex);
		}
		
	};
	
	QueuePerformanceData disruptorPerformance = null;
	PerformanceReporter perfReporter;
	
	/**
	 * default constructor
	 * MPCS-8295 - 11/28/16 - Set up the executor, disruptor, producer, and performance reporting
	 */
	public AutomationDownlinkService(final ApplicationContext context) {
		
		this.context = context;
        this.log = context.getBean(IAutomationLogger.class);
		bus = context.getBean(IMessagePublicationBus.class);
		
		executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("automation-downlink-service-thread").build());
		
		this.disruptor = new Disruptor<ProductMetadataEvent>(
				new ProductMetadataEventFactory(),
				1024,
				executor,
				ProducerType.SINGLE,
				new BlockingWaitStrategy());
		
		this.producer = new ProductMetadataEventProducerWithTranslator(disruptor.getRingBuffer());
		
		//start up performance reporting when the buffer is created
		disruptorPerformance = new QueuePerformanceData(this.context.getBean(PerformanceProperties.class)
				,"Automation Downlink Service", 1024, false, false, "Product Metadata Events");
		perfReporter = new PerformanceReporter();
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean startService() {
		// Add the subscriber that will add the products to the internal queue.
		boolean started = false;
		
		subscriber = new HibernateDownlinkProductAdderSubscriber();

		/**
		 *  - R8 refactor - Don't need the adaptor, just get the adder from the context.
		 */
		/*
		 * Followed process in Eha adapter factory. This way the ProductMissionAdapter does not need to know about
		 * the AutomationProductAdderClasses
		 */

		try{

			adderHandler = this.context.getBean(IProductAutomationProductAdder.class);
		}
		catch (final Exception e) {
			log.error("Product automation adder class was not found in context.");
		}

		// Check that the adderHelper is not null. 
		if (adderHandler != null) {

			//exception handler must be specified before event handler(s)
			this.disruptor.handleExceptionsWith(exceptionHandler);

			this.disruptor.handleEventsWith(adderHandler);

			this.disruptor.start();

			started = true;
		}

		return started;
	}

	@Override
	public void stopService() {
		bus.unsubscribeAll(subscriber);
		
		this.disruptor.shutdown();
		
		this.executor.shutdown();
		
		perfReporter.deregister();
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public void forceStop(){
		context.getBean(IMessagePublicationBus.class).unsubscribeAll(subscriber);

		perfReporter.deregister();
		
		disruptor.halt();
	}
	
	// MPCS-8295 12/2/16 - added updateDisruptorPerformanceData and getDisruptorPerformanceData
	
	/**
     * {@inheritDoc}
     */
	@Override
    public void updateDisruptorPerformanceData(){
		
		if (disruptorPerformance != null) {
			//can't get how full a ring buffer is, just the remaining capacity
			final long newSize = this.disruptor.getRingBuffer().getBufferSize() - this.disruptor.getRingBuffer().remainingCapacity();
			
			this.disruptorPerformance.setCurrentQueueSize(newSize);
			
			if (newSize > this.disruptorPerformance.getHighWaterMark()) {
				this.disruptorPerformance.setHighWaterMark(newSize);
			}
		}
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public List<IPerformanceData> getDisruptorPerformanceData(){
		if(disruptorPerformance != null){
			updateDisruptorPerformanceData();
			return Arrays.asList((IPerformanceData)disruptorPerformance);
		}
		return new LinkedList<IPerformanceData>();
	}
	

	private class HibernateDownlinkProductAdderSubscriber extends BaseMessageHandler {

		public HibernateDownlinkProductAdderSubscriber() {
			bus.subscribe(ProductMessageType.ProductAssembled, this);
			bus.subscribe(ProductMessageType.PartialProduct, this);
            bus.subscribe(SessionMessageType.EndOfSession, this);
		}
		
		/**
		 * MPCS-6468 -  8/2014 - Real time extraction flag is no longer used.  Also adding the
		 * is compressed flag so that all information required for categorizing required product processing
		 * is stored in the product table.
		 */
		
		@Override
		public void handleMessage(final IMessage message) {
			/*
			 * Just get the full path to the product and push into the queue. Stop the service when EndOfSessionMessage is received (no more products will be coming this way.
			 * If it is any other kind of message, this service shouldn't be subscribed to it. Just warn that somehow it got here and ignore it. 
			 */
			
			// MPCS-8295 - 11/28/16 - Pass metadata to the producer. It will transform and add the metadata to the disruptor ring buffer for processing
			if (message instanceof IProductAssembledMessage) {
				producer.onData(((IProductAssembledMessage) message).getMetadata());
			} else if (message instanceof IPartialProductMessage) {
				producer.onData(((IPartialProductMessage) message).getMetadata());
			} else if (message instanceof EndOfSessionMessage) {
				stopService();
			} else {
				log.warn("Got unsubscribed message: " + message.getType());
			}
			
			updateDisruptorPerformanceData();
		}
	}
	
	/**
	 * PerformanceReporter class, to allow performance data to be reported
	 * 
	 * MPCS-8295 - 12/02/16 - Added
	 */
	public class PerformanceReporter implements IPerformanceProvider{
		
		/** The Performance provider name */
		private static final String THIS_PROVIDER = "Automation Downlink Service";
		
		/**
		 * Constructor. Registers with the performance summary publisher for performance data requests.
		 */
		public PerformanceReporter(){
			context.getBean(PerformanceSummaryPublisher.class).registerProvider(this);
		}
		
		/**
		 * {@inheritDoc}
		 * 
		 * @see jpl.gds.shared.performance.IPerformanceProvider#getProviderName()
		 */
		@Override
		public String getProviderName() {
			return THIS_PROVIDER;
		}
		
		/**
		 * {@inheritDoc}
		 * 
		 * @see jpl.gds.shared.performance.IPerformanceProvider#getPerformanceData()
		 */
		@Override
		public List<IPerformanceData> getPerformanceData() {

			final List<IPerformanceData> perfList = new LinkedList<IPerformanceData>();
			perfList.addAll(AutomationDownlinkService.this.getDisruptorPerformanceData());
			
			return perfList;
		}
		
		/**
		 * De-registers with the performance summary publisher for performance data requests.
		 */
		public void deregister() {
			context.getBean(PerformanceSummaryPublisher.class).deregisterProvider(this);
		}
	}
}
