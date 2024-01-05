package com.perelens.simulation.failure.consumers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

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
class AvailabilitySamplerTest {
	
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
		AvailabilitySampler c = new AvailabilitySampler("avail1",0,1000,1);
		
		ArrayList<Event> events = new ArrayList<>();
		
		Event fail = new TestEvent("f1",FailureSimulationEvent.FS_FAILED,500,ordinal++);
		Event repair = new TestEvent("f1",FailureSimulationEvent.FS_RETURN_TO_SERVICE, 600,ordinal++);
		events.add(fail);
		events.add(repair);
		
		c.consume(1000,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		assertEquals(0.9, c.getSamples()[0], 0.0000001);
	}
	
	@Test
	void testFailureAcrossOneWindow() {
		AvailabilitySampler c = new AvailabilitySampler("avail1",0,2000,1);
		
		ArrayList<Event> events = new ArrayList<>();
		
		Event fail = new TestEvent(FailureSimulationEvent.FS_FAILED, "f1",500,ordinal++);
		events.add(fail);
		
		c.consume(1000,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		events.clear();
		Event repair = new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 1500,ordinal++);
		events.add(repair);
		
		c.consume(2000,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		assertEquals(0.5, c.getSamples()[0], 0.0000001);
	}
	
	@Test
	void testMultipleFailuresOneWindow() {
		AvailabilitySampler c = new AvailabilitySampler("avail1",0,2000,1);
		
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
		
		assertEquals(0.5, c.getSamples()[0], 0.0000001);
	}
	
	@Test
	void testSamplePeriodStartsAfterFailure1() {
		AvailabilitySampler c = new AvailabilitySampler("avail1",500,1000,1);
		
		ArrayList<Event> events = new ArrayList<>();
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 250,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 1500,ordinal++));
		
		c.consume(1500,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		assertEquals(0.0, c.getSamples()[0], 0.0000001);
	}
	
	@Test
	void testSamplePeriodStartsAfterFailure2() {
		AvailabilitySampler c = new AvailabilitySampler("avail1",500,1000,1);
		
		ArrayList<Event> events = new ArrayList<>();
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 250,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 1250,ordinal++));
		
		c.consume(1500,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		assertEquals(0.250, c.getSamples()[0], 0.0000001);
	}
	
	@Test
	void testSamplePeriodStartsAfterFailure3() {
		AvailabilitySampler c = new AvailabilitySampler("avail1",500,1000,1);
		
		ArrayList<Event> events = new ArrayList<>();
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 250,ordinal++));
		c.consume(750,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		events.clear();
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 1250,ordinal++));
		c.consume(1500,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		assertEquals(0.250, c.getSamples()[0], 0.0000001);
	}
	
	@Test
	void testMultipleSamplePeriods() {
		AvailabilitySampler c = new AvailabilitySampler("avail1",500,1000,1000);
		
		//First sample period runs from 500 to 1500
		ArrayList<Event> events = new ArrayList<>();
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 250,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 1250,ordinal++));
		c.consume(2000,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		//Second sample period runs from 2500 to 3500
		events.clear();
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 2750,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 3250,ordinal++));
		c.consume(3250,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		//Third sample period runs from 4500 to 5500
		events.clear();
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 5250,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 6000,ordinal++));
		c.consume(7000,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		assertEquals(0.250, c.getSamples()[0], 0.0000001);
		assertEquals(0.50, c.getSamples()[1], 0.0000001);
		assertEquals(0.75, c.getSamples()[2], 0.0000001);
		assertEquals(3,c.getSamples().length);
		
	}

}
