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
import biovis.sierra.data.windows.Window;
import biovis.sierra.server.Commander.PeakCommander;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lydia Mueller, Dirk Zeckzer
 */
public class WindowFactoryCountTagParallel
        extends WindowFactory {

    /**
     * Approach A: Construct first the window list sequentially and count tags
     * afterwards in parallel
     */
    /**
     * Creates the window list as a single list and established neighbor
     * relationships between windows
     *
     * @param mapper data mapper with data set information to use
     * @param windowSize size of the window
     * @param windowOffset offset by which the windows are moved along the genome
     * @param pc peak commander
     * @return returns a windowlist object with no tags counted but all windows
     * created
     * @throws IOException Throws IOException if SamReader cannot close the
     * input files
     */
    //construct all windows and count afterwards (only count parallel)
    public static WindowListCountTagParallel constructWindows(
            DataMapper mapper,
            int windowSize,
            int windowOffset,
            PeakCommander pc
    ) throws IOException {
        Logger log = Logger.getLogger("WindowListCountTagParallel.constructWindows");
        log.info("Constructing windows and state A");

        WindowListCountTagParallel list = new WindowListCountTagParallel(mapper);

        //extract chromosomes and length from sam file
        Map<String, Integer> genome = WindowFactory.generateGenome(mapper);

        //estimate number of windows
        list.estimateWindowNumber(genome, windowSize, windowOffset);

        log.info("Creating windows");
        for (String chr : genome.keySet()) {

            Integer windowStart = 0;
            Integer windowEnd = windowStart + windowSize - 1;
            while (windowEnd < genome.get(chr)) {
                Window w = new Window(chr, windowStart, windowEnd,
                                      mapper.getNumberOfDataSets(),
                                      mapper.getReplicates().size());
                list.addWindow(w);
                windowStart += windowOffset;
                windowEnd += windowOffset;
            }

            if (windowStart < genome.get(chr)) {
                Window w = new Window(chr, windowStart, genome.get(chr) - 1,
                                      mapper.getNumberOfDataSets(),
                                      mapper.getReplicates().size());
                list.addWindow(w);
            }
        }

        log.info("windows available");

        log.info("count tags");
        list.countAllTags(pc);
        log.info("tags counted");

        log.info("flattening");
        list.flatten();
        log.info("flatten done");

        log.info("windows constructed");
        return list;
    }
}
