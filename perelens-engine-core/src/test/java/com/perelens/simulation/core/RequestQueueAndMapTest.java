/**
 * 
 */
package com.perelens.simulation.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.perelens.engine.TestEvent;
import com.perelens.engine.TestEventType;
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
class RequestQueueAndMapTest {
	
	protected RequestQueueAndMap newRequestQueueAndMap() {
		return new RequestQueueAndMap("id1") {

			@Override
			public EventGenerator copy() {
				throw new IllegalStateException();
			}

			@Override
			protected void process(Event curEvent) {
				throw new IllegalStateException();
			}
			
		};
	}

	/**
	 * Test method for {@link com.perelens.simulation.core.RequestQueueAndMap#r_enqueue(long)}.
	 */
	@Test
	void testR_enqueue_dequeue_peek_size() {
		
		RequestQueueAndMap rq = newRequestQueueAndMap();
		assertEquals(0, rq.r_size());
		
		for (int i = 0; i < 100; i++) {
			rq.r_enqueue(Integer.toString(i));
		}
		
		assertEquals(100,rq.r_size());
		
		for (int i = 0; i < 100; i++) {
			assertEquals(Integer.toString(i),rq.r_peek());
			assertEquals(Integer.toString(i),rq.r_dequeue());
		}
		
		assertEquals(0, rq.r_size());
		
		try {
			rq.r_dequeue();
			fail("Should have generated exception");
		}catch(IllegalStateException e) {
			
		}
	}

	@Test
	void testIterateQueue() {
		RequestQueueAndMap rq = newRequestQueueAndMap();
		assertEquals(0, rq.r_size());
		
		for (int i = 0; i < 100; i++) {
			rq.r_enqueue(Integer.toString(i));
		}
		
		assertEquals(100,rq.r_size());
		
		int size = rq.r_size();
		for (int i = 0; i < size; i++) {
			String val = rq.r_dequeue();
			assertEquals(Integer.toString(i),val);
			rq.r_enqueue(val);
		}
		
		assertEquals(100,rq.r_size());
		
		for (int i = 0; i < 100; i++) {
			assertEquals(Integer.toString(i),rq.r_peek());
			assertEquals(Integer.toString(i),rq.r_dequeue());
		}
		
		assertEquals(0, rq.r_size());
	}
	
	@Test
	void testMap_put_get_remove_size() {
		RequestQueueAndMap rq = newRequestQueueAndMap();
		assertEquals(0,rq.m_size());
		
		for (int i = 1; i < 100; i++) {
			rq.m_put("k"+i, new TestEvent("p1",TestEventType.TE_EVENT1,i,i));
		}
		
		assertEquals(99,rq.m_size());
		
		for (int i = 1; i < 100; i++) {
			Event ev = rq.m_get("k" + i);
			assertEquals(i,ev.getTime());
		}
		
		for (int i = 1; i < 10; i++) {
			Event ev = rq.m_remove("k" + i);
			assertEquals(i,ev.getTime());
		}
		
		assertEquals(90,rq.m_size());
		
		for (int i = 1; i < 100; i++) {
			Event ev = rq.m_get("k" + i);
			if (i < 10) {
				assertNull(ev);
			}else {
				assertEquals(i,ev.getTime());
			}
		}
		
		for (int i = 100; i < 150; i++) {
			rq.m_put("k"+i, new TestEvent("p1",TestEventType.TE_EVENT1,i,i));
		}
		
		assertEquals(140,rq.m_size());
		
		for (int i = 10; i < 200; i++) {
			Event ev = rq.m_get("k" + i);
			if (i  < 150) {
				assertEquals(i,ev.getTime());
			}else {
				assertNull(ev);
			}
		}
	}
	
	@Test
	void testM_iterator() {
		RequestQueueAndMap rq = newRequestQueueAndMap();
		assertEquals(0,rq.m_size());
		
		for (int i = 1; i < 100; i++) {
			rq.m_put(""+i, new TestEvent("p1",TestEventType.TE_EVENT1,i,i));
		}
		
		int count = 0;
		for(Iterator<Map.Entry<String,Event>> iter = rq.m_iterator(); iter.hasNext();) {
			Map.Entry<String, Event> e = iter.next();
			int i = Integer.parseInt(e.getKey());
			Event val = e.getValue();
			assertEquals(i,val.getTime());
			count++;
		}
		
		assertEquals(99,count);
		
		count = 0;
		for(Iterator<Map.Entry<String,Event>> iter = rq.m_iterator(); iter.hasNext();) {
			Map.Entry<String, Event> e = iter.next();
			int i = Integer.parseInt(e.getKey());
			Event val = e.getValue();
			assertEquals(i,val.getTime());
			count++;
		}
		
		assertEquals(99,count);
		
		count=0;
		for(Iterator<Map.Entry<String,Event>> iter = rq.m_iterator(); iter.hasNext();) {
			Map.Entry<String, Event> e = iter.next();
			int i = Integer.parseInt(e.getKey());
			Event val = e.setValue(null);
			assertEquals(i,val.getTime());
			count++;
		}
		
		assertEquals(99,count);
		assertEquals(0,rq.m_size());
	}
	
	@Test
	void testSyncInternalStateRequestQueueAndMap() {
		RequestQueueAndMap rq = newRequestQueueAndMap();

		for (int i = 1; i < 100; i++) {
			rq.m_put("k"+i, new TestEvent("p1",TestEventType.TE_EVENT1,i,i));
		}
		
		assertEquals(0, rq.r_size());
		
		for (int i = 0; i < 100; i++) {
			rq.r_enqueue(Integer.toString(i));
		}
		
		assertEquals(100,rq.r_size());
		
		RequestQueueAndMap rq2 = newRequestQueueAndMap();
		assertEquals(0,rq2.r_size());
		assertNull(rq2.m_get("k1"));
		
		rq.syncInternalState(rq2);
		
		assertEquals(100,rq2.r_size());
		assertNotNull(rq2.m_get("k1"));
		
		for (int i = 1; i < 100; i++) {
			Event ev = rq.m_get("k" + i);
			assertEquals(i,ev.getTime());
		}
		
		for (int i = 1; i < 10; i++) {
			Event ev = rq.m_remove("k" + i);
			assertEquals(i,ev.getTime());
		}
		
		assertNull(rq.m_get("k1"));
		assertNotNull(rq2.m_get("k1"));
		
		for (int i = 1; i < 100; i++) {
			Event ev = rq2.m_get("k" + i);
			assertEquals(i,ev.getTime());
		}
		
		for (int i = 1; i < 10; i++) {
			Event ev = rq2.m_remove("k" + i);
			assertEquals(i,ev.getTime());
		}
		
		assertNull(rq2.m_get("k1"));
		
		for (int i = 0; i < 100; i++) {
			assertEquals(Integer.toString(i),rq.r_peek());
			assertEquals(Integer.toString(i),rq.r_dequeue());
		}
		
		assertEquals(0,rq.r_size());
		assertEquals(100,rq2.r_size());
		
		for (int i = 0; i < 100; i++) {
			assertEquals(Integer.toString(i),rq2.r_peek());
			assertEquals(Integer.toString(i),rq2.r_dequeue());
		}
		
	}

}
