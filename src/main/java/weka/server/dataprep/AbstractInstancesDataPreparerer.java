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

import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.CSVLoader;

import java.io.ByteArrayInputStream;

/**
 * Abstract base class for data preparers. Most of the time, the default
 * DefaultJsonDataPreparer will be sufficient for directly converting
 * JSON-formatted pandas-split data into Weka Instances. Specific sub-classes
 * can be used to cover data-prep cases that can't be handled by using Weka
 * filter chains encapsulated along with the model (i.e. via Weka's
 * FilteredClassifier).
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @author Ben Birch (Ben.Birch{[at]}hitachivantara{[dot]}com>
 * @version : $
 */
public abstract class AbstractInstancesDataPreparerer
  extends AbstractDataPreparer {

  /** Key in the properties file to look for options for CSV conversion */
  public static final String PROP_SCORER_CSV_OPTS_KEY =
    "weka.scorer.data.preparer.csvOpts";

  /**
   * Utility method that returns a set of Instances read from the supplied CSV
   * string data
   *
   * @param csvInput the data, in CSV format, to convert to Weka Instances
   * @param csvLoaderOpts options to pass to the CSVLoader object
   * @return a set of Instances
   * @throws Exception if a problem occurs
   */
  public static Instances prepareCSVData(String csvInput, String csvLoaderOpts)
    throws Exception {

    CSVLoader loader = new CSVLoader();
    if (csvLoaderOpts != null && csvLoaderOpts.length() > 0) {
      loader.setOptions(Utils.splitOptions(csvLoaderOpts));
    }

    loader.setSource(new ByteArrayInputStream(csvInput.getBytes("UTF-8")));

    return loader.getDataSet();
  }
}
