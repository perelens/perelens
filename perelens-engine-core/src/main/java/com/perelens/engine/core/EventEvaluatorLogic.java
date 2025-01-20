/**
 * 
 */
package com.perelens.engine.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.perelens.engine.api.EvaluatorResources;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventEvaluator;
import com.perelens.engine.core.EventResponderLogic.RespResourceImpl;
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
class EventEvaluatorLogic implements Runnable {
	
	private EvalEntry teEntry;
	private long timeOffset;
	private long targetOffset;
	private EventEvaluator teEval;
	private CoreEngine engine;

	protected EventEvaluatorLogic(EvalEntry te, long timeOffset, long targetOffset, CoreEngine eng) {
		teEntry = te;
		this.timeOffset = timeOffset;
		this.targetOffset = targetOffset;
		teEval =  (EventEvaluator) te.getObject();
		this.engine = eng;
	}

	@Override
	public void run() {
		EvalResourceImpl resources = new EvalResourceImpl(teEntry,timeOffset,targetOffset);
		boolean isComplete = false;
		SubEntry[] subs;
		
		//Enter the critical section
		for(;;) {
			teEntry.acquire();
			if (teEntry.getProcessingLock()) {
				break;
			}else {
				teEntry.release();
			}
		}
		try {
			teEval.consume(targetOffset, resources);

			//Clear out the event queue
			teEntry.clearEvents();

			//See if the entry can be marked completed
			if (!teEntry.needsResponse()) {
				teEntry.markComplete();
				isComplete = true;
			}

			//Get the subscriber list so events can be raised to the subscribers
			subs = teEntry.getSubscribers();
		}finally {
			teEntry.release();
		}

		//Dispatch subscriber events
		int subEventCount = resources.getSubEventCount();
		
		//Dispatch direct responses
		Map<String, Event[]> responses = resources.getResponses();
		
		List<Event> allEvents = null;
		boolean globalReg = engine.isGlobalRegistered();
		if (globalReg) {
			allEvents = new ArrayList<>();
		}
		
		//Only getting one lock at a time so we do not need to order lock acquisition
		Event[] subEvents = com.perelens.engine.utils.Utils.EMPTY_QUEUE;
		if (subEventCount > 0) {
			subEvents = resources.getSubscriberEvents();
			if (globalReg) {
				for(int i = 0;i < subEventCount;i++) {
					allEvents.add(subEvents[i]);
				}
			}
		}

		for (SubEntry curSub : subs) {
			//Enter the critical section.
			curSub.acquire();
			try {
				boolean enqueue = false;
				
				//Dispatch subscriber events
				for(int i = 0;i < subEventCount;i++) {
					curSub.recieveEvent(subEvents[i]);
				}

				if (isComplete) {
					//The subscriber has not been evaluated yet, and this entry that it is dependent on
					//is complete
					enqueue = curSub.incrementAndCheckCompleteDependencies(targetOffset);
				}
				
				//Dispatch any responses to this subscriber
				Event[] rEvents = responses.remove(curSub.getId());
				if (rEvents != null) {
					RespEntry curSubRes = ((RespEntry) curSub);
					boolean needsResponseBefore = false;
					boolean needsResponseAfter = false;
					
					needsResponseBefore = curSubRes.needsResponse();
					if (rEvents != null) {
						for (int i = 0; i < rEvents.length; i += 2) {
							Event resp = rEvents[i];
							Event respTo = rEvents[i+1];
							curSubRes.recieveResponse(resp, respTo);
							if(globalReg) {
								allEvents.add(resp);
							}
						}
					}
					needsResponseAfter = curSubRes.needsResponse();
					
					if (needsResponseBefore && !needsResponseAfter) {
						enqueue = true;
					}
				}
				
				if (enqueue) {
					engine.enqueue(curSub, targetOffset);
				}
			}finally {
				curSub.release();
			}
		}
		
		//Dispatch any remaining responses
		if (responses.size() > 0) {
			EventResponderLogic.processResponses(responses, targetOffset, allEvents, engine);
		}
		
		if (globalReg) {
			engine.checkGlobal(allEvents); //Hook for global consumer
		}
		
		if (isComplete) {
			engine.entryCompleted();
		}
		
		teEntry.acquire();
		teEntry.releaseProcessingLock();
		teEntry.release();
		
		engine.finishLogic();
	}

	static class EvalResourceImpl extends RespResourceImpl implements EvaluatorResources{

		private Event[] subEvents = com.perelens.engine.utils.Utils.EMPTY_QUEUE;
		private int sIndex = 0;
		
		EvalResourceImpl(EvalEntry te, long timeOffset, long targetOffset) {
			super(te, timeOffset, targetOffset);
		}

		@Override
		public void raiseEvent(Event toRaise) {
			Utils.checkNull(toRaise);
			checkTime(toRaise);
			checkId(toRaise);
			
			if (subEvents.length == 0) {
				subEvents = new Event[4];
			}else if (sIndex == subEvents.length) {
				subEvents = Arrays.copyOf(subEvents, subEvents.length + subEvents.length);
			}
			subEvents[sIndex] = toRaise;
			sIndex++;
			if (!toRaise.getResponseTypes().isEmpty()) {
				getToEval().waitForResponse(toRaise);
			}
		}
		
		private Event[] getSubscriberEvents() {
			return subEvents;
		}

		private int getSubEventCount() {
			return sIndex;
		}
	}
}
