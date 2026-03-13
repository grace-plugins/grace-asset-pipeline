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

/**
 * Holds config for nested class loaders for dynamically loading, and unloading assets
 * @author David Estes
 */
class AssetPipelineClassLoaderEntry {

	public ClassLoader classLoader
	Properties manifest
	ClassLoader classLoader 

	static final String MANIFEST_LOCATION = "assets/manifest.properties"

	AssetPipelineClassLoaderEntry(ClassLoader classLoader) {
		this.classLoader = classLoader
		URL res = classLoader.getResource(MANIFEST_LOCATION)
		if (res) {
			def manifestProps = new Properties()
			def propertiesStream = res.openStream()
			manifestProps.load(propertiesStream)
			manifest = manifestProps
		}
	}

}
