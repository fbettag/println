# println

[println](http://println.io) is a blogging / publishing software written in [Scala](http://www.scala-lang.org) with the [Lift Web Framework](http://www.liftweb.net). It tries to fulfill the need for an un-obustrive approach of writing, publishing and managing ones blog posts with it's overlay-editor-window.

It is heavily inspired by [Nesta CMS](http://nestacms.com/) a Ruby based CMS Software and also uses parts of its default layout (since i like it for its simplicity).

The official website is currently being done with println itself, please be patient as it will be well-done. There will also be a webcast soon.


## Features

* High Performance (thanks to Lift and Scala)
* Easy to use (thanks to me)
* Easy to write (Live-Preview of your Markdown or Plaintext)
* Easy to customize (by default only layout or css changes are needed)
* Easy to deploy (.war Archive)
* Internal web-tracking with MongoDB (optional)
* Lots of widgets by default (see next section)
* Atom-Feed (/atom.xml)
* Google Sitemap (/sitemap.xml)
* AJAX-/Facebook-Style Tagging with Tag-cloud

## Caveats

Currently it is text-only. I will implement media-management as soon as possible. For now i suggest storing your images elsewhere (Flickr, Picasa) as they offer better upload-possibilities from mobile devices anyway.

Another caveat is, that JavaScript is not rendered in the Live-Preview (but properly on the resulting published page). Meaning that if you enter <script src="..."></script> into the Live-Editor, it won't show in the Live-Preview, but will be perfectly normal on the website.


## Layout and Widgets

The main layout is in src/main/webapp/template-hidden/default.html and has all the widgets that are currently implemented. Here is the overview:

* BitPit: ```<span class="lift:Helpers.bitpit?id=7019"/>```
* Twitter: ```<span class="lift:Helpers.twitter?user=fbettag"/>```
* Google Analytics: ```<span class="lift:Helpers.analytics?ua="/>```
* Tag-Cloud: ```<span class="lift:Tags.cloud"></span>```
* Copyright Helper: ```&copy; <span class="lift:Helpers.years?since=2010"></span>```

If you want to implement your own, feel free to look at src/main/scala/code/snippets/Helpers.scala for how to do so.


## Made with love

It is made with the following pieces of software:

* [Scala](http://www.scala-lang.org) Programming Language
* [Lift Web Framework](http://www.liftweb.net)
* [jQuery UI](http://www.jqueryui.com) Dialog
* modified Version of [WMD MarkDown-Editor](https://github.com/klipstein/wmd) with Live-Content-Preview


## How to get started

In order to get, compile and run the project locally, you need:

* git of course
* [simple-build-tool](https://github.com/harrah/xsbt/wiki)
* [PostgreSQL](http://www.postgresql.org) for Content-Storage ([check alternatives](http://www.assembla.com/spaces/liftweb/wiki/Persistence_Alternatives))
* [mongoDB](http://www.mongodb.org) for Statistics (optional)


After installing PostgreSQL, run the following in your shell:

```shell
$ createuser -Upostgres printlndemo
$ createdb -Upostgres --owner println printlndemo
```


If everything is up and running:

```shell
$ git clone git://github.com/fbettag/println.git
$ cd println
$ edit src/main/resources/props/default.props (or production.props)
$ ./sbt update
$ ./sbt ~jetty-run
```

Visit http://127.0.0.1:8080 with your browser and follow the on-screen instructions.


## Without MongoDB

MongoDB is solely used for statistical analysis like Browser-, Referer- or Target-URL-tracking. This is not fully tested yet, but it has interesting results and more flexibility as opposed to other commercial products.

If you want to try it without MongoDB, feel free to do so. Just make sure you unset/comment "mo.host" in your .props-files.


## Page title

Instead of writing stupid SQL-Queries to get the default pagetitle, the pagetitle is defined in the properties files (default.props, production.default.props). IMHO this is a performance-saver as well as practiable.. How often do you change your main-site title?


## Tracking atom.xml and sitemap.xml

Simply place the following in one or both of the files (not in any of the repeated sections of course):

```
<lift:Stats.track/>
```


## Remarks

Some of the JavaScript- and Request-Routing-Stuff is **very** hackish, but it also shows the capabilities Lift has to offer or how you can reuse or abuse them.


## Todo

* AutoScroll/Sticky Editor-Window
* JavaScript-Evaluation in Live-Preview
* Media Management -> Image-Upload, etc.
* Twitter, Facebook and Google+ Auto-Publish


## Footnote

Thanks to everybody in the Lift Community and on [Liftweb Google Groups](http://groups.google.com/group/liftweb).
