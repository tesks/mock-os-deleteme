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
package jpl.gds.watcher.responder.app;

import jpl.gds.common.options.ProductNameListOption;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.service.GlobalLadDownlinkService;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.session.message.SessionMessageType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.ExitWithSessionOption;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.types.ByteStream;
import jpl.gds.watcher.IResponderAppHelper;
import jpl.gds.watcher.WatcherProperties;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import java.util.*;

/**
 * This is the application helper class for a message responder that reacts to
 * complete products.
 * The handler logic seen below is used to call handle() on a product and display data about a
 * particular product.
 *      E.g. SamsMessageLogHandler or RecordedEngineeringProductHandler
 *
 *      This handle logic is not necessary (and expensive) if we want to just 'watch' for products
 *      without 'handling' anything. A new implementation of ProductWatcherApp will be used to
 *      'watch' products without performing a handle() operation.
 */
public class ProductHandlerApp implements IResponderAppHelper {
    /**
     * Interface to handle products.
     */
    public interface ProductHandler {
        /**
         * Get usage text.
         *
         * @return the product handler usage
         */
        public String getUsageText();

        /**
         * Handle product.
         *
         * @param metadata
         *            about the product
         * @param bytestream
         *            of product bytes expected to process
         * @throws ProductHandlerException
         *             if unable to handle the product
         */
        public void handle(IProductMetadataProvider metadata, ByteStream bytestream) throws ProductHandlerException;

        /**
         * Shutdown handler, close all JMS and other connections.
         */
        public void shutdown();
    }

    /**
     * Exception which represents that an error with processing product data
     * was encountered.
     */
    public static class ProductHandlerException extends Exception {

        /**
         * Default serialization id.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Default constructor.
         */
        public ProductHandlerException() {
            super();
        }

        /**
         * Constructor which takes a message.
         * 
         * @param arg0
         *            message
         */
        public ProductHandlerException(final String arg0) {
            super(arg0);
        }

        /**
         * Constructor which takes a message and an additional exception to
         * pass along.
         * 
         * @param arg0
         *            message
         * @param arg1
         *            additional exception
         */
        public ProductHandlerException(final String arg0, final Throwable arg1) {
            super(arg0, arg1);
        }

        /**
         * Constructor which takes an additional exception to pass along.
         * 
         * @param cause
         *            additional exception
         */
        public ProductHandlerException(final Throwable cause) {
            super(cause);
        }

    }

    /**
     * chill_product_watch application name.
     */
    private static final String               APP_NAME               = ApplicationConfiguration.getApplicationName(ResponderAppName.PRODUCT_WATCHER_APP_NAME.getAppName());
  
    private boolean                           exitWithContext      = false;
    private Set<String>                       productNames;

    protected long                            drainTime;
    private int                               queueLimit;

    /** Product types => handlers */
    private final Map<String, ProductHandler> productHandlerMap      = new HashMap<>();

    private boolean                           useProductTypePatterns = false;

    private IContextConfiguration              contextConfig;

    private GlobalLadDownlinkService          gladService;
    protected final ApplicationContext          appContext;
    private final ExitWithSessionOption exitOption  = new ExitWithSessionOption();
    // Use shared product name list option
    private final ProductNameListOption namesOption = new ProductNameListOption(true);
    
    private final SseContextFlag              sseFlag;

    private final Tracer                      log;

    /**
     * Constructor.
     * 
     * @param appContext
     *            the current application context
     */
    public ProductHandlerApp(final ApplicationContext appContext) {
        this.appContext = appContext;
        this.sseFlag = appContext.getBean(SseContextFlag.class);
        this.log = TraceManager.getDefaultTracer(appContext);
    }

