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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import jpl.gds.shared.swt.BorderedLabel;

/**
 * This is an abstract class that provides the facilities to create a table-like
 * layout in an EditDialog
 * 
 * @since AMPCS R3
 */
public abstract class EditTable extends Composite {

	/**
	 * Constructor
	 * @param parent the parent composite
	 * @param style the SWT style
	 * @param numColumns the number of columns in the "table"
	 * @param equalColWidth whether or not the columns should be equal in width
	 */
	public EditTable(Composite parent, int style, int numColumns,
			boolean equalColWidth) {
		super(parent, style);

		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		layout.makeColumnsEqualWidth = equalColWidth;
		this.setLayout(layout);

		createHeader();
	}

	/**
	 * Create the header
	 */
	protected abstract void createHeader();

	/**
	 * A table element that represents one edit item
	 *
	 */
	public abstract class EditDialogElement {
		protected Composite parent;
		protected Button checkbox;
		protected BorderedLabel label;
		protected Composite checkboxComp;

		public EditDialogElement(EditTable parent) {
			this(parent, true);
		}

		public EditDialogElement(EditTable parent, boolean showCheckbox) {
			this.parent = parent;
			this.label = new BorderedLabel(parent, SWT.NONE, SWT.CENTER,
					SWT.FILL);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			this.label.setLayoutData(gd);

			createValueElement(parent);

			if (showCheckbox) {
				checkboxComp = new Composite(parent, SWT.BORDER);
				checkboxComp.setLayout(new GridLayout());

				gd = new GridData(SWT.FILL, SWT.FILL, true, false);
				checkboxComp.setLayoutData(gd);

				this.checkbox = new Button(checkboxComp, SWT.CHECK);
				gd = new GridData(SWT.CENTER, SWT.CENTER, true, true);
				this.checkbox.setLayoutData(gd);
			}
		}

		public void setLabelFontStyle(int style) {
			FontData titleFontData = this.label.getLabelWidget().getFont()
					.getFontData()[0];
			Font titleFont = new Font(this.label.getDisplay(), new FontData(
					titleFontData.getName(), titleFontData.getHeight(), style));

			this.label.getLabelWidget().setFont(titleFont);
		}

		public void setBackgroundColor(Color color) {
			this.label.setBackground(color);
			this.label.getLabelWidget().setBackground(color);
			this.checkboxComp.setBackground(color);
		}

		public boolean isSelected() {
			if (this.checkbox != null) {
				return this.checkbox.getSelection();
			} else {
				return true;
			}
		}

		public void setLabel(String label) {
			this.label.getLabelWidget().setText(label);
		}

		public void layout() {
			this.parent.layout();
		}

		public void setEnabled(boolean enabled) {
			if (this.checkbox != null) {
				this.checkbox.setEnabled(enabled);
			}
		}

		public Button getCheckboxControl() {
			return this.checkbox;
		}

		public abstract String getValue();

		public abstract void createValueElement(EditTable parent);
	}

	public class EditDialogCombo extends EditDialogElement {
		private Combo value;

		public EditDialogCombo(EditTable parent, boolean showCheckbox) {
			super(parent, showCheckbox);
		}

		public EditDialogCombo(EditTable parent) {
			this(parent, true);
		}

		public void setItems(String[] items) {
			this.value.setItems(items);

			if (this.checkbox != null) {
				this.checkbox.setSelection(false);
			}
		}

		public void setDefaultSelected(int index) {
			this.value.select(index);

			if (this.checkbox != null) {
				this.checkbox.setSelection(false);
			}

			this.layout();
		}

		public String getValue() {
			return value.getItem(value.getSelectionIndex());
		}

		@Override
		public void createValueElement(EditTable parent) {
			Composite valueComp = new Composite(parent, SWT.BORDER);
			valueComp.setLayout(new GridLayout());

			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);

			valueComp.setLayoutData(gd);

			this.value = new Combo(valueComp, SWT.READ_ONLY);
			this.value.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					if (EditDialogCombo.this.checkbox != null) {
						EditDialogCombo.this.checkbox.setSelection(true);
					}
				}
			});

			gd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
			this.value.setLayoutData(gd);
		}

		public void addCheckboxSelectionListener(SelectionListener listener) {
			checkbox.addSelectionListener(listener);
		}

		public void addComboModifyListener(ModifyListener listener) {
			value.addModifyListener(listener);
		}

		public Button getCheckboxControl() {
			return this.checkbox;
		}

		public Combo getComboControl() {
			return this.value;
		}

		public void setSelection(boolean selection) {
			if (this.checkbox != null) {
				this.checkbox.setSelection(selection);
			}
		}

		@Override
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			this.value.setEnabled(enabled);
		}
	}
}
