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

import weka.core.Instance;
import weka.core.Instances;
import weka.server.TaskConfigUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static weka.server.dataprep.DefaultJsonInstancesDataPreparer.MAPPER;

/**
 * Quick and dirty execution from the command line.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version 1: $
 */
public class Test {

  public static void main(String[] args) {
    TaskConfigUtils scorerUtils =
      new TaskConfigUtils("wekaServer_" + args[0] + ".props");

    // convert an arff file in the pandas-split format
    try {
      String arffF = args[1];
      Instances input =
        new Instances(new BufferedReader(new FileReader(arffF)));

      Map<String, Object> m = new LinkedHashMap<>();
      List<String> atts = new ArrayList<>();
      for (int i = 0; i < input.numAttributes(); i++) {
        atts.add(input.attribute(i).name());
      }

      m.put("columns", atts);
      List<List<Object>> data = new ArrayList<>();
      for (int i = 0; i < input.numInstances(); i++) {
        Instance current = input.instance(i);
        List<Object> row = new ArrayList<>();
        for (int j = 0; j < current.numAttributes(); j++) {
          if (current.isMissing(j)) {
            row.add("Null");
          } else {
            if (current.attribute(j).isNominal()
              || current.attribute(j).isString()) {
              row.add(current.stringValue(j));
            } else {
              row.add(current.value(j));
            }
          }
        }
        data.add(row);
      }

      m.put("data", data);

      WekaScoringModelPool pool = new WekaScoringModelPool(scorerUtils);

      String result = pool.getScorer().scoreData(MAPPER.writeValueAsString(m));
      if (!scorerUtils.debug) {
        System.out.println(result);
      }

      // second time
      /*
       * System.out.
       * println("Predicting the data a second time (results should be " +
       * "identical to the first time)"); result =
       * pool.getScorer().scoreData(MAPPER.writeValueAsString(m)); if
       * (!scorerUtils.debug) { System.out.println(result); }
       */

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
