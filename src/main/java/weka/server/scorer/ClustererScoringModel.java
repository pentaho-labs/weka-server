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

import weka.clusterers.Clusterer;
import weka.core.Instances;
import weka.server.TaskConfigUtils;

/**
 * Scoring model that handles weka.clusterers.Cluster instances.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @author Ben Birch (Ben.Birch{[at]}hitachivantara{[dot]}com>
 * @version : $
 */
public class ClustererScoringModel extends WekaScoringModel {

  /** The clusterer to use */
  protected Clusterer m_clusterer;

  /**
   * Constructor
   *
   * @param taskConfigUtils the configuration/utils to use
   */
  public ClustererScoringModel(TaskConfigUtils taskConfigUtils) {
    super(taskConfigUtils);
  }

  @Override
  protected void setUnderlyingModel(Object wekaModel, Instances modelHeader)
    throws Exception {

    if (!(wekaModel instanceof Clusterer)) {
      TaskConfigUtils.generateError(this, "Object is not an "
        + "instance of Clusterer!");
    }

    m_clusterer = (Clusterer) wekaModel;
    m_modelTrainingHeader = modelHeader;
  }

  @Override protected double[][] distributionsForInstances(Instances toScore)
    throws Exception {

    Instances mappedToScore =
      new Instances(m_modelTrainingHeader, toScore.numAttributes());

    for (int i = 0; i < toScore.numInstances(); i++) {
      mappedToScore.add(constructMappedInstance(toScore.instance(i)));
    }

    double[][] preds = new double[mappedToScore.numInstances()][];

    for (int i = 0; i < mappedToScore.numInstances(); i++) {
      preds[i] = m_clusterer.distributionForInstance(mappedToScore.instance(i));
    }

    return preds;
  }

  @Override protected String[] getPredictionColumnNames() throws Exception {
    String[] names = new String[m_clusterer.numberOfClusters()];

    for (int i = 0; i < names.length; i++) {
      names[i] = "prob_cluster_" + i;
    }
    return names;
  }
}
