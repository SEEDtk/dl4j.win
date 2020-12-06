/**
 *
 */
package org.theseed.dl4j.win;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.theseed.io.ParmDescriptor;

/**
 * This object manages a group of controls to set and display the value of a parameter in a parms.prm file.
 * It takes as input the parent composite and the parameter descriptor, then generates a trio of controls
 * in the current row of the grid.  There are two subclasses-- one for normal text parameters and one
 * for selection-style parameters.
 *
 * @author Bruce Parrello
 *
 */
public abstract class ParmDialogGroup {

    // FIELDS
    /** descriptor being managed */
    private ParmDescriptor descriptor;
    /** checkbox for enable/disable */
    private Button checkBox;
    /** parent composite */
    private Composite parent;
    /** main control for entering value */
    private Control mainControl;
    /** linked parameters-- these are enabled and disabled in concert */
    private List<ParmDialogGroup> others;
    /** mutually exclusive parameters-- these are disabled when we are enabled */
    private List<ParmDialogGroup> exclusives;
    /** default layout data for labels; right-aligned, occupying a single grid column */
    private static final GridData LABEL_LAYOUT = ParmDialogGroup.labelLayout();
    /** default layout data for main control; filling, occupying a single grid column */
    private static final GridData MAIN_LAYOUT = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);


    /**
     * Set up a parameter dialog group and make it ready for use.
     *
     * @param parent	parent composite
     * @param desc		descriptor with specs for the parameter
     */
    public void init(Composite parent, ParmDescriptor desc) {
        // Initialize the lists.
        this.others = new ArrayList<ParmDialogGroup>();
        this.exclusives = new ArrayList<ParmDialogGroup>();
        // Connect to the container and the descriptor.
        this.parent = parent;
        this.descriptor = desc;
        // Create the checkbox.
        this.checkBox = new Button(this.parent, SWT.CHECK);
        this.checkBox.addSelectionListener(new CheckListener());
        this.checkBox.setSelection(! desc.isCommented());
        this.checkBox.setToolTipText("Check to enable, clear to disable.");
        // Label the parameter.
        Label label = new Label(this.parent, SWT.NONE);
        label.setLayoutData(LABEL_LAYOUT);
        label.setText(desc.getName());
        label.setToolTipText(desc.getDescription());
        // Create the main control.
        this.mainControl = this.createMainControl();
        this.mainControl.setLayoutData(MAIN_LAYOUT);
        this.initMainControl();
        // Configure the enable state.
        this.configure();
    }

    /**
     * @return the default layout for labels
     */
    private static GridData labelLayout() {
        GridData retVal = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        retVal.widthHint = ParmsDialog.LABEL_WIDTH;
        return retVal;
    }

    /**
     * Initialize the main control for this group.  This is called after the layout is set.
     */
    protected abstract void initMainControl();

    /**
     * @return the main control for this group
     */
    protected abstract Control createMainControl();

    /**
     * Configure the enable-disable state based on the descriptor.
     */
    private void configure() {
        // We are enabled iff we are NOT commented.
        this.mainControl.setEnabled(! this.descriptor.isCommented());
    }

    /**
     * Update the descriptor and configure the states.
     *
     * @param newState	TRUE to enable, FALSE to disable
     */
    private void configure(boolean newState) {
        // Note that commented is disabled, uncommented is enabled.
        this.descriptor.setCommented(! newState);
        // Update the checkbox.
        this.checkBox.setSelection(newState);
        // Configure the controls.
        this.configure();
    }

    /**
     * Specify mutually exclusive parameters.
     *
     * @param groups	array of groups to mark mutually exclusive
     */
    public void setExclusive(ParmDialogGroup... groups) {
        Arrays.stream(groups).forEach(x -> this.exclusives.add(x));
    }

    /**
     * Specify grouped parameters.  Only one parameter should do this.  The
     * others will have their checkboxes disabled.
     *
     * @param groups	array of groups to mark as subordinate to this one
     */
    public void setGrouped(ParmDialogGroup... groups) {
        for (ParmDialogGroup group : groups) {
            this.others.add(group);
            group.checkBox.setEnabled(false);
        }
    }

    /**
     * Inner class for handling checkbox events.
     */
    private class CheckListener extends SelectionAdapter {

        @Override
        public void widgetSelected(SelectionEvent e) {
            descriptor.setCommented(! checkBox.getSelection());
            configure();
            for (ParmDialogGroup other : others)
                other.configure(! descriptor.isCommented());
            for (ParmDialogGroup exclusive : exclusives)
                exclusive.configure(descriptor.isCommented());
        }
    }

    /**
     * Update the parameter value.
     *
     * @param	newValue		new parameter value
     */
    protected void setValue(String newValue) {
        this.descriptor.setValue(newValue);
    }

    /**
     * @return the descriptor
     */
    protected ParmDescriptor getDescriptor() {
        return this.descriptor;
    }

    /**
     * @return the parent
     */
    protected Composite getParent() {
        return this.parent;
    }

    /**
     * @return the parameter description
     */
    protected String getDescription() {
        return this.descriptor.getDescription();
    }

}
