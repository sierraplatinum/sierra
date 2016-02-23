// Created on 22.06.2008
package parallel4;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * (C) Copyright 2008 Markus Junginger.
 * (C) Copyright 2013 Daniel Gerighausen
 *
 * @author Markus Junginger, Daniel Gerighausen
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
//TODO infite loop (explicit break only)
public class Parallel2 {

    private int numberOfCores = 1;

    private ExecutorService corePool;

    /**
     * Constructor.
     *
     * @param numberOfCores number of cores to use
     */
    public Parallel2(int numberOfCores) {
        this.numberOfCores = numberOfCores;
    }

    /**
     * Execute Runnable.
     *
     * @param runnable method to execute
     */
    public void execute(Runnable runnable) {
        Logger log = Logger.getLogger("Starting Corepool with " + numberOfCores + " threads");
        log.log(Level.INFO, "Starting Corepool with {0} threads", numberOfCores);

        corePool = Executors.newFixedThreadPool(numberOfCores);
        for (int i = 0; i < numberOfCores; i++) {
            corePool.execute(runnable);
        }
        corePool.shutdown();
        try {
            corePool.awaitTermination(10, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Set number of cores to use.
     *
     * @param numberOfCores number of cores to use
     */
    public void setNumberOfCores(int numberOfCores) {
        this.numberOfCores = numberOfCores;
    }

    /**
     * Get number of cores to use.
     *
     * @return number of cores to use
     */
    public int getNumberOfCores() {
        return numberOfCores;
    }

    /**
     * shutdown core pool
     */
    public void shutdownNow()
    {
    	System.err.println("Shutdown 2 in progress!");
    	System.err.println("__________________________");
    	corePool.shutdownNow();
    }

}
