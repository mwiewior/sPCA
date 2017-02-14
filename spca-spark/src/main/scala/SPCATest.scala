package main.scala
import org.qcri.sparkpca._
import org.apache.spark.mllib.linalg.Matrix
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.mllib.linalg.DenseVector
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.api.java.JavaSparkContext
/**
  * Created by marek on 14.02.17.
  */
object SPCATest {
  def main(args: Array[String]): Unit = {
    val sconf = new SparkConf()
    sconf.setMaster("local[2]")
    sconf.setAppName("sPCA")
    val sc = new SparkContext(sconf)
    val jsc = new JavaSparkContext(sc)
    val sampleCnt = 20
    val featureCnt = 15
    val gen = sc.parallelize( (1 to sampleCnt).map{r=>val rnd = new scala.util.Random();  Vectors.sparse (featureCnt,
      (0 to featureCnt-1).map(k=>(k,rnd.nextInt(2).toDouble)).toArray ) } )
    val rowMat = new RowMatrix(gen)
    SparkPCA.computePrincipalComponents(
      jsc,
      rowMat,
      "output.txt",
      sampleCnt,
      featureCnt,
      10,
      0.01,
      100,
      1
    )
  }

}
