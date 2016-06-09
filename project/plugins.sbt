// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.7" exclude("com.typesafe.akka", "akka-slf4j"))

// Compile less
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0" exclude("com.typesafe.akka", "akka-slf4j"))

