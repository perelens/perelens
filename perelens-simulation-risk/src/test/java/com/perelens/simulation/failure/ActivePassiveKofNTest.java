package com.perelens.simulation.failure;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.perelens.engine.TestResources;
import com.perelens.engine.api.Event;
import com.perelens.simulation.api.FunctionInfo;
import com.perelens.simulation.api.RandomGenerator;
import com.perelens.simulation.api.RandomProvider;
import com.perelens.simulation.api.TimeTranslator;
import com.perelens.simulation.failure.events.FailureSimulationEvent;
import com.perelens.simulation.random.RanluxProvider;

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
class ActivePassiveKofNTest extends FunctionKofNTest {

	
	@Override
	protected final FunctionKofN newFunctionKofN(String id, int k, int n) {
		return newActivePassiveKofN(id,k,n);
	}
	
	protected ActivePassiveKofN newActivePassiveKofN(String id, int k, int n) {
		return new ActivePassiveKofN(id,k,n);
	}

	@Test
	void testActivePassive1of2() {
		RandomProvider rp = new RanluxProvider(6);

		//Create and initiate the test function
		ActivePassiveKofN tf = newActivePassiveKofN("testFunction",1,2);
		tf.setMeanTimeToFailOver(20);
		tf.setActiveNodes(1);
		
		tf.initiate(new FunctionInfo() {

			@Override
			public Set<String> getDependencies() {
				return new HashSet<>(Arrays.asList(new String[] {"dep0","dep1"}));
			}

			@Override
			public Set<String> getResourcePools() {
				return Collections.emptySet();
			}

			@Override
			public RandomGenerator getRandomGenerator() {
				return rp.createGenerator();
			}

			@Override
			public TimeTranslator getTimeTranslator() {
				return null;
			}});
		
		
		//Need to ensure dep1 is the active node before we can actually test
		Event fail0 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0",50,ordinal++);
		Event rep0  = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep0",60, ordinal++);
		
		TestResources tr = new TestResources(Arrays.asList(new Event[] {fail0,rep0}));
		tf.consume(100, tr);
		
		//Fail dep0 again and make sure there is no outage due to failover
		fail0 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0",150,ordinal++);
		rep0  = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep0",200, ordinal++);
		tr = new TestResources(Arrays.asList(new Event[] {fail0,rep0}));
		
		tf.consume(200, tr);
		
		//There should be 0 raised events
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		//Fail dep1 and make sure there is an outage due to failover
		Event fail1 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep1",250,ordinal++);
		Event rep1 = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep1",300, ordinal++);
		tr = new TestResources(Arrays.asList(new Event[] {fail1,rep1}));
		
		tf.consume(300, tr);
		
		//There should be 2 raised events
		assertEquals(2, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		Iterator<Event> events = tr.getRaisedEvents().iterator();
		Event raisedFail = events.next();
		Event raisedRep = events.next();
		assertFalse(events.hasNext());
		
		//Check failure
		assertEquals(FailureSimulationEvent.FS_FAILED, raisedFail.getType());
		assertEquals(fail1.getTime(),raisedFail.getTime());
		assertEquals(tf.getId(), raisedFail.getProducerId());
		Iterator<Event> causes = raisedFail.causedBy();
		Event cause = causes.next();
		assertEquals(FailureSimulationEvent.FS_FAILING_OVER, cause.getType());
		assertEquals(tf.getId(), cause.getProducerId());
		assertEquals(fail1.getTime(), cause.getTime());
		assertEquals(0, cause.getResponseTypes().size());
		assertFalse(causes.hasNext());

		//Check first repair
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, raisedRep.getType());
		assertEquals(fail1.getTime()+20, raisedRep.getTime());
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, raisedRep.getType());
		assertEquals(0, raisedRep.getResponseTypes().size());
		assertFalse(raisedRep.causedBy().hasNext());
		
		//Fail dep1 again and make sure there is no outage
		fail1 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep1",350,ordinal++);
		rep1 = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep1",400, ordinal++);
		tr = new TestResources(Arrays.asList(new Event[] {fail1,rep1}));

		tf.consume(400, tr);

		//There should be 0 raised events
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		//Fail dep0 and make sure there is an outage due to failover
		fail0 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0",450,ordinal++);
		rep0 = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep0",500, ordinal++);
		tr = new TestResources(Arrays.asList(new Event[] {fail0,rep0}));

		tf.consume(500, tr);

