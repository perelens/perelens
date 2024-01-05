/**
 * 
 */
package com.perelens.engine.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;


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
class TimeCallbackQueueTest {
	
	protected TimeCallbackQueue newTimeCallbackQueue() {
		return new TimeCallbackQueue();
	}

	/**
	 * Test method for {@link com.perelens.engine.core.TimeCallbackQueue#tc_enqueue(long)}.
	 */
	@Test
	void testTc_enqueue_dequeue_hasMore() {
		TimeCallbackQueue tc = newTimeCallbackQueue();
		
		assertFalse(tc.tc_hasMore());
		
		ArrayList<Long> values = new ArrayList<>();
		values.add(90L);
		values.add(30L);
		values.add(5L);
		values.add(10L);
		values.add(90L);
		values.add(49L);
		values.add(100L);
		values.add(14L);
		values.add(89L);
		values.add(1000L);
		values.add(500L);
		
		for (Long val : values) {
			tc.tc_enqueue(val);
		}
		
		assertTrue(tc.tc_hasMore());
		
		long last = 0;
		
		while(tc.tc_hasMore()) {
			long cur = tc.tc_dequeue();
			assert(cur >= last );
			assertTrue(values.contains(cur));
			assertTrue(values.remove(cur));
			last = cur;
		}
		
		assertEquals(0,values.size());
		assertFalse(tc.tc_hasMore());
	}

	/**
	 * Test method for {@link com.perelens.engine.core.TimeCallbackQueue#tc_peek()}.
	 */
	@Test
	void testTc_peek() {
		TimeCallbackQueue tc = newTimeCallbackQueue();
		
		assertFalse(tc.tc_hasMore());
		
		assertEquals(-1, tc.tc_peek());
		
		tc.tc_enqueue(10);
		assertEquals(10,tc.tc_peek());
		assertEquals(10,tc.tc_peek());
		
		tc.tc_enqueue(9);
		assertEquals(9,tc.tc_peek());
		assertEquals(9,tc.tc_peek());
		
		tc.tc_enqueue(12);
		assertEquals(9,tc.tc_peek());
		assertEquals(9,tc.tc_peek());
		
		tc.tc_dequeue();
		assertEquals(10,tc.tc_peek());
		assertEquals(10,tc.tc_peek());
		
		tc.tc_dequeue();
		assertEquals(12,tc.tc_peek());
		assertEquals(12,tc.tc_peek());
		
		tc.tc_dequeue();
		assertEquals(-1, tc.tc_peek());
		
		assertFalse(tc.tc_hasMore());
		
	}

	/**
	 * Test method for {@link com.perelens.engine.core.TimeCallbackQueue#syncInternalState(com.perelens.engine.core.TimeCallbackQueue)}.
	 */
	@Test
	void tc_testSyncInternalState() {
		TimeCallbackQueue tc = newTimeCallbackQueue();
		
		assertFalse(tc.tc_hasMore());
		
		ArrayList<Long> values = new ArrayList<>();
		values.add(90L);
		values.add(30L);
		values.add(5L);
		values.add(10L);
		values.add(90L);
		values.add(49L);
		values.add(100L);
		values.add(14L);
		values.add(89L);
		values.add(1000L);
		values.add(500L);
		
		for (Long val : values) {
			tc.tc_enqueue(val);
		}
		
		assertTrue(tc.tc_hasMore());
		
		TimeCallbackQueue tc2 = newTimeCallbackQueue();
		
		tc.syncInternalState(tc2);
		
		assertEquals(tc.tc_peek(),tc2.tc_peek());
		
		tc.tc_dequeue();
		
		assertFalse(tc.tc_peek() == tc2.tc_peek());
		
		tc2.tc_dequeue();
		
		tc.tc_enqueue(1);
		tc2.tc_enqueue(2);
		
		assertFalse(tc.tc_peek() == tc2.tc_peek());
		assertFalse(tc.tc_dequeue() == tc2.tc_dequeue());
		
		while(tc.tc_hasMore() || tc2.tc_hasMore()) {
			long val1 = tc.tc_peek();
			long val2 = tc2.tc_peek();
			assertEquals(val1,val2);
			
			val1 = tc.tc_dequeue();
			val2 = tc2.tc_dequeue();
			assertEquals(val1,val2);
		}
		
		assertFalse(tc.tc_hasMore());
		assertFalse(tc2.tc_hasMore());
		
		tc2.tc_enqueue(3);;
		
		assertFalse(tc.tc_hasMore());
		assertTrue(tc2.tc_hasMore());
	}

}
