/**
 *
 */
package org.theseed.dl4j.win;

import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.theseed.dl4j.train.GradientUpdater;
import org.theseed.dl4j.train.RunStats;
import org.theseed.dl4j.train.Trainer;
import org.theseed.dl4j.train.TrainingProcessor;
import org.theseed.dl4j.LossFunctionType;
import org.theseed.dl4j.Regularization;
import org.theseed.io.ParmFile;
import org.theseed.win.ShellUtils;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.weightinit.WeightInit;

import java.io.File;
import java.io.IOException;

import org.deeplearning4j.nn.conf.GradientNormalization;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.TabItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

/**
 * @author Bruce Parrello
 *
 */
public class ParmsDialog extends Dialog {

    // FIELDS
    /** relationship of tab group to the parent layout */
    private static final GridData TAB_GROUP_LAYOUT_DATA = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(ParmsDialog.class);
    /** controlling window shell */
    protected Shell shell;
    /** parm file to save */
    private File parmFile;
    /** parameter map */
    private ParmFile parms;
    /** model type */
    TrainingProcessor.Type modelType;
    /** layout for tab composites */
    private static final GridLayout TAB_GROUP_LAYOUT = new GridLayout(3, false);
    /** target width for main controls */
    public static final int TARGET_WIDTH = 550;
    /** default width for label column */
    public static final int LABEL_WIDTH = 70;
    /** width of the form */
    private static final int FORM_WIDTH = TARGET_WIDTH + LABEL_WIDTH + 80;

    /**
     * Create the dialog.
     *
     * @param parent	parent shell
     * @param style		window style
     * @param parmFIle	file being editted
     * @param modelType	type of model for the parm file
     */
    public ParmsDialog(Shell parent, int style, File parmF, TrainingProcessor.Type modelType) {
        super(parent, style);
        String modelName = parmF.getParentFile().getName();
        parmFile = parmF;
        setText("Parameter Configuration for Model " + modelName);
        try {
            parms = new ParmFile(parmF);
        } catch (IOException e) {
            ShellUtils.showErrorBox(parent, "Cannot Read Parm File", e.getMessage());
        }
    }

