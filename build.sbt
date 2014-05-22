import SonatypeKeys._

name := "robo-mine"

version := "1.1-SNAPSHOT"

organization := "net.flatmap"

scalaVersion := "2.10.4"

libraryDependencies += "org.scala-lang" % "scala-swing" % "2.10.4"

libraryDependencies += "org.jbox2d" % "jbox2d-library" % "2.2.1.1"

libraryDependencies += "org.jbox2d" % "jbox2d-testbed" % "2.2.1.1"

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

startYear := Some(2014)

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>http://www.informatik.uni-bremen.de/~cxl/lehre/rp.ss14/</url>
  <licenses>
    <license>
      <name>BSD-style</name>
      <url>http://www.opensource.org/licenses/bsd-license.php</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:martinring/robo-mine.git</url>
    <connection>scm:git:git@github.com:martinring/robo-mine.git</connection>
  </scm>
  <developers>
    <developer>
      <id>martinring</id>
      <name>Martin Ring</name>
      <url>http://www.github.com/martinring</url>
    </developer>
  </developers>)

sonatypeSettings
