package com.sparkProject

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.SQLContext
import org.apache.log4j.{Level, Logger}
object Job {

  def main(args: Array[String]): Unit = {

    // SparkSession configuration
    val spark = SparkSession
      .builder
      .master("local")
      .appName("spark session TP_parisTech")
      .getOrCreate()

    val sc = spark.sparkContext
    val rootLogger = Logger.getRootLogger()
    rootLogger.setLevel(Level.ERROR)
    import spark.implicits._


    // ----------------- word count ------------------------

//    val df_wordCount = sc.textFile("/home/raphael/Bureau/Spark/sp/README.md")
//      .flatMap{case (line: String) => line.split(" ")}
//      .map{case (word: String) => (word, 1)}
//      .reduceByKey{case (i: Int, j: Int) => i + j}
//      .toDF("word", "count")
//
//    df_wordCount.orderBy($"count".desc).show()


    /********************************************************************************
      *
      *        TP 2 : début du projet
      *
      ********************************************************************************/
    val sqlContext = new org.apache.spark.sql.SQLContext(sc)


    val df = sqlContext.read
      .format("com.databricks.spark.csv")
      .option("header", "true") // Use first line of all files as header
      .option("inferSchema", "true") // Automatically infer data type
      .option("delimiter",",")
      .option("comment","#")
      .csv("/home/raphael/Bureau/Spark/INF729/cumulative.csv")
    print("Columns : " + df.columns.length)
    print("Lines : " + df.count())
    val cols = df.columns.slice(10, 20)
    val new_df = df.select(cols.head, cols.tail:_*)
    //new_df.show(5)
    df.groupBy("koi_disposition").count().show()
    val workdf = df.drop().where("koi_disposition = 'CANDIDATE'")
    //workdf.show(5)
    workdf.select("koi_eccen_err1").distinct().show() //La colonne est vide*
    workdf.drop("koi_eccen_err1","index","kepid","koi_fpflag_nt","koi_fpflag_ss","koi_fpflag_co","koi_fpflag_ec")
    workdf.drop("koi_sparprov", "koi_trans_mod", "koi_datalink_dvr", "koi_datalink_dvs", "koi_tce_delivname", "koi_parm_prov", "koi_limbdark_mod", "koi_fittype", "koi_disp_prov", "koi_comment", "kepoi_name", "kepler_name", "koi_vet_date", "koi_pdisposition")
    workdf.na.drop()
    workdf.dropDuplicates()
    val x = workdf.columns.slice(10, 20)
    print(workdf.select(x.head, x.tail:_*).describe())
    workdf.na.fill(0)
    workdf.show(5)
    val df_labels = workdf.select("rowid", "koi_disposition")
    val df_features = workdf.drop("koi_disposition")
    df_features.show(1)
    df_labels.join(df_features,usingColumn = "rowid").show(5)
    sqlContext.udf.register("udf_sum",(col1: Double, col2: Double) => col1 + col2)
//    val df_newFeatures = workdf
//      .withColumn("koi_ror_min", udf_sum($"koi_ror", $"koi_ror_err2"))
//      .withColumn("koi_ror_max", $"koi_ror" + $"koi_ror_err1")
//
    workdf.coalesce(1).write.mode("overwrite")
      .option("header", "true").csv("/home/raphael/Bureau/HadoopSpark/INF729/cleanedDataFrame.csv")


  }


}
