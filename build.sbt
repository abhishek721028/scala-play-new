lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "scala-play-library",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.13.15",
    javacOptions ++= Seq("--release", "11"),
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8", "-release", "11"),
    libraryDependencies ++= Seq(
      guice,
      "org.mongodb.scala" %% "mongo-scala-driver" % "5.2.1"
    )
  )
