package jpl.gds.tcapp.app.gui.fault.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import jpl.gds.shared.swt.SWTUtilities;

/**
 * This is the arguemnt GUI control class for bitmask (consolidated) command arguments
 * 
 *
 */
public class BitmaskArgumentGuiControl extends AbstractCommandArgumentGuiControl {

    @Override
    public Control createArgumentValueControl(Composite parentComposite, Font font) {
        
        Control argControl = null;

        final Text textControl = new Text(parentComposite, SWT.BORDER);
        textControl.setFont(font);
        
        String startingVal = this.cmd.getArgumentValue(argIndex);
        
        if(startingVal == null || startingVal.isEmpty()) {
            startingVal = this.cmd.getArgumentDefinition(argIndex).getDefaultValue();
        }
        textControl.setText(startingVal);
        
        textControl.setEditable(true);

        argControl = textControl;

        final FormData fd = SWTUtilities.getFormData(argControl, 1, 20);
        argControl.setLayoutData(fd);

        return (argControl);
    }

    @Override
    public Control createSuffixControl(Composite parent) {
        final Button suffix = new Button(parent, SWT.PUSH);
        suffix.setText("Edit...");
        return (suffix);
    }
    
    @Override
    public Label createPrefixControl(Composite parent) {
        final Label prefix = new Label(parent, SWT.LEFT);
        prefix.setText(getDisplayName() + " (array):");
        return (prefix);
    }
}
