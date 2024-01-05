/**
 * 
 */
package com.perelens.engine.core;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

import com.perelens.engine.api.CircularDependencyException;
import com.perelens.engine.api.ConsumerResources;
import com.perelens.engine.api.Engine;
import com.perelens.engine.api.EngineExecutionException;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventConsumer;
import com.perelens.engine.api.EventEvaluator;
import com.perelens.engine.api.EventResponder;
import com.perelens.engine.utils.Utils;

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
public class CoreEngine implements Engine {

	//all objects consuming and or producing events in the simulation
	private ConcurrentHashMap<String, SubEntry> simObjects = new ConcurrentHashMap<>();

	//Fields for accumulating all events if necessary.
	private SubEntry globalEntry = new SubEntry(new EventConsumer() {
		@Override
		public String getId() {
			return "global entry";
		}
		@Override
		public void consume(long timeWindow, ConsumerResources resources) {
			throw new IllegalStateException(EngineMsgs.badState());
		}}, this);
	private ConcurrentHashMap<String,EventConsumer> globalConsumers = new ConcurrentHashMap<>();
	
	//Fields for executing the simulation
	private long timeCompleted = 0;
	private ForkJoinPool fjPool;
	private AtomicInteger entriesCompleted = new AtomicInteger(0);
	private ConcurrentHashMap<String,RespEntry> activeResponders = new ConcurrentHashMap<>();

	//Fields for synchronizing the CoreEngine
	private AtomicInteger executionQueueDepth = new AtomicInteger(0);
	private Object windowSignal = new Object();
	
	private ConcurrentLinkedQueue<Throwable> throwables = new ConcurrentLinkedQueue<>();

	public CoreEngine (int parallelism) {
		fjPool = new ForkJoinPool(parallelism,ForkJoinPool.defaultForkJoinWorkerThreadFactory,
				new UncaughtExceptionHandler() {

					@Override
					public void uncaughtException(Thread t, Throwable e) {
						throwables.add(e);
						finishLogic();
					}},
				
				false);
	}
	
	@Override
	public void registerConsumer(EventConsumer consumer) {
		
		Utils.checkNull(consumer);
		Utils.checkId(consumer.getId());

		SubEntry e = simObjects.computeIfAbsent(consumer.getId(), (x) ->{
			if (globalConsumers.containsKey(x)) {
				throw new IllegalArgumentException(EngineMsgs.globalConsumer());
			}else {
				return new SubEntry(consumer, this);
			}
		});

		if (e.getObject() != consumer) {
			throw new IllegalArgumentException(EngineMsgs.duplicateSimObject(e.getObject().getId(), e.getObject().getClass()));
		}
			
	}
	


	@Override
	public void registerResponder(EventResponder responder) {
		Utils.checkNull(responder);
		Utils.checkId(responder.getId());

		SubEntry e = simObjects.computeIfAbsent(responder.getId(), (x) ->{
			if (globalConsumers.containsKey(x)) {
				throw new IllegalArgumentException(EngineMsgs.globalConsumer());
			}else {
				return new RespEntry(responder, this);
			}
		});

		if (e.getObject() != responder) {
			throw new IllegalArgumentException(EngineMsgs.duplicateSimObject(e.getObject().getId(), e.getObject().getClass()));
		}
	}

	@Override
	public void registerEvaluator(EventEvaluator evaluator) {
		Utils.checkNull(evaluator);
		Utils.checkId(evaluator.getId());

		SubEntry e = simObjects.computeIfAbsent(evaluator.getId(), (x) ->{
			if (globalConsumers.containsKey(x)) {
				throw new IllegalArgumentException(EngineMsgs.globalConsumer());
			}else {
				return new EvalEntry(evaluator,this);
			}
		});

		if (e.getObject() != evaluator) {
			throw new IllegalArgumentException(EngineMsgs.duplicateSimObject(e.getObject().getId(), e.getObject().getClass()));
		}
	}

	
	@Override
	public void registerGlobalConsumer(EventConsumer consumer) {
		Utils.checkNull(consumer);
		Utils.checkId(consumer.getId());

		globalConsumers.computeIfAbsent(consumer.getId(), (x) -> {
			if (simObjects.containsKey(x)) {
				throw new IllegalArgumentException(EngineMsgs.nonGlobalConsumer());
			}else {
				return consumer;
			}
		});
	}


	@Override
	public void registerSubscription(String producerId, String consumerId) {
		Utils.checkId(producerId);
		Utils.checkId(consumerId);
		if(producerId.equals(consumerId)) {
			throw new IllegalArgumentException(EngineMsgs.selfSubscription(producerId));
		}

		EvalEntry producer = (EvalEntry) simObjects.get(producerId);
		SubEntry consumer = simObjects.get(consumerId);

		if(producer == null){
			throw new IllegalArgumentException(EngineMsgs.noSuchSimObject(producerId, EventEvaluator.class));
		}

		if (consumer == null) {
			throw new IllegalArgumentException(EngineMsgs.noSuchSimObject(consumerId, EventConsumer.class));
		}

		producer.acquire();
		consumer.acquire();
		try {
			consumer.addDependency(producer);
			producer.addSubscriber(consumer);
		}finally {
			consumer.release();
			producer.release();
		}

	}

