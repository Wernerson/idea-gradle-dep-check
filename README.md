# Gradle Dependency Checker - IntelliJ Plugin

## Programming Assignment

### Assignment

This project was created as a solution to a programming assignment where the task was:

Please develop a plugin for IntelliJ IDEA that would call a server you've developed, check the hashes of artifacts from
gradle dependencies with reference ones, and highlight problems where the hashes do not match.
Important:

* Kotlin code.
* Server on Ktor.
* Ease of use of the plugin.
* Server performance.
* The solution should work.
  Doesn't matter:
* Design.
* Failness of the data on the server. (a plan of where to get it would suffice)

### Solution

I parse the gradle (build.gradle.kts or build.gradle) files and filter out dependencies (in the form
of `group:name:version`) and check these again a server. If they are present in the hard-coded server vulnerability
list, I highlight them in the IDE with a warning message.
