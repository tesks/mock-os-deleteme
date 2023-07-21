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
package jpl.gds.product.impl.spring.bootstrap;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;

import jpl.gds.product.api.builder.*;
import jpl.gds.product.api.file.IProductFilenameBuilder;
import jpl.gds.product.impl.builder.*;
import jpl.gds.product.utilities.file.ReferenceProductFilenameBuilder;
import jpl.gds.product.utilities.file.ReferenceProductFilenameBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.product.api.IProductTrackingService;
import jpl.gds.product.api.ProductApiBeans;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.ProductOutputFormat;
import jpl.gds.product.api.checksum.IProductDataChecksum;
import jpl.gds.product.api.checksum.ProductDataChecksumException;
import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.product.api.config.RecordedProductProperties;
import jpl.gds.product.api.decom.IProductDecomFieldFactory;
import jpl.gds.product.api.decom.IStoredProductInput;
import jpl.gds.product.api.decom.formatter.IProductDecomOutputFormatter;
import jpl.gds.product.api.dictionary.IProductDefinitionDumper;
import jpl.gds.product.api.dictionary.IProductDefinitionObjectFactory;
import jpl.gds.product.api.dictionary.IProductDictionary;
import jpl.gds.product.api.file.IProductFilenameBuilderFactory;
import jpl.gds.product.api.message.IProductMessageFactory;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.product.impl.ProductTrackingService;
import jpl.gds.product.impl.ReferenceProductAdaptor;
import jpl.gds.product.impl.checksum.CfdpProductDataChecksum;
import jpl.gds.product.impl.checksum.Crc32ProductDataChecksum;
import jpl.gds.product.impl.config.ProductProperties;
import jpl.gds.product.impl.decom.ProductDecomFieldFactory;
import jpl.gds.product.impl.decom.ReferenceStoredProductInput;
import jpl.gds.product.impl.decom.formatter.NullProductOutputFormatter;
import jpl.gds.product.impl.decom.formatter.ReferenceCsvOutputFormatter;
import jpl.gds.product.impl.decom.formatter.ReferenceTextOutputFormatter;
import jpl.gds.product.impl.decom.formatter.ReferenceXmlOutputFormatter;
import jpl.gds.product.impl.dictionary.ProductDefinitionObjectFactory;
import jpl.gds.product.impl.dictionary.ReferenceProductDefinitionDumper;
import jpl.gds.product.impl.dictionary.ReferenceProductDictionary;
import jpl.gds.product.impl.message.PartialProductMessage;
import jpl.gds.product.impl.message.ProductAssembledMessage;
import jpl.gds.product.impl.message.ProductMessageFactory;
import jpl.gds.product.impl.message.ProductPartMessage;
import jpl.gds.product.impl.message.ProductStartedMessage;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.RegisteredMessageConfiguration;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.ProductTemplateManager;
import jpl.gds.shared.template.TemplateException;

/**
 * 
 * Spring bootstrap configuration class for the product modules.
 * 
 *
 */
@Configuration
public class ProductSpringBootstrap {
	
	@Autowired
	private ApplicationContext appContext;
	
	/**
	 * Constructor.
	 */
	public ProductSpringBootstrap() {
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(ProductMessageType.ProductAssembled,
                ProductAssembledMessage.XmlParseHandler.class.getName(), null, new String[] {"ProductAssembled"}));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(ProductMessageType.PartialProduct,
                PartialProductMessage.XmlParseHandler.class.getName(), null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(ProductMessageType.ProductPart,
                ProductPartMessage.XmlParseHandler.class.getName(), null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(ProductMessageType.ProductStarted,
                ProductStartedMessage.XmlParseHandler.class.getName(), null));
	}
	
	
    /**
     * Creates the singleton decom field factory bean.
     * 
     * @param productConfig current product configuration properties bean, autowired
     * @param chanTable channel definition table, autowired
     * @return factory bean
     */
    @Bean(name=ProductApiBeans.PRODUCT_DECOM_FIELD_FACTORY) 
    @Scope("singleton")
    @Lazy(value = true)
    public IProductDecomFieldFactory createProductDecomFieldFactory(final IProductPropertiesProvider productConfig, final IChannelDefinitionProvider chanTable) {
    	return new ProductDecomFieldFactory(productConfig.isDoChannels(), chanTable);
    }
	
