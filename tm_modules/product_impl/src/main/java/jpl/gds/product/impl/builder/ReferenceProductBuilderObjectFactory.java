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
/**
 * 
 */
package jpl.gds.product.impl.builder;

import org.springframework.context.ApplicationContext;

import jpl.gds.product.api.IProductMetadataUpdater;
import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.builder.IProductTransactionProvider;
import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.product.utilities.file.ReferenceProductMetadata;
import jpl.gds.product.impl.ReferenceProductPart;
import jpl.gds.product.impl.config.ProductProperties;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * A factory for creating high volume product builder objects for the reference
 * product builder.
 * 
 * @since R8
 */
public class ReferenceProductBuilderObjectFactory extends AbstractBuilderProductObjectFactory {
    private final String filenameDvtMarker;
    private final String filenameDvtSeparator;
	

	/**
	 * Constructor.
	 * 
	 * @param springContext the current application context
	 */
	public ReferenceProductBuilderObjectFactory(final ApplicationContext springContext) {
		super(springContext);

		final ProductProperties config = (ProductProperties) appContext.getBean(IProductPropertiesProvider.class);
		
		this.filenameDvtMarker = config.getFileDvtMarker();
		this.filenameDvtSeparator = config.getFileDvtSeparator();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.api.builder.IProductBuilderObjectFactory#createPart(jpl.gds.tm.service.api.packet.ITelemetryPacketMessage)
	 */
	@Override
	public IProductPartProvider createPart(final ITelemetryPacketMessage packet) throws ProductException {
		//In order to determine if whether to use the 16 byte secondary header
		//or the 10 byte secondary header, the product part must have access
		//to the apid reference
		return new ReferenceProductPart(appContext, createProductMetadata(), packet, filenameDvtMarker, filenameDvtSeparator);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.api.builder.IProductBuilderObjectFactory#createPart()
	 */
	@Override
	public IProductPartProvider createPart() throws ProductException {
		return new ReferenceProductPart(appContext, createProductMetadata(), filenameDvtMarker, filenameDvtSeparator);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.api.builder.IProductBuilderObjectFactory#createProductMetadata()
	 */
	@Override
	public IProductMetadataUpdater createProductMetadata() {
	    return new ReferenceProductMetadata(appContext);
	}
	
    /**
     * {@inheritDoc}
     */
    @Override
    public IProductTransactionProvider createProductTransaction() {
        return new ReferenceProductTransaction(appContext);
    }
}
