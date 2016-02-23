// Created on 22.06.2008
package parallel4;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
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
public class Parallel {
	private static int numCores;
	AtomicInteger toGo = new AtomicInteger();
	
	private static ExecutorService corePool;
	private static ExecutorService freePool;

	static {
		//numCores = Runtime.getRuntime().availableProcessors();
		numCores = 16;//1;//4;
		//numCores = Runtime.getRuntime().availableProcessors() - 1;
            corePool = Executors.newFixedThreadPool(numCores);
		
		//TODO 2 pools - freePool = Executors.newCachedThreadPool();
		freePool = Executors.newCachedThreadPool(new ThreadFactory(){

			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "Parallel4-HiPrio-Thread");
				thread.setPriority(Thread.NORM_PRIORITY+1);
				return thread;
			}
			
		});
	}

	@Deprecated
	public void forLoop(int start, int stop, int step,
			final IterationInt iterationExecutor) {
		int count = 0;
		for (int i = start; i < stop; i += step) {
			count++;
			final int idx = i;
			
			corePool.execute(new Runnable() {
				@Override
				public void run() {
					iterationExecutor.iteration(idx);
					int x = toGo.decrementAndGet();
					if (x == 0) {
						synchronized (Parallel.this) {
							Parallel.this.notifyAll();
						}
					}
				}
			});
		}
		toGo.addAndGet(count);
		while (toGo.intValue() != 0) {
			synchronized (this) {
				try {
					wait();
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	public static void executeWithCorePool(Runnable runnable) {
		corePool = Executors.newFixedThreadPool(numCores);
		Logger log = Logger.getLogger("Starting Corepool with "+ numCores + " threads");
		log.log(Level.INFO, "Starting Corepool with {0} threads", numCores);
		for (int i = 0; i < numCores; i ++) {
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
	
	public static void executeWithFreePool(Runnable runnable) {
		freePool.execute(runnable);
	}
	public static void adjustCorePool(int cores)
	{
		numCores  = cores;
//		  corePool = Executors.newFixedThreadPool(cores);
	}
        
        public static int getNumberOfCores() {
            return numCores;
        }
        
        public static void shutdownNow()
        {
        	
        	System.err.println("Shutdown in progress!");
        	corePool.shutdownNow();
        }
}