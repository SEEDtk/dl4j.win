/**
 *
 */
package org.theseed.dl4j.win;

import java.util.Collection;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.theseed.dl4j.train.TrainingProcessor;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;

/**
 * This dialog box allows the user to select the metadata columns for model training.  For
 * a classification model, it also allows the user to select the label column.
 *
 * @author Bruce Parrello
 *
 */
public class MetaDialog extends Dialog {

    // FIELDS
    /** window shell */
    protected Shell shell;
    /** result list; first is ID, last is label */
    protected String[] result;
    /** TRUE if we need a label column */
    private boolean needLabel;
    /** column name list */
    private Collection<String> colNames;
    /** list of meta-columns */
    private org.eclipse.swt.widgets.List listMeta;

    /**
     * Create the dialog.
     *
     * @param parent	parent window
     * @param style		display style
     * @param headers	list of column headers to choose from
     * @param type		model type
     */
    public MetaDialog(Shell parent, int style, Collection<String> headers, TrainingProcessor.Type type) {
        super(parent, style);
        setText("Choose Meta-Columns");
        this.colNames = headers;
        this.needLabel = (type == TrainingProcessor.Type.CLASS);
    }

    /**
     * Open the dialog.
     *
     * @return the result
     */
    public String[] open() {
        createContents();
        shell.open();
        shell.layout();
        Display display = getParent().getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        return result;
    }

    /**
     * Create contents of the dialog.
     */
    private void createContents() {
        shell = new Shell(getParent(), SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL);
        shell.setImage(SWTResourceManager.getImage(MetaDialog.class, "/javax/swing/plaf/metal/icons/ocean/question.png"));
        shell.setSize(450, 438);
        shell.setText(getText());
        shell.setLayout(new GridLayout(1, false));
        String instructions = "Select items on the left to move them to the list of meta-data columns on the right.  The first one will be used as the row ID.";
        if (this.needLabel)
            instructions += "  The last one will be the classification label.";

        Composite composite_1 = new Composite(shell, SWT.NONE);
        composite_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        GridLayout gl_composite_1 = new GridLayout(2, false);
        gl_composite_1.horizontalSpacing = 1;
        composite_1.setLayout(gl_composite_1);

        Label lblNewLabel = new Label(composite_1, SWT.BORDER | SWT.WRAP | SWT.SHADOW_IN);
        GridData gd_lblNewLabel = new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1);
        gd_lblNewLabel.widthHint = 364;
        lblNewLabel.setLayoutData(gd_lblNewLabel);
        lblNewLabel.setText(instructions);

        Button btnOK = new Button(composite_1, SWT.NONE);
        btnOK.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                shell.close();
            }
        });
        btnOK.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.NORMAL));
        GridData gd_btnOK = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_btnOK.widthHint = 80;
        gd_btnOK.minimumWidth = 80;
        btnOK.setLayoutData(gd_btnOK);
        btnOK.setText("OK");

        Composite composite = new Composite(shell, SWT.NONE);
        composite.setLayout(new FillLayout(SWT.HORIZONTAL));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        org.eclipse.swt.widgets.List listFeatures = new org.eclipse.swt.widgets.List(composite, SWT.BORDER | SWT.V_SCROLL);
        listMeta = new org.eclipse.swt.widgets.List(composite, SWT.BORDER | SWT.V_SCROLL);
        this.colNames.stream().forEach(x -> listFeatures.add(x));
        listMeta.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                listSwap(listMeta, listFeatures);
            }
        });
        listFeatures.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                listSwap(listFeatures, listMeta);
            }
        });
        shell.setDefaultButton(btnOK);
    }

    /**
     * Move the currently-selected item from list1 to list2.
     *
     * @param list1		list containing selected item
     * @param list2		list to receive selected item
     */
    protected void listSwap(org.eclipse.swt.widgets.List list1, org.eclipse.swt.widgets.List list2) {
        int sel1 = list1.getSelectionIndex();
        if (sel1 >= 0) {
            String selectedItem = list1.getItem(sel1);
            list2.add(selectedItem);
            list1.remove(sel1);
            // Update the result.
            result = listMeta.getItems();
        }
    }

}
