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
package asset.pipeline

import asset.pipeline.fs.AssetResolver

/**
 * Holder for Asset Pipeline's configuration
 * Also Provides Helper methods for loading in config from properties
 * @author David Estes
 */
class AssetPipelineConfigHolder {

    public static Collection<AssetResolver> resolvers = []
    public static Properties manifest
    public static Map config = [:]

    private static Integer configHashCode
    private static Integer resolverHashCode
    private static String digestString
    private static Object configMutexLock = new Object()

    public static Map<String, AssetPipelineClassLoaderEntry> classLoaderRegistry = [:]

    static registerResolver(AssetResolver resolver) {
        resolvers << resolver
    }

    static Properties getManifest() {
        return this.manifest
    }

    static void setManifest(Properties manifest) {
        this.manifest = manifest
    }

    static Map getConfig() {
        return this.config
    }

    static void setConfig(Map config) {
        synchronized (configMutexLock) {
            this.config = config
        }
    }

    static Collection<AssetResolver> getResolvers() {
        return this.resolvers
    }

    static void setResolvers(Collection<AssetResolver> resolvers) {
        this.resolvers = resolvers
    }

    static registerClassLoader(String prefixPath, ClassLoader classLoader) {
        classLoaderRegistry[prefixPath] = new AssetPipelineClassLoaderEntry(classLoader)
    }

    static unregisterClassLoader(String prefixPath) {
        classLoaderRegistry.remove(prefixPath)
    }

    static String classLoaderKeyForUri(String fileUri) {
        for (String key in classLoaderRegistry.keySet()) {
            if (fileUri.startsWith(key)) {
                return key
                break
            }
        }
        return null
    }

    static String getDigestString() {
        synchronized (configMutexLock) {
            //check if the maps or arrays have changed to reset the digest
            if (resolvers?.hashCode() != resolverHashCode || config?.hashCode() != configHashCode) {
                digestString = AssetHelper.getByteDigest([
                        config   : config?.sort(),
                        resolvers: resolvers?.collect { AssetResolver resolver -> resolver.name }?.sort()
                ].sort().toString().bytes)
                resolverHashCode = resolvers?.hashCode()
                configHashCode = config?.hashCode()
            }
            return digestString
        }
    }

}
