/*
 * Copyright 2014-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package asset.pipeline.gradle

import asset.pipeline.AssetPipelineConfigHolder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.language.jvm.tasks.ProcessResources

/**
 * This is the Gradle Plugin implementation of asset-pipeline-core.
 * It provides a set of tasks useful for working with your assets directly.
 *
 * task: assetCompile Compiles your assets into your build directory
 * task: assetClean Cleans the build/assets directory
 *
 * @author David Estes
 * @author Graeme Rocher
 * @author Craig Burke
 * @author Michael Yan
 * @since 2.0
 */
class AssetPipelinePlugin implements Plugin<Project> {

    static final String ASSET_CONFIGURATION_NAME = 'assets'
    static final String ASSET_TASK_GROUP = 'Asset Pipeline'

    void apply(Project project) {
        createGradleConfiguration(project)

        Configuration assetsClasspath = project.configurations.getByName(AssetPipelinePlugin.ASSET_CONFIGURATION_NAME)

        AssetPipelineExtensionImpl assetPipelineExtension = project.extensions.create('assets', AssetPipelineExtensionImpl)
        Map config = AssetPipelineConfigHolder.config != null ? AssetPipelineConfigHolder.config : [:]
        config.cacheLocation = project.layout.buildDirectory.dir('.assetcache').get().asFile.absolutePath

        if (project.extensions.findByName('grails')) {
            assetPipelineExtension.assetsPath = project.file('app/assets').absolutePath
        }
        else {
            assetPipelineExtension.assetsPath = project.file('src/assets').absolutePath
        }
        assetPipelineExtension.compileDir = project.layout.buildDirectory.dir('assets').get().asFile.absolutePath

        TaskProvider<AssetCompile> assetCompileTask = project.tasks.register('assetCompile', AssetCompile) { AssetCompile task ->
            task.group = ASSET_TASK_GROUP
            task.description = 'Compiles assets.'
            task.classpath = getAssetClasspath(project)
            task.processorFiles = assetsClasspath
            task.destinationDir = project.file(assetPipelineExtension.compileDir)
            task.assetsDir = project.file(assetPipelineExtension.assetsPath)
            task.minifyJs = assetPipelineExtension.minifyJs
            task.minifyCss = assetPipelineExtension.minifyCss
            task.minifyOptions = assetPipelineExtension.minifyOptions
            task.includes = assetPipelineExtension.includes
            task.excludes = assetPipelineExtension.excludes
            task.excludesGzip = assetPipelineExtension.excludesGzip
            task.configOptions = assetPipelineExtension.configOptions
            task.skipNonDigests = assetPipelineExtension.skipNonDigests
            task.enableDigests = assetPipelineExtension.enableDigests
            task.enableSourceMaps = assetPipelineExtension.enableSourceMaps
            task.resolvers = assetPipelineExtension.resolvers
            task.enableGzip = assetPipelineExtension.enableGzip
            task.verbose = assetPipelineExtension.verbose
            task.maxThreads = assetPipelineExtension.maxThreads
        }

        TaskProvider<AssetPluginPackage> assetPluginTask = project.tasks.register('assetPluginPackage', AssetPluginPackage) { AssetPluginPackage task ->
            task.group = ASSET_TASK_GROUP
            task.description = 'Assembles assets.'
            task.assetsDir = project.file(assetPipelineExtension.assetsPath)
            task.destinationDir = project.file("${assetPipelineExtension.compileDir}/META-INF")
        }

        project.tasks.register('assetClean', Delete).configure { Delete task ->
            task.group = ASSET_TASK_GROUP
            task.description = 'Deletes the assets directory.'
            task.delete project.file(assetPipelineExtension.compileDir)
        }

        project.afterEvaluate {
            AssetPipelineExtensionImpl assetPipeline = project.extensions.getByType(AssetPipelineExtensionImpl)
            ProcessResources processResources = (ProcessResources) project.tasks.findByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME)

            configureBootRun(project)

            try {
                DistributionContainer distributionContainer = project.extensions.getByType(DistributionContainer)
                distributionContainer.getByName("main").contents.from(assetPipeline.compileDir) {
                    into "app/assets"
                }
            }
            catch (UnknownDomainObjectException ignored) {
                // we dont care this is just to see if it exists
            }

            if (assetPipeline.packagePlugin) { //this is just a lib we dont want to do assetCompile
                processResources.dependsOn(assetPluginTask)
            }
            else if (!assetPipeline.developmentRuntime && processResources) {
                processResources.dependsOn(assetCompileTask)
                processResources.from assetPipeline.compileDir, {
                    into "assets"
                }
            }
            else {
                if (assetPipeline.jarTaskName) {
                    Jar jarTask = project.tasks.findByName(assetPipeline.jarTaskName)
                    if (jarTask) {
                        jarTask.dependsOn assetCompileTask
                        jarTask.from assetPipeline.compileDir, {
                            into "assets"
                        }
                    }
                }
                else { //no jar task name specified we need to try and infer
                    def assetTasks = ['war', 'shadowJar', 'jar', 'bootWar','bootJar']

                    assetTasks?.each { taskName ->
                        Jar jarTask = project.tasks.findByName(taskName)
                        if (jarTask) {
                            jarTask.dependsOn assetCompileTask
                            jarTask.from assetPipeline.compileDir, {
                                into "assets"
                            }
                        }
                    }
                }
            }
        }
    }

    private void configureBootRun(Project project) {
        JavaExec bootRunTask = (JavaExec) project.tasks.findByName('bootRun')
        if (bootRunTask != null) {
            List<File> additionalFiles = []
            Set<File> buildDependencies = project.buildscript.configurations.findByName("classpath")?.files
            if (buildDependencies) {
                for (file in buildDependencies) {
                    if (file.name.startsWith('rhino-') || file.name.startsWith('closure-compiler-unshaded-')) {
                        additionalFiles.add(file)
                    }
                }
            }
            Set<File> assetDeps = project.configurations.findByName(ASSET_CONFIGURATION_NAME)?.files
            if (assetDeps) {
                for (file in assetDeps) {
                    additionalFiles.add(file)
                }
            }
            bootRunTask.classpath += project.files(additionalFiles)
        }
    }

    private void createGradleConfiguration(Project project) {
        Configuration configuration = project.configurations.create(ASSET_CONFIGURATION_NAME)
        JavaExec bootRunTask = (JavaExec) project.tasks.findByName('bootRun')

        project.plugins.withType(JavaPlugin) {
            if (bootRunTask) {
                Configuration runtimeConfiguration = project.configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)
                runtimeConfiguration.extendsFrom configuration        
            }
            else {
                Configuration runtimeConfiguration = project.configurations.getByName(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME)
                runtimeConfiguration.extendsFrom configuration        
            }
        }
    }

    private FileCollection getAssetClasspath(Project project) {
        try {
            FileCollection runtimeFiles = project.configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)

            FileCollection totalFiles = runtimeFiles
            try {
                FileCollection providedFiles = project.configurations.getByName('provided')
                if (providedFiles) {
                    totalFiles += providedFiles
                }
            }
            catch (ignored) {
            }

            try {
                FileCollection assetsFiles = project.configurations.getByName(ASSET_CONFIGURATION_NAME)
                if (assetsFiles) {
                    totalFiles += assetsFiles
                }
            }
            catch (ignored) {
            }

            return totalFiles
        }
        catch (ignored) {
            return null
        }
    }

}
