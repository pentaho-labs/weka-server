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

package weka.server;

import weka.core.PluginManager;
import weka.core.WekaException;
import weka.core.WekaPackageManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.LoggerFactory;

/**
 * Utilities for handling properties for models
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @author Ben Birch (Ben.Birch{[at]}hitachivantara{[dot]}com>
 * @version : $
 */
public class TaskConfigUtils {

  // General property file keys
  public static final String TASK_PROPS_FILENAME = "wekaServerTask.props";
  public static final String TASK_DEBUG_KEY = "weka.server.task.debug";
  public static final String TASK_TYPE_KEY = "weka.server.task.type";
  public static final String TASK_POOL_SIZE_KEY = "weka.server.task.poolSize";
  public static final int TASK_DEFAULT_POOL_SIZE = 1;

  public static final String PROP_WEKA_PACKAGE_MANAGER_OFFLINE_KEY =
    "weka.packageManager.offline";

  /** Properties object for this TaskConfigUtils instance */
  protected Properties properties;

  /** True for debugging output */
  public boolean debug;

  /**
   * The name of the properties file to use with this TaskConfigUtils instance
   */
  protected String propFileName;

  // load the global props and read the list of task types supported by this
  // sever
  static {
    try {
      PluginManager.addFromProperties(
        new File(System.getProperty("user.home") + File.separator + "config"
          + File.separator + "wekaServerTasks.props"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Constructor
   *
   * @param propFileName the name of the properties file to read and manage
   */
  public TaskConfigUtils(String propFileName) {
    if (propFileName == null) {
      propFileName = TASK_PROPS_FILENAME;
    }
    this.propFileName = propFileName;
    try {
      loadProperties();
      String debug = getTaskProperty(TASK_DEBUG_KEY);
      boolean dbg = debug != null && debug.equalsIgnoreCase("true");
      String offline = getTaskProperty(PROP_WEKA_PACKAGE_MANAGER_OFFLINE_KEY);
      if (offline != null && offline.length() > 0) {
        LoggerFactory.getLogger(TaskConfigUtils.class)
          .info("Setting package manager offline mode to: " + offline);
        System.setProperty(PROP_WEKA_PACKAGE_MANAGER_OFFLINE_KEY, offline);
      }

      // load Weka packages
      WekaPackageManager.loadPackages(dbg, false, false);
    } catch (IOException ex) {
      System.err.println(ex);
    }
  }

  /**
   * Load the properties file
   *
   * @throws IOException if a problem occurs
   */
  protected void loadProperties() throws IOException {
    properties = new Properties();
    properties
      .load(new BufferedReader(new FileReader(System.getProperty("user.home")
        + File.separator + "config" + File.separator + propFileName)));

    String debug_prop = getTaskProperty(TASK_DEBUG_KEY);
    debug = debug_prop != null && debug_prop.equalsIgnoreCase("true");
  }

  /**
   * Get a named property for this task
   * 
   * @param propName the name of the property to get
   * @return the property value (or null if the named property does not exist)
   * @throws IOException if a problem occurs
   */
  public String getTaskProperty(String propName) throws IOException {
    if (properties == null) {
      // load properties for the first time and cache in a class variable
      loadProperties();
    }

    return properties.getProperty(propName);
  }

  /**
   * Get an appropriately configured task pool for the task type specified in
   * the config property file
   * 
   * @return a configured WekaServerTaskPool that can serve requests of the type
   *         specified in the config/props file
   * @throws Exception if a problem occurs
   */
  public WekaServerTaskPool getTaskPool() throws Exception {
    String taskType = getTaskProperty(TASK_TYPE_KEY);
    if (taskType == null || taskType.length() == 0) {
      generateError(this, "No task type specified in " + propFileName);
    }

    WekaServerTaskPool taskPool = (WekaServerTaskPool) PluginManager
      .getPluginInstance(WekaServerTaskPool.class.getCanonicalName(), taskType);
    taskPool.setTaskConfigUtils(this);

    return taskPool;
  }

  /**
   * Generate an exception/error output with the supplied error message
   *
   * @param obj the object generating the error
   * @param message the message to include in the exception/error output
   * @throws Exception always
   */
  public static void generateError(Object obj, String message)
    throws Exception {
    LoggerFactory.getLogger(obj.getClass()).error(message);
    throw new WekaException(message);
  }
}
