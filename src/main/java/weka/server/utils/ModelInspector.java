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

import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.SerializationHelper;
import weka.core.Utils;
import weka.core.WekaPackageManager;

/**
 * Command line utility to print contents of serialized Weka model file
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @author Ben Birch (Ben.Birch{[at]}hitachivantara{[dot]}com>
 * @version : $
 */
public class ModelInspector {

  public static void main(String[] args) {
    try {
      WekaPackageManager.loadPackages(false, false, false);

      Object[] modelStuff = SerializationHelper.readAll(args[0]);

      if (modelStuff.length > 1 && modelStuff[1] instanceof Instances) {
        System.out
          .println("Header of data used to train model:\n\n" + modelStuff[1]);
      }

      System.out.println("Model:\n\n" + modelStuff[0]);
      System.out.println("\n\n");
      String modelType = modelStuff[0].getClass().getCanonicalName();
      if (modelStuff[0] instanceof OptionHandler) {
        modelType +=
          " " + Utils.joinOptions(((OptionHandler) modelStuff[0]).getOptions());
      }
      System.out.println(modelType);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
