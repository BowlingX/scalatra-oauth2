import sbt._
import Keys._

// Shell prompt which show the current project, git branch and build version
// git magic from Daniel Sobral, adapted by Ivan Porto Carrero to also work with git flow branches
object ShellPrompt {
 
  object devnull extends ProcessLogger {
    def info (s: => String) {}
    def error (s: => String) { }
    def buffer[T] (f: => T): T = f
  }
  
  val current = """\*\s+([^\s]+)""".r
  
  def gitBranches = ("git branch --no-color" lines_! devnull mkString)
  
  val buildShellPrompt = { 
    (state: State) => {
      val currBranch = current findFirstMatchIn gitBranches map (_ group(1)) getOrElse "-"
      val currProject = Project.extract (state).currentProject.id
      "%s:%s:%s> ".format (currBranch, currProject, RlSettings.buildVersion)
    }
  }
 
}

object ScalatraOAuth2Settings {
  val buildOrganization = "org.scalatra.oauth2"
  val buildScalaVersion = "2.9.0-1"
  val buildVersion      = "0.1-SNAPSHOT"

  lazy val formatSettings = ScalariformPlugin.settings ++ Seq(
    formatPreferences in Compile := formattingPreferences,
    formatPreferences in Test    := formattingPreferences
  )

  def formattingPreferences = {
    import scalariform.formatter.preferences._
    (FormattingPreferences()
        setPreference(IndentSpaces, 2)
        setPreference(AlignParameters, true)
        setPreference(AlignSingleLineCaseStatements, true)
        setPreference(DoubleIndentClassDeclaration, true)
        setPreference(RewriteArrowSymbols, true)
        setPreference(PreserveSpaceBeforeArguments, true))
  }

  val description = SettingKey[String]("description")

  val compilerPlugins = Seq(
    compilerPlugin("org.scala-lang.plugins" % "continuations" % "2.9.0-1"),
    compilerPlugin("org.scala-tools.sxr" % "sxr_2.9.0" % "0.2.7")
  )

  val buildSettings = Defaults.defaultSettings ++ formatSettings ++ Seq(
      name := "scalatra-oauth2",
      version := buildVersion,
      organization := buildOrganization,
      scalaVersion := buildScalaVersion,
      javacOptions ++= Seq("-Xlint:unchecked"),
      testOptions in Test += Tests.Setup( () => System.setProperty("akka.mode", "test") ),
      scalacOptions ++= Seq(
        "-optimize",
        "-deprecation",
        "-unchecked",
        "-Xcheckinit",
        "-encoding", "utf8",
        "-P:continuations:enable"),
      //retrieveManaged := true,
      libraryDependencies ++= compilerPlugins,
      autoCompilerPlugins := true,
      parallelExecution in Test := false,
      publishTo <<= (version) { version: String => 
        val nexus = "http://nexus.scala-tools.org/content/repositories/"
        if (version.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus+"snapshots/") 
        else                                   Some("releases" at nexus+"releases/")
      },
      shellPrompt  := ShellPrompt.buildShellPrompt)

  val packageSettings = Seq (
    packageOptions <<= (packageOptions, name, version, organization) map {
      (opts, title, version, vendor) =>
         opts :+ Package.ManifestAttributes(
          "Created-By" -> System.getProperty("user.name"),
          "Built-By" -> "Simple Build Tool",
          "Build-Jdk" -> System.getProperty("java.version"),
          "Specification-Title" -> title,
          "Specification-Vendor" -> "Mojolly Ltd.",
          "Specification-Version" -> version,
          "Implementation-Title" -> title,
          "Implementation-Version" -> version,
          "Implementation-Vendor-Id" -> vendor,
          "Implementation-Vendor" -> "Mojolly Ltd.",
          "Implementation-Url" -> "https://backchat.io"
         )
    },
    pomExtra <<= (pomExtra, name, description) { (extra, title, desc) => extra ++ Seq(
      <name>{title}</name>, <description>{desc}</description>)
    })
 
  val projectSettings = buildSettings ++ packageSettings
}

object ScalatraOAutDependencies {

  val akkaVersion = "1.2-RC3"
  val liftVersion = "2.4-M3"  
  val scalatraVersion = "2.0.0-SNAPSHOT"
  val grizzlyVersion = "2.1.1"
  val jettyVersion = "7.5.0.RC0"
  val metricsVersion = "2.0.0-BETA14"
  val specs2Version = "1.5"

  def akka(name: String) = "se.scalablesolutions.akka" % "akka-%s".format(name)               % akkaVersion
  def scalatra(name: String) = "org.scalatra"          %% "scalatra-%s".format(name)          % scalatraVersion
  def lift(name: String) = "net.liftweb"               %% "lift-%s".format(name)              % liftVersion
  def jetty(name: String) = "org.eclipse.jetty"        % "jetty-%s".format(name)              % jettyVersion


  val specs2             = "org.specs2"                %% "specs2"                % specs2Version    % "test"

  val scalatraCore       = "org.scalatra"              %% "scalatra"              % scalatraVersion

  val scalatraAuth       = scalatra("auth")

  val scalatraScalate    = scalatra("scalate")

  val scalatraFileUpload = scalatra("fileupload")

  val scalatraLiftJson   = scalatra("lift-json")

  val scalatraSpecs      = scalatra("specs2") % "test"

  val jettyWebApp        = jetty("webapp")

  val jettyServlet       = jetty("servlet")

  val jettyWebSocket     = jetty("websocket")

  val jettyServer        = jetty("server")

  val slf4j              = "org.slf4j"                 % "slf4j-api"               % slf4jVersion

  val base64             = "net.iharder"               % "base64"                  % "2.3.8"

  val jBcrypt            = "org.mindrot"               % "jbcrypt"                 % "0.3m"

  val bouncyCastle       = "org.bouncycastle"          % "bcprov-jdk16"            % "1.46"
  
  val liftJson           = lift("json")

  val liftJsonExt        = lift("json-ext")

  val liftMongoRecord    = lift("mongodb-record")

  val scalaInflector     = "com.mojolly.inflector"     %% "scala-inflector"         % "1.1"

  val logbackAkka        = "com.mojolly.logback"       %% "logback-akka"            % "0.6-SNAPSHOT"

  val metricsCore        = "com.yammer.metrics"        % "metrics-core"             % metricsVersion

  val metricsScala       = "com.yammer.metrics"   	   %% "metrics-scala"           % metricsVersion

  val akkaActor         = akka("actor")

  val scalaTime          = "org.scala-tools.time"      %% "time"                   % "0.4"
  
}

object ScalatraOAuth2Build extends Build {

  import ScalatraOAuth2Settings._
  

  lazy val root = Project ("scalatra-oauth2", file("."), settings = projectSettings ++ Seq(
    libraryDependencies ++= Seq(
      akkaActor, 
      metricsCore, metricsScala,
      logbackAkka, scalaInflector, 
      scalatraAuth, scalatraScalate, scalatraSpecs, scalatraLiftJson, scalatraFileUpload,
      base64, bouncyCastle,
      liftJsonExt, liftMongoRecord,
      scalaTime
    ),
    description := "An OAuth2 server and client library")) 
  
}


// vim: set ts=2 sw=2 et:
