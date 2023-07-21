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
package jpl.gds.product.automation.hibernate.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import jpl.gds.product.automation.hibernate.dao.ProductAutomationProductDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationUserDAO;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;
import jpl.gds.shared.exceptions.NestedException;
import jpl.gds.shared.log.TraceManager;


/**
 * Builds an ancestral map of products in the database.  Will weakly store a products ancestors as 
 * well as a products parent.  If the info is not found in the map will do database lookups to find it.
 * All of the information stared will be productIds.
 * 
 * All loading to the maps are synchronized and therefore are private.  
 * 
 * This is designed to be used to help with on demand loading of product information from the 
 * database.  
 * 
 * This is a singleton class, so always use the get instance.
 * 
 * MPCS-8182 - 08/08/16 - Added to and updated for AMPCS.
 */

public class AncestorMap {
	/**
	 *  R8 refactor - This should be used as a singleton and be the same regardless of context.
	 */
	private static final long INITIAL_LOAD_TIME_DAYS = 5;
	private static final int DAYS_TO_MS_CONVERSION = 24 * 60 * 60 * 1000;
	private static final Long NO_PARENT_DEFAULT = new Long(-1);
	
	private final Map<Long, Collection<Long>> descendants;
	private final Map<Long, Long> ancestors;
	private final ProductAutomationProductDAO productDao;
	private final ProductAutomationUserDAO userDao;
	
	public AncestorMap(final ProductAutomationProductDAO productDao, final ProductAutomationUserDAO userDao) {
		this((int) INITIAL_LOAD_TIME_DAYS, productDao, userDao);
	}
	
	
	/**
	 * Instantiates and returns an AncestorMap object that is set to only
	 * 
	 * @param initialLoadTimeDays
	 *            How many days back to load from the current time
	 */
	public AncestorMap(final int initialLoadTimeDays, final ProductAutomationProductDAO productDao, final ProductAutomationUserDAO userDao) {
		descendants = Collections.synchronizedMap(new WeakHashMap<Long, Collection<Long>>());
		ancestors = Collections.synchronizedMap(new WeakHashMap<Long, Long>());

		this.productDao = productDao;
		this.userDao = userDao;
	}
	
	/**
	 * Loads data from the database from loadTimeDays to current time.
	 * 
	 * @param loadTimeDays the number of days previous to the current time to load
	 */
	@SuppressWarnings("unused")
	private void loadForDays(final long loadTimeDays) {
		final long cms = System.currentTimeMillis();
		
		load(cms - loadTimeDays * DAYS_TO_MS_CONVERSION, cms);
	}
	
	/**
	 * Given the upper and lower bound, will load the map with products found in
	 * that time range. This will synchronize on the maps as they are updated.
	 * 
	 * @param lowerBoundMS
	 *            A Unix time for the oldest product to be loaded
	 * @param upperBoundMS
	 *            A Unix time for the newest product to be loaded
	 */
	private void load(final Long lowerBoundMS, final Long upperBoundMS) {
		for (final ProductAutomationProduct product : userDao.getProductsForLoad(lowerBoundMS, upperBoundMS)) {
			loadSingleProduct(product);
		}
	}

	private void loadSingleProduct(final Long productId) {
		loadSingleProduct(productDao.getProduct(productId));
	}

	/**
	 * This is the only method that will do any insertions into the internal
	 * maps. They actual spots doing this are synchronized.
	 * 
	 * @param product
	 *            the product that is to have its descendants loaded
	 */
	private void loadSingleProduct(final ProductAutomationProduct product) {
		synchronized(descendants) {
			descendants.put(product.getProductId(), userDao.getProductDescendants(product));
		}
		
		// Now for each child in the list, call the same thing.
		for (final Long cid : descendants.get(product.getProductId())) {
			// For each child that is found, need to add current product as the ancestor.
			synchronized(ancestors) {
				ancestors.put(cid, product.getProductId());
			}
			
			loadSingleProduct(cid);
		}
	}
	
	private void loadSingleProductParents(final Long productId) {
		final ProductAutomationProduct product = productDao.getProduct(productId);

		// Only need to do the work if the product was found.
		if (product != null) {
			loadSingleProductParents(product);
		}
	}
	
