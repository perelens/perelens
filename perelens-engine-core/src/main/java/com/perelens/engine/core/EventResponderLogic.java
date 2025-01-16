/**
 * 
 */
package com.perelens.engine.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventResponder;
import com.perelens.engine.api.ResponderResources;
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
class EventResponderLogic implements Runnable{
	private RespEntry teEntry;
	private long timeOffset;
	private long targetOffset;
	private EventResponder teEval;
	private CoreEngine engine;

	protected EventResponderLogic(RespEntry te, long timeOffset, long targetOffset, CoreEngine eng) {
		teEntry = te;
		this.timeOffset = timeOffset;
		this.targetOffset = targetOffset;
		teEval = (EventResponder) te.getObject();
		this.engine = eng;
	}

	@Override
	public void run() {
		RespResourceImpl resources = createResourceImpl(teEntry,timeOffset,targetOffset);
		boolean complete = false;
		
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
			if (!teEntry.needsResponse() && teEntry.dependenciesAreComplete()) {
				teEntry.markComplete();
				complete = true;
			}
		}finally {
			teEntry.release();
		}

		//Dispatch responses
		List<Event> allResp = null;
		boolean globalReg = engine.isGlobalRegistered();
		if (globalReg) {
			allResp = new ArrayList<>();
		}
		processResponses(resources.getResponses(),targetOffset,allResp,engine);
		
		//Hook for global consumer
		if (globalReg) {
			engine.checkGlobal(allResp); 
		}
		
		if (complete) {
			engine.entryCompleted();
		}
		
		teEntry.acquire();
		teEntry.releaseProcessingLock();
		teEntry.release();
		
		engine.finishLogic();
	}
	
	static void processResponses(Map<String,Event[]> responses, long targetOffset, List<Event> globalRegList, CoreEngine engine) {
		boolean globalReg = globalRegList != null;
		
		for (Map.Entry<String,Event[]> e : responses.entrySet()) {
			String id = e.getKey();
			RespEntry target = engine.getRespEntry(id);

			Event[] rEvents = e.getValue();
			boolean needsResponseBefore = false;
			boolean needsResponseAfter = false;
			target.acquire();
			try {
				needsResponseBefore = target.needsResponse();
				if (rEvents != null) {
					for (int i = 0; i < rEvents.length; i += 2) {
						Event resp = rEvents[i];
						Event respTo = rEvents[i+1];
						target.recieveResponse(resp, respTo);
						if (globalReg) {
							globalRegList.add(resp);
						}
					}
				}
				needsResponseAfter = target.needsResponse();
			}finally {
				target.release();
			}

			if (needsResponseBefore && !needsResponseAfter) {
				//Scenario one - a previously processed EventEvaluator receives the response it has been waiting for.
				//It needs to be invoked again during the same time window
				engine.enqueue(target,targetOffset);
			}
		}
	}
	
	protected RespResourceImpl createResourceImpl(RespEntry te,long timeOffset, long targetOffset) {
		return new RespResourceImpl(te,timeOffset,targetOffset);
	}
	
	static class RespResourceImpl implements ResponderResources{

		private RespEntry toEval;
		private long timeOffset;
		private long targetOffset;
		
		HashMap<String,Event[]> resEvents = null;
		
		
		RespResourceImpl(RespEntry te,long timeOffset, long targetOffset) {
			this.timeOffset = timeOffset;
			this.targetOffset = targetOffset;
			toEval = te;
		}

		@Override
		public void raiseResponse(Event toRaise, Event inResponseTo) {
			Utils.checkNull(toRaise);
			Utils.checkNull(inResponseTo);
			checkId(toRaise);
			checkTime(toRaise);
			//Response cannot have a time offset before the event that triggered it
			if (toRaise.getTime() < inResponseTo.getTime()) {
				throw new IllegalArgumentException(EngineMsgs.outsideTimeWindow(toRaise.getTime(), inResponseTo.getTime(), targetOffset));
			}
			//Response should have the proper type
			if (!inResponseTo.getResponseTypes().contains(toRaise.getType())) {
				throw new IllegalArgumentException(EngineMsgs.badResponseType(inResponseTo.getResponseTypes(), toRaise.getType()));
			}

			String target = inResponseTo.getProducerId();

			if (resEvents == null) {
				resEvents = new HashMap<>();
				resEvents.put(target, new Event[] {toRaise,inResponseTo});
			}else {
				Event[] events = resEvents.get(target);
				if (events == null) {
					resEvents.put(target, new Event[] {toRaise,inResponseTo});
				}else {
					events = Arrays.copyOf(events, events.length+2);
					events[events.length -2] = toRaise;
					events[events.length -1] = inResponseTo;
					resEvents.put(target, events);
				}
			}
			
			//wait for response if necessary
			if (!toRaise.getResponseTypes().isEmpty()) {
				toEval.waitForResponse(toRaise);
			}
		}

		Map<String,Event[]> getResponses(){
			if (resEvents == null) {
				return Collections.emptyMap();
			}else {
				return resEvents;
			}
		}
		
		protected RespEntry getToEval() {
			return toEval;
		}

		void checkTime(Event e) {
			long time = e.getTime();
			if (time <= timeOffset || time > targetOffset) {
				throw new IllegalArgumentException(EngineMsgs.outsideTimeWindow(time, timeOffset, targetOffset));
			}
		}
		
		void checkId(Event e) {
			if (!toEval.getId().equals(e.getProducerId())) {
				throw new IllegalArgumentException(EngineMsgs.noEventSpoofing(toEval.getId(),e.getProducerId()));
			}
		}

		@Override
		public Iterable<Event> getEvents() {
			return toEval;
		}

		@Override
		public void keepActive() {
			toEval.registerAsActive();
		}
	}
}
