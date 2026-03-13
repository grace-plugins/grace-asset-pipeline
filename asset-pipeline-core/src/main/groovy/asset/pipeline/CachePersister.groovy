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
 * Receives asynchronous cache persistance requests and executes them.
 * Also acts as a debouncer
 *
 * @author David Estes
 */
// @CompileStatic
class CachePersister extends Thread {

    public Integer delay = 0
    public Boolean ran = true
    public final Integer RUN_DELAY = 1000

    void run() {
        while (true) {
            sleep(RUN_DELAY)
            if (ran == false) {
                delay -= RUN_DELAY
                if (delay <= 0) {
                    CacheManager.save()
                    ran = true
                }
            }

        }
    }

    /**
     * Asynchronously triggers a save with a debounce delay to reduce excessive persistence calls
     * @param delay (milliseconds) between debounce save
     */
    void debounceSave(Integer delay) {
        this.delay = delay
        ran = false
    }

}
