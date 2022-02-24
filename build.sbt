lazy val root = project
  .in(file("."))
  .settings(
    name := "featureserver",
    version := "0.1.0",

    scalaVersion := "2.13.7",

    scalacOptions += "-deprecation",

    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.18",
    libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.2.8",
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.18",
    libraryDependencies += "org.postgresql" % "postgresql" % "42.3.3",
    libraryDependencies += "com.typesafe" % "config" % "1.4.2",

    ThisBuild / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case PathList("reference.conf") => MergeStrategy.concat
      case x => MergeStrategy.first
    },

    assembly / assemblyJarName := "featureserver.jar"
  )
