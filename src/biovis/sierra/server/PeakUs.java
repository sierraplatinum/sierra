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
package biovis.sierra.server;

import biovis.sierra.data.DataMapper;
import biovis.sierra.data.windows.Window;
import biovis.sierra.data.windows.WindowList;
import biovislib.parallel4.IterationInt;
import biovislib.parallel4.Parallel2;
import biovislib.parallel4.ParallelForInt2;
import biovislib.parallel4.ParallelizationFactory;
import biovislib.statistics.InverseNormalMethod;

/**
 *
 * @author Lydia Mueller
 */
public class PeakUs {

    private InverseNormalMethod inm;
    private DataMapper mapper;

    /**
     * Constructor
     *
     * @param mapper data mapper
     */
    public PeakUs(
            DataMapper mapper
    ) {
        this.mapper = mapper;
        inm = new InverseNormalMethod(mapper.isCorrectCorrelation(), mapper.isWeighted(), mapper.getINMReplicates());
    }

    /**
     * Calculate final p values.
     * @param windowList window list
     */
    public void peak(
            WindowList windowList
    ) {
        int stepSize = windowList.getSize() / mapper.getNumCores() + 1;
        Parallel2 p2 = ParallelizationFactory.getInstance(mapper.getNumCores());
        new ParallelForInt2(p2, 0, windowList.getSize(), stepSize).loop(
                new IterationInt() {
            @Override
            public void iteration(int index) {
                int end = Math.min(index + stepSize, windowList.getSize());
                for (Window window : windowList.getWindows(index, end)) {
                    window.setFinalRawPValue(inm.getPValue(window.getPValueList()));
                }
            }
        });

    }
}
