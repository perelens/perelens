/**
 * 
 */
package com.perelens.engine.core;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import com.perelens.engine.api.ConsumerResources;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventSubscriber;
import com.perelens.engine.api.EventType;

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
class SubEntry implements Iterable<Event>,ConsumerResources{
	
	private static VarHandle entryMutex;				//Use a VarHandle to save AtomicBoolean instances
	
	static {
		try {
			entryMutex = MethodHandles.lookup().findVarHandle(SubEntry.class,"mutex",Boolean.TYPE);
		} catch (Exception e) {
			throw new IllegalStateException(EngineMsgs.badState(),e);
		}
	}
	
	private EventSubscriber object;
	
	//Concurrency
	@SuppressWarnings("unused")
	private boolean mutex = false;							//Variable used for CAS concurrency on this entry
	private boolean isProcessing = false;					//Variable used to force EvaluatorLogic to be sequential for this entry
	
	//EventSubscriber functionality
	private SubEntry[] dependencies = CoreUtils.NO_ENTRIES;		//The EventEvaluators that must complete executing before this EventConsumer gets called during the current time window
	private int depIndex = 0;
	private int completeDeps = 0;

	private Event[] queue = com.perelens.engine.utils.Utils.EMPTY_QUEUE;				//Queue of events raised to this EventConsumer
	private boolean qSorted = false;
	private int qIndex = 0;									//Current index inside the queue
	
	protected final CoreEngine engine;

	SubEntry(EventSubscriber object, CoreEngine engine){
		this.object = object;
		this.engine = engine;
	}
	
	//Basic Getters
	EventSubscriber getObject() {
		return object;
	}
	
	String getId() {
		return getObject().getId();
	}
	
	//Dependency Management Methods
	void addDependency(SubEntry toAdd) {
		dependencies = com.perelens.engine.utils.Utils.append(dependencies,toAdd,depIndex);
		depIndex++;
	}
	
	int getDependencyCount() {
		return depIndex;
	}
	
	SubEntry[] getDependencies() {
		if (depIndex < dependencies.length) {
			//Trim for for-each loop and make immutable
			dependencies = Arrays.copyOf(dependencies, depIndex);
		}
		return dependencies;
	}
	
	void prepareForNextInterval(long interval) {
		completeDeps = 0;
	}
	
	boolean incrementAndCheckCompleteDependencies(long interval) {
		completeDeps++;
		if (completeDeps > depIndex) {
			throw new IllegalStateException(EngineMsgs.badState());
		}
		return completeDeps == depIndex;
	}
	
	//Concurrency methods that need to be called before and after accessing this object
	void acquire() {
		while(!entryMutex.compareAndSet(this,false,true));
	}
	
	void release() {
		entryMutex.setRelease(this,false);
	}
	
	boolean getProcessingLock() {
		if (!isProcessing) {
			isProcessing = true;
			return true;
		}else {
			return false;
		}
	}
	
	void releaseProcessingLock() {
		if (isProcessing) {
			isProcessing = false;
		}else {
			throw new IllegalStateException(EngineMsgs.badState());
		}
	}
	
	//Event Subscription Methods
	boolean recieveEvent(Event e) {
		if(object.getEventFilter().filter(e)) {
			queue = com.perelens.engine.utils.Utils.append(queue,e,qIndex);
			qIndex++;
			qSorted = false;
			return true;
		}else {
			return false;
		}
	}
	
	@SuppressWarnings("unchecked")
	Iterator<Event> getEventIterator(){
		if (qIndex > 0 && !qSorted) {
			queue = Arrays.copyOf(queue, qIndex);
			Arrays.sort(queue,CoreUtils.getEventComparator((Comparator<EventType>) object.getEventTypeComparator()));
		}
		return new Iterator<Event>() {
			int index = 0;

			@Override
			public boolean hasNext() {
				return index < qIndex;
			}

			@Override
			public Event next() {
				return queue[index++];
			}
		};
	}
	
	void clearEvents() {
		for (int i = 0; i < qIndex; i++) {
			queue[i] = null;
		}
		qIndex = 0;
	}
	
	int getEventCount() {
		return qIndex;
	}
	
	@Override
	public Iterator<Event> iterator() {
		return getEventIterator();
	}
	
	/**
	 * Returns true if this Entry can be automatically queued at the start of the evaluation window.
	 * 
	 * @return
	 */
	boolean canStartEval() {
		return false;
	}
	
	boolean isDetached() {
		return getDependencyCount() == 0;
	}
	
	//Evaluation Logic Methods
	Runnable getEvaluator(CoreEngine engine, long timeOffset, long targetOffset) {
		return new EventConsumerLogic(this,targetOffset,engine);
	}

	@Override
	public Iterable<Event> getEvents() {
		return this;
	}
}