    @Override
    public void addAppOptions(final BaseCommandOptions opt) {
        opt.addOption(exitOption);
        opt.addOption(namesOption);
    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        productNames = new HashSet<>(namesOption.parse(commandLine));
      
        // Now make sure there are handler classes defined for the
        // above products.

        final WatcherProperties rc = new WatcherProperties(ApplicationConfiguration.getApplicationName(), sseFlag);

        this.drainTime = rc.getQueueDrainTime();
        this.queueLimit = rc.getQueueLimit();

        final List<String> handlers = new ArrayList<>(productNames.size());

        for (final String pn : productNames) {
            try {
                handlers.add(rc.getHandlerNameForProductType(pn));
            }
            catch (final IllegalArgumentException mce) {
                // Problem with handler

                final ParseException pe = new ParseException(mce.getMessage());

                pe.initCause(mce);

                throw pe;
            }
        }

        try {
            buildProductHandlerMap(productHandlerMap, productNames, handlers);
        }
        catch (final IllegalArgumentException iae) {
            // Problem with handler

            final ParseException pe = new ParseException(iae.getMessage());

            pe.initCause(iae);

            throw pe;
        }

        exitWithContext = exitOption.parse(commandLine);

        this.useProductTypePatterns = rc.useProductTypePatterns();

        // initializing and starting of Global Lad feature manager
        if (GlobalLadProperties.getGlobalInstance().isEnabled()) {

            gladService = new GlobalLadDownlinkService(appContext);
            if (!gladService.startService()) {
                log.warn("Unable to start globallad feature manager");

            }
        }

    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#getAdditionalHelpText()
     */
    @Override
    public String getAdditionalHelpText() {
        return "";
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#getOverrideTypes()
     */
    @Override
    public String[] getOverrideTypes() {
        return new String[] { ProductMessageType.ProductAssembled.getSubscriptionTag(),
                SessionMessageType.StartOfSession.getSubscriptionTag(),
                SessionMessageType.SessionHeartbeat.getSubscriptionTag(),
                SessionMessageType.EndOfSession.getSubscriptionTag() };
    }

    /**
     * Look up handler for product type.
     *
     * @param prodName
     *            name of the product to look up for in the map
     * @return the productHandler for given product
     */
    public ProductHandler getProductHandler(final String prodName) {
        if (!this.useProductTypePatterns) {
            return productHandlerMap.get(prodName);
        }
        else {
            for (final Map.Entry<String, ProductHandler> entry : productHandlerMap.entrySet()) {
                final String pattern = entry.getKey();
                if (prodName.matches(pattern)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Gets the names of the products to watch for.
     * 
     * @return names of products
     */
    public Set<String> getProductNames() {
        return productNames;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#getContextConfiguration()
     */
    @Override
    public IContextConfiguration getContextConfiguration() {
        return this.contextConfig;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#getUsageText()
     */
    @Override
    public String getUsageText() {
        
        // At this point we cannot get one of the handlers to get its
        // usage text, so we do the best we can.

        final StringBuilder sb = new StringBuilder("Usage: " + APP_NAME + " [session options] [jms options] [database options]\n");
        sb.append("                                  [--printLog --productNames <name-list> --exitWithSession]\n");
        sb.append("       " + APP_NAME + " --topics <topic-list> [jms options] [database options]\n");
        sb.append("                                  [--printLog --productNames <name-list> --exitWithSession]\n");
        return sb.toString();
    }

    /**
     * Gets the flag indicating whether the application should exit when the
     * session ends.
     * 
     * @return true if the application should exit when an end of session
     *         message is received
     */
    public boolean isExitContext() {
        return exitWithContext;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#setContextConfiguration(IContextConfiguration)
     */
    @Override
    public void setContextConfiguration(final IContextConfiguration session) {
        this.contextConfig = session;
    }

    /**
     * Shutdown operation.
     */
    public void shutdown() {
        for (final Map.Entry<String, ProductHandler> entry : productHandlerMap.entrySet()) {
            entry.getValue().shutdown();
        }

        // shutting down Global Lad feature manager
        if (gladService != null) {
            TraceManager.getDefaultTracer().info("Shutting down global LAD feature manager...");

            gladService.stopService();
            TraceManager.getDefaultTracer().info("Global LAD feature manager shut down.");

        }
    }

    /**
     * Create the mapping from the supplied product types to the
     * handler objects.
     *
     * @param productMap
     *            Map to populate
     * @param productNames
     *            Array of product names
     * @param productHandlers
     *            List of handlers
     */
    private void buildProductHandlerMap(final Map<String, ProductHandler> productMap, final Set<String> productNames,
                                        final List<String> productHandlers) {
        if (productHandlers.size() != productNames.size()) {
            throw new IllegalArgumentException("Products/handlers mismatch: " + productNames.size()
                    + " products specified but " + productHandlers.size() + " handlers specified.");
        }

        // Watch out because a handler class may be used for more than one
        // type. Keep track of them here.

        final Map<String, ProductHandler> oldMap = new HashMap<>();

        productMap.clear();

        int i = 0;
        for (final String productName : productNames) {
            final String productHandler = productHandlers.get(i++).trim();

            // See if we've seen this class before
            final ProductHandler oldHandlerObject = oldMap.get(productHandler);

            if (oldHandlerObject != null) {
                // We've already created one of those, use that one again

                productMap.put(productName, oldHandlerObject);
            }
            else {
                Class<?> clss = null;

                try {
                    clss = Class.forName(productHandler);
                }
                catch (final ClassNotFoundException cnfe) {
                    throw new IllegalArgumentException("Could not find class " + productHandler + ": " + cnfe);
                }

                ProductHandler productHandlerObject = null;

                try {
                    productHandlerObject = (ProductHandler) ReflectionToolkit.createObject(clss,
                                                                                           new Class[] {
                                                                                                   ApplicationContext.class },
                                                                                           new Object[] { appContext });
                }
                catch (final ReflectionException ie) {
                    throw new IllegalArgumentException("Could not instantiate class " + productHandler + ": " + ie);
                }

                // Remember the class name and its associated handler
                oldMap.put(productHandler, productHandlerObject);

                // Build map
                productMap.put(productName, productHandlerObject);
            }
        }
    }

    /**
     * Gets the product queue drain time.
     * 
     * @return drain time, milliseconds
     */
    public long getDrainTime() {
        return this.drainTime;
    }
    
    /**
     * Gets the product queue maximum.
     * 
     * @return queue max limit
     */
    public int getQueueLimit() {
        return this.queueLimit;
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return this.appContext;
    }
}