    /**
     * Open the dialog.
     */
    public void open() {
        createContents();
        shell.open();
        shell.layout();
        Display display = getParent().getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    /**
     * Create contents of the dialog.
     */
    private void createContents() {
        shell = new Shell(getParent(), SWT.BORDER | SWT.TITLE | SWT.APPLICATION_MODAL);
        shell.setSize(FORM_WIDTH, 700);
        shell.setText(getText());
        shell.setLayout(new GridLayout(1, false));

        TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
        tabFolder.setLayoutData(TAB_GROUP_LAYOUT_DATA);

        // STRUCTURE PARAMETERS

        TabItem tbtmStructure = new TabItem(tabFolder, SWT.NONE);
        tbtmStructure.setText("Structure");

        Composite grpStructure = new Composite(tabFolder, SWT.NONE);
        tbtmStructure.setControl(grpStructure);
        grpStructure.setLayout(TAB_GROUP_LAYOUT);
        grpStructure.setLayoutData(TAB_GROUP_LAYOUT_DATA);

        ParmDialogText.load(grpStructure, parms, "meta");
        ParmDialogText.load(grpStructure, parms, "col");
        ParmDialogChoices.load(grpStructure, parms, "init", Activation.values());
        ParmDialogChoices.load(grpStructure, parms, "activation", Activation.values());
        ParmDialogFlag.load(grpStructure, parms, "raw");
        ParmDialogFlag.load(grpStructure, parms, "batch");
        ParmDialogGroup cnn = ParmDialogText.load(grpStructure, parms, "cnn");
        ParmDialogGroup filters = ParmDialogText.load(grpStructure, parms, "filters");
        ParmDialogGroup strides = ParmDialogText.load(grpStructure, parms, "strides");
        ParmDialogGroup subs = ParmDialogText.load(grpStructure, parms, "sub");
        if (cnn != null) {
            if (filters != null) cnn.setGrouped(filters);
            if (strides != null) cnn.setGrouped(strides);
            if (subs != null) cnn.setGrouped(subs);
        }
        ParmDialogText.load(grpStructure, parms, "lstm");
        ParmDialogGroup wGroup = ParmDialogText.load(grpStructure, parms, "widths");
        ParmDialogGroup bGroup = ParmDialogText.load(grpStructure, parms, "balanced");
        if (bGroup != null && wGroup != null) {
            wGroup.setExclusive(bGroup);
            bGroup.setExclusive(wGroup);
        }

        // SEARCH CONTROL PARAMETERS

        TabItem tbtmSearch = new TabItem(tabFolder, SWT.NONE);
        tbtmSearch.setText("Training");

        ScrolledComposite grpSearch0 = new ScrolledComposite(tabFolder, SWT.V_SCROLL);
        grpSearch0.setLayout(new FillLayout());
        grpSearch0.setLayoutData(TAB_GROUP_LAYOUT_DATA);
        tbtmSearch.setControl(grpSearch0);
        Composite grpSearch = new Composite(grpSearch0, SWT.NONE);
        grpSearch0.setContent(grpSearch);
        grpSearch.setLayout(TAB_GROUP_LAYOUT);
        grpSearch.setLayoutData(TAB_GROUP_LAYOUT_DATA);

        Enum<?>[] preferTypes = (this.modelType == TrainingProcessor.Type.CLASS ? RunStats.OptimizationType.values()
                : RunStats.RegressionType.values());
        ParmDialogChoices.load(grpSearch, parms, "prefer", preferTypes);
        ParmDialogChoices.load(grpSearch, parms, "method", Trainer.Type.values());
        ParmDialogText.load(grpSearch, parms, "bound");
        ParmDialogChoices.load(grpSearch, parms, "lossFun", LossFunctionType.values());
        ParmDialogText.load(grpSearch, parms, "weights");
        ParmDialogText.load(grpSearch, parms, "iter");
        ParmDialogText.load(grpSearch, parms, "batchSize");
        ParmDialogText.load(grpSearch, parms, "testSize");
        ParmDialogText.load(grpSearch, parms, "maxBatches");
        ParmDialogText.load(grpSearch, parms, "earlyStop");
        ParmDialogChoices.load(grpSearch, parms, "regMode", Regularization.Mode.values());
        ParmDialogText.load(grpSearch, parms, "regFactor");
        ParmDialogText.load(grpSearch, parms, "seed");
        ParmDialogChoices.load(grpSearch, parms, "start", WeightInit.values());
        ParmDialogChoices.load(grpSearch, parms, "gradNorm", GradientNormalization.values());
        ParmDialogChoices.load(grpSearch, parms, "updater", GradientUpdater.Type.values());
        ParmDialogText.load(grpSearch, parms, "learnRate");
        ParmDialogChoices.load(grpSearch, parms, "bUpdater", GradientUpdater.Type.values());
        ParmDialogText.load(grpSearch, parms, "updateRate");

        grpSearch.setSize(grpSearch.computeSize(FORM_WIDTH - 50, SWT.DEFAULT));

        // BOTTOM BUTTON BAR

        Composite grpButtonBar = new Composite(shell, SWT.NONE);
        grpButtonBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
        grpButtonBar.setLayout(new FillLayout(SWT.HORIZONTAL));

        Button btnSave = new Button(grpButtonBar, SWT.NONE);
        btnSave.setText("Save");
        btnSave.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                try {
                    parms.save(parmFile);
                } catch (IOException e) {
                    ShellUtils.showErrorBox(getParent(), "Error Saving Parm File", e.getMessage());
                }
            }
        });

        Button btnClose = new Button(grpButtonBar, SWT.NONE);
        btnClose.setText("Cancel");
        btnClose.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                shell.close();
            }
        });

        Button btnOK = new Button(grpButtonBar, SWT.NONE);
        btnOK.setText("Save and Close");
        btnOK.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                try {
                    parms.save(parmFile);
                    shell.close();
                } catch (IOException e) {
                    ShellUtils.showErrorBox(getParent(), "Error Saving Parm File", e.getMessage());
                }
            }
        });

    }
}
