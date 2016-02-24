/**
 *****************************************************************************
 * Copyright (c) 2015 Daniel Gerighausen, Lydia Mueller, and Dirk Zeckzer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************
 */
package biovis.sierra.server.windowFactories;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Dirk Zeckzer
 */
public class WindowFactoryChunkMap
        extends TreeMap<Integer, String> {

    private int chunks = 0;

    /**
     * Construct chunk to chromosome map.
     *
     * @param genome genome
     * @param windowSize windowSize
     * @param offset offset
     * @param chunkSize chunk size
     */
    public WindowFactoryChunkMap(
            Map<String, Integer> genome,
            int windowSize,
            int offset,
            int chunkSize
    ) {
        // construct TreeMap to find corresponding chromosome
        int startChunk = 0;
        for (String chr : genome.keySet()) {
            put(startChunk, chr);
            int windowNum = (genome.get(chr) - windowSize + offset) / offset;
            if (windowNum * offset < (genome.get(chr) - windowSize + offset)) {
                windowNum++;
            }
            int numChunks = windowNum / chunkSize;
            if (numChunks * chunkSize < windowNum) {
                numChunks++;
            }
            startChunk += numChunks;
        }
        chunks = startChunk;
    }

    public int getChunks() {
        return chunks;
    }
}
