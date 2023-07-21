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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import jpl.gds.product.automation.hibernate.dao.ProductAutomationUserDAO;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;
import jpl.gds.product.automation.hibernate.gui.AncestorMap;
import jpl.gds.product.automation.hibernate.gui.AncestorPanel;
import jpl.gds.product.automation.hibernate.gui.renderers.HistoryTableCellRenderer;

/**
 * Tried to use a jtable but it did not work the way I needed it to work and would be more
 * trouble than it is worth.  This holds onto products to be viewed and builds the lineage
 * display.  
 * 
 * Also includes a table to show metadata for the product when the individual deal is clicked.
 * 
 * MPCS-8182 - 08/11/16 - Added to and updated for AMPCS.
 */
@SuppressWarnings("serial")
public class LineageCustomTable extends JPanel {
	private static final int ZERO = 0;
	
	private static final Color SELECTED_BG = Color.LIGHT_GRAY;
	private static final Color UNSELECTED_BG = UIManager.getColor("Panel.background");
	private static final int META_HEIGHT = 0;
	private static final int META_WIDTH = 400;
	private static final double SCLK_FINE_DIVISOR = 1000000;
	private static final int JUST = -15;
	private static final String UP_ACTION = "upArrow";
	private static final String DOWN_ACTION = "downArrow";
	private static final String LEFT_ACTION = "leftArrow";
	private static final String RIGHT_ACTION = "rightArrow";

	private static final String NO_PARENT = "No Parent";
	private static final String METADATA_FORMAT;
	
	static {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("%" + JUST + "s%%d\n", "Product DB ID"));
		sb.append(String.format("%" + JUST + "s%%d\n", "Apid"));
		sb.append(String.format("%" + JUST + "s%%d\n", "FSW Build ID"));
		sb.append(String.format("%" + JUST + "s%%s\n", "Dictionary"));
		sb.append(String.format("%" + JUST + "s%%d\n", "Session ID"));
		sb.append(String.format("%" + JUST + "s%%s\n", "Session Host"));
		sb.append(String.format("%" + JUST + "s%%10.6f\n", "Sclk"));
		sb.append(String.format("%" + JUST + "s%%s\n", "Product"));
		sb.append(String.format("%" + JUST + "s%%s\n", "Parent"));
		
