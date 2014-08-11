version       := "0.1"

scalaVersion  := "2.10.4"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

resolvers ++= Seq(
  "Sonatype"   at "https://oss.sonatype.org/content/repositories/public",
  "releases"   at "https://oss.sonatype.org/content/repositories/releases/",
  "spray repo" at "http://repo.spray.io/"
)

libraryDependencies ++= {
  val akkaV = "2.3.4"
  Seq(
    "ch.qos.logback"            %   "logback-classic"       % "1.0.0",
    "com.typesafe.akka"         %%  "akka-actor"            % akkaV,
    "com.typesafe.akka"         %%  "akka-testkit"          % akkaV,
    "com.typesafe.akka"         %% "akka-persistence-experimental" % akkaV,
    "org.streum"                %%  "configrity-core"       % "1.0.0"
  )
}

seq(Revolver.settings: _*)
