package jpl.gds.shared.config;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Class OrderedProperties
 *
 */
public class OrderedProperties extends Properties {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.Hashtable#keys()
	 */
	@Override
	public synchronized Enumeration<Object> keys() {
		/*
		 * This will make sure that the properties will be written in
		 * alphabetically sorted order
		 */
		return Collections.enumeration(new TreeSet<Object>(super.keySet()));
	}

}
