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
package jpl.gds.dictionary.api.decom.params;

import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.decom.IDecomStatement;
import jpl.gds.dictionary.api.decom.types.ICaseBlockDefinition;
import jpl.gds.dictionary.api.decom.types.IEnumDataDefinition;

/**
 * Parameter builder class for creating {@link ICaseBlockDefinition} instances.
 * Mutable and reusable.
 * 
 * All values set for an instance of this class will be applied to the {@link IDecomStatement}
 * instance it will be used to create. For more information for each of the parameters,
 * see the interface this parameter object corresponds to.
 */
public class EnumDataParams extends IntegerParams {
	
	private static final String DEFAULT_FORMAT = "%s";
	private String enumName = "";
	private String enumFormat = DEFAULT_FORMAT;
	private EnumerationDefinition enumDef;

	/**
	 * @see IEnumDataDefinition#getEnumName()
	 * @return the enum name
	 */
	public String getEnumName() {
		return enumName;
	}

	/**
	 * Set the enum name. If the String passed in
	 * is null, the enum name is set to the empty string.
	 * @param enumName the name for the decom enum
	 */
	public void setEnumName(String enumName) {
		if (enumName == null) {
			this.enumName = "";
		} else {
			this.enumName = enumName;
		}
	}
	
	/**
	 * @see IEnumDataDefinition#getEnumFormat()
	 * @return the format to use in displaying the enum
	 */
	public String getEnumFormat() {
		return enumFormat;
	}
	
	/**
	 * Set the enum format to use in displaying the enum.
	 * If the String passed in is null, the enum format
	 * to display only the enum symbol
	 * @param enumFormat the enum format
	 */
	public void setEnumFormat(String enumFormat) {
		if (enumFormat == null) {
			this.enumFormat = DEFAULT_FORMAT;
		}
		this.enumFormat = enumFormat;
	}
	
	
	@Override
	public void reset() {
		super.reset();
		enumName = "";
		this.enumFormat = DEFAULT_FORMAT;
	}

	/**
	 * Set the definition object that defines a lookup table for the enum.
	 * @param enumerationDefinition the definition object
	 */
	public void setEnumDefinition(EnumerationDefinition enumerationDefinition) {
		enumDef = enumerationDefinition;
	}

	/**
	 * 
	 * @return the enum definition object
	 */
	public EnumerationDefinition getEnumDefinition() {
		return enumDef;
	}
}