	/**
	 * Does a load of a products parents. This will fill in all the blanks if
	 * need be.
	 * 
	 * @param product
	 *            the product that is looking for a parent
	 */
	private void loadSingleProductParents(final ProductAutomationProduct product) {
		ProductAutomationProduct p = product;
		
		do {
			final Long parentId = p.getParent() == null ? NO_PARENT_DEFAULT : p.getParent().getProductId();
			
			synchronized(ancestors) {
				ancestors.put(p.getProductId(), parentId);
			}
			
			p = p.getParent();
		} while (p != null);
	}
	
	/**
	 * Reports whether the specified product has descendant products or not
	 * 
	 * @param product
	 *            The product being checked for descendants
	 * 
	 * @return TRUE if the product has descendants, FALSE if not
	 */
	public boolean hasDescendants(final ProductAutomationProduct product) {
		return hasDescendants(product.getProductId());
	}
	
	/**
	 * Looks up if the product has any descendants. If an exception occurs,
	 * returns false.
	 * 
	 * @param productId
	 *            the product ID for the product being checked for descendants
	 * @return TRUE if the product has descendants, FALSE if not
	 */
	public boolean hasDescendants(final Long productId) {
		boolean result = false;
		
		try {
			result = !getDescendants(productId).isEmpty();
		} catch (final AncestorMapLoadException e) {
            TraceManager.getDefaultTracer().warn("Ancestor Map was unable to load: " + e.getMessage());

		}
		
		return result;
	}
	
	/**
	 * Does a lookup for the product in the internal map. If not found will do
	 * an ambitious load of a time range around that product. if the map returns
	 * null, means that the lookup has been removed and will reload.
	 * 
	 * 
	 * @param product
	 *            the product for which descendants are being retrieved
	 * @return a Collection of the product IDs for the descendants of the
	 *         original product
	 * @throws AncestorMapLoadException
	 *             - If a load failed and returns a null value.
	 */
	public Collection<Long> getDescendants(final ProductAutomationProduct product) throws AncestorMapLoadException {
		return getDescendants(product.getProductId());
	}
	
	/**
	 * Does a lookup for the product in the internal map. If not found will do
	 * an ambitious load of a time range around that product. if the map returns
	 * null, means that the lookup has been removed and will reload.
	 * 
	 * 
	 * @param productId
	 *            the product ID of the product for which descendants are being
	 *            retrieved
	 * @return a Collection of the product IDs for the descendants of the
	 *         original product
	 * @throws AncestorMapLoadException
	 *             - If a load failed and returns a null value.
	 */
	public Collection<Long> getDescendants(final Long productId) throws AncestorMapLoadException {
		if (!descendants.containsKey(productId)) {
			loadSingleProduct(productId);
		}
		
		final Collection<Long> d = descendants.get(productId);
		
		if (d == null) {
			throw new AncestorMapLoadException("Failed to load product to internal map.  ProductID=" + productId);
		} else {
			return d;
		}
	}
	
	/**
	 * Checks to see if one product is the descendant of another. If it is a
	 * descendant, then it is returned.
	 * 
	 * @param productId
	 *            the product ID for which a specific descendant is being
	 *            located
	 * @param requiredDescendant
	 *            the product ID of the potential descendant in question
	 * @return A single ProductAutomationProduct returned as an Array if the
	 *         descendant was found. An empty list is returned if the
	 *         requiredDescendant was not a descendant of the original product
	 * @throws AncestorMapLoadException
	 */
	public Collection<ProductAutomationProduct> getDirectDescendantsCollection(final Long productId, final Long requiredDescendant) throws AncestorMapLoadException {
		final Long d = getDirectDescendants(productId, requiredDescendant);
		
		Collection<ProductAutomationProduct> result;

		// Don't want to return the same product as a descendant.  The recursive method to find products will do that.  So
		// do a check here to save the user from the headache.
		if (d > 0 && d != productId) {
			result = Arrays.asList(productDao.getProduct(d));
		} else {
			result = Collections.emptyList();
		}
		
		return result;
	}
	
