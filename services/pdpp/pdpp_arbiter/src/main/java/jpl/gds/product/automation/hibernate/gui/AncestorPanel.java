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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import jpl.gds.product.automation.hibernate.dao.ProductAutomationStatusDAO;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;
import jpl.gds.product.automation.hibernate.gui.AncestorMap.AncestorMapLoadException;

/**
 * In the automation GUI there is a Lineage View tab. Under this tab the user
 * can view the products that match the search criteria given. This class
 * organizes a product and all of its related products in a tree form. Depending
 * on the settings either the originating product or the final product is the
 * root and the descendant or ancestor products are diplayed beneath it
 * respectively.
 * 
 * Each displayed product is an AncestorPanel that can contain additional
 * products that are its ancestor or descendants that are also displayed as
 * AncestorPanels.
 * 
 *
 * @version - MPCS-8182 - 08/08/16 - Added to and updated for AMPCS.
 */
@SuppressWarnings("serial")
public class AncestorPanel extends JPanel implements Comparable<AncestorPanel> {
	private static final int GRIDX_ZERO = 0;
	private static final Insets INSET_WITH_CHILDREN = new Insets(0, 55, 0, 0);
	private static final Insets INSET_NO_CHILDREN = new Insets(0, 71, 0, 0);
	private static final Insets ZERO_INSET = new Insets(0, 0, 0, 0);
	private static final Insets INSET_FP_NO_CHILDREN = new Insets(0, 34, 0, 10);
	private static final Insets INSET_FP_W_CHILDREN = new Insets(0, 10, 0, 10);
	private static final Dimension MIN_SIZE = new Dimension(16, 24);
	
	private ProductAutomationProduct product;
	private JLabel productLabel;
	private JLabel actionType;
	
	private ArrayList<JPanel> children;
	private boolean firstProduct;
	private boolean hasDescendants;
	private boolean bottomUp;
	private boolean includeFilePath;
	
	private int gridy;
	private Long requiredLineage;
	
	private JToggleButton productButton;
	private MouseListener clickListener;
	private AncestorMap ancestorMap;
	
	/**
	 * Constructor that creates an empty panel
	 */
	public AncestorPanel(AncestorMap ancestorMap) {
		super(new GridBagLayout());
		
		this.ancestorMap = ancestorMap;

		children = new ArrayList<JPanel>();
		gridy = 0;
		bottomUp = false;
		includeFilePath = false;
		
		clickListener = null;
	}
	
	/**
	 * Constructor that creates an AncestorPanel with the given product. 
	 * 
	 * @param product
	 *            the product to be displayed by this panel
	 * @param bottomUp
	 *            TRUE if the panel starts with the last descendant product,
	 *            false if it starts with the greatest ancestor
	 */
	public AncestorPanel(ProductAutomationProduct product, boolean bottomUp, AncestorMap ancestorMap) {
		this(product, false, new Long(-1), bottomUp, ancestorMap);
	}
	
	/**
	 * Constructor that creates an AncestorPanel with the given product.
	 * 
	 * @param product
	 *            the product to be displayed by this panel
	 * @param firstProduct
	 *            true if this product is the ancestor of all related products,
	 *            false if not
	 * @param bottomUp
	 *            TRUE if the panel starts with the last descendant product,
	 *            false if it starts with the greatest ancestor
	 */
	public AncestorPanel(ProductAutomationProduct product, boolean firstProduct, boolean bottomUp, AncestorMap ancestorMap) {
		this(product, firstProduct, new Long(-1), bottomUp, ancestorMap);
	}
	
