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

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @author Ben Birch (Ben.Birch{[at]}hitachivantara{[dot]}com>
 * @version : $
 */
public abstract class WekaServerTask {

  /**
   * Process one or more datasets. Subclasses to override.
   *
   * @param input one or more input datasets to process
   * @return result of task processing in json format
   * @throws Exception if a problem occurs
   */
  public abstract String processData(String... input) throws Exception;
}
