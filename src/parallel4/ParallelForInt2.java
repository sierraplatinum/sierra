/**
 *
 */
package parallel4;

/**
 * (C) Copyright 2008 Markus Junginger.
 *
 * @author Markus Junginger
 */
/*
 * This file is part of Parallel4.
 *
 * Parallel4 is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Parallel4 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Parallel4. If not, see <http://www.gnu.org/licenses/>.
 */
public final class ParallelForInt2
        extends ParallelFor {

    // iteration method
    private IterationInt iterationDelegate;

    // for loop parameters
    private final int start;
    private final int stop;
    private final int step;

    // number of iterations and actual values
    private int[] iterationValues;

    // TODO exceptions: fail fast (default), collect, ignore
    /**
     * Constructor.
     *
     * @param parallel parallelization object
     * @param stop final index
     */
    public ParallelForInt2(Parallel2 parallel, int stop) {
        this(parallel, 0, stop, 1);
    }

    /**
     * Constructor.
     *
     * @param parallel parallelization object
     * @param start first index
     * @param stop final index
     */
    public ParallelForInt2(Parallel2 parallel, int start, int stop) {
        this(parallel, start, stop, 1);
    }

    /**
     * Constructor.
     *
     * @param parallel parallelization object
     * @param start first index
     * @param stop final index
     * @param step step width
     */
    public ParallelForInt2(Parallel2 parallel, int start, int stop, int step) {
        super(parallel);

        this.parallel = parallel;
        this.start = start;
        this.stop = stop;
        this.step = step;

        runner = new Runner(this);
    }

    /**
     * Loop.
     *
     * @param iterationDelegate iteration method
     */
    public void loop(IterationInt iterationDelegate) {
        if (start == stop) {
            return;
        } else if (start + step == stop) {
            iterationDelegate.iteration(start);
            return;
        } else if (parallel.getNumberOfCores() == 1) {
            for (int i = start; i < stop; i += step) {
                iterationDelegate.iteration(i);
            }
            return;
        } else {

            numberOfIterations = (stop - start) / step;
            // TODO use double to check too big steps, too
            if (numberOfIterations < 0) {
                throw new ParallelException("Loop would run infinitely");
            }
            //check if it is has to be upperbounded
            if (numberOfIterations * step < stop - start) {
                numberOfIterations++;
            }

            iterationValues = new int[numberOfIterations];
            int idx = 0;
            for (int i = start; i < stop; i += step) {
                iterationValues[idx++] = i;
            }

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
        iterationDelegate.iteration(iterationValues[index]);
    }
}
