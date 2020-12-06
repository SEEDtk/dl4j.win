/**
 *
 */
package org.theseed.dl4j.win;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.theseed.io.ParmDescriptor;
import org.theseed.io.ParmFile;

/**
 * This is a parameter dialog control for a text parameter.
 *
 * @author Bruce Parrello
 */
public class ParmDialogText extends ParmDialogGroup {

    // FIELDS
    /** text box containing value */
    private Text txtMainValue;

    /**
     * Create the parameter dialog group.
     *
     * @param parent		parent display object
     * @param descriptor	parameter descriptor
     */
    public ParmDialogText(Composite parent, ParmDescriptor descriptor) {
        init(parent, descriptor);
    }

    @Override
    protected Control createMainControl() {
        // Here we create the text box.  We give it a change event that automatically
        // updates the descriptor.
        txtMainValue = new Text(getParent(), SWT.BORDER);
        return txtMainValue;
    }

    @Override
    protected void initMainControl() {
        txtMainValue.addModifyListener(new TextListener());
        txtMainValue.setText(getDescriptor().getValue());
    }

    /**
     * Event for updating the parm descriptor with the new value.
     */
    private class TextListener implements ModifyListener {

        @Override
        public void modifyText(ModifyEvent arg0) {
            getDescriptor().setValue(txtMainValue.getText());
        }

    }

    /**
     * Check for the specified parameter and load a group for it onto the specified
     * container.
     *
     * @param grpStructure	container to receive the parameter
     * @param parms			parameter descriptor map
     * @param name			parameter name
     *
     * @return the dialog group loaded
     */
    public static ParmDialogGroup load(Composite grpStructure, ParmFile parms, String name) {
        ParmDialogGroup retVal = null;
        ParmDescriptor desc = parms.get(name);
        if (desc != null)
            retVal = new ParmDialogText(grpStructure, desc);
        return retVal;

    }


}
