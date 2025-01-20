/**
 * 
 */
package com.perelens.simulation.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import com.perelens.engine.TestResources;
import com.perelens.engine.api.Event;
import com.perelens.engine.core.TimePlusEventQueue;
import com.perelens.engine.core.TimePlusEventQueueTest;
import com.perelens.simulation.events.ResourcePoolEvent;

/**
 * Copyright 2025 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
   
 * @author Steve Branda
 *
 */
class CoreTimeOptimizedResourcePoolTest extends TimePlusEventQueueTest{

	@Override
	protected TimePlusEventQueue newTimeCallbackQueue() {
		return new CoreTimeOptimizedResourcePool("pool1",1);
	}
	
	long ordinal = 1;
	
	@Test
	void simpleResourceRequest() {
		var pool = new CoreTimeOptimizedResourcePool("pool1",2);
		
		//Send a request event to the resource pool
		var reqEvent = new ResPoolEvent("pro1",ResourcePoolEvent.RP_REQUEST,50,ordinal++);
		reqEvent.setTimeOptimization(100);
		TestResources tr = new TestResources(Arrays.asList(new Event[] {reqEvent}));
		
		pool.consume(100, tr);
		
		//There should be one raised GRANTED responses
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(1, tr.getRaisedResponses().size());
				
		Iterator<TestResources.ResponseEntry> raised = tr.getRaisedResponses().iterator();
		TestResources.ResponseEntry re = raised.next();
		Event raisedEvent = re.getResponse();
		assertFalse(raised.hasNext());
		assertEquals(reqEvent,re.getInResponseTo());
		assertEquals(ResourcePoolEvent.RP_GRANT,raisedEvent.getType());
		assertEquals(ResourcePoolEvent.GRANT_RESPONSE_TYPES,raisedEvent.getResponseTypes());
		assertEquals("pool1",raisedEvent.getProducerId());
		assertEquals(50,raisedEvent.getTime());
		assertEquals(50, raisedEvent.getTimeOptimization());
	}
	
	@Test
	void testExhaustingResources() {
		int resLimit = 2;
		var pool = new CoreTimeOptimizedResourcePool("pool1",resLimit);
		
		//Send enough resource requests to exhaust the pool
		var toRaise = new ArrayList<Event>();
		for (int i = 0; i < resLimit; i++) {
			var reqEvent = new ResPoolEvent("pro1",ResourcePoolEvent.RP_REQUEST,50 + i,ordinal++);
			reqEvent.setTimeOptimization(100);
			toRaise.add(reqEvent);
			
		}
		TestResources tr = new TestResources(toRaise);
		pool.consume(100, tr);
		
		//There should be resLimit raised GRANTED responses
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(resLimit, tr.getRaisedResponses().size());
				
		int j = 0;
		for (var re : tr.getRaisedResponses()) {
			Event raisedEvent = re.getResponse();
			assertEquals(toRaise.get(j),re.getInResponseTo());
			assertEquals(ResourcePoolEvent.RP_GRANT,raisedEvent.getType());
			assertEquals(ResourcePoolEvent.GRANT_RESPONSE_TYPES,raisedEvent.getResponseTypes());
			assertEquals("pool1",raisedEvent.getProducerId());
			assertEquals(50 + j, raisedEvent.getTime());
			assertEquals(50 + j, raisedEvent.getTimeOptimization());
			j++;
		}
		assertEquals(j,resLimit);
		
		//All resources should become available again 100 cycles after the RP_GRANT event.
		//These RP_GRANT events should come back with timeOptimization values that indicate they cannot be used until some point in the future
		toRaise = new ArrayList<Event>();
		for (int i = 0; i < resLimit; i++) {
			var reqEvent = new ResPoolEvent("pro1",ResourcePoolEvent.RP_REQUEST,100 + i,ordinal++);
			reqEvent.setTimeOptimization(100);
			toRaise.add(reqEvent);
		}
		
		tr = new TestResources(toRaise);
		pool.consume(120, tr);
		
		//There should be resLimit raised GRANTED responses
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(resLimit, tr.getRaisedResponses().size());
				
		j = 0;
		for (var re : tr.getRaisedResponses()) {
			Event raisedEvent = re.getResponse();
			assertEquals(toRaise.get(j),re.getInResponseTo());
			assertEquals(ResourcePoolEvent.RP_GRANT,raisedEvent.getType());
			assertEquals(ResourcePoolEvent.GRANT_RESPONSE_TYPES,raisedEvent.getResponseTypes());
			assertEquals("pool1",raisedEvent.getProducerId());
			assertEquals(100 + j, raisedEvent.getTime());
			assertEquals(150 + j, raisedEvent.getTimeOptimization());
			j++;
		}
		assertEquals(j,resLimit);
		
		//All resources should become available again starting at 250 cycles.
		//Make sure these requests return immediately usable RP_GRANT events
		toRaise = new ArrayList<Event>();
		for (int i = 0; i < resLimit; i++) {
			var reqEvent = new ResPoolEvent("pro1",ResourcePoolEvent.RP_REQUEST,250 + i,ordinal++);
			reqEvent.setTimeOptimization(100);
			toRaise.add(reqEvent);
		}
		
		tr = new TestResources(toRaise);
		pool.consume(300, tr);
		
		//There should be resLimit raised GRANTED responses
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(resLimit, tr.getRaisedResponses().size());
				
		j = 0;
		for (var re : tr.getRaisedResponses()) {
			Event raisedEvent = re.getResponse();
			assertEquals(toRaise.get(j),re.getInResponseTo());
			assertEquals(ResourcePoolEvent.RP_GRANT,raisedEvent.getType());
			assertEquals(ResourcePoolEvent.GRANT_RESPONSE_TYPES,raisedEvent.getResponseTypes());
			assertEquals("pool1",raisedEvent.getProducerId());
			assertEquals(250 + j, raisedEvent.getTime());
			assertEquals(250 + j, raisedEvent.getTimeOptimization());
			j++;
		}
		assertEquals(j,resLimit);
	}
	