    /**
     * Creates the singleton transaction log storage bean
     * @param partFactory the current product builder instance factory, autowired
     * @return transaction log storage bean
     */
    @Bean(name=ProductApiBeans.TRANSACTION_LOG_STORAGE) 
    @Scope("singleton")
    @Lazy(value = true)
    public ITransactionLogStorage createTransactionLogStorage(final IProductBuilderObjectFactory partFactory) {
    	final ReferenceTransactionLogStorage txLogStorage = new ReferenceTransactionLogStorage(appContext, partFactory);
    	partFactory.setTransactionLogStorage(txLogStorage);
    	return txLogStorage;
    }
	
    /**
     * Creates the singleton product builder object instance factory.
     * 
     * @return product builder object factory bean
     */
    @Bean(name=ProductApiBeans.PRODUCT_INSTANCE_FACTORY) 
    @Scope("singleton")
    @Lazy(value = true)
    public IProductBuilderObjectFactory createInstanceFactory() {
    	return new ReferenceProductBuilderObjectFactory(appContext);
    }

    /**
     * Creates the singleton product builder manager service bean.
     * 
     * @return product builder manager service bean
     */
    @Bean(name=ProductApiBeans.PRODUCT_BUILDER_MANAGER) 
    @Scope("singleton")
    @Lazy(value = true)
    public IProductBuilderManager createProductBuilderManager() {
    	return new ProductBuilderManager(appContext);
    }
    
    /**
     * Creates the singleton product builder table bean.
     * 
     * @return product builder table bean
     */
    @Bean(name=ProductApiBeans.PRODUCT_BUILDER_TABLE) 
    @Scope("singleton")
    @Lazy(value = true)
    public Map<Integer, IProductBuilderService> createProductBuilderTable() {
        return new ConcurrentHashMap<>();
    }

    /**
     * Creates a prototype product builder service bean. Only one instance will be
     * created per VCID. The ProductBuilderTable bean is used to cache instances.
     * 
     * @param vcid the virtual channel
     * @return product builder service bean
     */
    @Bean(name=ProductApiBeans.PRODUCT_BUILDER) 
    @Scope("prototype")
    @Lazy(value = true)
    public IProductBuilderService createProductBuilder(final int vcid) {
    	@SuppressWarnings("unchecked")
		final
		ConcurrentHashMap<Integer, IProductBuilderService> pbTable = (ConcurrentHashMap<Integer, IProductBuilderService>) appContext.getBean(ProductApiBeans.PRODUCT_BUILDER_TABLE, Map.class);
    	synchronized(pbTable) {
    		if (!pbTable.containsKey(vcid)) {

    			final IProductBuilderService builder = new ProductBuilder(appContext, vcid);
    			pbTable.put(vcid, builder);
    			
    			// When we add a new product builder, add it to the manager as well.
    			final IProductBuilderManager manager = appContext.getBean(IProductBuilderManager.class);
    			manager.addProductBuilder(builder);
    		}
    		
    		return pbTable.get(vcid);
    	}
    }

    /** 
     * Creates the singleton disk product storage table bean.
     * 
     * @return disk product storage table bean
     */
    @Bean(name=ProductApiBeans.DISK_PRODUCT_STORAGE_TABLE) 
    @Scope("singleton")
    @Lazy(value = true)
    public Map<Integer, IProductStorage> createProductStorageTable() {
        return new ConcurrentHashMap<>();
    }

    /**
     * Creates a prototype disk product storage bean.  Only one instance will be created per VCID.
     * The DiskProductStorageTable bean is used to cache instances.
     * 
     * @param vcid the virtual channel ID
     * @return disk product storage bean
     */
    @Bean(name=ProductApiBeans.DISK_PRODUCT_STORAGE) 
    @Scope("prototype")
    @Lazy(value = true)
    public IProductStorage createDiskProductStorage(final int vcid) {
    	@SuppressWarnings("unchecked")
		final
		ConcurrentHashMap<Integer, IProductStorage> dpsTable = (ConcurrentHashMap<Integer, IProductStorage>) appContext.getBean(ProductApiBeans.DISK_PRODUCT_STORAGE_TABLE, Map.class);
    	synchronized(dpsTable) {
    		if (!dpsTable.containsKey(vcid)) {
    			dpsTable.put(vcid, new ReferenceDiskProductStorage(appContext, vcid));
    		}
    		
    		return dpsTable.get(vcid);
    	}
    }
    
    /**
     * Creates the singleton stored product input bean.
     * 
     * @return stored product input bean
     * @throws ProductException if there is an issue creating the bean
     */
    @Bean(name=ProductApiBeans.STORED_PRODUCT_INPUT) 
    @Scope("singleton")
    @Lazy(value = true)
    public IStoredProductInput createStoredProductInput() throws ProductException {
		return new ReferenceStoredProductInput(appContext);
    }