		METADATA_FORMAT = sb.toString();
	}
	
	private TreeSet<AncestorPanel> products;
	private TreeSet<Long> containedProducts;
	private TreeSet<Long> productsToRemove;
	
	private AncestorPanel selected;
	private JPanel viewingPanel;
	private JScrollPane viewScrollPanel; 
	private MouseListener clickListener;
	private JTable history;
	private JTextArea metadata;
	
	private int gridx;
	private int gridy;
	private boolean includeFullPath;
	private boolean bottomUp;
	
	private TableColumnAdjuster adjuster;

	private AncestorMap ancestorMap;
	
	/**
	 * Default constructor for the LineageCustomTable. Starts out empty.
	 */
	public LineageCustomTable(AncestorMap ancestorMap) {
		super(new GridBagLayout());
		
		this.ancestorMap = ancestorMap;

		init();
	}
	
	private void init() {
		bottomUp = false;
		
		products = new TreeSet<AncestorPanel>();
		containedProducts = new TreeSet<Long>();
		productsToRemove = new TreeSet<Long>();
		
		viewingPanel = new JPanel(new GridBagLayout());
		viewingPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		history = new JTable(new HistoryTableModel());
		history.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		history.setGridColor(Color.BLACK);
		history.setShowGrid(true);
		history.setRowHeight(20);
		history.getTableHeader().setReorderingAllowed(false);

		HistoryTableCellRenderer r = new HistoryTableCellRenderer();
		
		for (int col = 0; col < history.getColumnCount(); col++) {
			history.getColumnModel().getColumn(col).setCellRenderer(r);
		}
		
		adjuster = new TableColumnAdjuster(history);
		
		
		metadata = new JTextArea();   
		metadata.setMinimumSize(new Dimension(META_WIDTH, META_HEIGHT));
		metadata.setFont(new Font("Courier", Font.PLAIN, 12));
		metadata.setMargin(new Insets(0, 5, 0, 5));
		metadata.setLineWrap(true);
		
		includeFullPath = false;
		
 		gridx = ZERO;
		gridy = ZERO;
		
		selected = null;
		
		// Set up the mouse listener.  This will be passed to all lineage panels so that clicks on them can be identified.
		clickListener = new MouseListener() {
			private static final int LEFT_MASK = MouseEvent.BUTTON1_DOWN_MASK;
			private static final int LEFT_CTRL_MASK = MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK;
			
			@Override
			public void mouseReleased(MouseEvent e) {
				viewingPanel.requestFocusInWindow();
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				// Using masks to figure out what is pressed.
				// Set the focus.
				if (e.getSource().equals(viewingPanel)) {
					viewingPanel.requestFocusInWindow();
				} else {
					int mod = e.getModifiersEx();
					AncestorPanel source = (AncestorPanel) e.getSource();
					
					if (mod == LEFT_MASK && !source.equals(selected)) {
						setSelected(source);
					} else if (mod == LEFT_CTRL_MASK && source.equals(selected)) {
						// Want to clear out the metadata and deselect this bad boy.
						selected.changeBackgroundColor(UNSELECTED_BG, BorderFactory.createEmptyBorder());
						clearSelected();
					}
				}
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
			}
		};
		
		viewingPanel.setFocusable(true);
		viewingPanel.addMouseListener(clickListener);
		
		addBindings();
		addListeners();
		doPanelLayout();
	}

	/**
	 * Sets the keyboard bindings for this panel. 
	 */
	private void addBindings() {
		/*
		 * These could all be in one class but in order to do that would need to write another class.  Just
		 * seems easier to use anonomous classes for each one.
		 */
		
		// Up and down bindings.
		viewingPanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), DOWN_ACTION);
		viewingPanel.getActionMap().put(DOWN_ACTION, new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				selectedDownOne();
			}
		});

		viewingPanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), UP_ACTION);
		viewingPanel.getActionMap().put(UP_ACTION, new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				selectedUpOne();
			}
		});		
		
		// Left and right bindings.  Will show parent and the like.
		viewingPanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), LEFT_ACTION);
		viewingPanel.getActionMap().put(LEFT_ACTION, new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (selected != null) {
					selected.hideChildren();
				}
			}
		});	
		
		viewingPanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), RIGHT_ACTION);
		viewingPanel.getActionMap().put(RIGHT_ACTION, new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (selected != null) {
					selected.showChildren();
				}
			}
		});			
	}
	
	private void addListeners() {
	}
	
	private void doPanelLayout() {
		viewScrollPanel = new JScrollPane(viewingPanel, 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		viewScrollPanel.setPreferredSize(new Dimension(920, 350));

		add(viewScrollPanel, 
				new GridBagConstraints(gridx, //gridx, 
					gridy, // gridy, 
					1, // gridwidth, 
					1, // gridheight, 
					1, // weightx, 
					0.7, // weighty, 
					GridBagConstraints.NORTHWEST, // anchor, 
					GridBagConstraints.BOTH, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));		
		
		add(metadata, 
				new GridBagConstraints(++gridx, //gridx, 
					gridy, // gridy, 
					GridBagConstraints.RELATIVE, // gridwidth, 
					1, // gridheight, 
					0, // weightx, 
					0.7, // weighty, 
					GridBagConstraints.NORTHWEST, // anchor, 
					GridBagConstraints.BOTH, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));	
		
		 JScrollPane hisotryScrollPanel = new JScrollPane(history, 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		 
		hisotryScrollPanel.setPreferredSize(new Dimension(1319, 175));
		
		gridx = 0;
		add(hisotryScrollPanel, 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					GridBagConstraints.REMAINDER, // gridheight, 
					1, // weightx, 
					0.3, // weighty, 
					GridBagConstraints.NORTHWEST, // anchor, 
					GridBagConstraints.BOTH, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));		
		
	}

	/**
	 * Set if parent products hold the child products or child products hold
	 * parent products
	 * 
	 * @param bup
	 *            TRUE if child down to parent, FALSE if parent down to child
	 */
	public void setBottomUp(boolean bup) {
		bottomUp = bup;
	}
	
	/**
	 * Forces the product names to be show with their full file path
	 */
	public void useFullPaths() {
		setFullFilePath(true);
	}
	
	/**
	 * Forces the product names to only show the product name, no file path
	 */
	public void useShortPaths() {
		setFullFilePath(false);
	}
	
	private void setFullFilePath(boolean fp) {
		includeFullPath = fp;
		
		// Get all of the viewer panels and set their include.
		for (Component c : viewingPanel.getComponents()) {
			if (c instanceof AncestorPanel) {
				((AncestorPanel) c).setFullFilePath(includeFullPath);
			}
		}
	}
	
	private HistoryTableModel getModel() {
		return (HistoryTableModel) history.getModel();
	}
	
	/**
	 * Returns the selected product.  Null if nothing is selected.
	 * @return the ProductAutomationProduct selected
	 */
	public ProductAutomationProduct getSelectedProduct() {
		if (selected != null) {
			return selected.getProduct();
		} else {
			return null;
		}
	}
	
	/**
	 * Builds the history objects of the selected product.
	 */
	private void buildHistory() {
		clearHistory();
		
		if (selected != null) {
			SortedSet<Object> rawHistory = new TreeSet<Object>(ProductAutomationUserDAO.MESH_COMPARATOR);
			rawHistory.addAll(getSelectedProduct().getActions());
			rawHistory.addAll(getSelectedProduct().getStatuses());
			rawHistory.addAll(getSelectedProduct().getLogs());
			
			getModel().addRows(rawHistory);
			adjuster.adjustColumns();
		}
	}
	
	/**
	 * Will add the product to the internal list if it does not already exist.  Will then create a new
	 * AncestorViewerPanel and add it the the viewing panel.  Assumes that the products passed 
	 * in have been filtered so no checking will be done.  Make sure that all duplicates and the 
	 * like have been removed before adding to this viewer.
	 * 
	 * Does not update what is viewed after each addition.  Either call the addProducts or call the
	 * doUpdate to change the view.  This is an expensive operation and should be done as little as possible.
	 * 
	 * @param product the product to be added to the table
	 *
	 *	@return T/F indicating if an addition was made.
	 */
	public boolean addProduct(ProductAutomationProduct product) {
		if (!containedProducts.contains(product.getProductId())) {
			// Add to the id list and create a new panel.
			containedProducts.add(product.getProductId());
			AncestorPanel np = new AncestorPanel(product, true, bottomUp, ancestorMap);
			np.setMouseListener(clickListener);
			np.setFullFilePath(includeFullPath);
			
			products.add(np);
			
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Calls addProduct on all of the given products and then will call the doUpdate if products
	 * needed to be added.  This will clear out the products that are currently displayed but 
	 * should not be.  Assumes that only what is passed in should be displayed.
	 * 
	 * @param products a Collection of the ProductAutomationProducts to be added to the table
	 */
	@SuppressWarnings("unchecked")
	public void addProducts(Collection<ProductAutomationProduct> products) {
		TreeSet<Long> current = new TreeSet<Long>();
		boolean productAdded = false;
		
		for (ProductAutomationProduct product : products) {
			current.add(product.getProductId());
			productAdded = addProduct(product) || productAdded;
		}
		
		productsToRemove = (TreeSet<Long>) containedProducts.clone();
		productsToRemove.removeAll(current);		
		
		if (productAdded || !productsToRemove.isEmpty()) {
			doUpdate();
		}
	}
	
	/**
	 * This will order all of the internal ancestor viewer panels and re-add all of them to the 
	 * viewer panel.  Checks if the productId for the panel being added is in the remove list.  If
	 * it is will delete that panel. 
	 */
	public void doUpdate() {
		// First clear everything from the panel.  
		viewingPanel.removeAll();
		
		gridx = ZERO;
		gridy = ZERO;
		
		// Based on the ordering button, pick either an ascending or descending iterator.
		Iterator<AncestorPanel> it =  products.descendingIterator();
		Collection<AncestorPanel> toBeRemoved = new ArrayList<AncestorPanel>();
		
		while (it.hasNext()) {
			AncestorPanel vp = it.next();
			
			if (!productsToRemove.contains(vp.getProductId())) {
				viewingPanel.add(vp, 
						new GridBagConstraints(gridx, //gridx, 
							++gridy, // gridy, 
							GridBagConstraints.REMAINDER, // gridwidth, 
							1, // gridheight, 
							1, // weightx, 
							0, // weighty, 
							GridBagConstraints.NORTHWEST, // anchor, 
							GridBagConstraints.HORIZONTAL, // fill - no fill for text controls.
							new Insets(0, 0, 0, 0), // insets - 
							0, // ipadx, 
							0 // ipady
						));	
			} else {
				toBeRemoved.add(vp);
			}
		}

		// Now need to remove all the dead wood.
		containedProducts.removeAll(productsToRemove);
		products.removeAll(toBeRemoved);
		
		// Once all is done, add a spacer just in case to keep everything nice and tight. 
		viewingPanel.add(Box.createVerticalGlue(), 
				new GridBagConstraints(gridx, //gridx, 
					++gridy, // gridy, 
					GridBagConstraints.REMAINDER, // gridwidth, 
					GridBagConstraints.REMAINDER, // gridheight, 
					1, // weightx, 
					1, // weighty, 
					GridBagConstraints.NORTHWEST, // anchor, 
					GridBagConstraints.BOTH, // fill - no fill for text controls.
					new Insets(0, 0, 0, 0), // insets - 
					0, // ipadx, 
					0 // ipady
				));	
		
		viewingPanel.revalidate();
		revalidate();
	}

	/**
	 * Clear all data from the table
	 */
	public void clear() {
		viewingPanel.removeAll();
		containedProducts.clear();
		products.clear();
		
		clearSelected();
		
		doUpdate();
	}
	
	/**
	 * Unselects the selected product so that nothing is selected.
	 */
	private void clearSelected() {
		clearHistory();
		clearMetadata();
	}

	private void clearHistory() {
		getModel().clearData();
	}
	
	private void clearMetadata() {
		metadata.setText("");
	}
	
	/**
	 * Sets the panel as selected.  Will color the background of the previous selected panel
	 * back to default.  Sets the background of the new selected and populates the history table
	 * and metadata objects.
	 * 
	 * @param panel the AncestorPanel to be colored as selected
	 */
	private void setSelected(AncestorPanel panel) {
		// Set the background of the last selected back to normal if there is one.
		if (selected != null) {
			selected.changeBackgroundColor(UNSELECTED_BG, BorderFactory.createEmptyBorder());
		} 

		// Now, set the background of the new selected.
		selected =  panel;
		selected.changeBackgroundColor(SELECTED_BG, BorderFactory.createLineBorder(Color.RED));
	
		clearHistory();
		buildHistory();
		// Update the metadata tab with relevant info for the product.
		updateMetadata();		
	}
	
	/**
	 * Tries to find the next or previous selected value in the current selected
	 * nodes lineage. If nothing is found returns null and the previous or next
	 * top level ancestor panel should be selected.
	 * 
	 * @param increment
	 *            positive for the first or next child, 0 for the the next
	 *            child, negative for the previous child
	 * @return a child AncestorPanel
	 */
	private AncestorPanel findNextInLineage(int increment) {
		// First see if selected has a child.  But only if the increment is positive. 
		AncestorPanel n = null;
		
		if (increment > 0) {
			n = (AncestorPanel) selected.getFirstChild();
		}
		
		// If no child was found, have to walk the walk.
		if (n == null) {
			
			while(selected.getParent() instanceof AncestorPanel) {
				AncestorPanel parent = (AncestorPanel) selected.getParent();
				
				// based on the increment.
				if (increment < 0) {
					n = (AncestorPanel) parent.getPreviousChild(selected);
					
					// Going backward, so if n is null then select the parent and we are done.
					// Previous should be the easier one because it will always be found or be the parent in this stage.
					if (n == null) {
						n = parent;
					}
				} else {
					n = (AncestorPanel) parent.getNextChild(selected);
				}
				
				if (n == null) {
					// Nothing found on this level, selected is not the parent.
					setSelected(parent);
				} else {
					// Something was found so we are done.
					break;
				}
			}
		}
		
		return n;
	}
	
	/**
	 * Pass in an increment to select a panel. Negative will go backward and
	 * positive will go forward. Will check and if out of bounds does nothing.
	 * If the selected has children will cycle through them as well, I think...?
	 * 
	 * @param increment
	 *            positive to move to the next panel, negative to the previous
	 */
	private void getPanelRelative(int increment) {
		if (increment == 0) {
			// TODO log or something
			System.out.println("Increment can not be zero.");
		} else if (selected != null) {
			// Change the background since we know this will change. 
			AncestorPanel newSelected = findNextInLineage(increment);
			
			if (newSelected == null) {
				int incrementIndex = -1;
				
				for (int index = 0; index < viewingPanel.getComponentCount(); index++) {
					if (viewingPanel.getComponent(index).equals(selected)) {
						incrementIndex = index;
						break;
					}
				}
			
				// We have the index, check if the increment addition puts it out of bounds.
				incrementIndex += increment;
				
				if (incrementIndex >= 0 && incrementIndex < viewingPanel.getComponentCount()) {
					// Have a valid component.  Just select it. 
					Component c = viewingPanel.getComponent(incrementIndex);
					
					if (c instanceof AncestorPanel) {
						newSelected = (AncestorPanel) c;
						
						// If the increment was negative, need to find the last child of the parent that is open
						// All the way down the lineage.
						if (increment < 0) {
							newSelected = (AncestorPanel) newSelected.getLastAncestor();
						}
					}
				}
			}
			
			// If something was found set selected.
			if (newSelected != null) {
				setSelected(newSelected);
				setCenter();
			}
		}
	}
	
	private JViewport getViewPort() {
		return viewScrollPanel.getViewport();
	}
	
	private void setCenter() {
		Dimension d = getViewPort().getExtentSize();
		Double centerY = d.getHeight() / 2;
		int selectedY = selected.computeY();
		
		if (selectedY > centerY) {
			getViewPort().setViewPosition(new Point(0, selectedY - centerY.intValue()));
		} else {
			
		}
	}
	/**
	 * Selects the previous lineage panel.  If at the beginning, does nothing.
	 */
	private void selectedUpOne() {
		getPanelRelative(-1);
	}
	
	/**
	 * Selects the next lineage panel.  If at the end, does nothing.
	 */
	private void selectedDownOne() {
		getPanelRelative(1);
	}
	
	/**
	 * Uses the format string to create a meta data string for the selected product and updates the 
	 * text in the metadata text area.  If selected is null, clears the text field.
	 */
	private void updateMetadata() {
		Double sclk = new Double(selected.getProduct().getSclkCoarse() + selected.getProduct().getSclkFine() / SCLK_FINE_DIVISOR);
		String newText = selected == null ? "" :
			String.format(METADATA_FORMAT, 
					selected.getProductId(), // DB product id
					selected.getProduct().getApid(),
					selected.getProduct().getFswBuildId(), // Build id.
					selected.getProduct().getDictVersion(), // Dict version
					selected.getProduct().getSessionId(), // SID
					selected.getProduct().getSessionHost(), // Host
					sclk, 
					selected.getProduct().getProductPath(),
					selected.getProduct().getParent() == null ? NO_PARENT : selected.getProduct().getProductPath()
					);
		
		metadata.setText(newText);
	}
	
	/**
	 * Returns the number of product panels this is holding onto.
	 * 
	 * @return number of product panels in the display
	 */
	public int getNumberProducts() {
		return containedProducts.size();
	}
}
