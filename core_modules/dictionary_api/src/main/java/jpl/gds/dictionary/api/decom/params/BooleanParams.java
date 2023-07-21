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

import jpl.gds.dictionary.api.decom.IDecomStatement;
import jpl.gds.dictionary.api.decom.types.IBooleanDefinition;

/**
 * Parameter builder class for creating {@link IBooleanDefinition} instances.
 * Mutable and reusable.
 * 
 * All values set for an instance of this class will be applied to the {@link IDecomStatement}
 * instance it will be used to create. For more information for each of the parameters,
 * see the interface this parameter object corresponds to.
 */
public class BooleanParams extends IntegerParams {

	private static String TRUE_DEFAULT = "true";
	private static String FALSE_DEFAULT = "false";

	private String trueString = TRUE_DEFAULT; 
	private String falseString = FALSE_DEFAULT;

	/**
	 * Get the string to be displayed along a "true" boolean value.  
	 * @return the string to be displayed if a boolean extracted using this definition
	 * 		   should be displayed with
	 */
	public String getTrueString() {
		return trueString;
	}


	/**
	 * Set the truth string.
	 * @param trueString string to display for a true value
	 * 	      Null argument value replaced by the default truth string.	
	 */
	public void setTrueString(String trueString) {
		if (trueString == null) {
			this.trueString = TRUE_DEFAULT;
		} else {
			this.trueString = trueString;
		}
	}

	/**
	 * Get the string to be displayed along a "false" boolean value.  
	 * An empty string indicates the default string should be used.
	 * @return the string to be displayed if a boolean extracted using this definition
	 * 		   should be displayed with
	 */
	public String getFalseString() {
		return falseString;
	}

	/**
	 * Set the false string
	 * @param falseString string to display for a false value
	 * 	      Null argument value replaced by the default false string.	
	 */ 
	public void setFalseString(String falseString) {
		if (falseString == null) {
			this.falseString = FALSE_DEFAULT;
		} else {
			this.falseString = falseString;
		}
	}
	
	@Override
	public void reset() {
		super.reset();
		trueString = TRUE_DEFAULT;
		falseString = FALSE_DEFAULT;
	}
}
