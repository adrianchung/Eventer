import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "eventer"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      // Add your project dependencies here,

      // MongoDB Salat plugin installation stuff
      "se.radley" %% "play-plugins-salat" % "1.0.7"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      routesImport += "se.radley.plugin.salat.Binders._",
      templatesImport += "org.bson.types.ObjectId"
    )
}
