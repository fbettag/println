#!/bin/sh
exec java -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=768m -Xmx1536M -Xss4M -Dfile.encoding=UTF8 -jar $( dirname $0 )/sbt-launch.jar "$@"

