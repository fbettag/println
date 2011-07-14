# println

[println](http://println.io) is a blogging / publishing software written in [Scala](http://www.scala-lang.org) with the [Lift Web Framework](http://www.liftweb.net). It tries to fulfill the need for an un-obustrive approach of writing, publishing and managing ones blog posts with it's overlay-editor-window.


The official website is currently being done with println itself, please be patient as it well well-done. :)


## How to get started

In order to get, compile and run the project locally, you need:

* git of course
* [simple-build-tool](https://github.com/harrah/xsbt/wiki)
* [PostgreSQL](http://www.postgresql.org) for Content-Storage ([check alternatives](http://www.assembla.com/spaces/liftweb/wiki/Persistence_Alternatives))
* [mongoDB](http://www.mongodb.org) for Statistics (optional)


After installing PostgreSQL, run the following in your shell:

<code>
createuser -Upostgres printlndemo
createdb -Upostgres --owner println printlndemo
</code>


If everything is up and running

<code>
git clone git://github.com/fbettag/println.git
cd println
./sbt update
./sbt ~jetty-run
</code>

Visit http://127.0.0.1:8080 with your browser.


## Todo

* XmlResponse for "post" needs a Sitemap. ("No navigation defined"-error)
	http://groups.google.com/group/liftweb/browse_thread/thread/18d4334601bacf57

* jQuery Dialog for adding new posts with auto-slugging ([a-zA-Z0-9/,-])
	http://groups.google.com/group/liftweb/browse_thread/thread/d42aaa377f62f12f

* Tagging -> return way to server

* Twitter, Facebook and Google+ Integration

* grep for FIXME or FIXIT ;)