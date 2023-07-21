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

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.ApplicationContext;

import jpl.gds.product.automation.AutomationException;
import jpl.gds.product.automation.hibernate.AutomationSessionFactory;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;

/**
 * Product automation product table data accessor object.
 * 
 * MPCS-8179 - 06/07/16 - Added to AMPCS, updated from original version in MPCS for MSL G9.
 * MPCS-8568 - 12/12/16 - Added fswDirectory column
 */
public class ProductAutomationProductDAO extends AbstractAutomationDAO {
	
	public ProductAutomationProductDAO(ApplicationContext appContext, AutomationSessionFactory sessionFactory) {
		super(appContext, sessionFactory);
	}

	//queried column names as enum
	enum Column{
		
		PRODUCT_ID("productId"),
		PATH("productPath"),
		PARENT("parent"),
		FSW_BUILD_ID("fswBuildId"),
		DICT_VERSION("dictVersion"),
		FSW_DIRECTORY("fswDirectory"),
		SESSION_ID("sessionId"),
		SESSION_HOST("sessionHost"),
		APID("apid"),
		VCID("vcid"),
		SCLK_COARSE("sclkCoarse"),
		SCLK_FINE("sclkFine"),
		IS_COMPRESSED("isCompressed"),
		REAL_TIME_EXTRACTION("realTimeExtraction"),
		ADD_TIME("addTime");
		
		private String columnName;
		
		private Column(String columnName){
			this.columnName = columnName;
		}
		
		@Override
		public String toString(){
			return columnName;
		}
	}
	
	// Setters 
	
	/**
	 * Will check to see if the product is already persisted. If it is not, will
	 * create it.
	 * 
	 * NOTE: This will start a transaction if one is not already active for this
	 * thread and will add the new product, and flush the session. This will not
	 * commit or rollback the transaction. you must catch the exceptions and
	 * rollback if necessary on the calling end of this method.
	 * 
	 * 
	 * This will deal with the transaction for you and flush the session. You
	 * must commit the transaction yourself. If there is an exception, you must
	 * call the rollback yourself.
	 * 
	 * @param productPath
	 *            the path and file name to the product on the filesystem
	 * 
	 * @param parent
	 *            the ProductAutomationProduct that is the parent product of
	 *            this one
	 * 
	 * @param fswBuildId
	 *            the flight software build id of the dictionary for this
	 *            product
	 * 
	 * @param dictVersion
	 *            the dictionary version for this product
	 * 
	 * @param sessionId
	 *            the session that this product is to be associated with
	 * 
	 * @param sessionHost
	 *            the name of the host that has this product
	 * 
	 * @param apid
	 *            the application process ID of this product
	 * 
	 * @param vcid
	 *            virtual channel ID of the product
	 * 
	 * @param sclkCoarse
	 *            the coarse spacecraft clock time value
	 * 
	 * @param sclkFine
	 *            the fine spacecraft clock time value
	 * 
	 * @param isCompressed
	 *            1 if the product is compressed, 0 if not
	 * 
	 * @param realTimeExtraction
	 *            1 if the product was processed by chill down, 0 if not
	 * 
	 * @return a ProductAutomationProduct with values matching the supplied
	 *         arguments
	 * 
	 * @throws AutomationException
	 *             an error is encountered querying for or creating the product
	 */
	public ProductAutomationProduct addProduct(String productPath, ProductAutomationProduct parent,
			Long fswBuildId, String dictVersion, String fswDirectory, Long sessionId,
			String sessionHost, Integer apid, Integer vcid, Long sclkCoarse,
			Long sclkFine, int isCompressed, int realTimeExtraction) throws AutomationException{
		ProductAutomationProduct product = getProduct(productPath);
		
		// Only add the product if it does not already exist.
		if (product == null) {
			product = new ProductAutomationProduct(productPath, parent, fswBuildId, 
				dictVersion, fswDirectory, sessionId, sessionHost, apid, vcid, sclkCoarse, sclkFine, isCompressed, realTimeExtraction);
			
			saveProduct(product);
			
		} else {
			trace.debug("Product already added: " + product.getProductPath());
		}
		
		return product;
	}
	
	private ProductAutomationProduct saveProduct(ProductAutomationProduct product) throws AutomationException {
		try {
			getSession().save(product);
		} catch (Exception e) {
			throw new AutomationException("Could not save product: " + e.toString(), e);
		}
		return product;
	}
	
	// Getters.
	
	/**
	 * This will only do a lookup to find a product that is added and returns
	 * it. If one is not found, returns null. Uses a criteria search and returns
	 * a uniqueResults object.
	 * 
	 * @param productPath
	 *            the filepath for a product
	 * 
	 * @return the ProductAutomationProduct associated with the file
	 */
	public ProductAutomationProduct getProduct(String productPath) {
		return (ProductAutomationProduct) getSession()
				.createCriteria(ProductAutomationProduct.class)
				.add(Restrictions.eq(Column.PATH.toString(), productPath))
				.uniqueResult();
	}

	/**
	 * Does a get() to get the object. Returns null if none found.
	 * 
	 * @param productId
	 *            the ID of the product to be found
	 * 
	 * @return a ProductAutomationProduct with the supplied productId
	 */
	public ProductAutomationProduct getProduct(Long productId) {
		return (ProductAutomationProduct) getSession().get(ProductAutomationProduct.class, productId);
	}

	/**
	 * Calls the Session.contains method and returns the result.
	 * 
	 * @param product a ProductAutomationProduct to be checked
	 * 
	 * @return true if the session contains the supplied product, false otherwise
	 */
	public boolean productExists(ProductAutomationProduct product) {
		return getSession().contains(product);
	}
	
	/**
	 * Checks to see if the product exists based on the path.
	 * 
	 * @param productPath the filepath for a product file
	 * 
	 * @return true if a ProductAutomationProduct is associated with the supplied file, false otherwise
	 */
	public boolean productExists(String productPath) {
		Long count = (Long) getSession().createCriteria(ProductAutomationProduct.class)
				.add(Restrictions.eq(Column.PATH.toString(), productPath))
				.setProjection(Projections.rowCount())
				.list().get(0);
		
		if (count <= 0) {
			return false;
		} else {
			return true;
		}		
	}
}
