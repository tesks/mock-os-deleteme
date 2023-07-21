/**
 * 
 */
package jpl.gds.product.context;

import jpl.gds.common.spring.context.IContextContainer;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.dictionary.api.mapper.IFswToDictionaryMapper;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.processors.PostDownlinkProductProcessingException;
import jpl.gds.product.processors.PostDownlinkProductProcessorOptions;
import jpl.gds.product.processors.descriptions.IPdppDescription;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;

/**
 * Used by PDPP.  Caches instances of the context, creates new sessions and contexts
 * based on the input.
 * 
 *
 */
public class PdppContextCache extends AbstractContextCache implements IPdppContextCache {
	private final IFswToDictionaryMapper mapper;
	private IPdppContextContainerCreator containerCreator;

	/*
	 * MPCS-10081  - 09/25/18 - added. Need to keep a reference to it for use in
	 * loading dictionary JAR files in createNewContainer
	 */
	private final ApplicationContext mainContext;

	/**
	 * @param mainContext
	 */
	public PdppContextCache(final ApplicationContext mainContext) {
		this.mapper = mainContext.getBean(IFswToDictionaryMapper.class);
		
		this.mainContext = mainContext;
		this.containerCreator = mainContext.getBean(IPdppContextContainerCreator.class);
	}
	
	/**
	 * 
	 * @param md
	 * @param description
	 * @return
	 */
	private String getLookupKey(final IProductMetadataProvider md, final IPdppDescription description) {
		final String key = StringUtils.join(new Object[]{md.getSessionId(), 
				description.getSessionSuffix(),
				description.getBacklinkExplanation(),
				md.getSessionHost()}, 
				IContextContainer.keySeparator);
		return key;
	}

	/**
	 * @param md the product
	 * @param description the suffix description for the new session
	 * @param options PostDownlinkProductProcessorOptions
	 * @param startLdiStores
	 * @return
	 * @throws PostDownlinkProductProcessingException
	 */
	public IPdppContextContainer getContextContainer(final IProductMetadataProvider md,
			IPdppDescription description,
			PostDownlinkProductProcessorOptions options,
			final boolean startLdiStores) throws PostDownlinkProductProcessingException {
		final String key = this.getLookupKey(md, description);

		IPdppContextContainer container;
		try {
			if (containsContextContainer(key)) {
				container = (IPdppContextContainer)this.getContextContainer(key);
			} else {
				container = createNewContainer(key, md, description, options);
			}
		} catch (final DatabaseException e) {
			throw new PostDownlinkProductProcessingException("Failed to get context container", e);
		}

		if (startLdiStores)	{
			container.startChildDbStores();
		}

		return container;
	}
	
	/**
	 * @param childKey
	 * @param md
	 * @param description
	 * @param options
	 * @return
	 * @throws PostDownlinkProductProcessingException
	 * @throws DatabaseException
	 */
	private IPdppContextContainer createNewContainer(final String childKey,
													final IProductMetadataProvider md,
													final IPdppDescription description,
													final PostDownlinkProductProcessorOptions options
													) throws PostDownlinkProductProcessingException, DatabaseException {

		IPdppContextContainer container = containerCreator.createContextContainer(childKey, md, description, options, mapper);

		addContextContainer(childKey, container);

		return container;
	}
}
