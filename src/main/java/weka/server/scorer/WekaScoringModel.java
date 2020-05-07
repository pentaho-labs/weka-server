/*******************************************************************************
 * Pentaho Data Science
 * <p/>
 * Copyright (c) 2002-2020 Hitachi Vantara. All rights reserved.
 * <p/>
 * ******************************************************************************
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 * <p/>
 ******************************************************************************/

package weka.server.scorer;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.server.dataprep.AbstractInstancesDataPreparerer;
import weka.server.TaskConfigUtils;
import weka.server.WekaServerTask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static weka.server.dataprep.DefaultJsonInstancesDataPreparer.MAPPER;

/**
 * Base class for Weka-based scoring models. getScorer() factory method returns
 * a suitable implementation based on the type of the serialized Weka model to
 * load. Currently, only subclass is ClassifierScoringModel (which handles Weka
 * supervised classifiers/regressors). TODO implement a ClustererScoringModel
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @author Ben Birch (Ben.Birch{[at]}hitachivantara{[dot]}com>
 * @version 1: $
 */
public abstract class WekaScoringModel extends WekaServerTask {

  public static final String TASK_ID = "ScoringTask";

  public static final String SCORER_MODEL_DEFAULT_DIRECTORY_KEY =
    "weka.server.scorer.model.defaultDirectory";
  public static final String SCORER_MODEL_OVERRIDE_DIRECTORY_KEY =
    "weka.server.scorer.model.overrideDirectory";
  public static final String PROP_SCORER_IMPL_KEY =
    "weka.server.scorer.scorer.impl";
  public static final String PROP_SCORER_MODEL_FILE_NAME_KEY =
    "weka.server.scorer.model.filename";
  public static final String PROP_DATA_PREP_KEY =
    "weka.server.scorer.data.preparer";

  /** Data preparer to use */
  protected AbstractInstancesDataPreparerer m_dataPreparerer;

  /** Header of the data used to train the model */
  protected Instances m_modelTrainingHeader;

  /** Utils/configuration props for this model */
  protected TaskConfigUtils taskConfigUtils;

  /**
   * Constructor
   *
   * @param taskConfigUtils the configuration/utils to use
   */
  public WekaScoringModel(TaskConfigUtils taskConfigUtils) {
    this.taskConfigUtils = taskConfigUtils;
  }

  /**
   * Construct an instance with the fields in the order that the model expects
   * 
   * @param input the incoming instance
   * @return an instance with columns mapped
   * @throws Exception if a problem occurs (i.e. there are missing inputs or
   *           type mismatches
   */
  protected Instance constructMappedInstance(Instance input) throws Exception {

    StringBuilder mappingProbs = new StringBuilder();
    mappingProbs.append("Input to model matching problems:\n\n");
    double[] vals = new double[m_modelTrainingHeader.numAttributes()];
    int probCount = 0;
    for (int i = 0; i < m_modelTrainingHeader.numAttributes(); i++) {
      Attribute modelA = m_modelTrainingHeader.attribute(i);

      if (i != m_modelTrainingHeader.classIndex()) {
        Attribute matchA = input.dataset().attribute(modelA.name());
        if (matchA == null) {
          mappingProbs.append("Model attribute '" + modelA.name()
            + "' does not seem " + "to have a match in the incoming data!")
            .append("\n");
          vals[i] = Utils.missingValue();
          probCount++;
        } else if (modelA.type() != matchA.type()) {
          mappingProbs
            .append(
              "Type mismatch between model attribute '" + modelA.toString()
                + "' and incoming attribute '" + matchA.toString() + "'")
            .append("\n");
          vals[i] = Utils.missingValue();
          probCount++;
        } else {
          vals[i] = input.value(matchA.index());
        }
      } else {
        // set class missing
        vals[i] = Utils.missingValue();
      }
    }

    if (probCount > 0) {
      TaskConfigUtils.generateError(this, mappingProbs.toString());
    }

    Instance inst = new DenseInstance(1.0, vals);
    inst.setDataset(m_modelTrainingHeader);

    return inst;
  }

  /**
   * Set the data prepararer to use
   * 
   * @param dataPreparer the data preparer to use
   */
  public void setDataPreparer(AbstractInstancesDataPreparerer dataPreparer) {
    m_dataPreparerer = dataPreparer;
  }

  @Override
  public String processData(String... input) throws Exception {
    return scoreData(input);
  }

  /**
   * convert and score the incoming dataset(s)
   *
   * @param input one or more datasets to score
   * @return scored data
   * @throws Exception if a problem occurs
   */
  public String scoreData(String... input) throws Exception {

    // convert/prepare input to Instances via DataPreparer
    Instances toScore = m_dataPreparerer.prepareInputData(input);

    // score data via distributionsForInstances
    double[][] preds = distributionsForInstances(toScore);

    // convert predictions to return string value via DataPreparer
    return prepareJsonPredictions(preds, getPredictionColumnNames());
  }

  protected void debugScoreDataNoPrep(Instances toScore) throws Exception {
    double[][] preds = distributionsForInstances(toScore);

    for (int i = 0; i < preds.length; i++) {
      String p = "";
      for (int j = 0; j < preds[i].length; j++) {
        p += " " + preds[i][j];
      }
      System.out.println("pred: " + p);
    }
  }

  /**
   * Takes an array of predictions and converts them to JSON output format
   *
   * @param preds the predictions to convert
   * @param columnNames an array of column names for the predictions
   * @return JSON formatted (pandas-split) output string
   * @throws Exception if a problem occurs
   */
  protected static String prepareJsonPredictions(double[][] preds,
    String[] columnNames) throws Exception {
    Map<String, Object> m = new LinkedHashMap<>();

    List<String> columns = new ArrayList<>();
    for (int i = 0; i < columnNames.length; i++) {
      columns.add(columnNames[i]);
    }

    m.put("columns", columns);

    List<List<Double>> predL = new ArrayList<>();
    for (int i = 0; i < preds.length; i++) {
      List<Double> p = new ArrayList<>();
      for (int j = 0; j < columnNames.length; j++) {
        p.add(preds[i][j]);
      }
      predL.add(p);
    }

    m.put("data", predL);

    return MAPPER.writeValueAsString(m);
  }

  /**
   * Set the underlying Weka model
   * 
   * @param wekaModel the underlying Weka model
   * @param modelHeader the training header of the data used to train the model
   *          (used for field mapping)
   * @throws Exception if a problem occurs
   */
  protected abstract void setUnderlyingModel(Object wekaModel,
    Instances modelHeader) throws Exception;

  /**
   * Returns predictions for the supplied set of instances
   *
   * @param toScore a set of instances to score
   * @return an array of predictions, one row for each instance to predict
   * @throws Exception if a problem occurs
   */
  protected abstract double[][] distributionsForInstances(Instances toScore)
    throws Exception;

  /**
   * Get the names of the columns in the prediction array (i.e. for supervised
   * classification models this corresponds to the class labels)
   * 
   * @return an array of column names
   * @exception if the column names cannot be determined for some reason
   */
  protected abstract String[] getPredictionColumnNames() throws Exception;
}
