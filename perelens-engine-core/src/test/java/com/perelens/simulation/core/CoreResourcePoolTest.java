/**
 * 
 */
package com.perelens.simulation.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import com.perelens.engine.TestResources;
import com.perelens.engine.api.Event;
import com.perelens.simulation.events.ResourcePoolEvent;

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
class CoreResourcePoolTest extends RequestQueueAndMapTest{

	@Override
	protected RequestQueueAndMap newRequestQueueAndMap() {
		return new CoreResourcePool("pool1",1);
	}

	long ordinal = 1;
	
	@Test
	void simpleResourceRequest() {
		CoreResourcePool pool = new CoreResourcePool("pool1",2);
		
		//Send a request event to the resource pool
		Event reqEvent = new ResPoolEvent("pro1",ResourcePoolEvent.RP_REQUEST,50,ordinal++);
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
	}
	
	@Test
	void requestGrantInSameWindowAfterReturn() {
		for (int i = 1; i < 100; i++) {
			for (int j = 0; j <= i; j++) {
				grantInSameWindowAfterReturn(i,j);
			}
		}
	}
	
	void grantInSameWindowAfterReturn(int capacity, int extraRequests) {
		CoreResourcePool pool = new CoreResourcePool("pool1",capacity);
		
		//Send capacity + request events to the pool
		ArrayList<Event> events = new ArrayList<>(capacity+extraRequests);
		HashMap<String,Event> requests = new HashMap<>();
		for (int i = 0; i < capacity + extraRequests; i++) {
			String producer = "pro" + i;
			Event e = new ResPoolEvent(producer,ResourcePoolEvent.RP_REQUEST,10,ordinal++);
			events.add(e);
			requests.put(producer,e);
		}
		TestResources tr = new TestResources(events);
		
		pool.consume(100, tr);
		
		//There should be "capacity" raised GRANTED responses
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(capacity, tr.getRaisedResponses().size());
				
		Iterator<TestResources.ResponseEntry> raised = tr.getRaisedResponses().iterator();
		
		for (int i = 0; i < capacity; i++) {
			TestResources.ResponseEntry re = raised.next();
			Event raisedEvent = re.getResponse();
			Event origEvent = requests.remove(re.getInResponseTo().getProducerId());
			assertNotNull(origEvent);
			assertEquals(origEvent,re.getInResponseTo());
			assertEquals(ResourcePoolEvent.RP_GRANT,raisedEvent.getType());
			assertEquals(ResourcePoolEvent.GRANT_RESPONSE_TYPES,raisedEvent.getResponseTypes());
			assertEquals("pool1",raisedEvent.getProducerId());
			assertEquals(10,raisedEvent.getTime());
		}
		
		//Only the unfulfilled request should be left
		assertFalse(raised.hasNext());
		assertEquals(extraRequests, requests.size());
		
		//Return a resource and make sure the final request is granted
		ArrayList<Event> returnEvents = new ArrayList<Event>();
		for (int i = 0; i < extraRequests; i++) {
			Event returnEvent = new ResPoolEvent("pro" + i, ResourcePoolEvent.RP_RETURN,50,ordinal++);
			returnEvents.add(returnEvent);
		}
		
		tr = new TestResources(returnEvents);
		
		pool.consume(100, tr);
		
		//There should be "extraRequests" raised GRANTED responses
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(extraRequests, tr.getRaisedResponses().size());
		raised = tr.getRaisedResponses().iterator();
		
		for (int i = 0; i < extraRequests; i++) {
			
			TestResources.ResponseEntry re = raised.next();
			Event raisedEvent = re.getResponse();
			assertTrue(requests.containsKey(re.getInResponseTo().getProducerId()));
			assertNotNull(requests.remove(re.getInResponseTo().getProducerId()));
			assertEquals(ResourcePoolEvent.RP_GRANT,raisedEvent.getType());
			assertEquals(ResourcePoolEvent.GRANT_RESPONSE_TYPES,raisedEvent.getResponseTypes());
			assertEquals("pool1",raisedEvent.getProducerId());
			assertEquals(50,raisedEvent.getTime());
		}
		
		assertFalse(raised.hasNext());
		assertEquals(0, requests.size());
	}
	
