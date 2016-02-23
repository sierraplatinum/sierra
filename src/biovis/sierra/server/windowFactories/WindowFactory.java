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

import biovis.sierra.data.DataMapper;
import biovis.sierra.data.Replicate;

import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReaderFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class WindowFactory {

    private static Double progress = 0.0;

    /**
     * add new part to progress
     *
     * @param part part being added
     */
    protected static synchronized void addProgress(Double part) {
        progress += part;
    }

    /**
     * get progress
     * @return progress
     */
    protected static synchronized Double getProgress() {
        return progress;
    }

    /**
     * reset progress
     */
    protected static synchronized void resetProgress() {
        progress = 0.0;
    }

    /**
     * Reads the file header of all data set files and compiles a list of
     * chromosomes and the corresponding lengths out of it. If a chromosome
     * identifier occurs twice but with different length only the longer
     * length is kept.
     *
     * @param mapper data mapper containing the files
     * @return map mapping chromosome identifiers (string) to their length (integer)
     * @throws IOException IOException is thrown if data set files cannot be closed
     */
    protected static Map<String, Integer> generateGenome(DataMapper mapper) throws IOException {

        Map<String, Integer> genome = new HashMap<>();
        SamReaderFactory samReaderDefaultFactory = SamReaderFactory.makeDefault();
        Iterator<SAMSequenceRecord> it;
        for (Replicate replicate : mapper.getReplicates()) {
            it = samReaderDefaultFactory.getFileHeader(replicate.getBackground().getFile()).getSequenceDictionary().getSequences().iterator();
            checkSAMRecords(it, genome);
            it = samReaderDefaultFactory.getFileHeader(replicate.getExperiment().getFile()).getSequenceDictionary().getSequences().iterator();
            checkSAMRecords(it, genome);
        }

        return genome;
    }

    /**
     *
     * @param it iterator
     * @param genome genome map
     */
    private static void checkSAMRecords(Iterator<SAMSequenceRecord> it, Map<String, Integer> genome) {
        while (it.hasNext()) {
            SAMSequenceRecord ssr = it.next();
            String name = ssr.getSequenceName();
            Integer length = ssr.getSequenceLength();
            if (!genome.containsKey(name) || genome.get(name) < length) {
                genome.put(name, length);
            }
        }
    }
}
