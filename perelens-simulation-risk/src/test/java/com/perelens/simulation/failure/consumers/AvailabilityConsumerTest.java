package com.perelens.simulation.failure.consumers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.perelens.engine.api.AbstractEvent;
import com.perelens.engine.api.ConsumerResources;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventType;
import com.perelens.simulation.failure.events.FailureSimulationEvent;

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
class AvailabilityConsumerTest {
	
	private long ordinal = 1;
	
	static class TestEvent extends AbstractEvent{

		protected TestEvent(EventType type,String producerId,  long time, long ordinal) {
			super(producerId, type, time, ordinal);
		}
		
		protected TestEvent(String producerId, EventType type, long time, long ordinal) {
			super(producerId, type, time, ordinal);
		}
		
		protected TestEvent(String producerId, EventType type, long time, long ordinal, Event cause) {
			super(producerId,type,time,ordinal,cause);
		}
	}

	@Test
	void testSingleFailureSingleWindow() {
		AvailabilityConsumer c = new AvailabilityConsumer("avail1");
		
		ArrayList<Event> events = new ArrayList<>();
		
		Event fail = new TestEvent("f1",FailureSimulationEvent.FS_FAILED,500,ordinal++);
		Event repair = new TestEvent("f1",FailureSimulationEvent.FS_RETURN_TO_SERVICE, 600,ordinal++);
		events.add(fail);
		events.add(repair);
		
		c.consume(1000, new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		assertEquals(0.9, c.getAvailability(), 0.0000001);
		assertEquals(900, c.getUpTime());
		assertEquals(100, c.getDownTime());
	}
	
	@Test
	void testNoFailures() {
		AvailabilityConsumer c = new AvailabilityConsumer("avail1");
		
		ArrayList<Event> events = new ArrayList<>();
		Random r = new Random();
		
		
		for (int i = 1000; i < 1000000; i += r.nextInt(1000) + 10) {
			
			c.consume(i, new ConsumerResources() {

				@Override
				public Iterable<Event> getEvents() {
					return events;
				}});
			
			assertEquals(1.0, c.getAvailability(), 0.0000001);
			assertEquals(i, c.getUpTime());
			assertEquals(0, c.getDownTime());
		}
	}
	
	@Test
	void testFailureAcrossOneWindow() {
		AvailabilityConsumer c = new AvailabilityConsumer("avail1");
		
		ArrayList<Event> events = new ArrayList<>();
		
		Event fail = new TestEvent(FailureSimulationEvent.FS_FAILED, "f1",500,ordinal++);
		events.add(fail);
		
		c.consume(1000,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		assertEquals(0.5, c.getAvailability(), 0.0000001);
		assertEquals(500, c.getUpTime());
		assertEquals(500, c.getDownTime());
		
		events.clear();
		Event repair = new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 1500,ordinal++);
		events.add(repair);
		
		c.consume(2000,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		assertEquals(0.5, c.getAvailability(), 0.0000001);
		assertEquals(1000, c.getUpTime());
		assertEquals(1000, c.getDownTime());
	}
	
	@Test
	void testMultipleFailuresOneWindow() {
		AvailabilityConsumer c = new AvailabilityConsumer("avail1");
		
		ArrayList<Event> events = new ArrayList<>();
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 250,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 500,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 750,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 1000,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 1250,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 1500,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 1750,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 2000,ordinal++));
		
		c.consume(2000,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		assertEquals(0.5, c.getAvailability(), 0.0000001);
		assertEquals(1000, c.getUpTime());
		assertEquals(1000, c.getDownTime());
	}

}
