/**
 *
 */
package org.theseed.reports;

import java.util.Arrays;
import java.util.List;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.theseed.dl4j.train.ClassPredictError;
import org.theseed.dl4j.train.IPredictError;

/**
 * This computes the confusion matrices for a classification model.
 *
 * @author Bruce Parrello
 *
 */
public class ClassValidationConfusion extends ValidationDisplayReport {

    // FIELDS
    /** confusion matrix for the training set [o][e] */
    private int[][] trainMatrix;
    /** confusion matrix for the testing set [o][e] */
    private int[][] testMatrix;
    /** confusion matrix for the sum of the two sets [o][e] */
    private int[][] allMatrix;
    /** array of matrices */
    private int[][][] matrixArray;
    /** number of labels */
    int nLabels;

    public ClassValidationConfusion() {
        super();
    }

    /**
     * Get the testing matrix value for a pair of labels.
     *
     * @param o		output label index
     * @param e		expected label index
     *
     * @return the count for the specified combination
     */
    public int getTestCount(int o, int e) {
        return this.testMatrix[o][e];
    }

    /**
     * Get the training matrix value for a pair of labels.
     *
     * @param o		output label index
     * @param e		expected label index
     *
     * @return the count for the specified combination
     */
    public int getTrainCount(int o, int e) {
        return this.testMatrix[o][e];
    }

    /**
     * Get the specified matrix value for a pair of labels.
     *
     * @param type	0 = testing, 1 = training, 2 = both
     * @param o		output label index
     * @param e		expected label index
     *
     * @return the count for the specified combination
     */
    public int getCount(int type, int o, int e) {
        return this.matrixArray[type][o][e];
    }

    @Override
    public void startReport(List<String> metaCols, List<String> labels) {
        // Clear the matrices.
        nLabels = labels.size();
        this.trainMatrix = new int[nLabels][nLabels];
        this.testMatrix = new int[nLabels][nLabels];
        this.allMatrix = new int[nLabels][nLabels];
        for (int i = 0; i < nLabels; i++) {
            Arrays.fill(this.trainMatrix[i], 0);
            Arrays.fill(this.testMatrix[i], 0);
            Arrays.fill(this.allMatrix[i], 0);
        }
    }

    @Override
    public void reportOutput(List<String> metaData, INDArray expected, INDArray output) {
        for (int r = 0; r < metaData.size(); r++) {
            String id = metaData.get(r);
            int e = ClassPredictError.computeBest(expected, r);
            int o = ClassPredictError.computeBest(output, r);
            // Now we have the expected and output values.
            if (this.isTrained(id))
                this.trainMatrix[o][e]++;
            else
                this.testMatrix[o][e]++;
        }
    }

    @Override
    public void finishReport(IPredictError errors) {
        for (int o = 0; o < nLabels; o++) {
            for (int e = 0; e < nLabels; e++)
                this.allMatrix[o][e] = this.testMatrix[o][e] + this.trainMatrix[o][e];
        }
        this.matrixArray = new int[][][] { this.testMatrix, this.trainMatrix, this.allMatrix };
    }

}
