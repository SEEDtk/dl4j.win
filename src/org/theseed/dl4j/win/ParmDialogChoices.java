/**
 *
 */
package org.theseed.dl4j.win;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.theseed.io.ParmDescriptor;
import org.theseed.io.ParmFile;

/**
 *
 * @author Bruce Parrello
 *
 */
public class ParmDialogChoices extends ParmDialogGroup {

    /** list of legal values */
    private List<String> possibilities;
    /** set of selected values */
    private Set<String> selected;
    /** composite containing the checkboxes */
    private Composite container;
    /** map of option names to checkboxes */
    private Map<String, Button> checkBoxMap;
    /** layout for the composite */
    private static final Layout CONTAINER_LAYOUT = ParmDialogChoices.choiceLayout();
    /** layout for the groupings in the composite */
    private static final Layout GROUP_LAYOUT = ParmDialogChoices.groupLayout();
    /** layout for the ALL button */
    private static final RowData ALL_BUTTON_SIZE = new RowData(SWT.DEFAULT, 16);

    /**
     * Create a parameter dialog group for an enum type.
     *
     * @param parent
     * @param desc
     */
    public ParmDialogChoices(Composite parent, ParmDescriptor desc, Enum<?>[] values) {
        this.possibilities = Arrays.stream(values).map(x -> x.name()).collect(Collectors.toList());
        this.selected = new TreeSet<String>(Arrays.asList(StringUtils.split(desc.getValue(), ", ")));
        this.checkBoxMap = new TreeMap<String, Button>();
        init(parent, desc);
    }

    /**
     * @return the default row layout for a choice control
     */
    private static Layout choiceLayout() {
        RowLayout retVal = new RowLayout(SWT.HORIZONTAL);
        retVal.wrap = true;
        retVal.fill = true;
        retVal.spacing = 8;
        return retVal;
    }

    /**
     * @return the default row layout for a choice control
     */
    private static Layout groupLayout() {
        RowLayout retVal = new RowLayout(SWT.HORIZONTAL);
        retVal.wrap = true;
        retVal.fill = true;
        retVal.spacing = 4;
        return retVal;
    }

    @Override
    protected Control createMainControl() {
        // The main control is a composite with flow layout.
        container = new Composite(getParent(), SWT.BORDER);
        return container;
    }

    @Override
    protected void initMainControl() {
        container.setLayout(CONTAINER_LAYOUT);
        // Now we add the checkboxes.  For each possible enum, we add the box and then the label.
        for (String choice : possibilities) {
            Composite grouper = new Composite(container, SWT.NONE);
            grouper.setLayout(GROUP_LAYOUT);
            Button chkBox = new Button(grouper, SWT.CHECK);
            chkBox.setSelection(selected.contains(choice));
            chkBox.addSelectionListener(new CheckListener(choice, chkBox));
            this.checkBoxMap.put(choice, chkBox);
            Label lbl = new Label(grouper, SWT.NONE);
            lbl.setText(choice);
            lbl.addMouseListener(new LabelListener(choice));
        }
        // Add the ALL button.
        Button allButton = new Button(container, SWT.NONE);
        allButton.setText("ALL");
        allButton.addSelectionListener(new AllListener());
        allButton.setToolTipText("Click to select all options.");
        allButton.setLayoutData(ALL_BUTTON_SIZE);
        Point size = container.computeSize(ParmsDialog.TARGET_WIDTH, SWT.DEFAULT);
        // Make sure we're tall enough to see everything.
        GridData layoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
        layoutData.heightHint = size.y;
        container.setLayoutData(layoutData);
    }

    /**
     * Update the parm value to reflect the state of the checkboxes.
     */
    public void updateValue() {
        getDescriptor().setValue(selected.stream().collect(Collectors.joining(", ")));
    }

    /**
     * Update the checkboxes to match the current state of the selected set.
     */
    public void fixCheckBoxes() {
        for (Map.Entry<String, Button> chkEntry : this.checkBoxMap.entrySet())
            chkEntry.getValue().setSelection(this.selected.contains(chkEntry.getKey()));
    }

    /**
     * Click event for a checkbox-- adds or removes string.
     */
    private class CheckListener extends SelectionAdapter {

        private String optionName;
        private Button target;

        public CheckListener(String option, Button target) {
            this.optionName = option;
            this.target = target;
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            // Modify the set accordingly.
            if (this.target.getSelection())
                selected.add(optionName);
            else
                selected.remove(optionName);
            // Update the parm value in the descriptor.
            updateValue();
        }

    }

    /**
     * Double-click event for a label-- sets to only that string.
     */
    private class LabelListener extends MouseAdapter {

        private String optionName;

        public LabelListener(String option) {
            this.optionName = option;
        }

        @Override
        public void mouseDoubleClick(MouseEvent e) {
            selected.clear();
            selected.add(optionName);
            // Update the parm value in the descriptor.
            updateValue();
            // Update all the checkboxes.
            fixCheckBoxes();
        }

    }

    /**
     * Click event for ALL button-- sets to all strings.
     */
    private class AllListener extends SelectionAdapter {

        @Override
        public void widgetSelected(SelectionEvent e) {
            selected.addAll(possibilities);
            // Update the parm value in the descriptor.
            updateValue();
            // Update all the checkboxes.
            fixCheckBoxes();
        }

    }

    /**
     * Check for the specified parameter in the parameter stricture and add a parm dialog
     * group for it to the specified container.
     *
     * @param container		container to hold controls
     * @param parms			parameter descriptor map
     * @param name			parameter name
     * @param values		permissible enumeration values
     */
    public static ParmDialogGroup load(Composite container, ParmFile parms, String name, Enum<?>[] values) {
        ParmDialogGroup retVal = null;
        ParmDescriptor desc = parms.get(name);
        if (desc != null)
            retVal = new ParmDialogChoices(container, desc, values);
        return retVal;
    }

}
