import scala.util.Properties


name := """spca-spark"""

version := "0.1"

scalaVersion := "2.10.5"

val DEFAULT_SPARK_VERSION = "1.6.3"
val DEFAULT_HADOOP_VERSION = "2.6.1"

lazy val sparkVersion = Properties.envOrElse("SPARK_VERSION", DEFAULT_SPARK_VERSION)
lazy val hadoopVersion = Properties.envOrElse("SPARK_HADOOP_VERSION", DEFAULT_HADOOP_VERSION)

fork := true

javaOptions in run ++= Seq(
  "-Dlog4j.debug=true",
  "-Dlog4j.configuration=log4j.properties")

outputStrategy := Some(StdoutOutput)

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "3.0.0-M15" % "test",
  // "org.apache.parquet" % "parquet-hadoop" % "1.8.1",
  //"org.apache.hive" % "hive-common" % "1.2.1" excludeAll ExclusionRule(organization = "com.sun.jersey"),
  "org.apache.spark" % "spark-core_2.10" % sparkVersion /*% "provided"*/
    excludeAll ExclusionRule(organization = "javax.servlet"),
  "org.apache.spark" %% "spark-sql" % sparkVersion  /*% "provided"*/  excludeAll ExclusionRule(organization = "javax.servlet")
    excludeAll ExclusionRule(organization = "org.apache.parquet.schema"),
  "org.apache.spark" %% "spark-hive" % sparkVersion % "provided"  excludeAll ExclusionRule(organization = "javax.servlet")
    excludeAll ExclusionRule(organization = "org.apache.parquet.schema"),
  "org.apache.spark" %% "spark-mllib" % sparkVersion  /*% "provided"*/  excludeAll ExclusionRule(organization = "javax.servlet"),
  "org.apache.hadoop" % "hadoop-client" % hadoopVersion /*% "provided"*/
    excludeAll ExclusionRule(organization = "javax.servlet"),
  "org.bdgenomics.adam" % "adam-core_2.10" % "0.20.0"
    exclude("org.apache.spark", "spark-core_2.10")
    exclude("org.apache.hadoop", "hadoop-client")
    excludeAll ExclusionRule(organization = "org.apache.parquet")
    // exclude("org.bdgenomics.utils", "utils-metrics_2.10")
    excludeAll ExclusionRule(organization = "javax.servlet"),
  "org.bdgenomics.utils" % "utils-misc_2.10" % "0.2.7"
    exclude("org.apache.spark", "spark-core_2.10")
    exclude("org.apache.hadoop", "hadoop-client")
    excludeAll ExclusionRule(organization = "javax.servlet"),
  "org.seqdoop" % "hadoop-bam" % "7.5.0"
    exclude("org.apache.hadoop", "hadoop-client")
    excludeAll ExclusionRule(organization = "javax.servlet"),
  "de.lmu.ifi.dbs.elki" % "elki" % "0.7.1",
  "org.apache.mahout" % "mahout-core" % "0.9"
)

resolvers ++= Seq(
  "Job Server Bintray" at "https://dl.bintray.com/spark-jobserver/maven"
)

parallelExecution in Test := false

//assemblyJarName in assembly := "santo.jar"
//mainClass in assembly := Some("pl.edu.pw.elka.cnv.Main")
assemblyMergeStrategy in assembly := {
  case PathList("org", "apache", "commons", xs@_*) => MergeStrategy.first
  /*case PathList("scala", xs@_*) => MergeStrategy.first
    a nasty workaround!!!
  case PathList("org", xs@_*) => MergeStrategy.first
  case PathList("javax", xs@_*) => MergeStrategy.first
  end*/
  case PathList("fi", "tkk", "ics", xs@_*) => MergeStrategy.first
  case PathList("com", "esotericsoftware", xs@_*) => MergeStrategy.first
  case PathList("org", "objectweb", xs@_*) => MergeStrategy.last
  case PathList("javax", "xml", xs@_*) => MergeStrategy.first
  case PathList("javax", "servlet", xs@_*) => MergeStrategy.first
  case PathList("javax", "annotation", xs@_*) => MergeStrategy.first
  case PathList("javax", "activation", xs@_*) => MergeStrategy.first
  case PathList("javax", "transaction", xs@_*) => MergeStrategy.first
  case PathList("javax", "mail", xs@_*) => MergeStrategy.first
  case PathList("com", "twitter", xs@_*) => MergeStrategy.first
  case PathList("org", "slf4j", xs@_*) => MergeStrategy.first
  //META-INF/maven/com.google.guava/guava/pom.xml
  // Added
  case PathList("htsjdk", xs@_*) => MergeStrategy.first
  case PathList("org", "apache", "bcel", xs@_*) => MergeStrategy.first
  case PathList("org", "apache", "regexp", xs@_*) => MergeStrategy.first
  case PathList("io", "netty", xs@_*) => MergeStrategy.first
  case PathList("com", "codahale", "metrics", xs@_*) => MergeStrategy.first
  case PathList("com", "google", "common", xs@_*) => MergeStrategy.first
  case PathList("org", "apache", "spark", "unused", xs@_*) => MergeStrategy.first
  case PathList("edu", "umd", "cs", "findbugs", xs@_*) => MergeStrategy.first
  case PathList("net", "jcip", "annotations", xs@_*) => MergeStrategy.first
  case PathList("org", "apache", "jasper", xs@_*) => MergeStrategy.first
  case PathList("org", "xmlpull", xs@_*) => MergeStrategy.first
  case "parquet.thrift" => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".html" => MergeStrategy.first
  case "application.conf" => MergeStrategy.concat
  case PathList("org", "apache", "hadoop", xs@_*) => MergeStrategy.first
  //case "META-INF/ECLIPSEF.RSA"     => MergeStrategy.discard
  case "META-INF/mimetypes.default" => MergeStrategy.first
  case ("META-INF/ECLIPSEF.RSA") => MergeStrategy.first
  case ("META-INF/mailcap") => MergeStrategy.first
  case ("plugin.properties") => MergeStrategy.first
  case ("META-INF/maven/org.slf4j/slf4j-api/pom.xml") => MergeStrategy.first
  case ("META-INF/maven/com.google.guava/guava/pom.xml") => MergeStrategy.first
  case ("META-INF/maven/org.slf4j/slf4j-api/pom.properties") => MergeStrategy.first
  // case ("META-INF/io.netty.versions.properties") => MergeStrategy.first
  case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.first
  case x if x.endsWith("pom.properties") => MergeStrategy.first
  case x if x.endsWith("pom.xml") => MergeStrategy.first
  case x if x.endsWith("plugin.xml") => MergeStrategy.first
  //case ("META-INF/maven/com.google.guava/guava/pom.xml") => MergeStrategy.first
  case ("log4j.properties") => MergeStrategy.first
  case ("git.properties") => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

lazy val copyDocAssetsTask = taskKey[Unit]("Copy doc assets")

copyDocAssetsTask := {
  val sourceDir = file("resources/doc-resources")
  val targetDir = (target in(Compile, doc)).value
  IO.copyDirectory(sourceDir, targetDir)
}

copyDocAssetsTask <<= copyDocAssetsTask triggeredBy (doc in Compile)

net.virtualvoid.sbt.graph.Plugin.graphSettings
