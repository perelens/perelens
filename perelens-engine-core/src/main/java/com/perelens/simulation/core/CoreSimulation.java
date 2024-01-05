/**
 * 
 */
package com.perelens.simulation.core;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.perelens.engine.api.Engine;
import com.perelens.engine.api.EventConsumer;
import com.perelens.engine.utils.Utils;
import com.perelens.simulation.api.Simulation;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
 * @author Steve Branda
 *
 */
class CoreSimulation implements Simulation {

	private Engine engine;
	private ExecutorService thread;
	private long windowSize = 15_000_000;
	private long timeExecuted = 0;
	private long joinTarget = Long.MAX_VALUE;
	private Object mutex = new Object();
	
	private Status status = Status.PAUSED;
	private Throwable thrown;
	
	protected CoreSimulation(Engine engine) {
		Utils.checkNull(engine);
		this.engine = engine;
		thread = Executors.newSingleThreadExecutor();
	}
	
	@Override
	public void registerGlobalConsumer(EventConsumer consumer) {
		Utils.checkNull(consumer);
		thread.execute(() -> {
			engine.registerGlobalConsumer(consumer);
		});
	}

	@Override
	public void subscribeToEvents(EventConsumer consumer, Collection<String> eventSourceIds) {
		Utils.checkNull(consumer);
		Utils.checkNull(eventSourceIds);
		if (eventSourceIds.size() == 0) {
			return;
		}
		
		thread.execute(()->{
			engine.registerConsumer(consumer);
			for (String id : eventSourceIds) {
				engine.registerSubscription(id, consumer.getId());
			}
		});
	}
	
	@Override
	public void subscribeToEvents(EventConsumer consumer, String eventSourceId) {
		Utils.checkNull(consumer);
		Utils.checkId(eventSourceId);
		
		thread.execute(()->{
			engine.registerConsumer(consumer);
			engine.registerSubscription(eventSourceId, consumer.getId());
		});
	}

	@Override
	public void start(long pauseAfterTime) {
		synchronized(mutex) {
			if (status != Status.PAUSED) {
				throw new IllegalStateException(SimMsgs.simulationNotPaused());
			}
			if (pauseAfterTime > 0) {
				if (pauseAfterTime <= timeExecuted) {
					throw new IllegalArgumentException(SimMsgs.invalidPauseTarget(pauseAfterTime, timeExecuted));
				}else {
					joinTarget = pauseAfterTime;
				}
			}
			
			setStatus(Status.RUNNING);
			thread.execute(simRunner);
		}
	}

	@Override
	public void destroy() {
		synchronized(mutex) {
			if (getStatus() != Status.DESTROYED) {
				setStatus(Status.DESTROYED);
				
				thread.execute(() -> {
					synchronized(mutex) {
						if (getStatus() == Status.DESTROYED) {
							engine.destroy();
							thread.shutdown();
							mutex.notifyAll();
						}
					}
				});
			}
		}
	}
	
	
	private Runnable simRunner = new Runnable() {
		@Override
		public void run() {
			try {
				long timeProcessed = engine.getTimeCompleted();
				long target = timeProcessed + windowSize;

				if (getStatus() == Status.RUNNING) {
					if (target < joinTarget) {
						engine.evaluate(target);
					}else {
						engine.evaluate(joinTarget);
					}
				}

				synchronized (mutex) {
					timeExecuted = engine.getTimeCompleted();

					if (getStatus() == Status.RUNNING) {
						if (joinTarget <= timeExecuted) {
							setStatus(Status.PAUSED);
							joinTarget = Long.MAX_VALUE;
						}else {
							thread.execute(simRunner);
						}
					}

					if (getStatus() == Status.PAUSED) {
						mutex.notifyAll();
					}
				}
			}catch(Throwable t) {
				synchronized(mutex) {
					//Encountered an exception so we need to stop the simulation and throw the exception if a synchronization method is called
					thrown = t;
					destroy();
					mutex.notifyAll();
				}
			}
		}
	};

	@Override
	public void start() {
		start(0);
	}

	@Override
	public void pause(boolean join) throws Throwable {
		synchronized(mutex) {
			checkException(false);
			if (getStatus() != Status.RUNNING) {
				throw new IllegalStateException("Simulation is not RUNNING.");
			}
			
			status = Status.PAUSED;
			if (join) {
				mutex.wait();
			}
			checkException(true);
		}
	}
	
	private void checkException(boolean destroy) throws Throwable{
		synchronized(mutex) {
			if (thrown != null) {
				if (destroy) {
					destroy();
				}
				throw thrown;
			}
		}
	}

	@Override
	public void join() throws Throwable {
		synchronized(mutex) {
			checkException(false);
			if (getStatus() != Status.RUNNING) {
				throw new IllegalStateException(SimMsgs.simulationNotRunning());
			}
			mutex.wait();
			checkException(true);
		}
	}

	@Override
	public Status getStatus() {
		synchronized(mutex) {
			return status;
		}
	}
	
	private void setStatus(Status s) {
		Utils.checkNull(s);
		synchronized(mutex) {
			status = s;
		}
	}

	@Override
	public long getTimeCompleted() {
		synchronized(mutex) {
			return timeExecuted;
		}
	}

	@Override
	public long setWindowSize(long windowSize) {
		synchronized(mutex) {
			if (windowSize < 1) {
				throw new IllegalArgumentException(SimMsgs.windowSize());
			}
			long toReturn = windowSize;
			this.windowSize = windowSize;
			return toReturn;
		}
	}
	
}
