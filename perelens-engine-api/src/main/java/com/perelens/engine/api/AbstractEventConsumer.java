/**
 * 
 */
package com.perelens.engine.api;

import java.util.function.Supplier;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License

 * 
 * @author Steve Branda
 *
 */
public abstract class AbstractEventConsumer implements EventConsumer{

	private String id;
	private long lastTime = 0;
	private Event lastEvent = null;
	private long timeWindow = 0;

	//Fields for synchronizing the CoreEngine
	private Object mutex = new Object();
	protected final <T> T execute(Supplier<T> code) {
		synchronized(mutex){
			return code.get();
		}
	}
	
	protected Event getLastEvent() {
		return lastEvent;
	}
	
	protected long getLastTime() {
		return lastTime;
	}
	
	protected long getTimeWindow() {
		return timeWindow;
	}
	
	protected AbstractEventConsumer(String id) {
		this.id = id;
	}
	
	@Override
	public String getId() {
		return id;
	}
	
	protected abstract void processEvent(Event currentEvent);
	
	protected void preInvoke() {};
	
	protected void postInvoke() {};
	
	@Override
	public void consume(long timeWindow, ConsumerResources resources) {
		if (timeWindow < lastTime) {
			throw new IllegalArgumentException("Time must advance");
		}
		
		synchronized(mutex) {	
			
			this.timeWindow = timeWindow;
			
			preInvoke();
			
			//Process any events inside the window
			for (Event curEvent : resources.getEvents()) {
				if (curEvent.getTime() < lastTime){
					throw new IllegalArgumentException("Time must advance");
				}
				if (curEvent.getTime() > timeWindow) {
					throw new IllegalArgumentException("Event should be in future time window");
				}
				
				
				processEvent(curEvent);
					
				
				lastTime = curEvent.getTime();
				lastEvent = curEvent;
			}
			
			postInvoke();
			
			lastTime = timeWindow;
		}
	}

}
