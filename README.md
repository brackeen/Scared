# Scared

Scared is a 3D shooter in the style of Wolfenstein 3D.

It was originally written as a Java applet in 1998.

## Download

An executable jar is available here: [http://www.brackeen.com/scared/](http://www.brackeen.com/scared/)

## Building

The source is organized as a Gradle project. You can build it from an IDE or from the command line.

Assuming [Git](https://help.github.com/articles/set-up-git),
[Java SE Development Kit 7](http://www.oracle.com/technetwork/java/javase/downloads/index.html), and
[Gradle 2.0](http://gradle.org/gradle-download/)
is installed, open a terminal and enter:
```
git clone https://github.com/brackeen/Scared.git
cd Scared
gradle clean build
```
An executable jar is created in the `build/libs` folder.