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

package jpl.gds.product.impl.decom;

import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.product.api.decom.DecomDataType;
import jpl.gds.product.api.decom.IArrayField;
import jpl.gds.product.api.decom.IBitField;
import jpl.gds.product.api.decom.IFieldContainer;
import jpl.gds.product.api.decom.IProductDecomField;
import jpl.gds.product.api.decom.IProductDecomFieldFactory;
import jpl.gds.product.api.decom.ISimpleField;
import jpl.gds.product.api.decom.IStructureField;

/**
 * Class ProductDecomFieldFactory
 *
 */
@SuppressWarnings("deprecation")
public class ProductDecomFieldFactory implements IProductDecomFieldFactory {
	private final boolean isDoChannels;
	private final IChannelDefinitionProvider channelTable;

	/**
	 * @param isDoChannels passed to the fields that need them.  This should be the value set 
	 * in the product config.
	 * @param channelTable channel definition table
	 */
	public ProductDecomFieldFactory(final boolean isDoChannels, final IChannelDefinitionProvider channelTable) {
		this.isDoChannels = isDoChannels;
		this.channelTable = channelTable;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.api.decom.IProductDecomFieldFactory#isShouldDoChannels()
	 */
	@Override
	public boolean isShouldDoChannels() {
		return this.isDoChannels;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.api.decom.IProductDecomFieldFactory#getChannelTable()
	 */
	@Override
	public IChannelDefinitionProvider getChannelTable() {
		return channelTable;
	}

	@Override
	public IFieldContainer createBitArrayField(final String name, final int bitlength) {
		return new BitArray(name, bitlength);
	}

	@Override
	public IBitField createBitField(final String name, final int bitlength) {
		return new BitField(name, bitlength);
	}

	@Override
	public ISimpleField createSimpleField(final String name, final DecomDataType type, final IChannelDefinitionProvider chanTable,
			final boolean shouldDoChannels) {
		return new SimpleField(name, type, chanTable, shouldDoChannels);
	}

	@Override
	public ISimpleField createSimpleField(final String name, final DecomDataType dataType, final IChannelDefinitionProvider chanTable,
			final boolean shouldDoChannels, final int lengthLength) {
		return new SimpleField(name, dataType, chanTable, shouldDoChannels, lengthLength, this);
	}

	@Override
	public IProductDecomField createStreamField(final String name, final int maxlength, final String display) {
		return new StreamField(name, maxlength, display);
	}

	@Override
	public IArrayField createArrayField(final String name, final int maxlength) {
		return new ArrayField(name, maxlength);
	}

	@Override
	public IArrayField createArrayField(final String name, final int maxlength, final int lengthLength) {
		return new ArrayField(name, maxlength, lengthLength, this);
	}

	@Override
	public IArrayField createArrayField(final String name, final ISimpleField lengthField) {
		return new ArrayField(name, lengthField);
	}


	@Override
	public IStructureField createStructureField(final String name) {
		return new StructureField(name);
	}
}
