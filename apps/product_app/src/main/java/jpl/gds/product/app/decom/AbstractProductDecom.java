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
package jpl.gds.product.app.decom;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.List;

import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.dictionary.api.apid.IApidDictionary;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.apid.IApidUtilityDictionaryManager;
import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.product.api.decom.IProductDecomUpdater;
import jpl.gds.product.api.decom.formatter.IProductDecomOutputFormatter;
import jpl.gds.product.api.dictionary.IProductDefinitionObjectFactory;
import jpl.gds.product.api.dictionary.IProductDictionary;
import jpl.gds.product.api.message.IPartialProductMessage;
import jpl.gds.product.api.message.IProductArrivedMessage;
import jpl.gds.product.api.message.IProductAssembledMessage;
import jpl.gds.product.api.message.InternalProductMessageType;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.security.loader.AmpcsUriPluginClassLoader;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.types.ByteStream;
import jpl.gds.shared.types.FileByteStream;
import jpl.gds.shared.types.HexDump;

/**
 *
 * AbstractProductDecom is the base class for project-specific product decommutation classes.
 * It handles the message subscriptions necessary to decom products, as well as
 * a container for the decom formatter and product dictionary objects used for
 * the decom.
 *
 */
public abstract class AbstractProductDecom implements IProductDecomUpdater {
    /**
     * Tracer for Product Decom
     */
    protected final Tracer                 log;

    /** Message context for publications **/
    protected IMessagePublicationBus messageContext;
    /** Override value for dictionary directory. **/
    protected String dictDirOverride;
    /** Override value for dictionary version. **/
    protected String dictVersionOverride;
    private int returnCode = SUCCESS;
    
    /**
     * Flag indicating that products/DPOs with failed checksum validation should still be processed.
     */
    protected boolean ignoreChecksum = false;
    /**
     * Shared reference to the product dictionary.
     */
    protected IProductDictionary dictionary;
    /**
     * Shared reference to the APID dictionary.
     */
    protected IApidUtilityDictionaryManager apidDictionary;
    /**
     * Shared reference to the decom output stream.
     */
    protected PrintStream out;
    /**
     * Shared reference to the decom output formatter.
     */
    protected IProductDecomOutputFormatter outf = null;
    /**
     * Shared HexDump object. Used when no definition can be found for product format.
     */
    protected HexDump hexdump;
    /**
     * Flag indicating that product viewers should be launched.
     */
    protected boolean showProductViewer = false;
    /**
     * Flag indicating that DPO viewers should be launched.
     */
    protected boolean showDpoViewer = false;
    /**
     * Flag indicating that command lines used to launch viewers should be shown.
     */
    protected boolean showLaunch = false;
    /**
     * List of DPO types to process.
     */
    protected List<String> dpoList;
    /**
     * Project-configured directory where DPO and product viewers are located.
     */
    protected String viewerDir;
    /**
     * Flag indicating that text decom output should be suppressed.
     */
    protected boolean suppressText = false;
    
    /** The current application context */
    protected ApplicationContext appContext;
    /** 
     * The database fetch factory
     * rather than archive controller
     */
    protected IDbSqlFetchFactory fetchFactory;
    /** The product definition object factory */
    protected IProductDefinitionObjectFactory definitionFactory;
    /** The secure class loader */
    protected AmpcsUriPluginClassLoader secureLoader;
    
