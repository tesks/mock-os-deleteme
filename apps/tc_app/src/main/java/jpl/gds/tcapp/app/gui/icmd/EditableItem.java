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

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.BorderedLabel;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.tcapp.app.gui.icmd.EditTable.EditDialogCombo;
import jpl.gds.tcapp.app.gui.icmd.IEditListener.EditEvent;

/**
 * A generic widget that displays a currently selected value and allows editing
 * of the selected value
 * 
 * @since AMPCS R3
 */
public class EditableItem extends Composite {
	private static final Tracer logger = TraceManager.getDefaultTracer();


	/** The path to the default edit button image */
	private static final String EDIT_BUTTON_IMAGE_PATH = "jpl/gds/tcapp/icmd/gui/pencil-icon.png";

	/** The label of the widget */
	private Label label;

	/** The value of the widget */
	private BorderedLabel value;

	/** The edit button to edit the value of the widget */
	private Button editButton;

	/**
	 * An auxiliary button that can be swapped back and forth with the edit
	 * button
	 */
	private Button auxiliaryButton;

	/** The possible values to select from */
	private String[] editValues;

	/** The message to display while editing */
	private String editDialogMessage;

	/** The listeners interested when there is an edit */
	private List<IEditListener> listeners;

	/** The string to display as the label for edit choices */
	private String editLabel;

	/** The comparator to use to determine which item is currently selected */
	private Comparator<String> valueComparator;

