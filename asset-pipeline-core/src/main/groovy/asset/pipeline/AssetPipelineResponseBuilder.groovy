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

import java.text.SimpleDateFormat

import groovy.transform.CompileStatic

@CompileStatic
class AssetPipelineResponseBuilder {

    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz"

    public String uri
    public String ifNoneMatchHeader
    public String ifModifiedSinceHeader
    public Map<String, String> headers = [:]

    public Integer statusCode = 200
    private Date lastModifiedDate

    AssetPipelineResponseBuilder(String uri, String ifNoneMatchHeader = null, String ifModifiedSinceHeader = null, Date lastModifiedDate = null) {
        this.uri = uri
        this.ifNoneMatchHeader = ifNoneMatchHeader
        this.ifModifiedSinceHeader = ifModifiedSinceHeader
        this.lastModifiedDate = lastModifiedDate
        boolean digestVersion = isDigestVersion()

        if (!checkDateChanged()) {
            statusCode = 304
        }
        else if (checkETag()) {
            headers['Vary'] = 'Accept-Encoding'
            if (digestVersion && !uri.endsWith(".html")) {
                headers['Cache-Control'] = 'public, max-age=31536000'
            }
            else {
                headers['Cache-Control'] = 'no-cache'
            }
        }
    }

    Map<String, String> getHeaders() {
        return headers
    }

    Integer getStatusCode() {
        return statusCode
    }

    String getCurrentETag() {

        String manifestPath = uri
        if (uri.startsWith('/')) {
            manifestPath = uri.substring(1) //Omit forward slash
        }

        Properties manifest = AssetPipelineConfigHolder.manifest
        return "\"" + (manifest?.getProperty(manifestPath) ?: manifestPath) + "\""
    }

    boolean isDigestVersion() {
        String manifestPath = uri
        if (uri.startsWith('/')) {
            manifestPath = uri.substring(1) //Omit forward slash
        }
        Properties manifest = AssetPipelineConfigHolder.manifest

        return manifest?.getProperty(manifestPath, null) ? false : true
    }

    Boolean checkETag() {
        String etagName = getCurrentETag()
        if (ifNoneMatchHeader && ifNoneMatchHeader == etagName) {
            statusCode = 304
            return false
        }
        headers["ETag"] = etagName
        return true
    }

    Boolean checkDateChanged() {
        SimpleDateFormat sdf = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US)
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"))
        boolean hasNotChanged = false
        if (lastModifiedDate) {
            headers["Last-Modified"] = getLastModifiedDate(lastModifiedDate)
        }
        if (ifModifiedSinceHeader && lastModifiedDate) {
            try {
                hasNotChanged = lastModifiedDate <= sdf.parse(ifModifiedSinceHeader)
            }
            catch (Exception ignored) {
                // Ignore this just a parse error
            }
        }
        return !hasNotChanged
    }

    private String getLastModifiedDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US)
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"))
        String lastModifiedDateTimeString = sdf.format(new Date())

        try {
            lastModifiedDateTimeString = sdf.format(date)
        }
        catch (Exception ignored) {
            //Ignore
        }
        return lastModifiedDateTimeString
    }

}
