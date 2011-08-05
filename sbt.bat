set SCRIPT_DIR=%~dp0
java -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=768m -Xmx1536M -Xss4M -Dfile.encoding=UTF8 -jar "%SCRIPT_DIR%\sbt-launch.jar" %*