	/**
	 * If requiredLineage is set only display parents and children in the direct
	 * lineage of the product.
	 * 
	 * @param product
	 *            the product to be displayed by this panel
	 * @param firstProduct
	 *            true if this product is the anscestor of all related products,
	 *            false if not
	 * @param requiredLineage
	 *            Greater than zero if all lineage is added, only direct parents
	 *            or children are shown if not
	 * @param bottomUp
	 *            TRUE if the panel starts with the last descendant product,
	 *            false if it starts with the greatest ancestor
	 */
	public AncestorPanel(ProductAutomationProduct product, boolean firstProduct, Long requiredLineage, boolean bottomUp, AncestorMap ancestorMap) {
		this(ancestorMap);
		this.product = product;
		this.requiredLineage = requiredLineage;
		this.firstProduct = firstProduct;
		this.bottomUp = bottomUp;
		
		init();
	}
	
	/**
	 * Sets the parameter that indicates to show the full file path for the
	 * products. Will set it for all children and then do an update on the
	 * window.
	 * 
	 * @param fp
	 *            true if the full path is shown, false otherwise
	 */
	public void setFullFilePath(boolean fp) {
		includeFilePath = fp;
		
		// Reset the value of the product label.
		productLabel.setText(includeFilePath ? product.getProductPath() : getProductName(product.getProductPath()));
		
		for (JPanel c : children) {
			((AncestorPanel) c).setFullFilePath(includeFilePath);
		}
	}
	
	/**
	 * Sets the mouse listener in self. Will add it to all children created.
	 * 
	 * @param ml
	 *            the mouse listener to be used
	 */
	public void setMouseListener(MouseListener ml) {
		clickListener = ml;
		// Set ml for this.
		addMouseListener(clickListener);

		// Pass the listener to any children as well.
		for (JPanel c : children) {
			c.addMouseListener(clickListener);
		}
	}
	
	/**
	 * When this is focused, will change the background color of this and all
	 * children.
	 * 
	 * @param bgc
	 *            the background color of this ancestor panel
	 * @param b
	 *            the border of this ancestor panel
	 */
	public void changeBackgroundColor(Color bgc, Border b) {
		setBorder(b);
		setBackground(bgc);
		changeChildrenBackgroundColor(bgc);

	}
	
	/**
	 * Change the background color for all of the child products in this
	 * ancestor map
	 * 
	 * @param bgc
	 *            the new background color to be set
	 */
	public void changeChildrenBackgroundColor(Color bgc) {
		for (Component c : children) {
			c.setBackground(bgc);
		}
	}
	
	/**
	 * Gets and returns the AncestorPanel that is the first child added to this
	 * AncestorPanel
	 * 
	 * @return a child AncestorPanel for this panel
	 */
	public JPanel getFirstChild() {
		JPanel result = null;
		
		if (!children.isEmpty()) {
			result = children.get(0);
		}
		
		return result;
	}
	
	/**
	 * Gets and returns the AncestorPanel that is the last child added to this
	 * AncestorPanel
	 * 
	 * @return a child AncestorPanel for this panel
	 */
	public JPanel getLastChild() {
		JPanel result = null;
		
		if (!children.isEmpty()) {
			result = children.get(children.size() - 1);
		}
		
		return result;
	}
	
	/**
	 * Finds the last ancestor down the chain. If none are found below this,
	 * this is returned.
	 * 
	 * @return the last ancestor in the chain that this panel belongs to
	 */
	public JPanel getLastAncestor() {
		JPanel result = getLastChild();
		JPanel tmp = result;
		
		while (tmp != null) {
			tmp = ((AncestorPanel) result).getLastChild();

			if (tmp != null) {
				result = tmp;
			}
		}
		
		return result == null ? this : result;
	}
	
	/**
	 * Checks if selected is in children. If it is, will return the next child
	 * if there is one, or null if there are no more children, or no children at
	 * all.
	 * 
	 * @param selected
	 *            the panel a child panel is being retrieved from
	 * @return the panel of the next child or null if there is no other child to
	 *         be returned
	 */
	public JPanel getNextChild(JPanel selected) {
		JPanel result = null;
		
		if (children.contains(selected) && children.size() > 1 && children.indexOf(selected) < children.size() - 1) {
			result = children.get(children.indexOf(selected) + 1);
		}
		
		return result;
	}
	
