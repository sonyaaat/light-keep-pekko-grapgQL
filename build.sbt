name := "light-keep"
//version      := "1.0"
lazy val scalaVersionString = "2.13.11"

lazy val pekkoVersion = "1.0.1"
lazy val PekkoHttpVersion = "1.0.0"
lazy val ScalikeJdbcVersion = "3.5.0"
lazy val circeVersion = "0.14.1"
lazy val tapirVersion = "1.8.2"
lazy val zioVersion = "2.0.18"
lazy val zioHttpVersion = "3.0.0-RC2"

// Run in a separate JVM, to make sure sbt waits until all threads have
// finished before returning.
// If you want to keep the application running while executing other
// sbt tasks, consider https://github.com/spray/sbt-revolver/
fork := true

lazy val root =
  (project in file(".")).aggregate(backend, frontend)

lazy val backend =
  (project in file("backend"))
    .settings(
      name         := "light-keep-be",
      version      := "1.0",
      scalaVersion := "2.13.11",
      libraryDependencies ++= Seq(
        "org.apache.pekko"             %% "pekko-actor-typed"             % pekkoVersion,
        "ch.qos.logback"                % "logback-classic"               % "1.2.12",
        "org.apache.pekko"             %% "pekko-actor-testkit-typed"     % pekkoVersion                 % Test,
        "org.scalatest"                %% "scalatest"                     % "3.2.15"                     % Test,
        "org.apache.pekko"             %% "pekko-persistence-testkit"     % pekkoVersion                 % Test,
        "org.apache.pekko"             %% "pekko-projection-testkit"      % "0.0.0+79-2f39f1e4-SNAPSHOT" % Test,
//        "org.apache.pekko"             %% "pekko-http"                    % PekkoHttpVersion,
        "org.apache.pekko"             %% "pekko-persistence-typed"       % pekkoVersion,
        "org.apache.pekko"             %% "pekko-persistence-cassandra"   % "0.0.0-1110-6b7494d3-SNAPSHOT",
        "org.apache.pekko"             %% "pekko-projection-core"         % "0.0.0+67-d4a2bbfe-SNAPSHOT",
        "org.apache.pekko"             %% "pekko-projection-jdbc"         % "0.0.0+67-d4a2bbfe-SNAPSHOT",
        "org.apache.pekko"             %% "pekko-persistence"             % pekkoVersion,
        "org.apache.pekko"             %% "pekko-persistence-query"       % pekkoVersion,
        "org.apache.pekko"             %% "pekko-cluster-tools"           % pekkoVersion,
        "org.apache.pekko"             %% "pekko-persistence-query"       % pekkoVersion,
        "org.apache.pekko"             %% "pekko-cluster-sharding-typed"  % pekkoVersion,
        "org.apache.pekko"             %% "pekko-http-cors"               % PekkoHttpVersion,
        "com.h2database"                % "h2"                            % "1.4.200",
        "ch.qos.logback"                % "logback-classic"               % "1.2.3",
//        "org.apache.pekko"             %% "pekko-cluster-sharding-typed"  % pekkoVersion,
//        "org.apache.pekko"             %% "pekko-persistence-query"       % pekkoVersion,
        "org.apache.pekko"             %% "pekko-discovery"               % pekkoVersion,
        "org.apache.pekko"             %% "pekko-cluster-sharding"        % pekkoVersion,
        "io.spray"                     %% "spray-json"                    % "1.3.6",
        "org.apache.pekko"             %% "pekko-http-spray-json"         % PekkoHttpVersion,
        "org.apache.pekko"             %% "pekko-http-testkit"            % PekkoHttpVersion,
        "org.apache.pekko"             %% "pekko-serialization-jackson"   % pekkoVersion,
        "com.fasterxml.jackson.module" %% "jackson-module-scala"          % "2.14.2",
        "org.apache.pekko"             %% "pekko-projection-core"         % "0.0.0+75-1d3f6fab-SNAPSHOT",
        "org.scalikejdbc"              %% "scalikejdbc"                   % ScalikeJdbcVersion,
        "org.scalikejdbc"              %% "scalikejdbc-config"            % ScalikeJdbcVersion,
        "org.apache.pekko"             %% "pekko-projection-eventsourced" % "0.0.0+75-1d3f6fab-SNAPSHOT",
        "org.apache.pekko"             %% "pekko-projection-cassandra"    % "0.0.0+75-1d3f6fab-SNAPSHOT",
        // Postgres
        "org.postgresql"                % "postgresql"                    % "42.5.4",
        // JSON
        "io.circe"                     %% "circe-core"                    % circeVersion,
        "io.circe"                     %% "circe-generic"                 % circeVersion,
        "io.circe"                     %% "circe-parser"                  % circeVersion,
        "io.circe"                     %% "circe-generic-extras"          % circeVersion,
        "org.mdedetrich"               %% "pekko-stream-circe"            % "1.0.0",
        "org.mdedetrich"               %% "pekko-http-circe"              % "1.0.0",
        // ZIO
        "dev.zio"                      %% "zio"                           % zioVersion,
        "dev.zio"                      %% "zio-streams"                   % zioVersion,
        "dev.zio"                      %% "zio-test"                      % zioVersion                   % Test,
        "dev.zio"                      %% "zio-test-sbt"                  % zioVersion                   % Test,
        "dev.zio"                      %% "zio-http"                      % zioHttpVersion,
        // graphQL
        "com.github.ghostdogpr"        %% "caliban"                       % "2.4.1",
        "com.softwaremill.sttp.tapir"  %% "tapir-json-circe"              % "1.2.11", // Circe
//        "com.softwaremill.sttp.tapir"   %% "tapir-akka-http-server"     % tapirVersion,
        "com.github.ghostdogpr"        %% "caliban-zio-http"              % "2.4.1" // routes for zio-http
      ),
      resolvers += "Apache Snapshots" at "https://repository.apache.org/content/groups/snapshots",
      resolvers += "Pekko library repository" at "https://repo.pekko.io/maven"
    )

lazy val frontend =
  (project in file("frontend"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      scalaJSUseMainModuleInitializer := true,
      Compile / mainClass             := Some("com.example.Main"),
      name                            := "light-keep-fe",
      version                         := "0.1",
      scalaVersion                    := "2.13.11",
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom"          % "2.0.0",
        "com.raquo" %%% "laminar"                 % "16.0.0",
        "com.raquo" %%% "airstream"               % "15.0.0",
        "io.circe" %%% "circe-core"               % circeVersion,
        "io.circe" %%% "circe-generic"            % circeVersion,
        "io.circe" %%% "circe-parser"             % circeVersion,
        "io.circe" %%% "circe-generic-extras"     % circeVersion,
        "io.github.cquiroz" %%% "scala-java-time" % "2.5.0"
//        "org.openmole.scaladget" %%% "bootstrapnative" % "1.9.4",
//        "org.openmole.scaladget" %%% "ace"             % "1.9.4",
//        "org.openmole.scaladget" %%% "bootstrapslider" % "1.3.7"
//        "org.openmole.scaladget" %%% "lunr"            % "1.9.4"
      )
    )
