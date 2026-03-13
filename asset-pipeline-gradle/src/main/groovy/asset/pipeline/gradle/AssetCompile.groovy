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

import asset.pipeline.AssetCompiler
import asset.pipeline.AssetPipelineConfigHolder
import asset.pipeline.AssetSpecLoader
import asset.pipeline.fs.FileSystemAssetResolver
import asset.pipeline.fs.JarAssetResolver
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.file.FileCollection

/**
 * A Gradle task for compiling assets
 *
 * @author Graeme Rocher
 * @author Michael Yan
 */
@CompileStatic
@CacheableTask
class AssetCompile extends DefaultTask {

    private FileCollection classpath
    private FileCollection processorFiles

    @Delegate(methodAnnotations = false)
    private AssetPipelineExtensionImpl pipelineExtension = new AssetPipelineExtensionImpl()

    @Input
    boolean flattenResolvers = false

    AssetCompile() {
    }

    void setProcessorFiles(FileCollection processorFiles) {
        this.processorFiles = processorFiles
    }

    @OutputDirectory
    File getDestinationDir() {
        this.pipelineExtension.compileDir ? new File(this.pipelineExtension.compileDir) : null
    }

    void setDestinationDir(File dir) {
        this.pipelineExtension.compileDir = dir.absolutePath
    }

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    File getAssetsDir() {
        def path = this.pipelineExtension.assetsPath
        return path ? new File(path) : null
    }

    void setAssetsDir(File assetsDir) {
        this.pipelineExtension.assetsPath = assetsDir.absolutePath
    }

    @Optional
    @Classpath
    FileCollection getClasspath() {
        return this.classpath
    }

    void setClasspath(FileCollection classpath) {
        setClasspath((Object) classpath)
    }

    void setClasspath(Object classpath) {
        this.classpath = getProject().files(classpath)
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    FileTree getSource() {
        FileTree src = project.files(this.assetsDir).getAsFileTree();
        this.pipelineExtension.resolvers.each { String path ->
            File resolverFile = project.file(path)
            if (resolverFile.exists() && resolverFile.directory) {
                src += project.files(path).getAsFileTree()
            }
        }
        return src
    }

    @TaskAction
    @CompileDynamic
    void compile() {
        AssetPipelineConfigHolder.config = AssetPipelineConfigHolder.config ?: [:]
        if (configOptions) {
            AssetPipelineConfigHolder.config = AssetPipelineConfigHolder.config + configOptions
        }
        AssetPipelineConfigHolder.resolvers = []
        registerResolvers()     
        loadAssetSpecifications()

        GradleEventListener listener = verbose ? new GradleEventListener() : null
        AssetCompiler assetCompiler = new AssetCompiler(this.pipelineExtension.toMap(), listener)
        assetCompiler.excludeRules.default = this.pipelineExtension.excludes
        assetCompiler.includeRules.default = this.pipelineExtension.includes
        assetCompiler.compile()
    }

    void registerResolvers() {
        FileSystemAssetResolver mainFileResolver = new FileSystemAssetResolver('application', assetsDir.canonicalPath)
        AssetPipelineConfigHolder.registerResolver(mainFileResolver)

        this.pipelineExtension.resolvers.each { String path ->
            File resolverFile = project.file(path)
            boolean isJarFile = resolverFile.exists() && resolverFile.file && resolverFile.name.endsWith('.jar')
            boolean isAssetFolder = resolverFile.exists() && resolverFile.directory
            if (isJarFile) {
                registerJarResolvers(resolverFile)
            }
            else if (isAssetFolder) {
                def fileResolver = new FileSystemAssetResolver(path, resolverFile.canonicalPath, flattenResolvers)
                AssetPipelineConfigHolder.registerResolver(fileResolver)
            }
        }

        getClasspath()?.files?.each { registerJarResolvers(it) }
    }
    
    void registerJarResolvers(File jarFile) {
        def isJarFile = jarFile.name.endsWith('.jar') || jarFile.name.endsWith('.zip')
        if (jarFile.exists() && isJarFile) {
            AssetPipelineConfigHolder.registerResolver(new JarAssetResolver(jarFile.name, jarFile.canonicalPath, 'META-INF/assets'))
            AssetPipelineConfigHolder.registerResolver(new JarAssetResolver(jarFile.name, jarFile.canonicalPath, 'META-INF/static'))
            AssetPipelineConfigHolder.registerResolver(new JarAssetResolver(jarFile.name, jarFile.canonicalPath, 'META-INF/resources'))
        }
    }
    
    void loadAssetSpecifications() {
        Set<File> processorFiles = this.processorFiles?.files

        if (processorFiles) {
            List<URL> urls = processorFiles.collect { it.toURI().toURL() }
            ClassLoader classLoader = new URLClassLoader(urls as URL[], getClass().classLoader)
            AssetSpecLoader.loadSpecifications(classLoader)
        }
        else {
            AssetSpecLoader.loadSpecifications()
        }
    }

}
