/**
 * 
 */
package com.perelens.engine.core;

import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventGenerator;

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
public class RespEntry extends SubEntry {

	//Marked true when this EventConsumer is finished executing during the current time window
	private boolean complete = false;	
	private boolean registered = false;
	
	//Events requiring response before this EventEvaluator can finish executing for the given time window
	private Event[] needResponse = com.perelens.engine.utils.Utils.EMPTY_QUEUE;		
	private int rIndex = 0;                         								//Current index in needResponse list
	
	RespEntry(EventGenerator object, CoreEngine engine) {
		super(object, engine);
	}
	
	//Response handling methods
	boolean needsResponse() {
		return rIndex > 0;
	}
	
	void recieveResponse(Event response, Event inResponseTo) {
		boolean found = false;
		for (int i = 0; i < rIndex; i++) {
			if (found) {
				needResponse[i-1] = needResponse[i];
			}else {
				Event cur = needResponse[i];
				if (cur == inResponseTo) {
					found = true;
					needResponse[i] = null;
				}
			}
		}
		
		if (found) {
			rIndex--;
			needResponse[rIndex] = null;
			recieveEvent(response);
		}else {
			throw new IllegalStateException(EngineMsgs.badState());
		}
	}

	void waitForResponse(Event e) {
		needResponse = com.perelens.engine.utils.Utils.append(needResponse,e,rIndex);
		rIndex++;
	}
	
	@Override
	void prepareForNextInterval(long interval) {
		super.prepareForNextInterval(interval);
		complete = false;
	}

	//Time interval processing methods
	boolean isComplete() {
		return complete;
	}

	void markComplete() {
		if (!complete) {
			complete = true;
		}else {
			throw new IllegalStateException();
		}
	}
	
	//Evaluation Logic Methods
	@Override
	Runnable getEvaluator(CoreEngine engine, long timeOffset, long targetOffset) {
		return new EventResponderLogic(this,timeOffset,targetOffset,engine);
	}

	@Override
	boolean recieveEvent(Event e) {
		if (super.recieveEvent(e)) {
			if (!e.getResponseTypes().isEmpty()) {
				registerAsActive();
			}
			return true;
		}else {
			return false;
		}
	}
	
	void registerAsActive() {
		registered = true;
	}
	
	void deregisterAsActive() {
		registered = false;
	}
	
	boolean isActive() {
		return registered;
	}
}
