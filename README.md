# Scared

Scared is a 3D shooter in the style of Wolfenstein 3D, written in Java.

It was originally written in 1998, cleaned up through the years, hosted on various sites, and released as open source in 2012.

The source is organized as a Maven project.

## Download and Build

* Install [Java SE 6 JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* Install [Maven 3](http://maven.apache.org/download.html)
* Install [Git](https://help.github.com/articles/set-up-git)
* Download the code: `git clone https://github.com/brackeen/Scared.git`
* `cd Scared`
* Build: `mvn clean install`

In the `target` folder, there is now a jar and an index.html file.

## Running

* Option 1: Open `target/index.html` in your browser.
* Option 2: Some browsers don't like local applets. If you're having trouble, try appletviewer:
`appletviewer target/index.html`
