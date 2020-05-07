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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import io.javalin.Javalin;
import weka.core.WekaException;

/**
 * Simple Javalin server for executing WekaServerTasks
 *
 * @author Ben Birch (Ben.Birch{[at]}hitachivantara{[dot]}com>
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public class WekaRESTServer {
    // json string sample for testing purposes
    private static String json;

    // private static Dictionary<String, WekaScoringModelPool> poolDictionary = new Hashtable<String, WekaScoringModelPool>();
    private static Map<String, WekaServerTaskPool> poolMap = new HashMap<>();

    /**
     * Entry point, no args required
     * 
     * @param args
     */
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7000);
        // load sample json object and instance of scorer
        try (Stream<String> stream = Files.lines(Paths.get("input_data/iris.json"), StandardCharsets.UTF_8)) {
            StringBuilder contentBuilder = new StringBuilder();
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
            json = contentBuilder.toString();
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        app.get("/", ctx -> ctx
                .result("Post JSON data to /invocations, or get /sample to get a sample JSON input data set"));
        app.post("/invocations", ctx -> {
            try {
                // taskid is the suffix of the wekaServer props file name for the task to be executed
                String taskid = ctx.queryParam("taskid");
                if (taskid == null) {
                    throw new Exception("missing query parameter taskid");
                }
                String propsFileName = "wekaServer_" + taskid + ".props";

                WekaServerTaskPool pool = poolMap.get(propsFileName);
                if (pool == null) {
                    System.out.println("Creating new server pool with " + propsFileName);
                    TaskConfigUtils configUtils = new TaskConfigUtils(propsFileName);
                    pool = configUtils.getTaskPool();
                    poolMap.put(propsFileName, pool);
                }
                ctx.result(pool.getTask().processData(ctx.body()));
            } catch (WekaException e) {
                ctx.status(400);
                ctx.json(new Object() {
                    public String error = e.getMessage();
                });
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new Object() {
                    public String error = e.getMessage();
                });
            }
        });
        app.get("/sample", ctx -> ctx.result(json));
    }
}