		//There should be 2 raised events
		assertEquals(2, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());

		events = tr.getRaisedEvents().iterator();
		raisedFail = events.next();
		raisedRep = events.next();
		assertFalse(events.hasNext());

		//Check failure
		assertEquals(FailureSimulationEvent.FS_FAILED, raisedFail.getType());
		assertEquals(fail0.getTime(),raisedFail.getTime());
		assertEquals(tf.getId(), raisedFail.getProducerId());
		causes = raisedFail.causedBy();
		cause = causes.next();
		assertEquals(FailureSimulationEvent.FS_FAILING_OVER, cause.getType());
		assertEquals(tf.getId(), cause.getProducerId());
		assertEquals(fail0.getTime(), cause.getTime());
		assertEquals(0, cause.getResponseTypes().size());
		assertFalse(causes.hasNext());

		//Check repair
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, raisedRep.getType());
		assertEquals(fail0.getTime()+20, raisedRep.getTime());
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, raisedRep.getType());
		assertEquals(0, raisedRep.getResponseTypes().size());
		assertFalse(raisedRep.causedBy().hasNext());
		
		//Fail both dependencies to create a real outage. Should be no failover fault before hand
		fail0 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0",520,ordinal++);
		fail1 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep1",550,ordinal++);
		
		tr = new TestResources(Arrays.asList(new Event[] {fail0,fail1}));

		tf.consume(600, tr);
		
		//There should be one raised event
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());

		Iterator<Event> raised = tr.getRaisedEvents().iterator();
		Event raisedEvent = raised.next();
		assertFalse(raised.hasNext());
		assertEquals(FailureSimulationEvent.FS_FAILED, raisedEvent.getType());
		assertEquals(fail1.getTime(),raisedEvent.getTime());

		//It should be caused by 2 failure events
		Iterator<Event> caused = raisedEvent.causedBy();
		Event cause1 = caused.next();
		Event cause2 = caused.next();
		assertFalse(caused.hasNext());
		assertEquals(fail0,cause1);
		assertEquals(fail1,cause2);
		
		//Repair both dependencies and make sure the one repaired first (dep0) becomes the active one
		rep0 = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep0",620, ordinal++);
		rep1 = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep1",630, ordinal++);
		tr = new TestResources(Arrays.asList(new Event[] {rep0,rep1}));

		tf.consume(700, tr);
		
		//There should be one raised event
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());

		raised = tr.getRaisedEvents().iterator();
		Event raisedRep1 = raised.next();
		assertFalse(raised.hasNext());

		//Check the value
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, raisedRep1.getType());
		assertEquals(rep0.getTime(),raisedRep1.getTime());
		causes = raisedRep1.causedBy();
		assertEquals(rep0, causes.next());
		assertFalse(causes.hasNext());
		
		//Fail dep1 again and make sure there is no outage
		fail1 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep1",750,ordinal++);
		rep1 = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep1",800, ordinal++);
		tr = new TestResources(Arrays.asList(new Event[] {fail1,rep1}));

		tf.consume(800, tr);

		//There should be 0 raised events
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		//Fail dep0 and make sure there is an outage due to failover
		fail0 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0",850,ordinal++);
		rep0 = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep0",900, ordinal++);
		tr = new TestResources(Arrays.asList(new Event[] {fail0,rep0}));

		tf.consume(900, tr);

		//There should be 2 raised events
		assertEquals(2, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());

		events = tr.getRaisedEvents().iterator();
		raisedFail = events.next();
		raisedRep = events.next();
		assertFalse(events.hasNext());

		//Check failure
		assertEquals(FailureSimulationEvent.FS_FAILED, raisedFail.getType());
		assertEquals(fail0.getTime(),raisedFail.getTime());
		assertEquals(tf.getId(), raisedFail.getProducerId());
		causes = raisedFail.causedBy();
		cause = causes.next();
		assertEquals(FailureSimulationEvent.FS_FAILING_OVER, cause.getType());
		assertEquals(tf.getId(), cause.getProducerId());
		assertEquals(fail0.getTime(), cause.getTime());
		assertEquals(0, cause.getResponseTypes().size());
		assertFalse(causes.hasNext());

		//Check repair
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, raisedRep.getType());
		assertEquals(fail0.getTime()+20, raisedRep.getTime());
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, raisedRep.getType());
		assertEquals(0, raisedRep.getResponseTypes().size());
		assertFalse(raisedRep.causedBy().hasNext());
	}

}
