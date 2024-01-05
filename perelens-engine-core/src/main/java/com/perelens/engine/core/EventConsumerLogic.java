/**
 * 
 */
package com.perelens.engine.core;

import com.perelens.engine.api.EventConsumer;

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
class EventConsumerLogic implements Runnable{
	private SubEntry teEntry;
	private long targetOffset;
	private EventConsumer teEval;
	private CoreEngine engine;

	EventConsumerLogic(SubEntry te, long to, CoreEngine eng) {
		teEntry = te;
		targetOffset = to;
		teEval = (EventConsumer)te.getObject();
		engine = eng;
	}

	@Override
	public void run() {
		
		for(;;) {
			teEntry.acquire();
			if (teEntry.getProcessingLock()) {
				break;
			}else {
				teEntry.release();
			}
		}
		try {
			teEval.consume(targetOffset, teEntry);
			//Clear out the event queue
			teEntry.clearEvents();
		}finally {
			teEntry.release();
		}
		engine.entryCompleted();
		
		teEntry.acquire();
		teEntry.releaseProcessingLock();
		teEntry.release();
		
		engine.finishLogic();
	}
}