	/**
	 * Finds the single descendant of the product that is in the lineage of
	 * requiredDescendant. Because it is recursive, this if the last child in
	 * the lineage is passed in, that id will be given. So this is used
	 * internally to this class only and the calling class deals with this
	 * issue.
	 * 
	 * @param productId
	 *            the product being checked for a descendant
	 * @param requiredDescendant
	 *            the descendant being located
	 * @return the product ID of the descendant being located, or -1 if the
	 *         requiredDescendant is not a descendant of the target product
	 * @throws AncestorMapLoadException
	 */
	private Long getDirectDescendants(final Long productId, final Long requiredDescendant) throws AncestorMapLoadException {
		if (productId.equals(requiredDescendant)) {
			return productId;
		} else {
			for (final Long d : getDescendants(productId)) {
				if (getDirectDescendants(d, requiredDescendant) > 0) {
					// Set child equal to productId since it was found.  Want to propogate the productId
					// This method was called with up to the top caller.
					return d;
				}
			}
		}
		
		return NO_PARENT_DEFAULT;
	}
	
	/**
	 * Looks up all immediate descendants of the supplied product. The resulting
	 * products are added to a collection and returned
	 * 
	 * @param product
	 *            the product that descendants are being retrieved
	 * @return a collection of all of the products that are immediate
	 *         descendants of the supplied product
	 * @throws AncestorMapLoadException
	 *             if there is a problem with the AncestorMap
	 */
	public Collection<ProductAutomationProduct> getDescendantsObjects(final ProductAutomationProduct product) throws AncestorMapLoadException {
		final ArrayList<ProductAutomationProduct> results = new ArrayList<ProductAutomationProduct>();

		for (final Long childId : getDescendants(product)) {
			results.add(productDao.getProduct(childId));
		}
		
		return results;
	}
	
	/**
	 * Does a lookup to find the parent product ID of the supplied product ID.
	 * Will throw if it failed to load the products into the internal map.
	 * 
	 * @param productId
	 *            the product ID that is looking for its parent
	 * @return the product ID of the parent product
	 * @throws AncestorMapLoadException
	 *             if there is a problem with the AncestorMap
	 */
	public Long getAncestors(final Long productId) throws AncestorMapLoadException {
		Long parentId = ancestors.get(productId);
		
		if (parentId == null) {
			// It is not there, so do a load.
			loadSingleProductParents(productId);
		}
		
		// Try another lookup.  If it is still null, throw, else return.
		parentId = ancestors.get(productId);
		
		if (parentId == null) {
			throw new AncestorMapLoadException("Failed to load ancestors of product with productId: " + productId);
		} else {
			return parentId;
		}
	}
	
	/**
	 * Does a lookup for the product ancestor.
	 * 
	 * @param product
	 *            the product looking for its parent
	 * @return the product ID of the parent product
	 * @throws AncestorMapLoadException
	 *             if there is a problem with the AncestorMap
	 */
	public Long getAncestors(final ProductAutomationProduct product) throws AncestorMapLoadException {
		// First do a lookup for the product.  If we get the no parent default, return null.  
		// If null is returned, need to do a lookup and then get the value again.
		return getAncestors(product.getProductId());
	}
	
	/**
	 * Exception class used when there is an issue with loading or using the
	 * AncestorMap
	 * 
	 */
	@SuppressWarnings("serial")
	public class AncestorMapLoadException extends NestedException {
		 /**
	     * Creates an instance of AutomationException.
	     */
	    public AncestorMapLoadException() {
	        super();
	    }

	    /**
	     * Creates an instance of AutomationException with the given detail text.
	     * @param message the exception message
	     */
	    public AncestorMapLoadException(final String message) {
	        super(message);
	    }

	    /**
	     * Creates an instance of AutomationException with the given detail text and triggering
	     * Throwable.
	     * @param message the exception message
	     * @param rootCause the exception that Trigger this one
	     */
	    public AncestorMapLoadException(final String message, final Throwable rootCause) {
	        super(message, rootCause);
	    }		
	}
}
