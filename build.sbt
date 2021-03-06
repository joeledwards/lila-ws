name := "lila-ws"

version := "2.0"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)

val akkaVersion          = "2.6.6"
val kamonVersion         = "2.1.1"
val nettyVersion         = "4.1.50.Final"
val reactivemongoVersion = "0.20.11"

scalaVersion := "2.13.2"

libraryDependencies += "org.reactivemongo"          %% "reactivemongo"                % reactivemongoVersion
libraryDependencies += "org.reactivemongo"          %% "reactivemongo-bson-api"       % reactivemongoVersion
libraryDependencies += "org.reactivemongo"           % "reactivemongo-shaded-native"  % s"$reactivemongoVersion-linux-x86-64"
libraryDependencies += "io.lettuce"                  % "lettuce-core"                 % "5.3.1.RELEASE"
libraryDependencies += "io.netty"                    % "netty-handler"                % nettyVersion
libraryDependencies += "io.netty"                    % "netty-codec-http"             % nettyVersion
libraryDependencies += "io.netty"                    % "netty-transport-native-epoll" % nettyVersion classifier "linux-x86_64"
libraryDependencies += "org.lichess"                %% "scalachess"                   % "9.3.1"
libraryDependencies += "com.typesafe.akka"          %% "akka-actor-typed"             % akkaVersion
libraryDependencies += "com.typesafe.akka"          %% "akka-slf4j"                   % akkaVersion
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging"                % "3.9.2"
libraryDependencies += "joda-time"                   % "joda-time"                    % "2.10.6"
libraryDependencies += "com.github.blemale"         %% "scaffeine"                    % "4.0.1" % "compile"
libraryDependencies += "ch.qos.logback"              % "logback-classic"              % "1.2.3"
libraryDependencies += "com.typesafe.play"          %% "play-json"                    % "2.9.0"
libraryDependencies += "io.kamon"                   %% "kamon-core"                   % kamonVersion
libraryDependencies += "io.kamon"                   %% "kamon-influxdb"               % kamonVersion
libraryDependencies += "io.kamon"                   %% "kamon-system-metrics"         % kamonVersion
libraryDependencies += "com.softwaremill.macwire"   %% "macros"                       % "2.3.7" % "provided"

resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += "lila-maven" at "https://raw.githubusercontent.com/ornicar/lila-maven/master"

scalacOptions ++= Seq(
  "-language:implicitConversions",
  "-feature",
  "-deprecation",
  "-unchecked",
  "-Wdead-code",
  "-Xlint:unused,inaccessible,nullary-unit,adapted-args,infer-any,missing-interpolator,eta-zero",
  "-Xfatal-warnings"
)

sources in (Compile, doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

javaOptions in reStart += "-Xmx128m"

/* scalafmtOnCompile := true */
