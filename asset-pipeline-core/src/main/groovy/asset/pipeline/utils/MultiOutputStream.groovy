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
package asset.pipeline.utils

import groovy.transform.CompileStatic

/**
 * An OutputStream capable of writing to a collection of output streams underneath simultaneously
 *
 * @author David Estes
 */
@CompileStatic
class MultiOutputStream extends OutputStream {

    private final Collection<OutputStream> streams

    /**
     * Constructor method for creating the input Stream
     * @param streams a list or collection of output streams
     */
    MultiOutputStream(Collection<OutputStream> streams) {
        if (streams == null)
            throw new NullPointerException()
        this.streams = streams
    }

    @Override
    void write(int b) throws IOException {
        streams.each { OutputStream stream ->
            stream.write(b)
        }
    }

    @Override
    void write(byte[] b) throws IOException {
        streams.each { OutputStream stream ->
            stream.write(b)
        }
    }

    @Override
    void write(byte[] b, int off, int len) throws IOException {
        streams.each { OutputStream stream ->
            stream.write(b, off, len)
        }
    }

    @Override
    void flush() throws IOException {
        streams.each { OutputStream stream ->
            stream.flush()
        }
    }

}