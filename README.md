Lightweight Weka server framework that can be deployed in a
container and accessed as a micro-service via web-services
endpoints. Currently supports serving predictions from Weka
models (classifiers and clusterers).

Install any required Weka packages before executing. E.g.:

java -cp lib/weka.jar weka.core.WekaPackageManager -install-package SMOTE

Expects a wekaServer_<task_id>.props file to be present in
${user.home}/config.  This file specifies parameters for the task to
be executed - e.g. such as the task type, model file to load (from
${user.home}/models), any specific data preparer class to use, and
various other options relating to input. See the iris example in
config.

The default data peraparation class for model scoring expects incoming
data to score in the JSON pandas-split format. This is converted
directly to a Weka Instances object and passed to the model for
prediction. Predictions are output/returned as JSON-formatted
pandas-split.

To run a test from the command line:

1. copy/move config and models to ${user.home}
2. mvn clean compile assembly:single
3. java -cp target/weka-server-1.0-SNAPSHOT-jar-with-dependencies.jar weka.server.scorer.Test irisClassifier input_data/iris.arff

This loads the ARFF file, converts it to JSON and then invokes the
WekaScoringModel; which, in-turn, loads the j48_iris.model from the
models directory, converts the JSON back Weka Instances and passes it
to the J48 model for prediction. Resulting probabilty distributions
are returned in JSON pandas-split format.

To build:

    ``` sh
    mvn clean compile assembly:single
    ```

Docker container build (assumes maven build complete):

    ``` sh
    docker build -t server:test .
    docker run -it -p 7000:7000 server:test
    curl http://localhost:7000
    ```

Send iris data for scoring:

     ``` sh
     curl -X POST -H "Content-Type: application/json" -d @input_data/iris.json http://localhost:7000/invocations?taskid=irisClassifier
     ```