	/**
	 * Checks if selected is in children. If it is, will return the previous
	 * child if there is one, or null if selected is the first child or there
	 * are no children at all.
	 * 
	 * @param selected
	 *            the panel a child panel is being retrieved from
	 * @return a child panel or null if there is no other child to be returned
	 */
	public JPanel getPreviousChild(JPanel selected) {
		JPanel result = null;
		
		if (children.contains(selected) && children.size() > 1 && children.indexOf(selected) > 0) {
			result = children.get(children.indexOf(selected) - 1);
		}
		
		return result;
	}
	
	/**
	 * Puts a product in this AncestorPanel
	 * 
	 * @param product
	 *            the product being identified by this panel
	 */
	public void setProduct(ProductAutomationProduct product) {
		this.product = product;
	}
	
	/**
	 * Initializes this panel with a product and relational information
	 * @param product
	 *            the product to be displayed by this panel
	 * @param requiredLineage
	 *            Greater than zero if all lineage is added, only direct parents
	 *            or children are shown if not
	 * @param firstProduct
	 *            true if this product is the ancestor of all related products,
	 *            false if not
	 */
	public void init(ProductAutomationProduct product, Long requiredLineage, boolean firstProduct) {
		this.product = product;
		this.requiredLineage = requiredLineage;
		this.firstProduct = firstProduct;
		
		init();		
	}
	
	/**
	 * Adds the child to the layout as well as to the internal list of children.
	 * 
	 * @param child
	 *            the child panel to be added to this panel
	 */
	public void addChild(JPanel child) {
		children.add(child);
		
		add(child, 
				new GridBagConstraints(GRIDX_ZERO, //gridx, 
					++gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.NORTHWEST, // anchor, 
					GridBagConstraints.HORIZONTAL, // fill - no fill for text controls.
					hasDescendants ? INSET_WITH_CHILDREN : INSET_NO_CHILDREN,
					0, // ipadx, 
					0 // ipady
				));		
		
		revalidate();
	}
	
	/**
	 * Looks at the products last status and the actions and figures out what
	 * Icon to use.
	 * 
	 * @param product
	 *            the ProductAutomationProduct in question
	 * @return the icon that is appropriate for the supplied product
	 */
	private Icon getIconForProduct(ProductAutomationProduct product) {
		if (product == null) {
			return null;
		} 

		String parentAction;
		String lastStatName;
		
		try {
			parentAction = product.getParent().getActions().last().getActionName().getMnemonic();
		} catch (Exception e) {
			parentAction = null;
		}
		
		// This should never happen, but do a check anyway.
		try {
			lastStatName = product.getStatuses().last().getStatusName();
		} catch (Exception e) {
			lastStatName = null;
		}
		
		Icon result;
		if (parentAction == null && lastStatName == null) {
			result = IconFactory.pendingAction();
		} else if (parentAction == null) {
			// This guy either failed or is chill down complete.
			if (ProductAutomationStatusDAO.Status.COMPLETE_PRE_PB.toString().equals(lastStatName)) {
				result = IconFactory.downlinkComplete();
			} else if (ProductAutomationStatusDAO.Status.FAILED.toString().equals(lastStatName)) {
				result = IconFactory.downlinkFailed();
			} else if (ProductAutomationStatusDAO.Status.COMPLETED.toString().equals(lastStatName)) {
				result = IconFactory.downlink();
			} else {
				result = IconFactory.downlinkPending();
			}
		} else {
			result = IconFactory.getActionIcon(parentAction, lastStatName);
		}
		
		return result;
	}
		

