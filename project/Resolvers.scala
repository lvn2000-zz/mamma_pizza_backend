import sbt._

object Resolvers {

  val myResolvers = Seq(
    "MVNRepositoryHttps" at "https://mvnrepository.com/artifact",
    "Central" at "http://repo1.maven.org/maven2/",
    "Akka repository" at "https://dl.bintray.com/akka/maven/",
    "JBoss" at "https://repository.jboss.org/",
    "Spray Repo" at "http://repo.spray.io",
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/maven-releases/",
    "Atlassian Releases" at "https://maven.atlassian.com/public/",
    "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
    )
}
