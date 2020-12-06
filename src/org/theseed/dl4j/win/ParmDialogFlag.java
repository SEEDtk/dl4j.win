/**
 *
 */
package org.theseed.dl4j.win;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.theseed.io.ParmDescriptor;
import org.theseed.io.ParmFile;

/**
 * Here we have a dialog group where the parameter is a single on/off switch.  The
 * parameter is either enabled or disabled and has no value.
 *
 * @author Bruce Parrello
 *
 */
public class ParmDialogFlag extends ParmDialogGroup {

    /** label containing parameter description */
    private Label description;

    public ParmDialogFlag(Composite parent, ParmDescriptor desc) {
        init(parent, desc);
    }

    @Override
    protected void initMainControl() {
        description.setText(getDescription());
    }

    @Override
    protected Control createMainControl() {
        description = new Label(getParent(), SWT.NONE);
        return description;
    }

    /**
     * Check for a parameter with the given name and load a dialog group for it onto the
     * specified container
     *
     * @param container		parent container
     * @param parms			parameter descriptor map
     * @param name			name of parameter
     */
    public static ParmDialogGroup load(Composite container, ParmFile parms, String name) {
        ParmDialogGroup retVal = null;
        ParmDescriptor desc = parms.get(name);
        if (desc != null)
            retVal = new ParmDialogFlag(container, desc);
        return retVal;
    }

}
