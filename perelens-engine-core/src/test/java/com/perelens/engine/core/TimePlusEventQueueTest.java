package com.perelens.engine.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.perelens.engine.TestEvent;
import com.perelens.engine.TestEventType;
import com.perelens.engine.api.Event;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
 * 
 */
class TimePlusEventQueueTest extends TimeCallbackQueueTest{

	
	@Override
	protected TimeQueue newTimeCallbackQueue() {
		return new TimePlusEventQueue();
	}
	
	protected TimePlusEventQueue newTimePlusEventQueue() {
		TimePlusEventQueue toReturn = new TimePlusEventQueue();
		toReturn.setComparator(CoreUtils.EVENT_COMPARATOR);
		
		return toReturn;
	}

	@Test
	void testEv_enqueue_dequeue_peek_hasMore() {
		
		ArrayList<Event> events = new ArrayList<>();
		
		events.add(new TestEvent("p",TestEventType.TE_EVENT1,9,2));
		events.add(new TestEvent("p",TestEventType.TE_EVENT1,74,7));
		events.add(new TestEvent("p",TestEventType.TE_EVENT1,27,5));
		events.add(new TestEvent("p",TestEventType.TE_EVENT1,1,1));
		events.add(new TestEvent("p",TestEventType.TE_EVENT1,60,6));
		events.add(new TestEvent("p",TestEventType.TE_EVENT1,15,3));
		events.add(new TestEvent("p",TestEventType.TE_EVENT1,86,8));
		events.add(new TestEvent("p",TestEventType.TE_EVENT2,27,4));
		
		TimePlusEventQueue tq = newTimePlusEventQueue();
		assertFalse(tq.ev_hasMore());
		
		for (Event e : events) {
			tq.ev_enqueue(e);
			assertTrue(tq.ev_hasMore());
		}
		
		long lastTime = 0;
		long lastOrd = 0;
		
		while (tq.ev_hasMore()) {
			Event evp = tq.ev_peek();
			Event ev = tq.ev_dequeue();
			assertEquals(evp, ev);
			assertTrue(lastTime <= ev.getTime());
			assertTrue(lastOrd < ev.getOrdinal());
			assertTrue(events.remove(ev));
			
			lastTime = ev.getTime();
			lastOrd = ev.getOrdinal();
		}
		
		assertFalse(tq.ev_hasMore());
		assertEquals(0, events.size());
	}

	@Test
	void testSyncInternalStateTimePlusEventQueue() {
		ArrayList<Event> events = new ArrayList<>();
		
		events.add(new TestEvent("p",TestEventType.TE_EVENT1,9,2));
		events.add(new TestEvent("p",TestEventType.TE_EVENT1,74,7));
		events.add(new TestEvent("p",TestEventType.TE_EVENT1,27,5));
		events.add(new TestEvent("p",TestEventType.TE_EVENT1,1,1));
		events.add(new TestEvent("p",TestEventType.TE_EVENT1,60,6));
		events.add(new TestEvent("p",TestEventType.TE_EVENT1,15,3));
		events.add(new TestEvent("p",TestEventType.TE_EVENT1,86,8));
		events.add(new TestEvent("p",TestEventType.TE_EVENT2,27,4));
		
		TimePlusEventQueue tq = newTimePlusEventQueue();
		assertFalse(tq.ev_hasMore());
		
		for (Event e : events) {
			tq.ev_enqueue(e);
			assertTrue(tq.ev_hasMore());
		}
		
		TimePlusEventQueue tq2 = newTimePlusEventQueue();
		assertFalse(tq2.ev_hasMore());
		
		tq.syncInternalState(tq2);
		assertTrue(tq2.ev_hasMore());
		
		assertEquals(tq.ev_peek(),tq2.ev_peek());
		
		tq.ev_dequeue();
		
		assertFalse(tq.ev_peek() == tq2.ev_peek());
		
		tq2.ev_dequeue();
		
		while (tq.ev_hasMore() || tq2.ev_hasMore()) {
			assertEquals(tq.ev_dequeue(),tq2.ev_dequeue());
		}
		
		tq2.ev_enqueue(new TestEvent("p",TestEventType.TE_EVENT1,1,1));
		
		assertFalse(tq.ev_hasMore());
		assertTrue(tq2.ev_hasMore());
		
	}

}
