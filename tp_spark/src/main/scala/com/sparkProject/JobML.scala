package com.sparkProject
import org.apache.spark.sql.SparkSession
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.feature.{StandardScaler}
import org.apache.spark.ml.feature.{OneHotEncoder, StringIndexer}
import org.apache.spark.ml.tuning.{ParamGridBuilder, CrossValidator}
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.tuning.{ParamGridBuilder, TrainValidationSplit}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator


/**
  * Created by raphael on 26/10/16.
  */

object JobML {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder
      .master("local")
      .appName("spark session TP_parisTech")
      .getOrCreate()
    val sc = spark.sparkContext

    val df = spark.read
      .format("com.databricks.spark.csv")
      .option("header", "true") // Use first line of all files as header
      .option("inferSchema", "true") // Automatically infer data type
      .option("delimiter", ",")
      .option("comment", "#")
      .parquet("../../INF729/cleanedDataFrame.parquet")


    // On drop les rows de type string restantes
    val cols = df.drop("row_id","koi_disposition").columns
    val assembled = new VectorAssembler()
      .setInputCols(cols)
      .setOutputCol("assembled_features")
      .transform(df)


    // Normalisation du vecteur de features
    // Fait avec setStandardization(true) dans la logisticregression

    // Dummisation de la colonne koi disposition

    val indexed = new StringIndexer()
    .setInputCol("koi_disposition")
    .setOutputCol("label")
    .fit(assembled)
    .transform(assembled)

    println("indexed schema")
    indexed.printSchema()


    val Array(training, test) =  indexed.randomSplit(Array(0.9,0.1))

    val lr = new LogisticRegression()
      .setElasticNetParam(1.0)
      .setFeaturesCol("assembled_features")
      .setPredictionCol("prediction")
      .setStandardization(true)
      .setFitIntercept(true)
      .setTol(1.0e-6)
      .setMaxIter(300)
      .setLabelCol("label")

    val paramGrid = new ParamGridBuilder()
      .addGrid(lr.regParam, Array(0.1, 0.01))
      .addGrid(lr.fitIntercept)
      .addGrid(lr.elasticNetParam, Array(0.0, 0.5, 1.0))
      .build()

  val trainValidationSplit = new TrainValidationSplit()
      .setEstimator(lr)
      .setEvaluator(new BinaryClassificationEvaluator())
      .setEstimatorParamMaps(paramGrid)

  val model = trainValidationSplit.fit(training)

  val predictions = model.transform(test)
  model.write.overwrite().save("../../INF729/model")

  val evaluator = new BinaryClassificationEvaluator()
  .setLabelCol("label")
  .setRawPredictionCol("prediction")

  val accuracy = evaluator.evaluate(predictions)
  println("Test Error = " + (1.0 - accuracy)) //test error below 5%
  }
}
