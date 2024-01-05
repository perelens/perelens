package com.perelens.simulation.failure.consumers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.perelens.engine.api.AbstractEvent;
import com.perelens.engine.api.ConsumerResources;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventType;
import com.perelens.simulation.failure.consumers.OutageConsumer.Outage;
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
 * @author Steve Branda
 *
 */
class OutageConsumerTest {
	
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

		public TestEvent(String producerId, EventType type, long time, long ordinal, Event[] causedBy) {
			super(producerId, type, time, ordinal, causedBy);
		}
	}

	@Test
	void testSingleFailureSingleWindow() {
		OutageConsumer c = new OutageConsumer("outage1",10);
		
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
		
		assertEquals(1, c.getDowntimeDurations().length);
		assertEquals(1, c.getTopOutages().length);
		assertEquals(100,c.getDowntimeDurations()[0]);
		Outage o = c.getTopOutages()[0];
		assertEquals(fail,o.getStart());
		assertEquals(repair,o.getEnd());
	}
	
	@Test
	void testNoFailures() {
		OutageConsumer c = new OutageConsumer("outage1",10);
		
		ArrayList<Event> events = new ArrayList<>();
		Random r = new Random();
		
		
		for (int i = 1000; i < 1000000; i += r.nextInt(1000) + 10) {
			
			c.consume(i,  new ConsumerResources() {

				@Override
				public Iterable<Event> getEvents() {
					return events;
				}});
			
			assertEquals(0, c.getDowntimeDurations().length);
			assertEquals(0, c.getTopOutages().length);
		}
	}
	
	@Test
	void testFailureAcrossOneWindow() {
		OutageConsumer c = new OutageConsumer("outage1",10);
		
		ArrayList<Event> events = new ArrayList<>();
		
		Event fail = new TestEvent(FailureSimulationEvent.FS_FAILED, "f1",500,ordinal++);
		events.add(fail);
		
		c.consume(1000,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		assertEquals(0,c.getDowntimeDurations().length);
		assertEquals(0,c.getTopOutages().length);
		
		events.clear();
		Event repair = new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 1500,ordinal++);
		events.add(repair);
		
		c.consume(2000,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		assertEquals(1,c.getDowntimeDurations().length);
		assertEquals(1000,c.getDowntimeDurations()[0]);
		Outage o = c.getTopOutages()[0];
		assertEquals(fail,o.getStart());
		assertEquals(repair,o.getEnd());
	}
	
	@Test
	void testMultipleFailuresOneWindow() {
		OutageConsumer c =  new OutageConsumer("outage1",10);
		
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
		
		assertEquals(4, c.getDowntimeDurations().length);
		
		for (long duration : c.getDowntimeDurations()) {
			assertEquals(250,duration);
		}
		
		assertEquals(4, c.getTopOutages().length);
		
		for (Outage o : c.getTopOutages()) {
			assertEquals(250, o.getEnd().getTime() - o.getStart().getTime());
		}
	}
	
	@Test
	void testTopOutages() {
		OutageConsumer c = new OutageConsumer("outage1",3);
		
		ArrayList<Event> events = new ArrayList<>();
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 250,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 500,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 750,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 850,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 1250,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 1800,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 2000,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 3000,ordinal++));
		
		c.consume(3000,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		long[] dds = c.getDowntimeDurations();
		assertEquals(4, dds.length);
		
		assertEquals(250, dds[0]);
		assertEquals(100, dds[1]);
		assertEquals(550, dds[2]);
		assertEquals(1000, dds[3]);
		
		Outage[] tos = c.getTopOutages();
		assertEquals(3, tos.length);
		
		assertEquals(250, tos[0].getEnd().getTime() - tos[0].getStart().getTime());
		assertEquals(550, tos[1].getEnd().getTime() - tos[1].getStart().getTime());
		assertEquals(1000, tos[2].getEnd().getTime() - tos[2].getStart().getTime());
	}
	
	@Test
	void testTopOutagesZero() {
		OutageConsumer c = new OutageConsumer("outage1",0);
		
		ArrayList<Event> events = new ArrayList<>();
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 250,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 500,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 750,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 850,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 1250,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 1800,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 2000,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 3000,ordinal++));
		
		c.consume(3000,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		long[] dds = c.getDowntimeDurations();
		assertEquals(4, dds.length);
		
		assertEquals(250, dds[0]);
		assertEquals(100, dds[1]);
		assertEquals(550, dds[2]);
		assertEquals(1000, dds[3]);
		
		Outage[] tos = c.getTopOutages();
		assertEquals(0, tos.length);
	}
	
	
	@Test
	void testOutageDurationFilter() {
		OutageConsumer c = new OutageConsumer("outage1",2);
		c.setIgnoreLessThan(100);
		
		
		ArrayList<Event> events = new ArrayList<>();
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 1,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 100,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 101,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 201,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 250,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 450,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_FAILED, "f1", 500,ordinal++));
		events.add(new TestEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "f1", 501,ordinal++));
		
		c.consume(1000,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		long[] dds = c.getDowntimeDurations();
		assertEquals(2,dds.length);
		
		assertEquals(100,dds[0]);
		assertEquals(200,dds[1]);
		
		Outage[] tos = c.getTopOutages();
		assertEquals(2, tos.length);
		
		assertEquals(100, tos[0].getEnd().getTime() - tos[0].getStart().getTime());
		assertEquals(200, tos[1].getEnd().getTime() - tos[1].getStart().getTime());
	}
	
	@Test
	void testFailingOverFilter_shouldFilter_noRepair() {
		OutageConsumer c = new OutageConsumer("outage1",2);
		c.setIgnoreFailingOver(true);
		
		Event nodeFailure = new TestEvent("f1-node-a",FailureSimulationEvent.FS_FAILED,100, ordinal++);
		Event failingOver = new TestEvent("f1",FailureSimulationEvent.FS_FAILING_OVER, 100, ordinal++, nodeFailure);
		Event failure = new TestEvent("f1",FailureSimulationEvent.FS_FAILED,100,ordinal++,failingOver);
		
		Event repair = new TestEvent("f1",FailureSimulationEvent.FS_RETURN_TO_SERVICE, 104, ordinal++);
		
		ArrayList<Event> events = new ArrayList<>();
		events.add(failure);
		events.add(repair);
		
		c.consume(1000,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		long[] dds = c.getDowntimeDurations();
		assertEquals(0,dds.length);
	}
	
	@Test
	void testFailingOverFilter_shouldFilter_withRepair() {
		OutageConsumer c = new OutageConsumer("outage1",2);
		c.setIgnoreFailingOver(true);
		
		Event nodeFailure = new TestEvent("f1-node-a",FailureSimulationEvent.FS_FAILED,100, ordinal++);
		Event failingOver = new TestEvent("f1",FailureSimulationEvent.FS_FAILING_OVER, 100, ordinal++, nodeFailure);
		Event failure = new TestEvent("f1",FailureSimulationEvent.FS_FAILED,100,ordinal++,failingOver);
		
		Event nodeRepair = new TestEvent("f1-node-a",FailureSimulationEvent.FS_RETURN_TO_SERVICE,103, ordinal++);
		Event repair = new TestEvent("f1",FailureSimulationEvent.FS_RETURN_TO_SERVICE, 104, ordinal++, nodeRepair);
		
		ArrayList<Event> events = new ArrayList<>();
		events.add(failure);
		events.add(repair);
		
		c.consume(1000,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		long[] dds = c.getDowntimeDurations();
		assertEquals(0,dds.length);
	}
	
	@Test
	void testFailingOverFilter_shouldNotFilter_withAlternateRepair() {
		OutageConsumer c = new OutageConsumer("outage1",2);
		c.setIgnoreFailingOver(true);
		
		Event nodeFailure = new TestEvent("f1-node-a",FailureSimulationEvent.FS_FAILED,100, ordinal++);
		Event failingOver = new TestEvent("f1",FailureSimulationEvent.FS_FAILING_OVER, 100, ordinal++, nodeFailure);
		Event failure = new TestEvent("f1",FailureSimulationEvent.FS_FAILED,100,ordinal++,failingOver);
		
		Event nodeRepair = new TestEvent("f1-node-b",FailureSimulationEvent.FS_RETURN_TO_SERVICE,103, ordinal++);
		Event repair = new TestEvent("f1",FailureSimulationEvent.FS_RETURN_TO_SERVICE, 104, ordinal++, nodeRepair);
		
		ArrayList<Event> events = new ArrayList<>();
		events.add(failure);
		events.add(repair);
		
		c.consume(1000,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		long[] dds = c.getDowntimeDurations();
		assertEquals(1,dds.length);
	}
	
	@Test
	void testFailingOverFilter_shouldNotFilter_withExtraFailureRepair() {
		OutageConsumer c = new OutageConsumer("outage1",2);
		c.setIgnoreFailingOver(true);
		
		Event nodeFailure = new TestEvent("f1-node-a",FailureSimulationEvent.FS_FAILED,100, ordinal++);
		Event failingOver = new TestEvent("f1",FailureSimulationEvent.FS_FAILING_OVER, 100, ordinal++, nodeFailure);
		Event failure = new TestEvent("f1",FailureSimulationEvent.FS_FAILED,100,ordinal++,failingOver);
		
		Event nodeRepair1 = new TestEvent("f1-node-a",FailureSimulationEvent.FS_RETURN_TO_SERVICE,103, ordinal++);
		Event nodeFail2 = new TestEvent("f1-node-a",FailureSimulationEvent.FS_FAILED,120, ordinal++);
		Event nodeRepair2 = new TestEvent("f1-node-a", FailureSimulationEvent.FS_RETURN_TO_SERVICE,200,ordinal++);
		Event repair = new TestEvent("f1",FailureSimulationEvent.FS_RETURN_TO_SERVICE, 200, ordinal++, new Event[] {nodeRepair1,nodeFail2,nodeRepair2});
		
		ArrayList<Event> events = new ArrayList<>();
		events.add(failure);
		events.add(repair);
		
		c.consume(1000,  new ConsumerResources() {

			@Override
			public Iterable<Event> getEvents() {
				return events;
			}});
		
		long[] dds = c.getDowntimeDurations();
		assertEquals(1,dds.length);
	}

}