	@Test
	void simpleDeferSequence() {
		CoreResourcePool pool = new CoreResourcePool("pool1",1);

		Event reqEvent1 = new ResPoolEvent("pro1",ResourcePoolEvent.RP_REQUEST,49,ordinal++); //Should be granted
		Event reqEvent2 = new ResPoolEvent("pro2",ResourcePoolEvent.RP_REQUEST,50,ordinal++); //Should be deferred
		TestResources tr = new TestResources(Arrays.asList(new Event[] {reqEvent1, reqEvent2}));

		pool.consume(100, tr);

		//There should be one raised GRANTED responses
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(1, tr.getRaisedResponses().size());

		Iterator<TestResources.ResponseEntry> raised = tr.getRaisedResponses().iterator();
		TestResources.ResponseEntry re = raised.next();
		Event raisedEvent = re.getResponse();
		assertFalse(raised.hasNext());
		assertEquals(reqEvent1,re.getInResponseTo());
		assertEquals(ResourcePoolEvent.RP_GRANT,raisedEvent.getType());
		assertEquals(ResourcePoolEvent.GRANT_RESPONSE_TYPES,raisedEvent.getResponseTypes());
		assertEquals("pool1",raisedEvent.getProducerId());
		assertEquals(49,raisedEvent.getTime());
		
		//Send a defer event to the pool indicating that the resource will not be returned
		Event defer = new ResPoolEvent("pro1", ResourcePoolEvent.RP_DEFER, 60,ordinal++);
		tr = new TestResources(Arrays.asList(new Event[] {defer}));
		
		pool.consume(100, tr);
		
		//The second requester should get a DEFERRED event
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(1, tr.getRaisedResponses().size());

		raised = tr.getRaisedResponses().iterator();
		re = raised.next();
		raisedEvent = re.getResponse();
		assertFalse(raised.hasNext());
		assertEquals(reqEvent2,re.getInResponseTo());
		assertEquals(ResourcePoolEvent.RP_DEFER,raisedEvent.getType());
		assertEquals(0,raisedEvent.getResponseTypes().size());
		assertEquals("pool1",raisedEvent.getProducerId());
		assertEquals(60,raisedEvent.getTime());
		
		//The second requester sends a renew event at the start of the next window
		Event renew = new ResPoolEvent("pro2",ResourcePoolEvent.RP_RENEW,101,ordinal++);
		
		//The first requester returns the resource in the window
		Event ret = new ResPoolEvent("pro1",ResourcePoolEvent.RP_RETURN,150,ordinal++);
		tr = new TestResources(Arrays.asList(new Event[] {renew,ret}));
		
		pool.consume(200, tr);
		
		//The second requester should get a GRANT event
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(1, tr.getRaisedResponses().size());

		raised = tr.getRaisedResponses().iterator();
		re = raised.next();
		raisedEvent = re.getResponse();
		assertFalse(raised.hasNext());
		assertEquals(renew,re.getInResponseTo());
		assertEquals(ResourcePoolEvent.RP_GRANT,raisedEvent.getType());
		assertEquals(ResourcePoolEvent.GRANT_RESPONSE_TYPES,raisedEvent.getResponseTypes());
		assertEquals("pool1",raisedEvent.getProducerId());
		assertEquals(150,raisedEvent.getTime());
	}
	
	@Test
	void testManyDeferSequences() {
		for (int i = 1; i < 100; i++) {
			for (int j = 0; j <= i; j++) {
				deferSequence(i,j);
			}
		}
	}
	
