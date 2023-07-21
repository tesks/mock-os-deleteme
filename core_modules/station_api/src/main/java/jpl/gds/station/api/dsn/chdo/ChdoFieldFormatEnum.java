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

package jpl.gds.station.api.dsn.chdo;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.time.TimeUtility;

/**
 * This is an enumeration of the potential types of a field in a Compressed Header
 * Data Object (CHDO).
 * 
 */
public enum ChdoFieldFormatEnum
{
	/**
	 * Unsigned integer CHDO field type.
	 */
	UNSIGNED_INTEGER
	{
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean resolveComparison(final String desiredValue, final IChdo chdo, final String chdoFieldName)
		{
			boolean equalityStatementResult = true;
			
			try
			{
				final long uintDesiredValue = Long.parseLong(desiredValue);
				final Long uintActualValue = IChdoSfdu.getFieldValueAsUnsignedInt(chdo,chdoFieldName);

				if(uintActualValue == null)
				{
					equalityStatementResult = false;
				}
				else
				{
					equalityStatementResult = uintDesiredValue == uintActualValue.longValue();
				}
			}
			catch(final NumberFormatException nfe)
			{
				equalityStatementResult = false;
			}
			
			return(equalityStatementResult);
		}
	},
	
	/**
	 * Signed integer CHDO field type.
	 */
	SIGNED_INTEGER
	{
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean resolveComparison(final String desiredValue, final IChdo chdo, final String chdoFieldName)
		{
			boolean equalityStatementResult = true;
			
			try
			{
				final long signedDesiredValue = Long.parseLong(desiredValue);
				final Long signedActualValue = IChdoSfdu.getFieldValueAsSignedInt(chdo,chdoFieldName);

				if(signedActualValue == null)
				{
					equalityStatementResult = false;
				}
				else
				{
					equalityStatementResult = signedDesiredValue == signedActualValue;
				}
			}
			catch(final NumberFormatException nfe)
			{
				equalityStatementResult = false;
			}
			
			return(equalityStatementResult);
		}
	},
	
	/**
	 * Time CHDO field type (SCLK or Date)
	 */
	TIME
	{
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean resolveComparison(final String desiredValue, final IChdo chdo, final String chdoFieldName)
		{
			boolean equalityStatementResult = true;
			final SclkFormatter sclkFmt = TimeProperties.getInstance().getSclkFormatter();
			if(sclkFmt.matches(desiredValue) == true)
			{
				final ISclk desired = sclkFmt.valueOf(desiredValue);
				final ISclk actual = IChdoSfdu.getFieldValueAsSclk(chdo,chdoFieldName);
				
				equalityStatementResult = desired.equals(actual);
			}
			else
			{
				try
				{
					final IAccurateDateTime desired = new AccurateDateTime(TimeUtility.getFormatterFromPool().parse(desiredValue));
                    final IAccurateDateTime actual  = IChdoSfdu.getFieldValueAsDate(chdo, chdoFieldName);
					
					equalityStatementResult = desired.equals(actual);
				}
				catch(final ParseException e)
				{
					equalityStatementResult = false;
				}
			}
			
			return(equalityStatementResult);
		}	
	},
	
	/**
	 * Floating point CHDO field type.
	 */
	FLOATING_POINT
	{
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean resolveComparison(final String desiredValue, final IChdo chdo, final String chdoFieldName)
		{
			boolean equalityStatementResult = true;
			
			try
			{
				final double floatingDesiredValue = Double.parseDouble(desiredValue);
				final Double floatingActualValue = IChdoSfdu.getFieldValueAsFloatingPoint(chdo,chdoFieldName);

				if(floatingActualValue == null)
				{
					equalityStatementResult = false;
				}
				else
				{
					equalityStatementResult = Double.compare(floatingDesiredValue,floatingActualValue) == 0;
				}
			}
			catch(final NumberFormatException nfe)
			{
				equalityStatementResult = false;
			}
			
			return(equalityStatementResult);
		}	
	},
	
	/**
	 * String CHDO field type.
	 */
	STRING
	{
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean resolveComparison(final String desiredValue, final IChdo chdo, final String chdoFieldName)
		{
			boolean equalityStatementResult = true;
			
			try
			{
				final String strActualValue = IChdoSfdu.getFieldValueAsString(chdo,chdoFieldName);
			
				equalityStatementResult = desiredValue.equals(strActualValue);
			}
			catch(final UnsupportedEncodingException uee)
			{
				equalityStatementResult = false;
			}
			
			return(equalityStatementResult);
		}	
	},
	
	/**
	 * Binary data CHDO field type.
	 */
	BINARY
	{
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean resolveComparison(final String desiredValue, final IChdo chdo, final String chdoFieldName)
		{
			throw new UnsupportedOperationException("The CHDO parser does not support a ChdoProperty on fields that have a format of " + this);
		}	
	};
	
	/**
	 * Compares a CHDO field with a desired value.
	 * 
	 * @param desiredValue the desired value from
	 * @param chdo the CHDO to get the actual value from
	 * @param chdoFieldName the name of the CHDO field to compare
	 * @return true if value on CHDO matches desired value, false if not
	 */
	public abstract boolean resolveComparison(final String desiredValue, final IChdo chdo, final String chdoFieldName);
}
