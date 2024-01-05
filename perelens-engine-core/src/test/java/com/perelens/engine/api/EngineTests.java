/**
 * 
 */
package com.perelens.engine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.perelens.engine.api.EngineTests.TestEvent.EventTypes;

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
public abstract class EngineTests {

	protected abstract Engine getEngine();
	
	protected static class TestEvent implements Event{
		protected static enum EventTypes implements EventType{
			RESPONSE;
		}
		
		private long offset;
		private String producerId;
		private long ordinal = 1;
		
		public TestEvent(long offset, String producerId) {
			this.offset = offset;
			this.producerId = producerId;
		}
		
		public TestEvent(long offset, String producerId, long ordinal) {
			this(offset,producerId);
			this.ordinal = ordinal;
		}
		
		@Override
		public String getProducerId() {
			return producerId;
		}

		@Override
		public EventType getType() {
			return EventTypes.RESPONSE;
		}

		@Override
		public long getTime() {
			return offset;
		}

		@Override
		public Collection<EventType> getResponseTypes() {
			return Collections.emptyList();//Event does not require response
		}

		@Override
		public Iterator<Event> causedBy() {
			return Collections.<Event>emptyList().iterator();
		}

		@Override
		public long getOrdinal() {
			return ordinal;
		}
	}
	
	protected static class TestEventConsumer implements EventConsumer{

		private String id;
		
		public TestEventConsumer(String id) {
			this.id = id;
		}
		@Override
		public String getId() {
			return id;
		}

		@Override
		public void consume(long timeWindow, ConsumerResources events) {
		}
		
	}
	
	protected static class TestEventEvaluator implements EventEvaluator{

		private String id;
		
		public TestEventEvaluator(String id) {
			this.id = id;
		}
		
		@Override
		public String getId() {
			return id;
		}

		@Override
		public void consume(long timeWindow, EvaluatorResources resources) {
		}

		@Override
		public EventEvaluator copy() {
			return new TestEventEvaluator(id);
		}

		@Override
		public Map<ConfigKey, String> getConfiguration() {
			return Collections.singletonMap(EventGenerator.CONFIG_KEYS.EG_ID, id);
		}
		
	}
	
	protected static class TestEventResponder implements EventResponder{

		/**
		 * 
		 */
		private String id;
		
		public TestEventResponder(String id) {
			this.id = id;
		}
		
		@Override
		public EventGenerator copy() {
			return new TestEventResponder(id);
		}

		@Override
		public Map<ConfigKey, String> getConfiguration() {
			return Collections.singletonMap(EventGenerator.CONFIG_KEYS.EG_ID, id);
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public void consume(long timeWindow, ResponderResources resources) {
		}
		
	}
	
	@Test
	protected void testSingleEventEvaluatorConsume() {
		Engine e = getEngine();
		
		final AtomicInteger count = new AtomicInteger();
		EventEvaluator eval = new TestEventEvaluator("eval1") {
			long lastTime = 0;
			@Override
			public void consume(long timeWindow, EvaluatorResources resources) {
				Assertions.assertFalse(resources.getEvents().iterator().hasNext()); //Should be no events
				Assertions.assertTrue(timeWindow > lastTime);
				lastTime = timeWindow;
				count.incrementAndGet();
			}
		};
		
		e.registerEvaluator(eval);
		
		TestEventConsumer cons = new TestEventConsumer("cons1");
		e.registerConsumer(cons);
		e.registerSubscription(eval.getId(), cons.getId());
		
		for (int i = 99; i < 1000; i+=100) {
			e.evaluate(i);
		}
		
		assertEquals(10, count.get());
		
		e.destroy();
	}
	
	
	@Test
	protected void testSingleEventEvaluatorRaiseEvent() {
		Engine e = getEngine();
		
		EventEvaluator eval = new TestEventEvaluator("eval1") {
			@Override
			public void consume(long timeWindow, EvaluatorResources resources) {
				resources.raiseEvent(new TestEvent(timeWindow,this.getId()));
			}
		};
		
		ConcurrentLinkedQueue<Event> eventQueue = new ConcurrentLinkedQueue<>();
		TestEventConsumer cons = new TestEventConsumer("cons1") {

			@Override
			public void consume(long timeWindow, ConsumerResources events) {
				int count = 0;
				for (Event e : events.getEvents()) {
					count++;
					eventQueue.add(e);
				}
				Assertions.assertEquals(1, count);
			}
			
		};
		
		e.registerEvaluator(eval);
		e.registerConsumer(cons);
		e.registerSubscription(eval.getId(), cons.getId());
		
		for (int i = 99; i < 1000; i+=100) {
			e.evaluate(i);
		}
		
		//Make sure we got 10 events
		assertEquals(10,eventQueue.size());
		long prev = -1;
		for (Event ev : eventQueue) {
			assertEquals(eval.getId(),ev.getProducerId());
			assertEquals(prev + 100, ev.getTime());
			prev = ev.getTime();
		}
		
		e.destroy();
	}
	
