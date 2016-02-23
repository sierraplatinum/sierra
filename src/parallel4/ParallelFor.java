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
package parallel4;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class handling parallelization for given set of parameters.
 * Generic parameter type.
 *
 * @author Dirk Zeckzer
 */
public abstract class ParallelFor {

    // parallelization object
    protected Parallel2 parallel;

    // current index
    protected final AtomicInteger index;

    // number of iterations
    protected int numberOfIterations;

    // runner
    protected Runner runner = null;

    // should the loop stop
    protected boolean breakLoop = false;

    /**
     * Constructor.
     *
     * @param parallel parallelization object
     */
    public ParallelFor(
            Parallel2 parallel
    ) {
        this.parallel = parallel;
        index = new AtomicInteger();
    }

    /**
     * Loop.
     *
     */
    public void loop() {
        index.set(0);
        parallel.execute(runner);
        while (index.intValue() < numberOfIterations) {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    // TODO
                    ex.printStackTrace();
                }
            }
        }
        // TODO join all runners
        ParallelizationFactory.reset(parallel);
    }

    /**
     * Interrupt execution.
     */
    public void breakLoop() {
        breakLoop = true;
    }

    /**
     * Iteration
     *
     * @param index index to handle
     */
    abstract public void iteration(int index);

    /**
     * Object that will be parallelized.
     */
    protected class Runner implements Runnable {

        private ParallelFor parallelFor = null;

        public Runner(ParallelFor parallelFor) {
            this.parallelFor = parallelFor;
        }

        @Override
        public void run() {
            while (!breakLoop) {
                int idx = index.getAndIncrement();
                if (idx >= numberOfIterations) {
                    break;
                }
                parallelFor.iteration(idx);
//                iterationDelegate.iteration(iterationValues[idx]);
            }
            synchronized (ParallelFor.this) {
                ParallelFor.this.notifyAll();
            }
        }
    }
}
