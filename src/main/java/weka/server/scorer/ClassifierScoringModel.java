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

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.server.TaskConfigUtils;

/**
 * Scoring model that handles weka.classifier.Classifier instances.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @author Ben Birch (Ben.Birch{[at]}hitachivantara{[dot]}com>
 * @version 1: $
 */
public class ClassifierScoringModel extends WekaScoringModel {

  /** The classifier to score with */
  protected Classifier m_classifier;

  /**
   * Constructor
   *
   * @param scorerUtils the config/utils to use
   */
  public ClassifierScoringModel(TaskConfigUtils scorerUtils) {
    super(scorerUtils);
  }

  @Override
  protected void setUnderlyingModel(Object wekaModel, Instances modelHeader)
    throws Exception {
    if (!(wekaModel instanceof Classifier)) {
      TaskConfigUtils.generateError(this, "Object is not an "
        + "instance of Classifier!");
    }

    m_classifier = (Classifier) wekaModel;
    m_modelTrainingHeader = modelHeader;
  }

  @Override
  protected double[][] distributionsForInstances(Instances toScore)
    throws Exception {

    Instances mappedToScore =
      new Instances(m_modelTrainingHeader, toScore.numInstances());

    for (int i = 0; i < toScore.numInstances(); i++) {
      mappedToScore.add(constructMappedInstance(toScore.instance(i)));
    }

    double[][] preds = ((AbstractClassifier) m_classifier)
      .distributionsForInstances(mappedToScore);

    return preds;
  }

  @Override
  protected String[] getPredictionColumnNames() {
    String[] names = new String[m_modelTrainingHeader.classAttribute().numValues()];
    if (m_modelTrainingHeader.classAttribute().isNumeric()) {
      names[0] = "pred_" + m_modelTrainingHeader.classAttribute().name();
    } else {
      for (int i = 0; i < m_modelTrainingHeader.classAttribute().numValues(); i++) {
        names[i] = "prob_" + m_modelTrainingHeader.classAttribute().value(i);
      }
    }
    return names;
  }
}