	void deferSequence(int capacity, int numDeferred) {
		CoreResourcePool pool = new CoreResourcePool("pool1",capacity);

		
		ArrayList<Event> requests = new ArrayList<Event>();
		HashMap<String, Event> requestMap = new HashMap<>();
		for (int i = 0; i < capacity + numDeferred; i++) {
			Event e = new ResPoolEvent("pro" + i, ResourcePoolEvent.RP_REQUEST,50,ordinal++);
			requests.add(e);
			requestMap.put("pro"+i, e);
		}
		TestResources tr = new TestResources(requests);

		pool.consume(100, tr);

		//There should "capacity" raised GRANTED responses
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(capacity, tr.getRaisedResponses().size());

		Iterator<TestResources.ResponseEntry> raised = tr.getRaisedResponses().iterator();
		for (int i = 0; i < capacity; i++) {
			TestResources.ResponseEntry entry = raised.next();
			Event raisedEvent = entry.getResponse();
			Event origEvent = requestMap.remove(entry.getInResponseTo().getProducerId());
			assertNotNull(origEvent);
			assertEquals(origEvent, entry.getInResponseTo());
			assertEquals(ResourcePoolEvent.RP_GRANT,raisedEvent.getType());
			assertEquals(ResourcePoolEvent.GRANT_RESPONSE_TYPES,raisedEvent.getResponseTypes());
			assertEquals("pool1",raisedEvent.getProducerId());
			assertEquals(50,raisedEvent.getTime());
		}
		
		assertFalse(raised.hasNext());
		assertEquals(numDeferred, requestMap.size());
			
		//Send a defer event to the pool indicating that the resource will not be returned
		ArrayList<Event> defers = new ArrayList<Event>();
		for (int i = 0; i < capacity; i++) {
			Event e = new ResPoolEvent("pro" + i, ResourcePoolEvent.RP_DEFER, 60,ordinal++);
			defers.add(e);
		}
		tr = new TestResources(defers);
		
		pool.consume(100, tr);
		
		//The pool should generate "numDeferred" defer events
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(numDeferred, tr.getRaisedResponses().size());
		
		HashMap<String, Event> deferMap = new HashMap<>();
		raised = tr.getRaisedResponses().iterator();
		for (int i = capacity; i < capacity + numDeferred; i++) {
			TestResources.ResponseEntry re = raised.next();
			Event raisedEvent = re.getResponse();
			Event origEvent = requestMap.remove(re.getInResponseTo().getProducerId());
			deferMap.put(re.getInResponseTo().getProducerId(), raisedEvent);
			assertNotNull(origEvent);
			assertEquals(origEvent,re.getInResponseTo());
			assertEquals(ResourcePoolEvent.RP_DEFER,raisedEvent.getType());
			assertEquals(0,raisedEvent.getResponseTypes().size());
			assertEquals("pool1",raisedEvent.getProducerId());
			assertEquals(60,raisedEvent.getTime());
		}
		
		assertFalse(raised.hasNext());
		assertEquals(0,requestMap.size());
		
		//All the deferred requesters send a RENEW at the start of the next window
		ArrayList<Event> renews = new ArrayList<>();
		HashMap<String,Event> renewMap = new HashMap<>();
		for (String key : deferMap.keySet()) {
			Event e = new ResPoolEvent(key,ResourcePoolEvent.RP_RENEW,101,ordinal++);
			renews.add(e);
			renewMap.put(key, e);
		}
		
		//The granted requesters return their resources
		ArrayList<Event> returns = new ArrayList<>();
		HashMap<String,Event> returnMap = new HashMap<>();
		for (int i = 0; i < capacity; i++) {
			Event e = new ResPoolEvent("pro"+i,ResourcePoolEvent.RP_RETURN, 187,ordinal++);
			returns.add(e);
			returnMap.put("pro"+i, e);
		}
		
		ArrayList<Event> events = new ArrayList<>(renews);
		events.addAll(returns);
		tr = new TestResources(events);
		
		pool.consume(200, tr);
		
		//The there should be "numDeferred" GRANT requests
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(numDeferred, tr.getRaisedResponses().size());

		raised = tr.getRaisedResponses().iterator();
		for (int i = 0; i < numDeferred; i++) {
			TestResources.ResponseEntry entry = raised.next();
			Event raisedEvent = entry.getResponse();
			Event origEvent = renewMap.get(entry.getInResponseTo().getProducerId());
			assertNotNull(origEvent);
			assertEquals(origEvent, entry.getInResponseTo());
			assertEquals(ResourcePoolEvent.RP_GRANT,raisedEvent.getType());
			assertEquals(ResourcePoolEvent.GRANT_RESPONSE_TYPES,raisedEvent.getResponseTypes());
			assertEquals("pool1",raisedEvent.getProducerId());
			assertEquals(187,raisedEvent.getTime());
		}

		assertFalse(raised.hasNext());
		
		//Return all the resources and make sure that no more events are generated
		ArrayList<Event> finalReturn = new ArrayList<>();
		for(String key : renewMap.keySet()) {
			Event e = new ResPoolEvent(key, ResourcePoolEvent.RP_RETURN, 190,ordinal++);
			finalReturn.add(e);
		}
		
		tr = new TestResources(finalReturn);
		
		pool.consume(200, tr);
		
		//The there should be no raised events or responses
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
	}
}
