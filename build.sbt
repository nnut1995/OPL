import org.scalatra.sbt._
import org.scalatra.sbt.PluginKeys._
import com.mojolly.scalate.ScalatePlugin._
import ScalateKeys._

val ScalatraVersion = "2.5.0"

ScalatraPlugin.scalatraSettings

scalateSettings

organization := "OPL"

name := "NameCard App"

// version := "0.1.0-SNAPSHOT"
version := "0.1.0"

scalaVersion := "2.11.8"

resolvers += Classpaths.typesafeReleases

val javacppVersion = "1.1"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-optimize", "-Xlint")

// Some dependencies like `javacpp` are packaged with maven-plugin packaging
classpathTypes += "maven-plugin"

// Platform classifier for native library dependencies
lazy val platform = org.bytedeco.javacpp.Loader.getPlatform

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % ScalatraVersion,
  "org.scalatra" %% "scalatra-scalate" % ScalatraVersion,
  "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.5" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "9.2.15.v20160210" % "container",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "org.bytedeco" % "javacv" % javacppVersion,
  "org.bytedeco.javacpp-presets" % "tesseract" % ("3.04-"+ javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "tesseract" % ("3.04-"+ javacppVersion) classifier platform,
  "org.bytedeco.javacpp-presets" % "opencv" % ("3.0.0-" + javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "opencv" % ("3.0.0-" + javacppVersion) classifier platform,
  "org.bytedeco.javacpp-presets" % "leptonica" % ("1.72-"+ javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "leptonica" % ("1.72-"+ javacppVersion) classifier platform,
  "com.typesafe.slick" %% "slick" % "3.0.2",
  "com.h2database" % "h2" % "1.4.181",
  "com.mchange" % "c3p0" % "0.9.5.1"
)
scalateTemplateConfig in Compile := {
  val base = (sourceDirectory in Compile).value
  Seq(
    TemplateConfig(
      base / "webapp" / "WEB-INF" / "templates",
      Seq.empty,  /* default imports should be added here */
      Seq(
        Binding("context", "_root_.org.scalatra.scalate.ScalatraRenderContext", importMembers = true, isImplicit = true)
      ),  /* add extra bindings here */
      Some("templates")
    )
  )
}

enablePlugins(JettyPlugin)

fork := true

shellPrompt in ThisBuild := { state => "sbt:" + Project.extract(state).currentRef.project + "> " }
