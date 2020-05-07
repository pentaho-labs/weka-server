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

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ConcurrentLinkedDeque;

import weka.classifiers.Classifier;
import weka.core.Environment;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.Utils;
import weka.core.OptionHandler;
import weka.clusterers.Clusterer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.server.dataprep.AbstractInstancesDataPreparerer;
import weka.server.dataprep.DefaultJsonInstancesDataPreparer;
import weka.server.TaskConfigUtils;
import weka.server.WekaServerTask;
import weka.server.WekaServerTaskPool;

/**
 * Manages a pool of models.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @author Ben Birch (Ben.Birch{[at]}hitachivantara{[dot]}com>
 */
public class WekaScoringModelPool extends WekaServerTaskPool {
  private ConcurrentLinkedDeque<WekaScoringModel> modelPool;
  private int poolSize;
  private TaskConfigUtils taskConfigUtils;
  final static Logger logger =
    LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * No-args constructor (so PluginManager can instantiate us)
   */
  public WekaScoringModelPool() {
  }

  public WekaScoringModelPool(TaskConfigUtils taskConfigUtils)
    throws Exception {
    setTaskConfigUtils(taskConfigUtils);
  }

  @Override
  public void setTaskConfigUtils(TaskConfigUtils taskConfigUtils)
    throws Exception {
    this.taskConfigUtils = taskConfigUtils;
    if (modelPool == null) {
      String poolS =
        taskConfigUtils.getTaskProperty(TaskConfigUtils.TASK_POOL_SIZE_KEY);
      poolSize = TaskConfigUtils.TASK_DEFAULT_POOL_SIZE;
      if (poolS != null && poolS.length() > 0) {
        poolSize = Integer.parseInt(poolS);
      }

      modelPool = new ConcurrentLinkedDeque<>();
      logger.debug("Initializing a scorer pool of size: " + poolSize);
      for (int i = 0; i < poolSize; i++) {
        modelPool.add(createNewScorer());
      }
    }
  }

  /**
   * Creates a new WekaScoringModel configured with appropriate DataPreparer.
   *
   * @return a WekaScoringModel
   * @throws Exception if a problem occurs
   */
  protected WekaScoringModel createNewScorer() throws Exception {
    WekaScoringModel scoringModel = null;

    // Get the data preparer
    AbstractInstancesDataPreparerer dataPreparerer = null;
    String dataPrepClassName =
      taskConfigUtils.getTaskProperty(WekaScoringModel.PROP_DATA_PREP_KEY);
    if (dataPrepClassName != null && dataPrepClassName.length() > 0) {
      Object dp = Class.forName(dataPrepClassName)
        .getConstructor(TaskConfigUtils.class).newInstance(taskConfigUtils);
      if (!(dp instanceof AbstractInstancesDataPreparerer)) {
        TaskConfigUtils.generateError(this,
          "User specified data preparer '" + dp.getClass().getCanonicalName()
            + "' is not an instance " + "of AbstractDataPreparer");
      }
      dataPreparerer = (AbstractInstancesDataPreparerer) dp;
    } else {
      // use default json data preparer
      dataPreparerer = new DefaultJsonInstancesDataPreparer(taskConfigUtils);
    }

    String modelFileName = taskConfigUtils
      .getTaskProperty(WekaScoringModel.PROP_SCORER_MODEL_FILE_NAME_KEY);
    try {
      modelFileName = Environment.getSystemWide().substitute(modelFileName);
    } catch (Exception ex) {
      // ignore substitution problems
    }

    if (modelFileName == null || modelFileName.length() == 0) {
      TaskConfigUtils.generateError(this,
        "No serialized model filename provided!");
    }

    String filePath = System.getProperty("user.home") + File.separator
      + "models" + File.separator + modelFileName;

    Object[] modelStuff = SerializationHelper.readAll(filePath);
    Object model = modelStuff[0];
    if (modelStuff.length < 2) {
      TaskConfigUtils.generateError(this,
        "Model file does not seem to contain header of training data used "
          + "to build the model. We can't map incoming fields without this information!");
    }
    Instances modelHeader = (Instances) modelStuff[1];

    String scoringModelImp =
      taskConfigUtils.getTaskProperty(WekaScoringModel.PROP_SCORER_IMPL_KEY);
    if (scoringModelImp != null && scoringModelImp.length() > 0) {
      // TODO custom user-specified WekaScoringModel implementation
    } else {
      // infer from model type...
      if (model instanceof Classifier) {
        scoringModel = new ClassifierScoringModel(taskConfigUtils);
      } else if (model instanceof Clusterer) {
        scoringModel = new ClustererScoringModel(taskConfigUtils);
      } else {
        TaskConfigUtils.generateError(this,
          "Unsupported model type: " + model.getClass().getCanonicalName());
      }
    }

    if (scoringModel != null) {
      scoringModel.setDataPreparer(dataPreparerer);
      scoringModel.setUnderlyingModel(model, modelHeader);

      if (taskConfigUtils.debug) {
        logger.info("Setting data preparer to: "
          + dataPreparerer.getClass().getCanonicalName());
        logger.info("Loaded model: " + model.getClass().getCanonicalName() + " "
          + Utils.joinOptions(((OptionHandler) model).getOptions()));
      }
    }

    return scoringModel;
  }

  @Override
  public WekaServerTask getTask() throws Exception {
    return getPooledScorer();
  }

  protected WekaScoringModel getScorer() throws Exception {
    return getPooledScorer();
  }

  protected WekaScoringModel getPooledScorer() throws Exception {
    WekaScoringModel toUse = modelPool.poll();
    if (toUse == null) {
      toUse = createNewScorer();
    }
    logger.debug("Obtaining a scorer. Pool size now: " + modelPool.size());

    return toUse;
  }

  protected void releasePooledScorer(WekaScoringModel scorer) {
    if (modelPool.size() < poolSize) {
      modelPool.add(scorer);
    }
    logger.debug("Releasing a scorer. Pool size now: " + modelPool.size());
  }

  /**
   * Static utility method for scoring incoming JSON data. A pool of
   * WekaScoringModels is maintained, so this method is thread-safe. Pool size
   * can be configured using the configuration property
   * weka.scorer.modelPoolSize. If unspecified, the default pool size is 5. If
   * requests outstrip the pool size then additional WekaScoringModel objects
   * are created and configured on the fly to meet demand. Note that this
   * process is not memory bounded.
   * 
   * @param data data in JSON pandas-split format to score
   * @return scores in JSON pandas-split format
   * @throws Exception if a problem occurs.
   */
  public String score(String... data) throws Exception {
    WekaScoringModel model = getPooledScorer();
    String result = model.scoreData(data);
    releasePooledScorer(model);

    return result;
  }
}
