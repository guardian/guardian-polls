name := "Guardian Polls"

version := "1.0"

organization := "com.example"

scalaVersion := "2.9.1"

resolvers += "Objectify Repo" at "http://objectify-appengine.googlecode.com/svn/maven"

libraryDependencies ++= Seq(
  "com.googlecode.objectify" % "objectify" % "4.0a3",
  "org.scalatra" %% "scalatra" % "2.0.4",
  "javax.persistence" % "persistence-api" % "1.0",
  "org.skife.com.typesafe.config" % "typesafe-config" % "0.3.0",
  "com.google.appengine" % "appengine-api-1.0-sdk" % "1.6.5",
  "ch.qos.logback" % "logback-classic" % "0.9.26",
  "com.weiglewilczek.slf4s" %% "slf4s" % "1.0.7",
  "cc.spray" %%  "spray-json" % "1.1.1",
  "commons-codec" %  "commons-codec" % "1.6",
  "org.scalatest" %% "scalatest" % "1.6.1" % "test",
  "javax.servlet" % "servlet-api" % "2.3" % "provided",
  "org.mortbay.jetty" % "jetty" % "6.1.22" % "container")

seq(appengineSettings: _*)

seq(Twirl.settings: _*)

scalariformSettings

Twirl.twirlImports := Seq("com.gu.polls.model._")