	@Test
	protected void testDoubleEventEvaluatorRaiseEvent() {
		Engine e = getEngine();
		
		EventEvaluator eval = new TestEventEvaluator("eval1") {
			@Override
			public void consume(long timeWindow, EvaluatorResources resources) {
				resources.raiseEvent(new TestEvent(timeWindow-1,this.getId()));
			}
		};
		
		EventEvaluator eval2 = new TestEventEvaluator("eval2") {
			@Override
			public void consume(long timeWindow, EvaluatorResources resources) {
				resources.raiseEvent(new TestEvent(timeWindow,this.getId()));
			}
		};
		
		ConcurrentLinkedQueue<Event> eventQueue = new ConcurrentLinkedQueue<>();
		TestEventConsumer cons = new TestEventConsumer("cons1") {

			@Override
			public void consume(long timeWindow, ConsumerResources events) {
				int count = 0;
				for (Event e : events.getEvents()) {
					count++;
					eventQueue.add(e);
				}
				Assertions.assertEquals(2, count);
			}
			
		};
		
		e.registerEvaluator(eval);
		e.registerEvaluator(eval2);
		e.registerConsumer(cons);
		e.registerSubscription(eval.getId(), cons.getId());
		e.registerSubscription(eval2.getId(), cons.getId());
		
		for (int i = 99; i < 1000; i+=100) {
			e.evaluate(i);
		}
		
		//Make sure we got 20 events
		assertEquals(20,eventQueue.size());
		long prevEventTime = 0;
		for (Iterator<Event> i = eventQueue.iterator(); i.hasNext();) {
			Event e1 = i.next();
			Event e2 = i.next();
			
			assertEquals(eval.getId(),e1.getProducerId());
			assertEquals(eval2.getId(),e2.getProducerId());
			
			assertTrue(e1.getTime() < e2.getTime());
			
			assertTrue(e2.getTime() > prevEventTime);
			
			prevEventTime = e2.getTime();
		}
		
		e.destroy();
	}
	
	@Test
	protected void testEventEvaluatorRequestResponse() {
		Engine e = getEngine();
		
		AtomicInteger reqCount = new AtomicInteger(0);
		EventEvaluator reqEval = new TestEventEvaluator("reqEval") {
			int execCount = 0;
			@Override
			public void consume(long timeWindow, EvaluatorResources resources) {
				if (execCount == 0) {
					TestEvent toRaise = new TestEvent(timeWindow - 1,this.getId()) {
						@Override
						public Collection<EventType> getResponseTypes() {
							return Collections.singletonList(EventTypes.RESPONSE);
						}
					};
					resources.raiseEvent(toRaise);
					execCount++;
				}else if (execCount == 1) {
					int count = 0;
					for (Event ev : resources.getEvents()) {
						assertEquals("respEval", ev.getProducerId());
						count++;
					}
					assertEquals(1,count);
					execCount++;
				}else{
					fail("Invoked too many times");
				}
				
				reqCount.incrementAndGet();
			}
		};
		
		AtomicInteger respCount = new AtomicInteger(0);
		EventResponder respEval = new TestEventResponder("respEval") {
			int execCount = 0;
			@SuppressWarnings("unused")
			@Override
			public void consume(long timeWindow, ResponderResources resources) {
				if (execCount == 0) {
					int count = 0;
					for (Event e : resources.getEvents()) {
						assertEquals(Collections.singletonList(EventTypes.RESPONSE), e.getResponseTypes());
						TestEvent toRaise = new TestEvent(timeWindow - 1,this.getId());
						resources.raiseResponse(toRaise, e);
						count++;
					}
					assertEquals(1, count);
					execCount++;
				}else if (execCount == 1) {
					int count = 0;
					for (Event e : resources.getEvents()) {
						count++;
					}
					assertEquals(0, count);
					execCount++;
				}else {
					fail("Invoked too many times");
				}
				
				respCount.incrementAndGet();
			}
		};
		
		
		e.registerEvaluator(reqEval);
		e.registerResponder(respEval);
		e.registerSubscription(reqEval.getId(), respEval.getId());

		e.evaluate(100);

		assertEquals(2, reqCount.get());
		assertEquals(2, respCount.get());
	}
	
