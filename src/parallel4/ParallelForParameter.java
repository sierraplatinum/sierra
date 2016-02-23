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
 *****************************************************************************
 */
package parallel4;

import java.util.List;

/**
 * Class handling parallelization for given set of parameters.
 * Generic parameter type.
 *
 * @author Dirk Zeckzer
 * @param <ParallelParameter>
 */
public class ParallelForParameter<ParallelParameter>
        extends ParallelFor {

    // iteration method
    private IterationParameter iterationDelegate;

    // number of iterations and actual values
    List<ParallelParameter> parameterSet = null;

    // TODO exceptions: fail fast (default), collect, ignore
    /**
     * Constructor.
     *
     * @param parallel parallelization object
     * @param parameterSet parameter set
     */
    public ParallelForParameter(
            Parallel2 parallel,
            List<ParallelParameter> parameterSet
    ) {
        super(parallel);

        this.parameterSet = parameterSet;
        numberOfIterations = parameterSet.size();

        runner = new Runner(this);
    }

    /**
     * Loop.
     *
     * @param iterationDelegate iteration method
     */
    public void loop(IterationParameter iterationDelegate) {
        if (numberOfIterations == 1) {
            // shortcut: only one iteration
            iterationDelegate.iteration(parameterSet.get(0));
            return;
        } else if (parallel.getNumberOfCores() == 1) {
            // shortcut: only one core -> sequential computation
            for (ParallelParameter parameter : parameterSet) {
                iterationDelegate.iteration(parameter);
            }
            return;
        } else {
            // parallel computation
            this.iterationDelegate = iterationDelegate;
            super.loop();
        }
    }

    /**
     * Iteration
     *
     * @param index index to handle
     */
    public void iteration(int index) {
        iterationDelegate.iteration(parameterSet.get(index));
    }
}
