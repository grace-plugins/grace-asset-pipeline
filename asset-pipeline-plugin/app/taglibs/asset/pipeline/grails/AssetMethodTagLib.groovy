package asset.pipeline.grails

class AssetMethodTagLib {

	static namespace           = 'g'
	static returnObjectForTags = ['assetPath']

	AssetProcessorService assetProcessorService
	grails.web.mapping.LinkGenerator grailsLinkGenerator

	def assetPath = {final def attrs ->
		final def     src
		final String baseUrl
		final boolean useManifest

		if (attrs instanceof Map) {
			src         = attrs.src
			baseUrl     = attrs.absolute ? grailsLinkGenerator.serverBaseURL : trimToEmpty(grailsLinkGenerator.contextPath)
			useManifest = attrs.useManifest ?: true
		}
		else {
			src         = attrs
			baseUrl     = trimToEmpty(grailsLinkGenerator.contextPath)
            useManifest = true
		}

		return assetProcessorService.assetBaseUrl(request, baseUrl) + assetProcessorService.getAssetPath(Objects.toString(src), useManifest)
	}

	private static String trimToEmpty(String str) {
		return str == null ? "" : str.trim()
	}
}
