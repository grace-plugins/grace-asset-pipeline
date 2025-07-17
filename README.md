# Asset Pipeline for Grace

[![Grace CI](https://github.com/graceframework/grace-asset-pipeline/workflows/Grace%20CI/badge.svg?style=flat)](https://github.com/graceframework/grace-asset-pipeline/actions?query=workflow%3A%Grace+CI%22)
[![Apache 2.0 license](https://img.shields.io/badge/License-APACHE%202.0-green.svg?logo=APACHE&style=flat)](https://opensource.org/licenses/Apache-2.0)
[![Latest version on Maven Central](https://img.shields.io/maven-central/v/org.graceframework.plugins/asset-pipeline-core.svg?label=Maven%20Central&logo=apache-maven&style=flat)](https://search.maven.org/search?q=g:org.graceframework.plugins)
[![Grace Document](https://img.shields.io/badge/Grace_Document-latest-blue?style=flat&logo=asciidoctor&logoColor=E40046&labelColor=ffffff&color=f49b06)](https://plugins.graceframework.org/grace-asset-pipeline/latest/)
[![Grace on Gradle](https://img.shields.io/badge/Gradle_Plugin_Portal--blue?style=social&logo=gradle)](https://plugins.gradle.org/u/grace)
[![Grace on X](https://img.shields.io/twitter/follow/graceframework?style=social)](https://x.com/graceframework)

[![Groovy Version](https://img.shields.io/badge/Groovy-4.0.27-blue?style=flat&color=4298b8)](https://groovy-lang.org/releasenotes/groovy-4.0.html)
[![Grace Version](https://img.shields.io/badge/Grace-2023.3.0-blue?style=flat&color=f49b06)](https://github.com/graceframework/grace-framework/releases/tag/v2023.3.0)
[![Spring Boot Version](https://img.shields.io/badge/Spring_Boot-3.3.13-blue?style=flat&color=6db33f)](https://github.com/spring-projects/spring-boot/releases/tag/v3.3.13)

> [!IMPORTANT]
> This repository is a fork of [Asset-Pipeline](https://github.com/wondrify/asset-pipeline), only used to support Grace 2022/2023, but it has not been tested for Grails 5/6/7.


## Overview

The Asset-Pipeline is a plugin used for managing and processing static assets in Grace applications primarily via Gradle. Asset-Pipeline functions include processing and minification of both CSS and JavaScript files.

**Features:**

* Asset Bundling
* Css Minification / Relative Path assertion
* Js Minification
* Js SourceMap Generation
* File Encoding Support
* GZIP File Generation
* Last-Modified Header
* Cache Digest Names (Creates cache digested names and stores aliases in a manifest.properties)

## Gradle Usage

If using gradle, this plugin adds a series of tasks directly to your gradle plugin. 

All you have to do is `apply plugin: 'org.graceframework.asset-pipeline'` after confirming this is in the classpath of your `buildscript` block. i.e.:

```groovy
// Example build.gradle file
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath "org.graceframework.plugins:asset-pipeline-gradle:VERSION"
    }
}

apply plugin: 'org.graceframework.asset-pipeline'

assets {
    assetsPath = 'app/assets'
    minifyJs = true
    minifyCss = true
    enableSourceMaps = true
    configOptions = [:]
  
    minifyOptions = [
        languageMode: 'ES5',
        targetLanguage: 'ES5', //Can go from ES6 to ES5 for those bleeding edgers
        optimizationLevel: 'SIMPLE'
    ]
  
    includes = []
    excludes = ['**/*.less'] // Example Exclude GLOB pattern

    //for plugin packaging
    packagePlugin = false // Set to true if this is a plugin

    // Can add custom asset locations (directories or individual jar files)
    from '/vendor/lib'
    from '/path/to/file.jar'

    // can be used to customize the hashing of the assets
    digestAlgorithm = 'MD5'
    digestSalt = ''

    enableDigests = true
    skipNonDigests = true
}

dependencies {
    runtimeOnly "org.graceframework.plugins:asset-pipeline-plugin:VERSION"
}

```

Now that you have your build.gradle files. All you need to do is put files in your projects `app/assets/javascripts`, `app/assets/stylesheets`, `app/assets/images`, or whatever subdirectory you want.
When you run `gradle assetCompile` these files will be processed and output into your `build/assets` folder by default.

## Versions

To make it easier for users to use and upgrade, Plugin adopts a version policy consistent with the [Grace Framework](https://github.com/graceframework/grace-framework).

| Plugin Version | Grace Version |
|----------------|---------------|
| 7.0.x          | 2024.0.x      |
| 6.3.x          | 2023.3.x      |
| 6.2.x          | 2023.2.x      |
| 6.1.x          | 2023.1.x      |
| 6.0.x          | 2023.0.x      |
| 5.2.x          | 2022.2.x      |
| 5.1.x          | 2022.1.x      |
| 5.0.x          | 2022.0.x      |

## Ducumentation

* [6.3.x](https://plugins.graceframework.org/grace-asset-pipeline/6.3.x/)

## Links

- [Grace Framework](https://github.com/graceframework/grace-framework)
- [Grace Plugins](https://github.com/grace-plugins)