	/**
	 * Sets the icon for this panel's product. Will be the action that GOT this
	 * product to this point by looking at the action on the parent. If there is
	 * no parent, will use the downlink icon.
	 * 
	 * @param product
	 *            the product being used to set this panel's icon
	 */
	private void setActionIcon(final ProductAutomationProduct product) {
		Thread t = new Thread() {
			public void run() {
				
				final Icon i = getIconForProduct(product);
				
				// Now call the invoke later.
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						actionType.setIcon(i);
						revalidate();
					}
				});
			}
		};				
		
		t.start();
	}
	 
	private void init() {
		// No matter the mode of viewing / building, need to set up the internal components.
		productButton = new JToggleButton();
		productButton.setFocusable(false);
		productButton.setContentAreaFilled(false);
		productButton.setBorderPainted(false);
		
 		actionType = new JLabel();
		productLabel = new JLabel();

		AncestorPanel initChild;
		
 		if (!bottomUp && firstProduct && product.getParent() != null) {
 			initChild = initTopDown();
 		} else {
 			initChild = null;
 		}
 		
 		// Set the product label and action label.
 		productLabel.setText(getProductName(product.getProductPath()));
 		
 		setActionIcon(getProduct());
 		
 		addListeners();
 		
 		setHasChildren();
 		doPanelLayout();
 		
 		// Add the child.
 		if (initChild != null) {
 			setOpenTreeIcon();
			initChild.setMouseListener(clickListener);
 			addChild(initChild);
 		} else {
 			setClosedTreeIcon();
 		}
	}
	
	/**
	 * Need to figure out if this panel has children or not. Remember, this
	 * could also mean if it has ancestors in bottomUp mode. Will check the
	 * ancestor map or check if the product has parents and then set the value
	 * accordingly.
	 */
	private void setHasChildren() {
		if (bottomUp) {
			// Pretty simple, is there a parent?
			hasDescendants = product.getParent() != null;
		} else {
			hasDescendants = ancestorMap.hasDescendants(getProductId());
		}
		
 		// If no descendants hide the product button.
 		if (!hasDescendants) {
 			productButton.setVisible(false);
 		}
	}
	
	/**
	 * Will check if the product passed in has ancestors because they were
	 * outside of the query range. Will alter product and add children up to the
	 * top level ancestor.
	 */
	private AncestorPanel initTopDown() {
		AncestorPanel child = null;

		// If first product and it is not a base ancestor, need to go up and find the base parent and set that as this.product.
		requiredLineage = product.getProductId();
		
		do {
			if (child == null) {
				child = new AncestorPanel(product, false, requiredLineage, bottomUp, ancestorMap);
			} else {
				child.addChild(new AncestorPanel(product, false, requiredLineage, bottomUp, ancestorMap));
			} 
			
			product = product.getParent();

		} while (product.getParent() != null);
		
		return child;
	}
	
	private void addListeners() {
		productButton.addActionListener(new ActionListener() {
			
			@Override
			/**
			 * GTK+ shows the selected Icon when hovering over a button.  have to change the 
			 * icon here instead.
			 */
			public void actionPerformed(ActionEvent e) {
				if (productButton.isSelected()) {
					showChildren();
				} else {
					hideChildren();
				}
			}
		});
	}
	
	/**
	 * The first row will always be the product button. Every row after that
	 * will be on of the children.
	 */
	private void doPanelLayout() {
		// Set min size, which really want to set min height.
		actionType.setPreferredSize(MIN_SIZE);
		
		// If this is the first panel, do not add the arrow.  Else do it.
		int gridx = 0;
		
		add(productButton, 
				new GridBagConstraints(gridx, //gridx, 
					gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.NORTHWEST, // anchor, 
					0, // fill - no fill for text controls.
					ZERO_INSET,
					0, // ipadx, 
					0 // ipady
				));		

		add(actionType, 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					firstProduct && hasDescendants ? INSET_FP_W_CHILDREN : INSET_FP_NO_CHILDREN,
					0, // ipadx, 
					0 // ipady
				));				
		
		add(productLabel, 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					0, // fill - no fill for text controls.
					new Insets(0, 5, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));
		
		add(Box.createHorizontalGlue(), 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0, // weighty, 
					GridBagConstraints.WEST, // anchor, 
					GridBagConstraints.HORIZONTAL, // fill - no fill for text controls.
					ZERO_INSET,
					0, // ipadx, 
					0 // ipady
				));
	}
	
	/**
	 * Need to find the Y value of this panel. However,each child will be the Y
	 * value based on its position in the parents container. This will
	 * recursively compute the Y by getting the position from each parent and
	 * adding them up.
	 * 
	 * @return the Y value of this panel
	 */
	public int computeY() {
		return computeY(0);
	}
	

	private int computeY(int startY) {
		if (getParent() instanceof AncestorPanel) {
			return ((AncestorPanel)getParent()).computeY(startY + getY());
		} else {
			return startY + getY();
		}
	}
	
	/**
	 * Creates children ancestor viewer panels and shows them. Needs to be more
	 * robust since it is a public method. If there are children already in the
	 * children map, this will be a no-op.
	 */
	public void showChildren() {
		if (children.isEmpty()) {
			setOpenTreeIcon();
			
			if (bottomUp && product.getParent() != null) {
				// Bottom up.  Just get the parent and treat as a child.
				AncestorPanel nc = new AncestorPanel(product.getParent(), false, bottomUp, ancestorMap);
				nc.setMouseListener(clickListener);
				nc.setFullFilePath(includeFilePath);

				addChild(nc);
			} else if (!bottomUp) {
				// Top down.
				productButton.setSelected(true);
				
				try {
					Collection<ProductAutomationProduct> kidz = requiredLineage > 0 ?
							ancestorMap.getDirectDescendantsCollection(product.getProductId(), requiredLineage) :
							ancestorMap.getDescendantsObjects(product);
							
					for (ProductAutomationProduct p : kidz) {
						AncestorPanel cp = new AncestorPanel(p, false, requiredLineage, bottomUp, ancestorMap);
						cp.addMouseListener(clickListener);
						cp.setFullFilePath(includeFilePath);
						
						addChild(cp);
					}
				} catch (AncestorMapLoadException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				// Got here because bottomUp and we are at the end of the chain.  Do nothing.
			}
	
			if (!children.isEmpty()) {
				changeChildrenBackgroundColor(getBackground());
				revalidate();
				repaint();
			}
		}
	}
	
	/**
	 * Used for key bindings 
	 */
	private void setOpenTreeIcon() {
		productButton.setSelected(true);
		productButton.setIcon(IconFactory.treeOpen());
	}
	
	private void setClosedTreeIcon() {
		productButton.setSelected(false);
		productButton.setIcon(IconFactory.treeClosed());
	}
	
	/**
	 * Hides children by deleting them. No reason to hold things around if they
	 * are not needed.
	 */
	public void hideChildren() {
		setClosedTreeIcon();
		
		for (JPanel c : children) {
			remove(c);
		}
		
		children.clear();
		gridy = 1;
		
		revalidate();
		repaint();
	}
	
	/**
	 * Takes a product file path and will strip off the file path.
	 * 
	 * @param fullPath
	 *            the full path and file name of the product for this panel
	 * @return the name of the product
	 */
	private String getProductName(String fullPath) {
		File f = new File(fullPath);
		
		return f.getName();
	}

	/**
	 * Get the ProductAutomationProduct identified by this panel
	 * 
	 * @return the product in this panel
	 */
	public ProductAutomationProduct getProduct() {
		return product;
	}
	
	/**
	 * Gets just the product ID from the ProductAutomationProduct in this panel
	 * 
	 * @return the product ID for the product in this panel
	 */
	public Long getProductId() {
		return getProduct().getProductId();
	}
	
	@Override
	/**
	 * Compares the products in the two panels
	 * 
	 * @return 0 if the products are identical, non-zero if they are not
	 */
	public int compareTo(AncestorPanel o) {
		return getProduct().compareTo(o.getProduct());
	}
}