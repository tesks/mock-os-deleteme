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
package jpl.gds.tcapp.app.gui.icmd;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

import jpl.gds.shared.swt.SWTUtilities;

/**
 * This class is a widget that provides the functionalities of a toggle button.
 * It allows a selected image and a deselected image to be set to represent the
 * different toggle states.
 * 
 */
public class ToggleImageButton extends Composite implements MouseListener {
	/**
	 * The default background color of the toggle button when the primary mouse
	 * betton his held down on the button
	 */
	private static final RGB STEEL_BLUE = new RGB(70, 130, 180);

	/** The state of the toggle button */
	private boolean selected;

	/** The image to show when the button is in a selected state */
	private Label selectedImage;

	/** The image to show when the button is in a deselected state */
	private Label deselectedImage;

	/** The image to show when the button is in a disabled state */
	private Label disabledImage;

	/** The whether or not this button is enabled to toggle */
	private boolean enabled;

	/**
	 * Constructor
	 * 
	 * @param parent parent compsite
	 * @param style SWT style
	 * @param selectedImagePath path to selected image
	 * @param deselectedImagePath path to deselected image
	 * @param disabledImagePath path to disabled image
	 */
	public ToggleImageButton(Composite parent, int style,
			String selectedImagePath, String deselectedImagePath,
			String disabledImagePath) {
		super(parent, style);

		FormLayout layout = new FormLayout();
		this.setLayout(layout);

		this.selectedImage = new Label(this, SWT.NONE);
		this.selectedImage.setImage(SWTUtilities.createImage(this.getDisplay(),
				selectedImagePath));

		FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0);

		this.selectedImage.setLayoutData(fd);

		this.deselectedImage = new Label(this, SWT.NONE);
		this.deselectedImage.setImage(SWTUtilities.createImage(
				this.getDisplay(), deselectedImagePath));

		fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0);

		this.deselectedImage.setLayoutData(fd);

		this.disabledImage = new Label(this, SWT.NONE);
		this.disabledImage.setImage(SWTUtilities.createImage(this.getDisplay(),
				disabledImagePath));

		fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0);

		this.disabledImage.setLayoutData(fd);

		this.setEnabled(true);
		this.selected = false;

		this.updateImage();
	}

	private void updateImage() {
		if (this.enabled) {
			this.selectedImage.setVisible(this.selected);
			this.deselectedImage.setVisible(!this.selected);
			this.disabledImage.setVisible(false);
		} else {
			this.selectedImage.setVisible(false);
			this.deselectedImage.setVisible(false);
			this.disabledImage.setVisible(true);
		}

		this.selectedImage.setForeground(null);
		this.deselectedImage.setForeground(null);
	}

	/**
	 * Set the tooltip that is shown when the button is in selected state
	 * 
	 * @param tooltip tooltip that is shown when the button is in selected state
	 */
	public void setSelectedTooltip(String tooltip) {
		this.selectedImage.setToolTipText(tooltip);
	}

	/**
	 * Set the tooltip that is shown when the button is in deselected state
	 * 
	 * @param tooltip tooltip that is shown when the button is in deselected
	 *            state
	 */
	public void setDeselectedTooltip(String tooltip) {
		this.deselectedImage.setToolTipText(tooltip);
	}

	/**
	 * Indicates if this the toggle is in the selected state
	 * 
	 * @return true if it is in the selected state, false otherwise
	 */
	public boolean isSelected() {
		return this.selected;
	}

	/**
	 * Set the selected state
	 * 
	 * @param selection true for selected, false otherwise
	 */
	public void setSelection(boolean selection) {
		this.selected = selection;
		this.updateImage();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.widgets.Control#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) { // no change
			return;
		} else {
			this.enabled = enabled;

			if (this.enabled) {
				this.selectedImage.addMouseListener(this);
				this.deselectedImage.addMouseListener(this);
			} else {
				this.selectedImage.removeMouseListener(this);
				this.deselectedImage.removeMouseListener(this);
			}

			this.updateImage();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt
	 * .events.MouseEvent)
	 */
	@Override
	public void mouseDoubleClick(MouseEvent e) {
		// this method is intentionally left blank
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.events.MouseListener#mouseDown(org.eclipse.swt.events
	 * .MouseEvent)
	 */
	@Override
	public void mouseDown(MouseEvent e) {
		this.setBackground(new Color(this.getDisplay(), STEEL_BLUE));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.events.MouseListener#mouseUp(org.eclipse.swt.events.
	 * MouseEvent)
	 */
	@Override
	public void mouseUp(MouseEvent e) {
		this.setBackground(null);
		this.selected = !this.selected;
		updateImage();

		this.notifyListeners(SWT.Selection, new Event());
	}
}