	/**
	 * Constructor
	 * 
	 * @param parent the parent composite
	 * @param style the SWT style
	 */
	public EditableItem(Composite parent, int style) {
		super(parent, style);

		this.listeners = new LinkedList<IEditListener>();

		FormLayout layout = new FormLayout();
		layout.marginTop = 10;
		this.setLayout(layout);

		this.label = new Label(this, SWT.NONE);

		this.value = new BorderedLabel(this, SWT.NONE, SWT.LEFT, SWT.CENTER);

		FontData titleFontData = this.value.getFont().getFontData()[0];
		Font titleFont = new Font(this.getDisplay(), new FontData(
				titleFontData.getName(), titleFontData.getHeight(), SWT.BOLD));
		this.value.getLabelWidget().setFont(titleFont);

		this.setValueColor(this.getDisplay().getSystemColor(SWT.COLOR_BLUE));

		FormData fd = new FormData();
		fd.left = new FormAttachment(this.label, 2);
		fd.top = new FormAttachment(this.label, 0, SWT.CENTER);
		this.value.setLayoutData(fd);

		Image editImage = SWTUtilities.createImage(this.getDisplay(),
				EDIT_BUTTON_IMAGE_PATH);

		this.editButton = new Button(this, SWT.PUSH);
		this.editButton.addSelectionListener(new SelectionAdapter() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				new EditDialog(getShell()).open();
			}
		});
		this.editButton.setImage(editImage);

		fd = new FormData();
		fd.left = new FormAttachment(this.value, 2);
		fd.top = new FormAttachment(this.value, 0, SWT.CENTER);
		this.editButton.setLayoutData(fd);

		this.editValues = new String[0];
		this.setReadOnly(false);
	}

	/**
	 * Constructor
	 * 
	 * @param parent the parent composite
	 * @param style the SWT style
	 * @param auxiliaryButtonImagePath the path to an image for the auxiliary
	 *            button
	 * @param auxiliaryButtonHandler a handler to handle auxiliary button events
	 */
	public EditableItem(Composite parent, int style,
			String auxiliaryButtonImagePath,
			SelectionListener auxiliaryButtonHandler) {
		this(parent, style);

		Image auxiliaryImage = SWTUtilities.createImage(this.getDisplay(),
				auxiliaryButtonImagePath);

		this.auxiliaryButton = new Button(this, SWT.PUSH);
		this.auxiliaryButton.addSelectionListener(auxiliaryButtonHandler);
		this.auxiliaryButton.setImage(auxiliaryImage);

		FormData fd = new FormData();
		fd.left = new FormAttachment(this.value, 2);
		fd.top = new FormAttachment(this.value, 0, SWT.CENTER);
		this.auxiliaryButton.setLayoutData(fd);

		this.auxiliaryButton.setVisible(false);
	}

	/**
	 * Add an edit listener
	 * 
	 * @param listener the edit listener to add
	 */
	public void addEditListener(IEditListener listener) {
		this.listeners.add(listener);
	}

	/**
	 * Remove an edit listener
	 * 
	 * @param listener the edit listener to remove
	 */
	public void removeEditListener(IEditListener listener) {
		this.listeners.remove(listener);
	}

	/**
	 * Set the message to show when editing
	 * 
	 * @param message the message to show when editing
	 */
	public void setEditDialogMessage(String message) {
		this.editDialogMessage = message;
	}

	/**
	 * Set the values available when editing
	 * 
	 * @param editValues the values available when editing
	 */
	public void setEditValues(String[] editValues) {
		this.editValues = editValues;
	}

	/**
	 * Set whether or not to enable editing
	 * 
	 * @param readOnly whether or not to enable editing
	 */
	public void setReadOnly(boolean readOnly) {
		this.editButton.setEnabled(!readOnly);
		this.editButton.setVisible(!readOnly);
	}

	/**
	 * Set the label text
	 * 
	 * @param label the label text
	 */
	public void setLabel(String label) {
		this.label.setText(label);
		this.layout();
	}

	/**
	 * Set the value text
	 * 
	 * @param value the value text
	 */
	public void setValue(String value) {
		this.value.getLabelWidget().setText(value);
		this.layout();
	}

	/**
	 * Set the value text color
	 * 
	 * @param color the value text color
	 */
	public void setValueColor(Color color) {
		this.value.getLabelWidget().setForeground(color);
	}

	/**
	 * Set edit button visibility. If edit button is invisible, then the
	 * auxiliary button will show. If the edit button is visible, then the
	 * auxiliary button will be hidden.
	 */
	public void setEditButtonVisibility(boolean editButtonVisible) {
		this.auxiliaryButton.setVisible(!editButtonVisible);
		this.editButton.setVisible(editButtonVisible);
	}

	/**
	 * Set the text to display as the label for the edit choices
	 * 
	 * @param label text to display as the label for the edit choices
	 */
	public void setEditLabel(String label) {
		this.editLabel = label;
	}

	/**
	 * Set a comparator to use when determining which value is currently
	 * selected
	 * 
	 * @param comparator the comparator to use when determining which value is
	 *            currently selected
	 */
	public void setComparator(Comparator<String> comparator) {
		this.valueComparator = comparator;
	}

	/**
	 * Set the tool tip for the auxiliary button
	 * 
	 * @param toolTip tool tip for auxiliary button
	 */
	public void setAuxiliaryButtonToolTip(String toolTip) {
		if (this.auxiliaryButton != null) {
			this.auxiliaryButton.setToolTipText(toolTip);
		}
	}

	private Comparator<String> getComparator() {
		if (this.valueComparator != null) {
			return this.valueComparator;
		} else {
			return new Comparator<String>() {

				@Override
				public int compare(String arg0, String arg1) {
					return String.CASE_INSENSITIVE_ORDER.compare(arg0, arg1);
				}

			};
		}
	}

	/**
	 * The edit dialog that shows when the edit button is clicked
	 * 
	 */
	private class EditDialog extends Dialog {
		private static final int NUM_COLUMNS = 2;
		private EditDialogCombo editCombo;

		protected EditDialog(Shell parentShell) {
			super(parentShell);
		}

		@Override
		protected Control createDialogArea(final Composite parent) {
			EditTable editTable = new EditTable(parent, SWT.NONE, NUM_COLUMNS,
					false) {
				@Override
				protected void createHeader() {
					if (EditableItem.this.editDialogMessage != null) {
						Label editDialogHeader = new Label(this, SWT.NONE);
						editDialogHeader
								.setText(EditableItem.this.editDialogMessage);

						GridData gd = new GridData(SWT.CENTER, SWT.CENTER,
								true, false);
						gd.horizontalSpan = NUM_COLUMNS;

						editDialogHeader.setLayoutData(gd);
					}
				}
			};

			GridData gd = new GridData(GridData.FILL_BOTH);
			editTable.setLayoutData(gd);

			editCombo = editTable.new EditDialogCombo(editTable, false);

			if (EditableItem.this.editLabel != null) {
				editCombo.setLabel(EditableItem.this.editLabel);
			} else {
				editCombo.setLabel(EditableItem.this.label.getText());
			}

			int selectedIndex = 0;
			for (int i = 0; i < EditableItem.this.editValues.length; i++) {
				String v = EditableItem.this.editValues[i];

				try {
					if (EditableItem.this.getComparator().compare(v,
							EditableItem.this.value.getLabelWidget().getText()) == 0) {
						selectedIndex = i;
						break;
					}
				} catch (ClassCastException e) {
					logger.warn("Encountered invalid bit rate: "
							+ e.getMessage());
				}
			}

			editCombo.setItems(EditableItem.this.editValues);
			editCombo.setDefaultSelected(selectedIndex);

			return editTable;
		}

		@Override
		protected void okPressed() {
			for (IEditListener l : EditableItem.this.listeners) {
				l.onEdit(new EditEvent(editCombo.getValue()));
			}
			this.close();
		}
	}
}
