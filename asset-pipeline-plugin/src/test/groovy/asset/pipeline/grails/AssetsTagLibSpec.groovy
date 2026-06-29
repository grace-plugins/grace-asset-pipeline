/*
 * Copyright 2014-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package asset.pipeline.grails

import asset.pipeline.AssetPipelineConfigHolder
import asset.pipeline.fs.FileSystemAssetResolver
import spock.lang.Specification
import grails.testing.web.taglib.TagLibUnitTest

/**
 * @author David Estes
 * @author Michael Yan
 */
class AssetsTagLibSpec extends Specification implements TagLibUnitTest<AssetsTagLib>  {
	private static final LINE_BREAK           = System.getProperty('line.separator') ?: '\n'
	private static final MOCK_BASE_SERVER_URL = 'http://localhost:8080'


	AssetProcessorService assetProcessorService = new AssetProcessorService()


	def setup() {
		AssetPipelineConfigHolder.registerResolver(new FileSystemAssetResolver('application', 'grails-app/assets'))

		assetProcessorService.grailsApplication   = grailsApplication
		grails.web.mapping.LinkGenerator linkGenerator = Mock(grails.web.mapping.LinkGenerator)
		linkGenerator.getServerBaseURL() >> MOCK_BASE_SERVER_URL

		final def assetMethodTagLibMock = mockTagLib(AssetMethodTagLib)
		assetMethodTagLibMock.assetProcessorService = assetProcessorService

		tagLib.assetProcessorService = assetProcessorService
	}

	void "should return assetPath"() {
		given:
		final def assetSrc = "asset-pipeline/test/test.css"

		expect:
		tagLib.assetPath(src: assetSrc) == '/assets/asset-pipeline/test/test.css'
	}

	void "should not return javascript link twice in uniq mode"() {
		given:
		final def assetSrc = "asset-pipeline/test/test_simple_require.js"
		final def depAssetSrc = "asset-pipeline/test/libs/file_a.js"

		expect:
		applyTemplate("<asset:javascript src=\"$assetSrc\" uniq=\"true\"/>") == ''
		applyTemplate("<asset:javascript src=\"$depAssetSrc\" uniq=\"true\"/>") == ''

		cleanup:
			request."${AssetsTagLib.ASSET_REQUEST_MEMO}" = null
	}

	void "should return javascript and stylesheets of the same basename"() {
		given:
		final def jsAssetSrc = "asset-pipeline/test/test.js"
		final def cssAssetSrc = "asset-pipeline/test/test.css"
		final Properties manifestProperties = new Properties()
		manifestProperties.setProperty(jsAssetSrc, jsAssetSrc)
		manifestProperties.setProperty(cssAssetSrc, cssAssetSrc)
		AssetPipelineConfigHolder.manifest = manifestProperties

		expect:
		applyTemplate("<asset:javascript src=\"$jsAssetSrc\" uniq=\"true\"/>") == ''
		applyTemplate("<asset:stylesheet src=\"$cssAssetSrc\" uniq=\"true\"/>") == ''

		cleanup:
		AssetPipelineConfigHolder.manifest = null
	}

	void "should return stylesheet link tag"() {
		given:
		final def assetSrc = "asset-pipeline/test/test.css"
		final Properties manifestProperties = new Properties()
		manifestProperties.setProperty(assetSrc, assetSrc)
		AssetPipelineConfigHolder.manifest = manifestProperties

		expect:
		applyTemplate("<asset:stylesheet href=\"$assetSrc\"/>") == '<link rel="stylesheet" href="/assets/asset-pipeline/test/test.css" />'

		cleanup:
		AssetPipelineConfigHolder.manifest = null
	}

	void "should return image tag"() {
		given:
		final def assetSrc = "grails_logo.png"

		expect:
		applyTemplate("<asset:image src=\"$assetSrc\" width=\"200\" height=\"200\"/>") == '<img src="/assets/grails_logo.png" width="200" height="200"/>'
	}

	void "should return image tag with absolute path"() {
		given:
			final def assetSrc = "grails_logo.png"
		expect:
		applyTemplate("<asset:image src=\"$assetSrc\" absolute=\"true\"/>") == "<img src=\"$MOCK_BASE_SERVER_URL/assets/grails_logo.png\" />"
	}

	void "should return link tag"() {
		given:
			final def assetSrc = "grails_logo.png"
		expect:
		applyTemplate("<asset:link href=\"$assetSrc\" rel=\"test\"/>") == '<link rel="test" href="/assets/grails_logo.png"/>'
	}

	void "test if asset path exists in dev mode"() {
		given:
			final def fileUri = "asset-pipeline/test/test.css"
		expect:
		applyTemplate("<asset:assetPathExists src=\"$fileUri\">Exists</asset:assetPathExists>") == ''
	}

	void "test if asset path is missing in dev mode"() {
		given:
			final def fileUri = "asset-pipeline/test/missing.css"
		expect:
		applyTemplate("<asset:assetPathExists src=\"$fileUri\">Exists</asset:assetPathExists>") == ''
	}

	void "test if asset path exists in dev mode and closure renders the body"() {
		given:
			final def fileUri = "asset-pipeline/test/test.css"
			final Properties manifestProperties = new Properties()
			manifestProperties.setProperty(fileUri, fileUri)
			AssetPipelineConfigHolder.manifest = manifestProperties
		expect:
			applyTemplate( "<asset:assetPathExists src=\"$fileUri\">text to render</asset:assetPathExists>" ) == 'text to render'
	}

	void "test if asset path is missing in dev mode and closure doesn't render the body"() {
		given:
			final def fileUri = "asset-pipeline/test/missing.css"
		expect:
			applyTemplate( "<asset:assetPathExists src=\"$fileUri\">text to render</asset:assetPathExists>" ) == ''
	}

	void "test if asset path exists in prod mode"() {
		given:
			final def fileUri = "asset-pipeline/test/test.css"
			final Properties manifestProperties = new Properties()
			manifestProperties.setProperty(fileUri,fileUri)
			AssetPipelineConfigHolder.manifest = manifestProperties
		expect:
		applyTemplate("<asset:assetPathExists src=\"$fileUri\">Exists</asset:assetPathExists>") == 'Exists'
	}

	void "asset path should not exist in dev mode"() {
		given:
			final def fileUri = "asset-pipeline/test/notfound.css"
		expect:
		applyTemplate("<asset:assetPathExists src=\"$fileUri\">Exists</asset:assetPathExists>") == ''
	}

	void "should render deferred scripts"() {
		given:
			final def script1 = "console.log('hello world 1');"
			final def script2 = "console.log('hello world 2');"

		when:
			applyTemplate("<asset:script type='text/javascript'>$script1</asset:script>")
			applyTemplate("<asset:script type='text/javascript'>$script2</asset:script>")
		then:
			applyTemplate("<asset:deferredScripts/>") == "<script type=\"text/javascript\">${script1}</script><script type=\"text/javascript\">${script2}</script>"
	}

	void "should render deferred scripts and evaluate nested groovy expressions"() {
		when:
			applyTemplate('<asset:script type="text/javascript"><g:if test="${isTrue}">alert("foo");</g:if></asset:script>', [isTrue: true])
		then:
			applyTemplate("<asset:deferredScripts/>") == '<script type="text/javascript">alert("foo");</script>'
	}
}
