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
package jpl.gds.product.automation.hibernate.dao;

import java.util.Collection;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.ApplicationContext;

import jpl.gds.product.automation.AutomationException;
import jpl.gds.product.automation.hibernate.AutomationSessionFactory;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationClassMap;

/**
 * Product automation class map table data accessor object.
 * 
 * MPCS-8179 - 06/07/16 - Added to AMPCS, updated from original
 *          version in MPCS for MSL G9.
 * MPCS-8180 - 07/19/16 - Changed enabled and disabled integers
 *          to an enum, to have more consistency in style. All uses were updated
 *          as well
 */
public class ProductAutomationClassMapDAO extends AbstractAutomationDAO {
	
	public ProductAutomationClassMapDAO(ApplicationContext appContext, AutomationSessionFactory sessionFactory) {
		super(appContext, sessionFactory);
	}

	//column names as enum
	enum Column{
		CLASS_ID("classId"),
		MNEMONIC("mnemonic"),
		NAME("className"),
		ENABLED("enabled");

		private String columnName;
		
		private Column(String columnName){
			this.columnName = columnName;
		}
		
		@Override
		public String toString(){
			return columnName;
		}
	}
	
	// MPC-8180 07/19/16 - changed Enabled and Disabled to enum for more consistent style
	/**
	 * Enum for the enabled and disabled values
	 */
	public enum Abled{
		/** Indicates the classmap object is not being used */
		DISABLED(0),
		/** Indicates the classmap ojbect IS being used */
		ENABLED(1);
		
		int value;
		
		private Abled(int value){
			this.value = value;
		}
		
		/**
		 * Get the numeric value stored by the enum
		 * @return the value of the enum
		 */
		public int value(){
			return value;
		}
	}

	private ProductAutomationClassMap findByProperty(String property, String value) {
		return (ProductAutomationClassMap) getSession().createCriteria(ProductAutomationClassMap.class)
				.add(Restrictions.eq(property, value))
				.uniqueResult();
	}

	/**
	 * Does a lookup based on the given string and returns the object. If not
	 * found returns null.
	 * 
	 * @param actionMnemonic
	 *            a String representing an action that can be performed by the
	 *            class specified in the className of the classmap
	 * 
	 * @return a ProductAutomationClassMap object
	 */
	public ProductAutomationClassMap getClassMap(String actionMnemonic) {
		return findByProperty(Column.MNEMONIC.toString(), actionMnemonic);
	}
	
	/**
	 * Looks up the map class with the given class name.
	 * 
	 * @param className
	 *            the full class name of the classmap object to be found
	 * @return a ProductAutomationClassMap with the given class name
	 */
	public ProductAutomationClassMap getClassMapFromClassName(String className) {
		return findByProperty(Column.NAME.toString(), className);
	}
	
	/**
	 * Fetches all of the classmaps from the database and returns them in id
	 * order.
	 * 
	 * @return a Collection of all ProductAutomationClassMaps stored in the
	 *         database
	 */
	public Collection<ProductAutomationClassMap> getClassMaps() {
		Collection<ProductAutomationClassMap> result = new TreeSet<ProductAutomationClassMap>();
		
		Criteria c = getSession().createCriteria(ProductAutomationClassMap.class)
				.addOrder(Order.asc(Column.CLASS_ID.toString()));
		
		for (Object cm : c.list()) {
			result.add((ProductAutomationClassMap) cm);
		}
		
		return result;
	}

	/**
	 * Changes the enabled flag of the classmap associated with the specified
	 * mnemonic. If it was enabled, it is disabled and vice-versa.
	 * 
	 * @param mnemonic the mnemonic (action type) of the classmap to be toggled
	 * 
	 * @throws AutomationException if the enabled flag value is not valid before being toggled
	 */
	public void toggleEnabled(String mnemonic) throws AutomationException {
		toggleEnabled(getClassMap(mnemonic));
	}
	
	/**
	 * Changes the enabled flag of a classmap. If it was enabled, it is disabled
	 * and vice-versa.
	 * 
	 * @param cm
	 *            the classmap to be toggled
	 * 
	 * @throws AutomationException
	 *             if the enabled flag value is not valid before being toggled
	 */
	public void toggleEnabled(ProductAutomationClassMap cm) throws AutomationException {
		if (cm.getEnabled() == Abled.ENABLED.value()) {
			disableClassMap(cm);
		} else if (cm.getEnabled() == Abled.DISABLED.value()) {
			enableClassMap(cm);
		} else {
			throw new AutomationException("Value of enabled invalid: ");
		}
	}
	
	/**
	 * Sets the enabled flag to enabled of the classmap associated with the
	 * specified mnemonic
	 * 
	 * @param mnemonic
	 *            the mnemonic (action type) of the classmap to be enabled
	 * 
	 * @throws AutomationException
	 *             if there is an error encountered while enabling the classmap
	 */
	public void enableClassMap(String mnemonic) throws AutomationException {
		enableClassMap(getClassMap(mnemonic));
	}	

	/**
	 * Sets the enabled flag to enabled for a classmap
	 * 
	 * @param cm
	 *            classmap to be enabled
	 * 
	 * @throws AutomationException
	 *             if there is an error encountered while enabling the classmap
	 */
	public void enableClassMap(ProductAutomationClassMap cm) throws AutomationException {
		setEnableField(cm, Abled.ENABLED);
	}
	
	/**
	 * Sets the enabled flag to disabled of the classmap associated with the
	 * specified mnemonic
	 * 
	 * @param mnemonic
	 *            the mnemonic (action type) of the classmap to be disabled
	 * 
	 * @throws AutomationException
	 *             if there is an error encountered while disabling the classmap
	 */
	public void disableClassMap(String mnemonic) throws AutomationException {
		disableClassMap(getClassMap(mnemonic));
	}
	
	/**
	 * Sets the enabled flag to disabled for a classmap
	 * 
	 * @param cm
	 *            classmap to be disabled
	 * 
	 * @throws AutomationException
	 *             if there is an error encountered while disabling the classmap
	 */
	public void disableClassMap(ProductAutomationClassMap cm) throws AutomationException {
		setEnableField(cm, Abled.DISABLED);
	}
	
	/**
	 * Takes a classmap and will set the enable field to a specified value in
	 * the db. This must be called inside of a transaction.
	 * 
	 * @param cm
	 *            the classmap that will have its enabled flag altered
	 * @param enabled
	 *            the new value of the enabled flag
	 * @throws AutomationException
	 *             if the new value is invalid or an error is encountered while
	 *             changing the enabled flag
	 */
	private void setEnableField(ProductAutomationClassMap cm, Abled enabled) throws AutomationException {
		if (Math.abs(enabled.value()) > 1) {
			throw new AutomationException("Enabled value must be either 0 or 1");
		}
		
		// Don't do it if it is already set.
		if (cm.getEnabled() != enabled.value()) {
			cm.setEnabled(enabled.value());
			
			try {
				getSession().saveOrUpdate(cm);
			} catch (Exception e) {
				throw new AutomationException(e.getMessage());
			}
		}
	}
	
}