    /**
     * Creates an instance of AbstractProductDecom.
     * @param appContext the current application context.
     *
     */
    public AbstractProductDecom(final ApplicationContext appContext) {
    	this.appContext = appContext;
    	this.viewerDir = appContext.getBean(IProductPropertiesProvider.class).getDpoViewerDir();
    	this.fetchFactory = appContext.getBean(IDbSqlFetchFactory.class);
    	this.definitionFactory = appContext.getBean(IProductDefinitionObjectFactory.class);
        this.log = TraceManager.getTracer(appContext, Loggers.PRODUCT_DECOM);
        this.secureLoader = appContext.getBean(AmpcsUriPluginClassLoader.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getReturnCode() {
    	return returnCode;
    }
    
    /**
     * Sets the result status. 
     * @param status completion status: must be SUCCESS, FAILURE, NO_PROD_VIEWER, NO_DPO_VIEWER
     */
    @Override
	public void setReturnCode(final int status) {
    	returnCode = status;
    }
    
    /**
     * Sets the flag indicating whether text output is suppressed.
     * @param suppress true if text output is suppressed
     */
    @Override
	public void setSupressText(final boolean suppress) {
    	suppressText = suppress;
    }
    
    /**
     * Overrides the dictionary directory in the product EMD file.
     * @param dictDirOverride the directory to set
     */
    @Override
	public void setDictDirOverride(final String dictDirOverride) {
        this.dictDirOverride = dictDirOverride;
    }

    /**
     * Overrides the dictionary version in the product EMD file.
     * @param dictVersionOverride the version to set
     */
    @Override
	public void setDictVersionOverride(final String dictVersionOverride) {
        this.dictVersionOverride = dictVersionOverride;
    }

    /**
     * Sets the OutputFormatter for formatting product decom output.
     * @param of the OutputFormatter to set
     */
    @Override
	public void setOutputFormatter(final IProductDecomOutputFormatter of) {
        outf = of;
    }

    /**
     * Sets the stream for product decom output. This is the stream that will be
     * used by the output formatter.
     * @param printStream the PrintStream to set
     */
    @Override
	public void setPrintStream(final PrintStream printStream) {
        out = printStream;
        hexdump = new HexDump(new OutputStreamWriter(out));
    }

    /**
     * Sets the message context for subscription purposes.
     * @param messageContext the MessageContext to set
     */
    @Override
	public void setMessageContext(final IMessagePublicationBus messageContext) {
        this.messageContext = messageContext;
    }

    /**
     * Causes this object to subscribe to "product arrived" messages.
     * Product arrival messages are generated by StoredProductInput
     * as it reads products from product storage, where they were written
     * by the product generator.
     *
     */
    @Override
	public void subscribeToArrivedProducts() {
        messageContext.subscribe(InternalProductMessageType.DecomProductArrived,
                                 new BaseMessageHandler() {
            @Override
            public void handleMessage(final IMessage m) {
                handleProductArrivedMessage((IProductArrivedMessage) m);
            }
        });
    }

    /**
     * Causes this object to subscribe to "product assembled" messages.
     * Product assembled messages are generated by the product generator
     * as it writes complete products to disk.
     */
    @Override
	public void subscribeToAssembledProducts() {
        messageContext.subscribe(ProductMessageType.ProductAssembled,
                                 new BaseMessageHandler() {
            @Override
            public void handleMessage(final IMessage m) {
                handleProductAssembledMessage((IProductAssembledMessage) m);
            }
        });
    }

    /**
     * Causes this object to subscribe to "partial product" messages.
     * Partial product messages are generated by the product generator
     * as it writes partial products to disk.
     */
    @Override
	public void subscribeToPartialProducts() {
        messageContext.subscribe(ProductMessageType.PartialProduct,
                                 new BaseMessageHandler() {
            @Override
            public void handleMessage(final IMessage m) {
                handlePartialProductMessage((IPartialProductMessage) m);
            }
        });
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.decom.IProductDecom#handleProductArrivedMessage(jpl.gds.product.message.ProductArrivedMessage)
     */
    @Override
    public void handleProductArrivedMessage(final IProductArrivedMessage message) {
        final IProductMetadataProvider metadata = message.getProductMetadata();
        if (metadata == null) {
            log.error("Internal Error: message did not include product metadata");
            return;
        }

        FileByteStream bytestream = null;
        try {
            bytestream = new FileByteStream(metadata.getFullPath(), log);
        }
        catch (final IOException e) {
            log.error("Could not load stored product ", metadata.getFullPath(), e.getCause());
            return;
        }

        handleProduct(metadata, bytestream);
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.decom.IProductDecom#handlePartialProductMessage(jpl.gds.product.message.PartialProductMessage)
     */
    @Override
    public void handlePartialProductMessage(final IPartialProductMessage message) {
        final IProductMetadataProvider metadata = message.getMetadata();
        if (metadata == null) {
            log.error("Internal Error: message did not include product metadata");
            return;
        }

        final String fullpath = metadata.getFullPath();
        if (fullpath == null) {
            log.error("Internal Error: message did not include product file path");
            return;
        }

        FileByteStream bytestream = null;
        try {
            bytestream = new FileByteStream(fullpath, log);
        }
        catch (final IOException e) {
            log.error("Could not load stored product ", fullpath, e.getCause());
            return;
        }

        handleProduct(metadata, bytestream);
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.decom.IProductDecom#handleProductAssembledMessage(jpl.gds.product.message.ProductAssembledMessage)
     */
    @Override
    public void handleProductAssembledMessage(final IProductAssembledMessage message) {
        final IProductMetadataProvider metadata = message.getMetadata();
        if (metadata == null) {
            log.error("Internal Error: message did not include product metadata");
            return;
        }

        final String fullpath = metadata.getFullPath();
        if (fullpath == null) {
            log.error("Internal Error: message did not include product file path");
            return;
        }

        FileByteStream bytestream = null;
        try {
            bytestream = new FileByteStream(fullpath, log);
        }
        catch (final IOException e) {
            log.error("Could not load stored product ", fullpath, e.getCause());
            return;
        }

        handleProduct(metadata, bytestream);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract boolean handleProduct(IProductMetadataProvider metadata, ByteStream bytestream);

    /**
     * Sets the flag indicating whether to process products with an invalid checksum
     * @param check true to ignore checksum
     */
    @Override
	public void setIgnoreChecksum(final boolean check) {
        ignoreChecksum = check;
    }
    
    /**
     * Sets the flag indicating whether to launch product viewers.
     * @param enable true to launch product viewers
     */
    @Override
	public void setShowProductViewer(final boolean enable) {
    	showProductViewer = enable;
    }
    
    /**
     * Sets the flag indicating whether to launch data product object (DPO) viewers.
     * @param enable true to launch DPO viewers
     */
    @Override
	public void setShowDpoViewer(final boolean enable) {
    	showDpoViewer = enable;
    }
   
    /**
     * Sets the flag indicating whether to output detailed messages about external viewers
     * as they are launched.
     * 
     * @param enable true to enable detailed output
     */
    @Override
	public void setShowLaunchInfo(final boolean enable) {
    	showLaunch = enable;
    }
    
    /**
     * Sets a specific list of DPOs to be viewed.
     * @param dpos list of DPO VIDs or Names
     */
    @Override
	public void setDpoList(final List<String> dpos) {
    	dpoList = dpos;
    }
    
    /**
     * Creates the ProductDictionary for the current configuration. By default, loads the product dictionary
     * referenced by the session configuration.
     * 
     * @param productDictDir optional dictionary override directory
     * @param productDictVersion optional dictionary override version
     * @throws DictionaryException if there is a problem loading the product dictionary
     */
    protected void loadDictionary(final String productDictDir, final String productDictVersion) throws DictionaryException {
        final DictionaryProperties dictConfig = appContext.getBean(DictionaryProperties.class);

        String newDictDir = dictDirOverride;
        if (newDictDir == null) {
            newDictDir = productDictDir;
        }
        if (newDictDir == null) {
            newDictDir = dictConfig.getFswDictionaryDir();
        }
        String newDictVersion = dictVersionOverride;
        if (newDictVersion == null) {
            newDictVersion = productDictVersion;
        }
        if (newDictVersion == null) {
            newDictVersion = dictConfig.getFswVersion();
        }

        boolean dictsAreNull = dictionary == null || apidDictionary == null;
        boolean dictsDiffer = !dictConfig.getFswDictionaryDir().equals(newDictDir) ||
                !dictConfig.getFswVersion().equals(newDictVersion);
        // if the dicts are null, or if they differ from default, we need to set and load
        if (dictsAreNull || dictsDiffer) {
           dictConfig.setFswDictionaryDir(newDictDir);
           dictConfig.setFswVersion(newDictVersion);
           // if null, get them
           if (dictsAreNull) {
               if (dictionary == null) {
                   dictionary = appContext.getBean(IProductDictionary.class);
               }
               if (apidDictionary == null) {
                   apidDictionary = appContext.getBean(IApidUtilityDictionaryManager.class);
               }
           }

           // if the dictionaries differ from default, clear and reload
           if (dictsDiffer) {
               dictionary.clear();
               dictionary.setDirectory(dictConfig.getProductDictionaryDir());
               apidDictionary.clear();
           }

           // in either case, we need to load the dictionaries
           dictionary.loadAll();
           if (!apidDictionary.isLoaded()) {
               apidDictionary.load();
           }
           /* Removed setting of product directory. The factory does it. */
        }
    }
}

