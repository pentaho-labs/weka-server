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

package weka.server.dataprep;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.core.Instance;
import weka.core.Instances;
import weka.server.TaskConfigUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.PropertyAccessor.FIELD;

/**
 * Default data preparer for JSON pandas-split formatted data. Simply converts
 * (via CSV intermediatary) to Weka Instances/ARFF format. Column types can be
 * coerced via options to the CSVLoader, specified via the
 * weka.scorer.data.preparer.csvOpts property in the props config file
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @author Ben Birch (Ben.Birch{[at]}hitachivantara{[dot]}com>
 */
public class DefaultJsonInstancesDataPreparer
  extends AbstractInstancesDataPreparerer {
  protected TaskConfigUtils taskConfigUtils;
  final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public DefaultJsonInstancesDataPreparer(TaskConfigUtils taskConfigUtils) {
    this.taskConfigUtils = taskConfigUtils;
  }

  public static final ObjectMapper MAPPER = new ObjectMapper() {
    {
      this.registerModule(new ParameterNamesModule());
      this.setVisibility(FIELD, ANY);
      this.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      this.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
      this.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
    }
  };

  /**
   * Converts a pandas-split json string to a CSV string
   *
   * @param json json pandas-split string
   * @return a csv string
   * @throws Exception if a problem occurs
   */
  @SuppressWarnings("unchecked")
  protected static String pandasSplitJsonToCsv(String json) throws Exception {

    Map<String, Object> m = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
    });

    List<Object> columnNames = (List<Object>) m.get("columns");
    if (columnNames == null || columnNames.size() == 0) {
      TaskConfigUtils
        .generateError(DefaultJsonInstancesDataPreparer.class, "No column names " + "declared in payload");
    }

    StringBuilder b = new StringBuilder();
    for (Object n : columnNames) {
      b.append(n.toString()).append(",");
    }
    b.setLength(b.length() - 1);
    b.append("\n");

    // now the data
    List<Object> data = (List<Object>) m.get("data");
    if (data == null || data.size() == 0) {
      TaskConfigUtils
        .generateError(DefaultJsonInstancesDataPreparer.class, "No data rows in payload");
    }

    for (Object r : data) {
      List<Object> row = (List<Object>) r;

      for (Object value : row) {
        String strValue = value != null ? value.toString() : "";
        if (strValue.trim().equalsIgnoreCase("null")) {
          strValue = "";
        }
        b.append(strValue).append(",");
      }
      b.setLength(b.length() - 1);
      b.append("\n");
    }

    return b.toString();
  }

  /**
   * Default implementation assumes only one input. Input data is assumed to be in
   * the JSON pandas-split format used by MLFlow. Data is convered to Weka
   * Instances format as-is (i.e. without any further pre-processing).
   *
   * @param input an array of one or more JSON input files
   * @return a set of Weka Instances representing the data
   * @throws Exception if a problem occurs
   */
  @Override
  @SuppressWarnings("unchecked")
  public Instances prepareInputData(String... input) throws Exception {

    if (input.length > 1) {
      TaskConfigUtils
        .generateError(DefaultJsonInstancesDataPreparer.class, "Was expecting only a " + "single input dataset");
    }

    String csvData = pandasSplitJsonToCsv(input[0]);
    String csvOpts = taskConfigUtils.getTaskProperty(
      AbstractInstancesDataPreparerer.PROP_SCORER_CSV_OPTS_KEY);

    Instances result = AbstractInstancesDataPreparerer
      .prepareCSVData(csvData, csvOpts);
    if (taskConfigUtils.debug) {
      logger.debug("Decoded input data:\n\n" + result.toString());
    }

    return result;
  }

  /**
   * Read an input ARFF file and output JSON pandas-split formatted version
   *
   * @param args
   */
  public static void main(String[] args) {

    // write an arff file in the pandas-split format
    try {
      String arffF = args[0];
      Instances input = new Instances(new BufferedReader(new FileReader(arffF)));

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
            if (current.attribute(j).isNominal() || current.attribute(j).isString()) {
              row.add(current.stringValue(j));
            } else {
              row.add(current.value(j));
            }
          }
        }
        data.add(row);
      }

      m.put("data", data);

      System.out.println(MAPPER.writeValueAsString(m));
    } catch (Exception ex) {
      ex.printStackTrace();
    }

  }
}
