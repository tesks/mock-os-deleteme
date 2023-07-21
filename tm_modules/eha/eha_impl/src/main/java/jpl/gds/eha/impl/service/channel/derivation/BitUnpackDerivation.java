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
package jpl.gds.eha.impl.service.channel.derivation;

import jpl.gds.dictionary.api.channel.BitRange;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IBitUnpackChannelDerivation;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.channel.api.DerivationException;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;



/**
 * BitUnpackDerivation is a specific type of channel derivation that creates a
 * child channel from a single parent channel by extracting bit fields from the
 * parent value.
 * 
 * 5/1/14. Separated this class into two: this one,
 *          which performs the derivation, and BitUnpackDerivationDefinition,
 *          which contains the dictionary definition of the derivation. The
 *          constructor now requires the definition object. The methods that
 *          perform the derivation here must go through the definition object 
 *          to get the attributes they need.
 */
public class BitUnpackDerivation extends Object 
{
	static private final long MASK_FOR_8_BIT_SIGN = makeMask(7, 1);
	static private final long MASK_FOR_16_BIT_SIGN = makeMask(15, 1);
	static private final long MASK_FOR_32_BIT_SIGN = makeMask(31, 1);

	static private final long LEADING_1S_FOR_8_BIT_SIGNED_INT = -256L;
	static private final long LEADING_1S_FOR_16_BIT_SIGNED_INT = -65536L;
	static private final long LEADING_1S_FOR_32_BIT_SIGNED_INT = -4294967296L;

	private final IBitUnpackChannelDerivation derivationDef;

	/**
	 * Creates an instance of BitUnpackDerivation.
	 * 
	 * @param def the definition object for this derivation
	 */
	public BitUnpackDerivation(final IBitUnpackChannelDerivation def)
	{
		super();
		this.derivationDef = def;
	}

	/**
	 * Gets the dictionary definition object for this derivation.
	 * 
	 * @return definition object
	 */
	public IBitUnpackChannelDerivation getDefinition() {
		return this.derivationDef;
	}

	    /**
     * Derive child channel from parent channel value.
     *
     * @param pcv
     *            the parent channel value
     * @param chanTable
     *            the channel definition provider
     * @param chanFactory
     *            the factory for creating channel values
     *
     * @return Child channel value
     *
     * @throws DerivationException
     *             if any error occurs during the derivation
     */
    public IServiceChannelValue deriveChannel(final IServiceChannelValue pcv,
            final IChannelDefinitionProvider chanTable, final IChannelValueFactory chanFactory)
			throws DerivationException
			{
		if (pcv == null)
		{
			throw new DerivationException("Parent channel not provided");
		}

		final String parent = this.derivationDef.getParent();
		final String child = this.derivationDef.getChild();

		if (parent == null)
		{
			throw new DerivationException("Parent not set");
		}

		if (child == null)
		{
			throw new DerivationException("Child not set");
		}
		
		if (chanTable.getDefinitionFromChannelId(parent) == null)
		{
			MissingChannels.reportMissingParent(parent);

			return null;
		}

		final IChannelDefinition cd = chanTable.getDefinitionFromChannelId(child);

		if (cd == null)
		{
			MissingChannels.reportMissingChild(child);

			return null;
		}

		int  startBit  = 0;
		long resultVal = 0L;

		if (! pcv.getChanId().equals(parent))
		{
			throw new DerivationException("Parent channel value not of " +
					"proper channel id, "          +
							pcv.getChanId()                +
							" instead of "                 +
							parent);
		}

		final long parentVal = pcv.longValue();

		for (final BitRange range : this.derivationDef.getBitRanges())
		{
			final int     start  = range.getStartBit();
			final int     length = range.getLength();

			final boolean error  = ((start  < 0) ||
					(length < 0) ||
					((start + length) >= 64));

			if (error)
			{
				throw new DerivationException(
						"Illegal start-bit/length combination (" +
								start                                +
								"/"                                  +
								length                               +
								") in definition of child channel",
								cd.getId(),
								pcv.getChanId());
			}

			final long mask      = makeMask(start, length);
			final long oneResult = parentVal & mask;

			resultVal |= (oneResult >> (start - startBit));

			startBit += length;
		}

		Number resultObj = null;
		final ChannelType ct = cd.getChannelType();

		if (!ct.isIntegralType()) {
            TraceManager.getTracer(Loggers.TLM_DERIVATION)
                    .error("Cannot derive non-integer type channel " + cd.getId() + " using bit unpack derivation");

			return null;
		}

		switch (cd.getSize())
		{
		case 8:

			if ( ( ct == ChannelType.SIGNED_INT || ct == ChannelType.STATUS ) &&
					(resultVal & MASK_FOR_8_BIT_SIGN) > 0L ) { // sign bit is 1
				resultVal = LEADING_1S_FOR_8_BIT_SIGNED_INT | resultVal;
			}

			resultObj = Short.valueOf((short) resultVal);
			break;

		case 16:

			if ( ( ct == ChannelType.SIGNED_INT || ct == ChannelType.STATUS ) &&
					(resultVal & MASK_FOR_16_BIT_SIGN) > 0L ) { // sign bit is 1
				resultVal = LEADING_1S_FOR_16_BIT_SIGNED_INT | resultVal;
			}

			resultObj = Integer.valueOf((int) resultVal);
			break;

		case 32:

			if ( ( ct == ChannelType.SIGNED_INT || ct == ChannelType.STATUS ) &&
					(resultVal & MASK_FOR_32_BIT_SIGN) > 0L ) { // sign bit is 1
				resultVal = LEADING_1S_FOR_32_BIT_SIGNED_INT | resultVal;
			}

			resultObj = Long.valueOf(resultVal);
			break;                

		case 64:
			resultObj = Long.valueOf(resultVal);
			break;

		default:
			throw new DerivationException("Unsupported size for integer " +
					"channel id: "                  +
					cd.getId()                      +
					"/"                             +
					cd.getSize());
		}
		IServiceChannelValue value = null;

		try {
			value = chanFactory.createServiceChannelValue(cd, resultObj);
		} catch (final Exception e) {
			throw new DerivationException("Error deriving child channel:" + e.toString(), parent, cd.getId());
		}
		return value;
			}


	/**
	 * Makes a mask for a single bit range
	 *
	 * @param startBit the starting bit of the bit range
	 * @param length   the length of the bit range
	 *
	 * @return the bit mask
	 */
	private static long makeMask(final int startBit,
			final int length)
	{
		final int endBit = startBit + length;
		long      result = 0L;

		for (int i = startBit; i < endBit; ++i)
		{
			result |= (1L << i);
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public final boolean equals(final Object o)
	{
		return (o == this);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public final int hashCode()
	{
		return super.hashCode();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return this.derivationDef.toString();
	}
}
