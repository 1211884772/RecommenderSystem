package com.mumu.recommender

import com.mongodb.casbah.Imports.MongoClientURI
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}


/**
 * Product数据集
 * 3982                            商品ID
 * Fuhlen 富勒 M8眩光舞者时尚节能    商品名称
 * 1057,439,736                    商品分类ID，不需要
 * B009EJN4T2                      亚马逊ID，不需要
 * https://images-cn-4.ssl-image   商品的图片URL
 * 外设产品|鼠标|电脑/办公           商品分类
 * 富勒|鼠标|电子产品|好用|外观漂亮   商品UGC标签
 */
// case class和case object的区别
//类中有参和无参，当类有参数的时候，用case class，当类没有参数的时候用case object


//case class是一种可以用来快速保存数据的类，可以认为是java中的pojo类，用于对象数据的保存。
case class Product(productId: Int, name: String, imageUrl: String, categories: String, tags: String)

/**
 * Rating数据集
 * 4867        用户ID
 * 457976      商品ID
 * 5.0         评分
 * 1395676800  时间戳
 */
case class Rating(userId: Int, productId: Int, score: Double, timestamp: Int)

/**
 * MongoDb链接配置
 *
 * @param uri MongoDB的连接uri
 * @param db  要操作的db
 */
case class MongoConfig(uri: String, db: String)


object DataLoader {
  val PRODUCt_DAtA_PAtH = "D:\\bigdata\\bigdata\\code\\GmallRecommendSystem\\recommender\\DataLoader\\src\\main\\resources\\products.csv"
  val RATING_DATA_PATH = "D:\\bigdata\\bigdata\\code\\GmallRecommendSystem\\recommender\\DataLoader\\src\\main\\resources\\ratings.csv"
  //定义mongoDB存储的表名
  val MONGODB_PRODUCT_COLLECTION = "Product"
  val MONGODB_RATING_COLLECTION = "Rating"

  //Unit返回值为空
  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.core" -> "local[*]",
      "mongo.uri" -> "mongodb://192.168.1.110:27017/recommender",
      "mongo.db" -> "recommender"
    )

    //创建一个spark config
    val sparkConf = new SparkConf().setMaster(config("spark.core")).setAppName("DataLoader")
    //创建spark session
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    //一个隐式implicit的转换方法，转换出正确的类型，完成编译
    import spark.implicits._

    //加载数据
    val productRDD = spark.sparkContext.textFile(PRODUCt_DAtA_PAtH)
    val productDF = productRDD.map(item => {
      //produc数据通过^分割，切分出来
      val attr = item.split("\\^")
      //转换成Product
      Product(attr(0).toInt, attr(1).trim, attr(4).trim, attr(5).trim, attr(6).trim)
    }).toDF()

    val ratingRDD = spark.sparkContext.textFile(RATING_DATA_PATH)
    val ratingDF = ratingRDD.map(item => {
      //rating数据通过，分割，切分出来
      val attr = item.split(",")
      //转换成Rating
      Rating(attr(0).toInt, attr(1).toInt, attr(2).toDouble, attr(3).toInt)
    }).toDF()


    //
    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    //存到mongoDB
    storeDataInMongoDB(productDF, ratingDF)

    //spark.stop()

  }

  def storeDataInMongoDB(productDF: DataFrame, ratingDF: DataFrame)(implicit mongoConfig: MongoConfig): Unit = {
    //新疆一个到mongoDB的链接,客户端
    val mongoClient = MongoClient(MongoClientURI(mongoConfig.uri))
    // 定义要操作的mongodb表，可以理解为 db.Product
    val productCollection = mongoClient(mongoConfig.db)(MONGODB_PRODUCT_COLLECTION)
    val ratingCollection = mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION)
    // 如果表已经存在，则删掉
    productCollection.dropCollection()
    ratingCollection.dropCollection()

    //将当前数据存入对应的表中
    productDF.write
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_PRODUCT_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    ratingDF.write
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    // 对表创建索引
    productCollection.createIndex(MongoDBObject("productId" -> 1))
    ratingCollection.createIndex(MongoDBObject("productId" -> 1))
    ratingCollection.createIndex(MongoDBObject("userId" -> 1))

    mongoClient.close()

  }

}
