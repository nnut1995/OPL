addSbtPlugin("com.mojolly.scalate" % "xsbt-scalate-generator" % "0.5.0")

addSbtPlugin("org.scalatra.sbt" % "scalatra-sbt" % "0.5.1")

classpathTypes += "maven-plugin"

libraryDependencies += "org.bytedeco" % "javacpp" % "1.1"

// libraryDependencies ++= Seq(
//   "org.bytedeco" % "javacpp" % "1.1",
//   "org.scalatra" %% "scalatra" % "2.4.0.M2",
//   "com.mchange" % "c3p0" % "0.9.5.1"
// )