	@Override
	public long getTimeCompleted() {
		return timeCompleted;
	}

	@Override
	public void evaluate(long targetOffset) {
		if (targetOffset <= timeCompleted) {
			throw new IllegalArgumentException(EngineMsgs.badOffset(timeCompleted, targetOffset));
		}
		
		if (!throwables.isEmpty()) {
			throw new IllegalStateException(EngineMsgs.engineStateCorrupt());
		}

		//Reset the completion counter
		entriesCompleted.set(0);

		int detachedEntries = 0;

		//prepare all the simulation objects for this round of execution
		ArrayList<SubEntry> toEnqueue = new ArrayList<>();
		
		for (SubEntry e : simObjects.values()) {
			e.acquire();
			try {
				//prepare the entry for the next evaluation cycle
				e.prepareForNextInterval(targetOffset);

				if (e.isDetached()) {
					detachedEntries++;
				}

				if (e.canStartEval()) {
					toEnqueue.add(e);
				}
			}finally {
				e.release();
			}	
		}

		//Submit all the Entries ready for execution
		for (SubEntry e : toEnqueue) {
			enqueue(e,targetOffset);
		}

		//wait for the time window to complete execution
		waitForExecution();

		while (entriesCompleted.get() < simObjects.size() - detachedEntries) {
			//The simulation has halted.  There are two possibilities

			int arSize = activeResponders.size();
			if (arSize > 0) {
				//First, EventEvaluators are waiting for responses from EventResponders
				//At this point evaluate all event responders that have events to process
				while(arSize > 0) {
					ArrayList<RespEntry> te = new ArrayList<RespEntry>(activeResponders.values());
					activeResponders.clear();
					for (RespEntry e : te) {
						enqueue(e,targetOffset);
					}

					waitForExecution();
					arSize = activeResponders.size();
				}
			}else {
				//Second, a circular dependency has been detected and must be resolved

				throw new CircularDependencyException(EngineMsgs.circularDependencyDetected());
			}
		}
		
		//Send events to any global consumers
		int gcSize = globalConsumers.size();
		if (gcSize > 0) {

			//Collect the events
			ArrayList<Event> events;
			globalEntry.acquire();
			try {
				events = new ArrayList<>(globalEntry.getEventCount());
				for (Event e : globalEntry) {
					events.add(e);
				}

				globalEntry.clearEvents();
			}finally {
				globalEntry.release();
			}

			ConsumerResources resources = new ConsumerResources() {

				List<Event> ge = Collections.unmodifiableList(events);
				@Override
				public Iterable<Event> getEvents() {
					return ge;
				}
			};

			//Run each in a separate thread in case they do heavy processing
			for(EventConsumer cur : globalConsumers.values()) {
				Runnable toRun = new Runnable() {
					@Override
					public void run() {
						cur.consume(targetOffset, resources);
						finishLogic();
					}
				};

				executionQueueDepth.incrementAndGet();
				fjPool.execute(toRun);
			}

			//wait for the threads to complete execution
			waitForExecution();

		}

		//Advance the time offset for the simulation
		timeCompleted = targetOffset;
	}
	
	private final void waitForExecution() {
		while(executionQueueDepth.get() > 0) {
			synchronized(windowSignal) {
				try {
					if (executionQueueDepth.get() > 0) {
						windowSignal.wait();
					}
				} catch (InterruptedException e1) {
					throw new IllegalStateException(EngineMsgs.badState());
				}
			}
			
			if (throwables.size() > 0) {
				//throw a runtime exception to kill the simulation
				throw new EngineExecutionException(EngineMsgs.badState(), throwables);
			}
		}
	}
	
	void enqueue(SubEntry e, long targetOffset) {
		executionQueueDepth.incrementAndGet();
		Runnable executor = e.getEvaluator(this, timeCompleted, targetOffset);
		fjPool.execute(executor);
	}
	
	void finishLogic() {
		int depth = executionQueueDepth.decrementAndGet();
		if (depth == 0) {
			synchronized(windowSignal) {
				windowSignal.notifyAll();
			}
		}
	}
	
	void activeResponder(RespEntry e) {
		activeResponders.putIfAbsent(e.getId(), e);
	}
	
	RespEntry getRespEntry(String id) {
		return (RespEntry) simObjects.get(id);
	}
	
	void entryCompleted() {
		entriesCompleted.incrementAndGet();
	}

	boolean isGlobalRegistered() {
		return globalConsumers.size() > 0;
	}
	
	void checkGlobal(Iterable<Event> e) {
		if (globalConsumers.size() > 0) {
			globalEntry.acquire();
			try {
				for (Event cur : e) {
					globalEntry.recieveEvent(cur);
				}
			}finally {
				globalEntry.release();
			}
		}
	}

	@Override
	public void destroy() {
		fjPool.shutdownNow();
	}
}
