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

package weka.server.utils;

import weka.core.Instance;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static weka.server.dataprep.DefaultJsonInstancesDataPreparer.MAPPER;

/**
 * Utility to convert ARFF file to pandas-split json format
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version : $
 */
public class InstancesToPandasSplit {

  public static void main(String[] args) {
    try {
      Instances toConvert = new Instances(new BufferedReader(new FileReader(args[0])));

      Map<String, Object> m = new LinkedHashMap<>();
      List<String> colNames = new ArrayList<>();
      for (int i = 0; i < toConvert.numAttributes(); i++) {
        colNames.add(toConvert.attribute(i).name());
      }

      m.put("columns", colNames);

      List<List<Object>> insts = new ArrayList<>();
      for (int i = 0; i < toConvert.numInstances(); i++) {
        Instance current = toConvert.instance(i);
        List<Object> instToAdd = new ArrayList<>();
        for (int j = 0; j < toConvert.numAttributes(); j++) {
          if (current.isMissing(j)) {
            instToAdd.add(null);
          } else if (current.attribute(j).isNumeric()) {
            instToAdd.add(current.value(j));
          } else {
            instToAdd.add(current.stringValue(j));
          }
        }
        insts.add(instToAdd);
      }

      m.put("data", insts);

      System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(m));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