	@Test
	protected void testExceptionInEventEvaluator() {
		Engine e = getEngine();
		
		EventEvaluator eval = new TestEventEvaluator("eval1") {
			@Override
			public void consume(long timeWindow, EvaluatorResources resources) {
				throw new IllegalStateException("Boom");
			}
		};
		
		e.registerEvaluator(eval);
		
		TestEventConsumer cons = new TestEventConsumer("cons1");
		e.registerConsumer(cons);
		e.registerSubscription(eval.getId(), cons.getId());
		
		try {
			e.evaluate(100);
			fail("Should have thrown exception.");
		}catch(EngineExecutionException ex) {
			assertEquals(1, ex.getCauses().size());
			assertEquals("Boom", ex.getCauses().iterator().next().getMessage());
		}
		
		try {
			e.evaluate(100);
			fail("Should have thrown exception.");
		}catch(IllegalStateException ex) {
		}
		
		e.destroy();
	}
	
	@Test
	protected void testCircularDependencyDetection1() {
		Engine e = getEngine();
		
		EventEvaluator eval1 = new TestEventEvaluator("eval1");
		EventEvaluator eval2 = new TestEventEvaluator("eval2");
		EventEvaluator eval3 = new TestEventEvaluator("eval3");
		
		e.registerEvaluator(eval1);
		e.registerEvaluator(eval2);
		e.registerEvaluator(eval3);
		
		e.registerSubscription(eval1.getId(), eval2.getId());
		e.registerSubscription(eval2.getId(), eval3.getId());
		e.registerSubscription(eval3.getId(), eval1.getId());
		
		try {
			e.evaluate(100);
			fail("Should have thrown exception.");
		}catch(CircularDependencyException ex) {
		}
		
		e.destroy();
	}
	
	@Test
	protected void testCircularDependencyDetection2() {
		Engine e = getEngine();
		
		EventEvaluator eval1 = new TestEventEvaluator("eval1");
		EventEvaluator eval2 = new TestEventEvaluator("eval2");
		
		e.registerEvaluator(eval1);
		e.registerEvaluator(eval2);
		
		e.registerSubscription(eval1.getId(), eval2.getId());
		e.registerSubscription(eval2.getId(), eval1.getId());
		
		try {
			e.evaluate(100);
			fail("Should have thrown exception.");
		}catch(CircularDependencyException ex) {
		}
		
		e.destroy();
	}
	