	@Test
	void testHalfImmediateHalfDelayed() {
		int resLimit = 10;
		var pool = new CoreTimeOptimizedResourcePool("pool1",resLimit);
		
		//Send enough resource requests to exhaust the pool
		var toRaise = new ArrayList<Event>();
		for (int i = 0; i < resLimit * 2; i++) {
			var reqEvent = new ResPoolEvent("pro1",ResourcePoolEvent.RP_REQUEST,50 + i,ordinal++);
			reqEvent.setTimeOptimization(100);
			toRaise.add(reqEvent);
			
		}
		TestResources tr = new TestResources(toRaise);
		pool.consume(100, tr);
		
		//There should be resLimit raised GRANTED responses
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(resLimit*2, tr.getRaisedResponses().size());
				
		int j = 0;
		for (var re : tr.getRaisedResponses()) {
			Event raisedEvent = re.getResponse();
			assertEquals(toRaise.get(j),re.getInResponseTo());
			assertEquals(ResourcePoolEvent.RP_GRANT,raisedEvent.getType());
			assertEquals(ResourcePoolEvent.GRANT_RESPONSE_TYPES,raisedEvent.getResponseTypes());
			assertEquals("pool1",raisedEvent.getProducerId());
			assertEquals(50 + j, raisedEvent.getTime());
			if (j < resLimit) {
				assertEquals(50 + j, raisedEvent.getTimeOptimization());
			}else {
				assertEquals(150 + j - resLimit, raisedEvent.getTimeOptimization());
			}
			j++;
		}
		assertEquals(j,resLimit * 2);
	}
	
	@Test
	void testMultipleDelayLevels() {
		int resLimit = 10;
		var pool = new CoreTimeOptimizedResourcePool("pool1",resLimit);
		
		//Send enough resource requests to exhaust the pool
		var toRaise = new ArrayList<Event>();
		for (int i = 0; i < resLimit * 3; i++) {
			var reqEvent = new ResPoolEvent("pro1",ResourcePoolEvent.RP_REQUEST,50 + i,ordinal++);
			reqEvent.setTimeOptimization(100);
			toRaise.add(reqEvent);
			
		}
		TestResources tr = new TestResources(toRaise);
		pool.consume(100, tr);
		
		//There should be resLimit raised GRANTED responses
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(resLimit*3, tr.getRaisedResponses().size());
				
		int j = 0;
		for (var re : tr.getRaisedResponses()) {
			Event raisedEvent = re.getResponse();
			assertEquals(toRaise.get(j),re.getInResponseTo());
			assertEquals(ResourcePoolEvent.RP_GRANT,raisedEvent.getType());
			assertEquals(ResourcePoolEvent.GRANT_RESPONSE_TYPES,raisedEvent.getResponseTypes());
			assertEquals("pool1",raisedEvent.getProducerId());
			assertEquals(50 + j, raisedEvent.getTime());
			if (j < resLimit) {
				assertEquals(50 + j, raisedEvent.getTimeOptimization());
			}else if (j < resLimit * 2) {
				assertEquals(150 + j - resLimit, raisedEvent.getTimeOptimization());
			}else {
				assertEquals(250 + j - resLimit*2, raisedEvent.getTimeOptimization());
			}
			j++;
		}
		assertEquals(j,resLimit * 3);
	}
}
