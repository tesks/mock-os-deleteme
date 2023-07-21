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
package jpl.gds.product.automation.hibernate.gui.models;

import java.util.ArrayList;
import java.util.Collection;

import jpl.gds.product.PdppApiBeans;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationUserDAO;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationLog;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;
import org.springframework.context.ApplicationContext;

/**
 * The table model for the logs table.  Deals with holding onto logs and is optimized
 * to not load logs that already exist. 
 * 
 * MPCS-8182 - 08/11/16 - Added to and updated for AMPCS.
 */
@SuppressWarnings("serial")
public class LogsTableModel extends AbstractGuiTableModel<ProductAutomationLog> {

	private ProductAutomationUserDAO userDao;
	private ApplicationContext appContext;

	/**
	 * Create a LogsTableModel for displaying ProductAutomationLog objects
	 * 
	 * @param columnHeaderNames
	 *            names of the columns to be displayed
	 */
	public LogsTableModel(Object[] columnHeaderNames, ProductAutomationUserDAO userDao, ApplicationContext appContext) {
		super(columnHeaderNames);

		this.userDao = userDao;
		this.appContext = appContext;
	}
	
	/**
	 * Default LogsTableModel constructor that sets the column names
	 */
	public LogsTableModel(ApplicationContext appContext) {
		super(new Object[] {
				"Level", 
				"Host", 
				"Reporter",
				"Event Time", 
				"Product", 
				"Message", 
				"id"
		});

		this.appContext = appContext;
		this.userDao = (ProductAutomationUserDAO) appContext.getBean(PdppApiBeans.USER_DAO);
	}

	/**
	 * Add all of the supplied ProductAutomationLog objects to the table. All
	 * log objects that are "stale" (not represented in the added set) are
	 * removed.
	 * 
	 * @param logs
	 *            a Collection of ProductAutomationLog objects to add to the
	 *            table
	 */
	@Override
	public void addRows(Collection<ProductAutomationLog> logs) {
		ArrayList<Long> inputIds = new ArrayList<Long>();
		
		for (ProductAutomationLog log : logs) {
			addRow(log);
			
			inputIds.add(log.getLogId());
		}

		removeStaleObjects(inputIds);
	}

	/**
	 * An in-between at this point for getting the product path until the log is
	 * mapped correctly.
	 * 
	 * @param productId
	 *            the productID of the product we want a full path for
	 * @return the full file path for a product as a String
	 */
	private String getProductPath(Long productId) {
		String path;

		// productId can be -1 if the log is not associated with any specific product
		if (productId != null && productId != -1) {
			ProductAutomationProduct product = (ProductAutomationProduct) userDao
					.getSession()
					.get(ProductAutomationProduct.class, productId);
			
			if (product != null) {
				path = product.getProductPath();
			} else {
				path = null;
			}
		} else {
			path = null;
		}

		return path;
	}
	
	/**
	 * Add a single ProductAutomaitonLog to the table
	 * 
	 * @param log the ProductAutomationLog to be added to the table
	 */
	@Override
	public void addRow(ProductAutomationLog log) {
		addRow(new Object[] {
				log.getLevel(),
				log.getHost(),
				log.getProcessorId(), // Reporter
				log.getEventTime(),
				getProductPath(log.getProduct()),
//				log.getProduct(),
				log.getMessage(),
				log.getLogId()
		});
	}

}
