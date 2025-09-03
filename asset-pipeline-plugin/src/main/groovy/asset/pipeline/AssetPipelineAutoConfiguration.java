/*
 * Copyright 2024-2025 the original author or authors.
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
package asset.pipeline;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import asset.pipeline.fs.ClasspathAssetResolver;
import asset.pipeline.fs.FileSystemAssetResolver;
import asset.pipeline.grails.AssetProcessorService;
import asset.pipeline.grails.AssetResourceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import asset.pipeline.grails.LinkGenerator;
import asset.pipeline.grails.CachingLinkGenerator;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;

import grails.config.Config;
import grails.config.Settings;
import grails.core.GrailsApplication;
import grails.plugins.GrailsPlugin;
import grails.plugins.GrailsPluginManager;
import grails.util.BuildSettings;
import grails.util.Environment;
import grails.web.mapping.UrlMappingsHolder;
import org.grails.config.NavigableMap;
import org.grails.plugins.BinaryGrailsPlugin;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Asset Pipeline
 *
 * @author Michael Yan
 * @since 6.1
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(AssetPipelineProperties.class)
public class AssetPipelineAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AssetPipelineAutoConfiguration.class);

    @Order(-20)
    @Bean({"linkGenerator", "grailsLinkGenerator", "assetLinkGenerator"})
    @ConditionalOnMissingBean
    public grails.web.mapping.LinkGenerator assetLinkGenerator(ObjectProvider<GrailsApplication> grailsApplication,
            ObjectProvider<UrlMappingsHolder> grailsUrlMappingsHolder, ObjectProvider<AssetProcessorService> assetProcessorService) {
        Config config = grailsApplication.getObject().getConfig();
        boolean isReloadEnabled = Environment.isDevelopmentMode() || Environment.getCurrent().isReloadEnabled();
        boolean cacheUrls = config.getProperty(Settings.WEB_LINK_GENERATOR_USE_CACHE, Boolean.class, !isReloadEnabled);
        String serverURL = config.getProperty(Settings.SERVER_URL);

        if (cacheUrls) {
            CachingLinkGenerator cachingLinkGenerator = new CachingLinkGenerator(serverURL);
            cachingLinkGenerator.setGrailsApplication(grailsApplication.getObject());
            cachingLinkGenerator.setUrlMappingsHolder(grailsUrlMappingsHolder.getObject());
            cachingLinkGenerator.setAssetProcessorService(assetProcessorService.getObject());
            return cachingLinkGenerator;
        }
        else {
            LinkGenerator linkGenerator = new LinkGenerator(serverURL);
            linkGenerator.setGrailsApplication(grailsApplication.getObject());
            linkGenerator.setAssetProcessorService(assetProcessorService.getObject());
            linkGenerator.setUrlMappingsHolder(grailsUrlMappingsHolder.getObject());
            return linkGenerator;
        }
    }

    @Bean
    @Order
    @ConditionalOnMissingBean
    public AssetResourceLocator assetResourceLocator() throws IOException {
        AssetResourceLocator assetResourceLocator = new AssetResourceLocator();
        assetResourceLocator.setSearchLocations(List.of(BuildSettings.BASE_DIR.getCanonicalPath()));
        return assetResourceLocator;
    }

    @Bean
    @ConditionalOnMissingBean
    public AssetProcessorService assetProcessorService(ObjectProvider<GrailsApplication> grailsApplication) {
        AssetProcessorService assetProcessorService = new AssetProcessorService(grailsApplication.getObject());
        return assetProcessorService;
    }

    @Bean
    @SuppressWarnings("rawtypes")
    public FilterRegistrationBean<AssetPipelineFilter> assetPipelineFilter(AssetPipelineProperties assetPipelineProperties,
            ObjectProvider<GrailsApplication> grailsApplication, ObjectProvider<GrailsPluginManager> grailsPluginManager,
            ApplicationContext applicationContext) {
        Config config = grailsApplication.getObject().getConfig();
        Map assetsConfig = config.getProperty("grails.assets", Map.class, new HashMap());

        Properties manifestProps = new Properties();
        Resource manifestFile = null;
        try {
            manifestFile = applicationContext.getResource("assets/manifest.properties");
            if (!manifestFile.exists()) {
                manifestFile = applicationContext.getResource("classpath:assets/manifest.properties");
            }
        }
        catch (Exception e) {
            if (grailsApplication.getObject().isWarDeployed()) {
                logger.warn("Unable to find asset-pipeline manifest, etags will not be properly generated");
            }
        }

        boolean useManifest = assetPipelineProperties.isUseManifest();
        if (useManifest && manifestFile != null && manifestFile.exists()) {
            try {
                manifestProps.load(manifestFile.getInputStream());
                assetsConfig.put("manifest", manifestProps);
                AssetPipelineConfigHolder.manifest = manifestProps;
            }
            catch (Exception e) {
                logger.warn("Failed to load manifest.properties");
            }
        }
        else {
            String assetsPath= assetPipelineProperties.getAssetsPath();
            FileSystemAssetResolver applicationResolver = new FileSystemAssetResolver("application", BuildSettings.BASE_DIR + "/" + assetsPath);
            AssetPipelineConfigHolder.registerResolver(applicationResolver);
            AssetPipelineConfigHolder.registerResolver(new ClasspathAssetResolver("classpath", "META-INF/assets", "META-INF/assets.list"));
            AssetPipelineConfigHolder.registerResolver(new ClasspathAssetResolver("classpath", "META-INF/static"));
            AssetPipelineConfigHolder.registerResolver(new ClasspathAssetResolver("classpath", "META-INF/resources"));
            try {
                for (GrailsPlugin plugin : grailsPluginManager.getObject().getAllPlugins()) {
                    if (plugin instanceof BinaryGrailsPlugin) {
                        File projectDirectory = ((BinaryGrailsPlugin) plugin).getProjectDirectory();
                        if (projectDirectory != null) {
                            String assetPath = new File(projectDirectory, assetsPath).getCanonicalPath();
                            AssetPipelineConfigHolder.registerResolver(new FileSystemAssetResolver(plugin.getName(), assetPath));
                        }
                    }
                }
            }
            catch (Exception e) {
                logger.warn("Error loading exploded plugins", e);
            }
        }

        if (assetsConfig instanceof NavigableMap) {
            AssetPipelineConfigHolder.config = ((NavigableMap) assetsConfig).toFlatConfig();
        }
        else {
            AssetPipelineConfigHolder.config = assetsConfig;
        }
        try {
            if (BuildSettings.TARGET_DIR != null) {
                AssetPipelineConfigHolder.config.put("cacheLocation", new File(BuildSettings.TARGET_DIR, ".assetcache").getCanonicalPath());
            }
        }
        catch (Exception ignored) {
        }

        String mapping = assetPipelineProperties.getMapping();
        AssetPipelineFilter filter = new AssetPipelineFilter();
        FilterRegistrationBean<AssetPipelineFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setUrlPatterns(List.of(String.format("/%s/*", mapping)));
        registration.setOrder(0);

        return registration;
    }

}
