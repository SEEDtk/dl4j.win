/**
 *
 */
package org.theseed.reports;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.swtchart.model.CartesianSeriesModel;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.theseed.dl4j.train.IPredictError;

/**
 * This class is the reporter for the regression scatter display.  It creates a map of the results from the prediction
 * validation run.  This map is then used as a data model for the graph.
 *
 * @author Bruce Parrello
 *
 */
public class RegressionValidationScatter extends ValidationDisplayReport implements IValidationReport {

    // FIELDS
    /** map of ID strings to predictions */
    private Map<String, Prediction> predictionMap;

    public RegressionValidationScatter() {
        super();
    }

    /**
     * This class represents the expected and output values for a single record.
     * Note the predictions come in in INDArray batches, but each of these
     * contains only a pointer to the matrix, not the matrix itself.
     */
    private class Prediction {

        private INDArray expect;
        private INDArray output;
        private int row;

        public Prediction(int r, INDArray expect, INDArray output) {
            this.expect = expect;
            this.output = output;
            this.row = r;
        }

        /**
         * @return the expected value for the given label
         *
         * @param labelIdx	index of the label of interest
         */
        public double getExpect(int labelIdx) {
            return this.expect.getDouble(this.row, labelIdx);
        }

        /**
         * @return the output value for the given label
         *
         * @param labelIdx	index of the label of interest
         */
        public double getOutput(int labelIdx) {
            return this.output.getDouble(this.row, labelIdx);
        }

    }

    @Override
    public void startReport(List<String> metaCols, List<String> labels) {
        // Create the prediction map.
        this.predictionMap = new HashMap<String, Prediction>(500);
    }

    @Override
    public void reportOutput(List<String> metaData, INDArray expected, INDArray output) {
        // Loop through the metadata, peeling off predictions.
        for (int r = 0; r < metaData.size(); r++) {
            String id = getId(metaData.get(r));
            this.predictionMap.put(id, new Prediction(r, expected, output));
        }
    }

    @Override
    public void finishReport(IPredictError errors) {
    }

    /**
     * This class produces the data model for the training or testing series on a specified label.
     */
    public class Model implements CartesianSeriesModel<String> {

        /** index of the label of interest */
        private int lblIdx;
        /** list of relevant IDs */
        private List<String> idList;

        /**
         * Create the model for a specified data series.
         *
         * @param lablIdx	index of the desired label column
         * @param training	TRUE for training data, FALSE for testing data
         */
        public Model(int labelIdx, boolean training) {
            this.lblIdx = labelIdx;
            // Get the IDs of the appropriate type.
            this.idList = predictionMap.keySet().stream().filter(x -> isTrained(x) == training).collect(Collectors.toList());
        }

        @Override
        public Iterator<String> iterator() {
            return this.idList.iterator();
        }

        @Override
        public Number getX(String data) {
            double retVal = 0.0;
            Prediction prediction = predictionMap.get(data);
            if (prediction != null)
                retVal = prediction.getExpect(this.lblIdx);
            return retVal;
        }

        @Override
        public Number getY(String data) {
            double retVal = 0.0;
            Prediction prediction = predictionMap.get(data);
            if (prediction != null)
                retVal = prediction.getOutput(this.lblIdx);
            return retVal;
        }

    }



}