    /**
     * Creates the singleton product definition dumper bean.
     * 
     * @return product definition dumper bean
     * @throws ProductException if there is an issue creating the bean
     */
    @Bean(name=ProductApiBeans.PRODUCT_DEFINITION_DUMPER) 
    @Scope("singleton")
    @Lazy(value = true)
    public IProductDefinitionDumper createProductDefinitionDumper() throws ProductException {
    	return new ReferenceProductDefinitionDumper();
    }

    /**
     * Creates a prototype product packet filter bean. Loads the APID dictionary.
     * 
     * @param vcid the virtual channel ID.
     * @return product packet filter bean
     */
    @Bean(name=ProductApiBeans.PRODUCT_FILTER) 
    @Scope("prototype")
    @Lazy(value = true)
    public IProductPacketFilter createProductsOnlyFilter(final int vcid) {
    	final IApidDefinitionProvider apidReference = appContext.getBean(IApidDefinitionProvider.class);
		final SortedSet<Integer> apids = apidReference.getProductApids();
		return new ProductsOnlyFilter(apids, vcid);
    }

    /**
     * Creates a prototype product decom output formatter bean. Loads the APID 
     * dictionary.
     * 
     * @param outputType the desired product output format
     * @param filename filename for output; may be null for some formatters
     * @return decom output formatter bean
     * @throws IOException if there is a file I/O problem creating the formatter
     */
    @Bean(name=ProductApiBeans.PRODUCT_DECOM_OUTPUT_FORMATTER) 
    @Scope("prototype")
    @Lazy(value = true)
    public IProductDecomOutputFormatter createProductDecomOutputFormatter(final ProductOutputFormat outputType, final String filename) throws IOException {
		IProductDecomOutputFormatter format = null;
		final SprintfFormat spformat = appContext.getBean(SprintfFormat.class);
		final MissionProperties missionProperties = appContext.getBean(MissionProperties.class);
		switch(outputType) {
		case TEXT_PRODUCT_OUTPUT:
			format = new ReferenceTextOutputFormatter(missionProperties, appContext, spformat);
			break;
		case XML_PRODUCT_OUTPUT:
			format = new ReferenceXmlOutputFormatter(appContext, spformat);
			break;
		case CSV_PRODUCT_OUTPUT:
			format = new ReferenceCsvOutputFormatter(appContext, spformat);
			break;
		case NO_PRODUCT_OUTPUT:
        default:
			format = new NullProductOutputFormatter(appContext, spformat);
			break;
		} 
		if (filename != null) {
			format.setPrintStream(new PrintStream(new FileOutputStream(filename, false), true));
		}

    	final IApidDefinitionProvider apidReference = appContext.getBean(IApidDefinitionProvider.class);
		format.setApidDictionary(apidReference);
		return format;
    }
	
    /**
     * Creates the singleton product builder mission adapter bean.
     * 
     * @return product mission adaptor bean
     * @throws ProductException if there is a problem creating the adaptor
     * @throws DictionaryException if there is a problem with the adaptor loading dictionaries
     */
    @Bean(name=ProductApiBeans.PRODUCT_MISSION_ADAPTOR) 
    @Scope("singleton")
    @Lazy(value = true)
    public IProductMissionAdaptor getProductMissionAdaptor() throws ProductException, DictionaryException {
    	return new ReferenceProductAdaptor(appContext);
    }
    
    /**
     * Gets the singleton product configuration properties bean. Output directory
     * must be set in the current general context information bean before this
     * is invoked.
     * 
     * @param genInfo
     *            the general context information bean, autowired
     * @param sseFlag
     *            The SSE context flag
     * @return product config bean
     */
    @Bean(name=ProductApiBeans.PRODUCT_PROPERTIES) 
    @Scope("singleton")
    @Lazy(value = true)
    public IProductPropertiesProvider getProductPropertiesProvider(final IGeneralContextInformation genInfo,
                                                                   final SseContextFlag sseFlag) {
        return new ProductProperties(genInfo, sseFlag);
    }

    /**
     * Creates the singleton product template manager bean.
     * 
     * @param sseFlag
     *            The current SSE context flag
     * 
     * @return product template manager bean
     * @throws TemplateException
     *             if there is a problem creating the manager
     */
    @Bean(name=ProductApiBeans.PRODUCT_TEMPLATE_MANAGER) 
    @Scope("singleton")
    @Lazy(value = true)
    public ProductTemplateManager getProductTemplateManager(final SseContextFlag sseFlag) throws TemplateException {
        return MissionConfiguredTemplateManagerFactory.getNewProductTemplateManager(sseFlag);
    }

