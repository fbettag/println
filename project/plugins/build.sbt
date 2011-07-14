resolvers ++= Seq(
	"Web plugin repo" at "http://siasia.github.com/maven2",
	"Typesafe" at "http://repo.typesafe.com/typesafe/ivy-releases/")

libraryDependencies ++= Seq(
	"com.github.siasia" %% "xsbt-web-plugin" % "0.10.0")