	@Test
	protected void testEventOrdinalOrdering() {
		Engine e = getEngine();
		
		EventEvaluator eval = new TestEventEvaluator("eval1") {
			@Override
			public void consume(long timeWindow, EvaluatorResources resources) {
				//raise multiple events that only differ by their ordinal setting
				resources.raiseEvent(new TestEvent(timeWindow,this.getId(),5));
				resources.raiseEvent(new TestEvent(timeWindow,this.getId(),6));
				resources.raiseEvent(new TestEvent(timeWindow,this.getId(),3));
				resources.raiseEvent(new TestEvent(timeWindow,this.getId(),4));
				resources.raiseEvent(new TestEvent(timeWindow,this.getId(),1));
			}
		};
		
		ConcurrentLinkedQueue<Event> eventQueue = new ConcurrentLinkedQueue<>();
		TestEventConsumer cons = new TestEventConsumer("cons1") {

			@Override
			public void consume(long timeWindow, ConsumerResources events) {
				int count = 0;
				for (Event e : events.getEvents()) {
					count++;
					eventQueue.add(e);
				}
				Assertions.assertEquals(5, count);
			}
			
		};
		
		e.registerEvaluator(eval);
		e.registerConsumer(cons);
		e.registerSubscription(eval.getId(), cons.getId());
		
		e.evaluate(100);
		
		//Make sure we got 5 events
		assertEquals(5,eventQueue.size());
		long prev = -1;
		for (Event ev : eventQueue) {
			assertEquals(eval.getId(),ev.getProducerId());
			assertEquals(100,ev.getTime());
			assertTrue(ev.getOrdinal() > prev);
			prev = ev.getOrdinal();
		}
		
		e.destroy();
	}
	
	@Test
	protected void testGlobalConsumers() {
		Engine e = getEngine();
		
		EventEvaluator eval = new TestEventEvaluator("eval1") {
			@Override
			public void consume(long timeWindow, EvaluatorResources resources) {
				resources.raiseEvent(new TestEvent(timeWindow-1,this.getId()));
			}
		};
		
		EventEvaluator eval2 = new TestEventEvaluator("eval2") {
			@Override
			public void consume(long timeWindow, EvaluatorResources resources) {
				resources.raiseEvent(new TestEvent(timeWindow,this.getId()));
			}
		};
		
		ConcurrentLinkedQueue<Event> eventQueue1 = new ConcurrentLinkedQueue<>();
		TestEventConsumer cons1 = new TestEventConsumer("cons1") {

			@Override
			public void consume(long timeWindow, ConsumerResources events) {
				int count = 0;
				for (Event e : events.getEvents()) {
					count++;
					eventQueue1.add(e);
				}
				Assertions.assertEquals(2, count);
			}
			
		};
		
		ConcurrentLinkedQueue<Event> eventQueue2 = new ConcurrentLinkedQueue<>();
		TestEventConsumer cons2 = new TestEventConsumer("cons2") {

			@Override
			public void consume(long timeWindow, ConsumerResources events) {
				int count = 0;
				for (Event e : events.getEvents()) {
					count++;
					eventQueue2.add(e);
				}
				Assertions.assertEquals(2, count);
			}
			
		};
		
		e.registerEvaluator(eval);
		e.registerEvaluator(eval2);
		e.registerGlobalConsumer(cons1);
		e.registerGlobalConsumer(cons2);
		
		try {
			for (int i = 99; i < 1000; i+=100) {
				e.evaluate(i);
			}
		}catch(EngineExecutionException e1) {
			for(Throwable t : e1.getCauses()) {
				t.printStackTrace();
			}
		}
		
		//Make sure we got 20 events
		assertEquals(20,eventQueue1.size());
		long prevEventTime = 0;
		for (Iterator<Event> i = eventQueue1.iterator(); i.hasNext();) {
			Event e1 = i.next();
			Event e2 = i.next();
			
			assertEquals(eval.getId(),e1.getProducerId());
			assertEquals(eval2.getId(),e2.getProducerId());
			
			assertTrue(e1.getTime() < e2.getTime());
			
			assertTrue(e2.getTime() > prevEventTime);
			
			prevEventTime = e2.getTime();
		}
		
		assertEquals(20,eventQueue2.size());
		prevEventTime = 0;
		for (Iterator<Event> i = eventQueue2.iterator(); i.hasNext();) {
			Event e1 = i.next();
			Event e2 = i.next();
			
			assertEquals(eval.getId(),e1.getProducerId());
			assertEquals(eval2.getId(),e2.getProducerId());
			
			assertTrue(e1.getTime() < e2.getTime());
			
			assertTrue(e2.getTime() > prevEventTime);
			
			prevEventTime = e2.getTime();
		}
		
		e.destroy();
	}

}