    /**
     * Gets the singleton received parts tracker bean.
     * 
     * @return received parts tracker bean
     */
    @Bean(name=ProductApiBeans.PARTS_TRACKER) 
    @Scope("singleton")
    @Lazy(value = true)
    public IReceivedPartsTracker getPartsTracker() {
    	return new ReceivedPartsTracker(appContext);
    }

    /**
     * Gets the singleton product dictionary bean.
     * 
     * @param dictConfig the dictionary configuration, autowired
     * @param fieldFactory the decom field factory, autowired
     * @return product dictionary bean
     * @throws DictionaryException if there is a problem loading the dictionary
     */
    @Bean(name=ProductApiBeans.PRODUCT_DICTIONARY) 
    @Scope("singleton")
    @Lazy(value = true)
    public IProductDictionary getProductDictionary(final DictionaryProperties dictConfig,
    		final IProductDecomFieldFactory fieldFactory) throws DictionaryException {
    	final ReferenceProductDictionary dict = new ReferenceProductDictionary(fieldFactory);
    	dict.setDirectory(dictConfig.getProductDictionaryDir());
    	return dict;
    }
    
    /**
     * Gets the singleton product tracking service bean.
     * 
     * @return product tracking service bean
     */
    @Bean(name=ProductApiBeans.PRODUCT_TRACKING_SERVICE) 
    @Scope("singleton")
    @Lazy(value = true)
    public IProductTrackingService getProductTrackingService() {
        return new ProductTrackingService(appContext);
    }
    
    /**
     * Creates the singleton product definition object factory bean.
     * @return product definition object factory bean
     */
    @Bean(name=ProductApiBeans.PRODUCT_DEFINITION_FACTORY) 
    @Scope("singleton")
    @Lazy(value = true)
    public IProductDefinitionObjectFactory createProductDefinitionFactory() {
        return new ProductDefinitionObjectFactory();
    }
    
    /**
     * Creates the singleton product filename builder factory bean.
     * 
     * @return product filename builder factory bean
     */
    @Bean(name=ProductApiBeans.PRODUCT_FILENAME_BUILDER_FACTORY) 
    @Scope("singleton")
    @Lazy(value = true)
    public IProductFilenameBuilderFactory createProductFilenameBuilderFactory() {
        return new ReferenceProductFilenameBuilderFactory(appContext);
    }

    /**
     * Creates the singleton product message factory bean.
     * 
     * @return product message factory bean
     */
    @Bean(name=ProductApiBeans.PRODUCT_MESSAGE_FACTORY) 
    @Scope("singleton")
    @Lazy(value = true)
    public IProductMessageFactory createProductMessageFactory() {
        return new ProductMessageFactory();
    }
    
    /**
     * Creates the singleton product data checksum bean.
     * 
     * @param productConfig
     *            product properties object
     * 
     * @return product data checksum bean
     * @throws ProductDataChecksumException
     *             When invalid checksum algorithm is configured
     */
    @Bean(name=ProductApiBeans.PRODUCT_CHECKSUM) 
    @Scope("singleton")
    @Lazy(value = true)
    public IProductDataChecksum createProductChecksum(final IProductPropertiesProvider productConfig)
            throws ProductDataChecksumException {
        final String checksum = productConfig.getChecksumAlgorithm();
        
        if (checksum.equalsIgnoreCase("cfdp")) {
            return new CfdpProductDataChecksum();
        } else if (checksum.equalsIgnoreCase("crc32")) {
            return new Crc32ProductDataChecksum();
        } else {
            throw new ProductDataChecksumException("Unable to load product checksum algorithm configuration. Property "
                    + ProductProperties.CHECKSUM_ALGORITHM + "=" + checksum + " is invalid.");
        }
    }
    
    /**
     * Creates the singleton recorded product properties bean.
     * 
     * @return recorded product properties bean
     * 
     * @param sseFlag
     *            The current SSE context flag
     */
    @Bean(name=ProductApiBeans.RECORDED_PRODUCT_PROPERTIES) 
    @Scope("singleton")
    @Lazy(value = true)
    public RecordedProductProperties createRecordedProductProperties(final SseContextFlag sseFlag) {
        return new RecordedProductProperties(sseFlag);
    }

    /**
     * Creates a singleton product output directory utility bean.
     *
     * @return product output directory bean
     */
    @Bean(name=ProductApiBeans.PRODUCT_OUTPUT_DIRECTORY_UTIL)
    @Scope("singleton")
    @Lazy(value = true)
    public IProductOutputDirectoryUtil createProductOutputDirectory() {
        return new ProductOutputDirectoryUtil(appContext);
    }

}
