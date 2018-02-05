# javafxmobile-plugin [![Build Status](https://travis-ci.org/javafxports/javafxmobile-plugin.svg?branch=master)](https://travis-ci.org/javafxports/javafxmobile-plugin) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.javafxports/jfxmobile-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.javafxports/jfxmobile-plugin)

The javafxmobile-plugin is a gradle plugin that unifies the building of Java and JavaFX
applications for different target platforms:

* desktop
* android
* ios

## Getting started

### IDE plugin

The easiest way to get started is by using the Gluon IDE plugin for your IDE:

* [NetBeans](http://plugins.netbeans.org/plugin/57602/gluon-plugin)
* [IntelliJ IDEA](https://plugins.jetbrains.com/plugin/7864-gluon-plugin)
* [Eclipse](https://marketplace.eclipse.org/content/gluon-plugin)

Once the IDE plugin is installed, you can create a new Gluon Mobile project.

### Sample

A complete basic sample is also available from the Gluon samples repository:

https://github.com/gluonhq/gluon-samples/tree/master/singleview-gluonvm

## Prerequisites

### General

Building your project requires Java 9. Make sure to set the source and target compatibility
to Java 8 to make your applications deployable on Android.

```
sourceCompatibility = 1.8
targetCompatibility = 1.8
```

### Android

1. The Android SDK command line tools, available [here](https://developer.android.com/studio/index.html#command-tools)
2. Use the `sdkmanager` command line tool to install the following packages:

    ```
    ANDROID_SDK/tools/bin/sdkmanager "platform-tools" "build-tools;27.0.3" "platforms;android-25" "extras;android;m2repository" "extras;google;m2repository"
    ```

3. Define a global gradle property named ANDROID_HOME inside ~/.gradle/gradle.properties that
points to the location of the Android SDK:

    ```
    ANDROID_HOME=/path/to/android-sdk-directory
    ```

### iOS

1. A Mac with MacOS X 10.13.2 or superior
2. Xcode 9.2 or superior, available from the Mac App Store
3. To deploy on an iOS device, the `usbmuxd` library, a cross-platform software library that talks the protocols to support iOS devices. Install it with the use of [Homebrew](https://brew.sh):

    ```
    brew install usbmuxd
    ``` 

4. It is highly recommended to increase the Java stack size, by adding the following option to the `~/.gradle/gradle.properties` file:
 
    ```
    org.gradle.jvmargs=-Xms256m -Xmx4096m -Xss2m
    ```

Be aware that the first time the plugin runs an iOS task, it will take a long time (more than 15 minutes) to ahead-of-time compile all the Java modules. 
After this finishes successfully, all these modules are cached locally (`~/.gvm/aot`), so the next runs will be shorter (just a few minutes).

## Usage

If you create your project without the use of the IDE plugins, add the following to your
`build.gradle`:

```
buildscript {
    repositories {
        jcenter()
        google()
        maven {
            url 'http://nexus.gluonhq.com/nexus/content/repositories/releases'
        }
    }
    dependencies {
        classpath 'org.javafxports:jfxmobile-plugin:2.0.0'
    }
}

apply plugin: 'org.javafxports.jfxmobile'
```

## Java 9

Full Java 9 support is in an experimental phase and is currently only supported on iOS.

## License

This project is licensed under the [3-Clause BSD license](https://opensource.org/licenses/BSD-3-Clause).
