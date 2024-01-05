package com.perelens.simulation.failure;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.perelens.engine.TestFunctionInfo;
import com.perelens.engine.TestResources;
import com.perelens.engine.api.Event;
import com.perelens.simulation.api.Distribution;
import com.perelens.simulation.api.DistributionProvider;
import com.perelens.simulation.api.FunctionInfo;
import com.perelens.simulation.api.RandomGenerator;
import com.perelens.simulation.api.RandomProvider;
import com.perelens.simulation.api.TimeTranslator;
import com.perelens.simulation.core.CoreDistributionProvider;
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
class FunctionKofNTest {

	long ordinal = 1;
	
	protected FunctionKofN newFunctionKofN(String id, int k, int n) {
		return new FunctionKofN(id,k,n);
	}
	
	@Test
	void testSimpleFailureAndRepair() {
		//Test all combinations of up to 100 dependencies
		for (int n = 1; n <= 100; n++) {
			for (int k = 1; k <= n; k++) {
				try {
					testKofN_simple(k,n);
				}catch(Throwable t) {
					throw new Error(t);
				}
			}
		}
	}
	
	@Test
	void testMultipleFailuresInOneWindow() {
		RandomProvider rp = new RanluxProvider(3);

		//Create and initiate the test function
		FunctionKofN tf = newFunctionKofN("testFunction",2,3);
		tf.initiate(new FunctionInfo() {

			@Override
			public Set<String> getDependencies() {
				return new HashSet<String>(Arrays.asList(new String[] {"dep0","dep1","dep2"}));
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
		
		Event fail1 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0",50,ordinal++);
		Event fail2 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep1",60,ordinal++);
		
		TestResources tr = new TestResources(Arrays.asList(new Event[] {fail1,fail2}));
		tf.consume(100, tr);
		
		//There should be one raised event
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		Iterator<Event> raised = tr.getRaisedEvents().iterator();
		Event raisedEvent = raised.next();
		assertFalse(raised.hasNext());
		assertEquals(FailureSimulationEvent.FS_FAILED, raisedEvent.getType());
		
		//It should be caused by 2 failure events
		Iterator<Event> caused = raisedEvent.causedBy();
		Event cause1 = caused.next();
		Event cause2 = caused.next();
		assertFalse(caused.hasNext());
		assertEquals(fail1,cause1);
		assertEquals(fail2,cause2);
		
		//The raised event should have the same time as the second failure event
		assertEquals(fail2.getTime(),raisedEvent.getTime());	
	}
	
	@Test
	void testRestoreTime() {
		RandomProvider rp = new RanluxProvider(4);

		//Create and initiate the test function
		FunctionKofN tf = newFunctionKofN("testFunction",3,4);
		tf.setRestoreTime(20);
		
		tf.initiate(new FunctionInfo() {

			@Override
			public Set<String> getDependencies() {
				return new HashSet<>(Arrays.asList(new String[] {"dep0","dep1","dep2","dep3"}));
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
		
		Event fail1 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0",50,ordinal++);
		Event fail2 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep1",60,ordinal++);
		
		TestResources tr = new TestResources(Arrays.asList(new Event[] {fail1,fail2}));
		tf.consume(100, tr);
		
		//There should be one raised failure event
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		Iterator<Event> raised = tr.getRaisedEvents().iterator();
		Event raisedEvent = raised.next();
		assertFalse(raised.hasNext());
		assertEquals(FailureSimulationEvent.FS_FAILED,raisedEvent.getType());
		
		Event repair = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep0",150,ordinal++);
		tr = new TestResources(Arrays.asList(new Event[] {repair}));
		tf.consume(200, tr);
		
		//There should be one raised return to service event
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());

		raised = tr.getRaisedEvents().iterator();
		raisedEvent = raised.next();
		assertFalse(raised.hasNext());
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE,raisedEvent.getType());
		
		//The return to service event should be after the injected event
		assertEquals(repair.getTime() + 20, raisedEvent.getTime());
		
	}
	
	@Test
	void testRestoreTimeInNextWindow() {
		RandomProvider rp = new RanluxProvider(5);

		//Create and initiate the test function
		FunctionKofN tf = newFunctionKofN("testFunction",3,4);
		tf.setRestoreTime(20);
		
		tf.initiate(new FunctionInfo() {

			@Override
			public Set<String> getDependencies() {
				return new HashSet<>(Arrays.asList(new String[] {"dep0","dep1","dep2","dep3"}));
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
		
		Event fail1 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0",50,ordinal++);
		Event fail2 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep1",60,ordinal++);
		
		TestResources tr = new TestResources(Arrays.asList(new Event[] {fail1,fail2}));
		tf.consume(100, tr);
		
		//There should be one raised failure event
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		Iterator<Event> raised = tr.getRaisedEvents().iterator();
		Event raisedEvent = raised.next();
		assertFalse(raised.hasNext());
		assertEquals(FailureSimulationEvent.FS_FAILED,raisedEvent.getType());
		
		//Inject repair event
		Event repair = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep0",190,ordinal++);
		tr = new TestResources(Arrays.asList(new Event[] {repair}));
		tf.consume(200, tr);
		
		//Should be no events because restore time pushes return to service into the next simulation window
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		//There should be one raised return to service event
		tr = new TestResources(Collections.emptyList());
		tf.consume(300, tr);
		
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());

		raised = tr.getRaisedEvents().iterator();
		raisedEvent = raised.next();
		assertFalse(raised.hasNext());
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE,raisedEvent.getType());
		
		//The return to service event should be after the injected event
		assertEquals(repair.getTime() + 20, raisedEvent.getTime());
	}
	
	@Test
	void testFailBeforeRestoreTime() {
		RandomProvider rp = new RanluxProvider(6);

		//Create and initiate the test function
		FunctionKofN tf = newFunctionKofN("testFunction",2,3);
		tf.setRestoreTime(50);
		
		tf.initiate(new FunctionInfo() {

			@Override
			public Set<String> getDependencies() {
				return new HashSet<>(Arrays.asList(new String[] {"dep0","dep1","dep2"}));
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
		
		Event fail1 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0",50,ordinal++);
		Event fail2 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep1",60,ordinal++);
		
		TestResources tr = new TestResources(Arrays.asList(new Event[] {fail1,fail2}));
		tf.consume(100, tr);
		
		//There should be one raised failure event
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		Iterator<Event> raised = tr.getRaisedEvents().iterator();
		Event raisedEvent = raised.next();
		assertFalse(raised.hasNext());
		assertEquals(FailureSimulationEvent.FS_FAILED,raisedEvent.getType());
		
		//Inject repair event
		Event repair = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep0",190,ordinal++);
		tr = new TestResources(Arrays.asList(new Event[] {repair}));
		tf.consume(200, tr);
		
		//Should be no events because restore time pushes return to service into the next simulation window
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		//The failure during restore period should prevent the return to service from being raised
		Event refail = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep3", 210,ordinal++);
		tr = new TestResources(Collections.singletonList(refail));
		tf.consume(300, tr);
		
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());

		//Make sure the repair and failure events are in the causedBy of the final event
		Event rerepair = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep3", 350,ordinal++);
		tr = new TestResources(Collections.singletonList(rerepair));
		tf.consume(400, tr);
		
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		raised = tr.getRaisedEvents().iterator();
		raisedEvent = raised.next();
		assertFalse(raised.hasNext());
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE,raisedEvent.getType());
		
		//The return to service event should be after the injected event
		assertEquals(rerepair.getTime() + 50, raisedEvent.getTime());
		Iterator<Event> cbiter = raisedEvent.causedBy();
		Event e1 = cbiter.next();
		Event e2 = cbiter.next();
		Event e3 = cbiter.next();
		assertFalse(raised.hasNext());
		
		assertEquals(repair,e1);
		assertEquals(refail,e2);
		assertEquals(rerepair,e3);
	}
	
	@Test
	void testMultipleRestoreAndFailsInSameWindow() {
		RandomProvider rp = new RanluxProvider(6);

		//Create and initiate the test function
		FunctionKofN tf = newFunctionKofN("testFunction",2,3);
		tf.setRestoreTime(10);
		
		tf.initiate(new FunctionInfo() {

			@Override
			public Set<String> getDependencies() {
				return new HashSet<>(Arrays.asList(new String[] {"dep0","dep1","dep2"}));
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
		
		Event fail1 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0",50,ordinal++);
		Event fail2 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep1",60,ordinal++);
		Event repair1 = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep0",100,ordinal++);
		Event fail3 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0", 150,ordinal++);
		Event repair2 = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep1", 200,ordinal++);
		Event repair3 = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep0", 210,ordinal++);
		Event fail4 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep3",300,ordinal++);
		Event fail5 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep1",350,ordinal++);
		
		TestResources tr = new TestResources(Arrays.asList(new Event[] {fail1,fail2,repair1,fail3,repair2,repair3,fail4,fail5}));
		tf.consume(400, tr);
		
		//There should be 5 raised events
		assertEquals(5, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		Iterator<Event> raised = tr.getRaisedEvents().iterator();
		Event raisedFail1 = raised.next();
		Event raisedRep1 = raised.next();
		Event raisedFail2 = raised.next();
		Event raisedRep2 = raised.next();
		Event raisedFail3 = raised.next();
		assertFalse(raised.hasNext());
		
		//Check first failure
		assertEquals(FailureSimulationEvent.FS_FAILED, raisedFail1.getType());
		assertEquals(fail2.getTime(),raisedFail1.getTime());
		Iterator<Event> causedBy = raisedFail1.causedBy();
		assertEquals(fail1, causedBy.next());
		assertEquals(fail2, causedBy.next());
		assertFalse(causedBy.hasNext());
		
		//Check first repair
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, raisedRep1.getType());
		assertEquals(repair1.getTime()+10, raisedRep1.getTime());
		causedBy = raisedRep1.causedBy();
		assertEquals(repair1,causedBy.next());
		assertFalse(causedBy.hasNext());
		
		//Check Second Fail
		assertEquals(FailureSimulationEvent.FS_FAILED,raisedFail2.getType());
		assertEquals(fail3.getTime(),raisedFail2.getTime());
		causedBy = raisedFail2.causedBy();
		assertEquals(fail2,causedBy.next());
		assertEquals(fail3,causedBy.next());
		assertFalse(causedBy.hasNext());
		
		//Check the second repair
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, raisedRep2.getType());
		assertEquals(repair2.getTime() + 10, raisedRep2.getTime());
		causedBy = raisedRep2.causedBy();
		assertEquals(repair2, causedBy.next());
		assertEquals(repair3, causedBy.next());
		assertFalse(causedBy.hasNext());
		
		//Check the third fail
		assertEquals(FailureSimulationEvent.FS_FAILED, raisedFail3.getType());
		assertEquals(fail5.getTime(), raisedFail3.getTime());
		causedBy = raisedFail3.causedBy();
		assertEquals(fail4, causedBy.next());
		assertEquals(fail5,causedBy.next());
		assertFalse(causedBy.hasNext());
	}
	
	@Test
	void testSimultaniousFailureAndRepair() {
		RandomProvider rp = new RanluxProvider(6);

		//Create and initiate the test function
		FunctionKofN tf = newFunctionKofN("testFunction",2,3);
		tf.setRestoreTime(50);
		
		tf.initiate(new FunctionInfo() {

			@Override
			public Set<String> getDependencies() {
				return new HashSet<>(Arrays.asList(new String[] {"dep0","dep1","dep2"}));
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
		
		Event fail1 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0",50,ordinal++);
		Event fail2 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep1",50,ordinal++);
		
		TestResources tr = new TestResources(Arrays.asList(new Event[] {fail1,fail2}));
		tf.consume(100, tr);
		
		//There should be 1 raised events
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		Iterator<Event> raised = tr.getRaisedEvents().iterator();
		Event raisedFail1 = raised.next();
		assertFalse(raised.hasNext());
		
		//Check first failure
		assertEquals(FailureSimulationEvent.FS_FAILED, raisedFail1.getType());
		assertEquals(fail2.getTime(),raisedFail1.getTime());
		assertEquals(fail1.getTime(),raisedFail1.getTime());
		Iterator<Event> causedBy = raisedFail1.causedBy();
		assertEquals(fail1, causedBy.next());
		assertEquals(fail2, causedBy.next());
		assertFalse(causedBy.hasNext());
		
		//Simultanious repair
		Event rep1 = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "dep0", 101,ordinal++);
		Event rep2 = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE, "dep1", 101,ordinal++);
		
		tr = new TestResources(Arrays.asList(new Event[] {rep1,rep2}));
		tf.consume(200, tr);
		
		//Should be 1 raised event
		raised = tr.getRaisedEvents().iterator();
		Event raisedRep1 = raised.next();
		assertFalse(raised.hasNext());
		
		//Check the value
		//Check first failure
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, raisedRep1.getType());
		assertEquals(rep1.getTime() + 50,raisedRep1.getTime());
		assertEquals(rep2.getTime() + 50,raisedRep1.getTime());
		causedBy = raisedRep1.causedBy();
		assertEquals(rep1, causedBy.next());
		assertEquals(rep2, causedBy.next());
		assertFalse(causedBy.hasNext());
	}
	
	@Test
	void testFailThenRepairAtSameTime_NoRestoreTime() {
		RandomProvider rp = new RanluxProvider(7);

		//Create and initiate the test function
		FunctionKofN tf = newFunctionKofN("testFunction",2,3);
		
		tf.initiate(new FunctionInfo() {

			@Override
			public Set<String> getDependencies() {
				return new HashSet<>(Arrays.asList(new String[] {"dep0","dep1","dep2"}));
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
		
		Event fail1 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0",50,ordinal++);
		Event fail2 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep1",50,ordinal++);
		Event rep1 = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep1",50,ordinal++);
		
		TestResources tr = new TestResources(Arrays.asList(new Event[] {fail1,fail2,rep1}));
		tf.consume(100, tr);
		
		//There should be 2 raised events
		assertEquals(2, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());

		Iterator<Event> raised = tr.getRaisedEvents().iterator();
		Event raisedFail = raised.next();
		Event raisedRep = raised.next();
		assertFalse(raised.hasNext());
		
		//Check the failure
		assertEquals(FailureSimulationEvent.FS_FAILED,raisedFail.getType());
		Iterator<Event> causedBy = raisedFail.causedBy();
		assertEquals(fail1, causedBy.next());
		assertEquals(fail2, causedBy.next());
		assertFalse(causedBy.hasNext());
		assertEquals(fail2.getTime(),raisedFail.getTime());
		
		//Check the repair
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, raisedRep.getType());
		causedBy = raisedRep.causedBy();
		assertEquals(rep1, causedBy.next());
		assertFalse(causedBy.hasNext());
		assertEquals(raisedFail.getTime(),raisedRep.getTime());
	}
	
	@Test
	void testRepairThenFailAtSameTime_NoRestoreTime() {
		RandomProvider rp = new RanluxProvider(9);

		//Create and initiate the test function
		FunctionKofN tf = newFunctionKofN("testFunction",2,3);
		tf.initiate(new FunctionInfo() {

			@Override
			public Set<String> getDependencies() {
				return new HashSet<>(Arrays.asList(new String[] {"dep0","dep1","dep2"}));
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
		
		Event fail1 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0",50,ordinal++);
		Event fail2 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep1",60,ordinal++);
		
		TestResources tr = new TestResources(Arrays.asList(new Event[] {fail1,fail2}));
		tf.consume(100, tr);
		
		//There should be one raised event
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		Iterator<Event> raised = tr.getRaisedEvents().iterator();
		Event raisedEvent = raised.next();
		assertFalse(raised.hasNext());
		assertEquals(FailureSimulationEvent.FS_FAILED, raisedEvent.getType());
		
		//Inject a repair and then a failure at the same time  should get a repair event then a failure event
		Event rep1 = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep0", 190,ordinal++);
		Event fail3 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep2",190,ordinal++);
		
		tr = new TestResources(Arrays.asList(new Event[] {rep1,fail3}));
		tf.consume(200, tr);
		
		//There should be no raised event
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		raised = tr.getRaisedEvents().iterator();
		assertFalse(raised.hasNext());
	}
	
	@Test
	void testFailThenRepairAtSameTime2_NoRestoreTime() {
		RandomProvider rp = new RanluxProvider(10);

		//Create and initiate the test function
		FunctionKofN tf = newFunctionKofN("testFunction",2,3);
		tf.initiate(new FunctionInfo() {

			@Override
			public Set<String> getDependencies() {
				return new HashSet<>(Arrays.asList(new String[] {"dep0","dep1","dep2"}));
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
		
		Event fail1 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0",50,ordinal++);
		Event fail2 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep1",60,ordinal++);
		
		TestResources tr = new TestResources(Arrays.asList(new Event[] {fail1,fail2}));
		tf.consume(100, tr);
		
		//There should be one raised event
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		Iterator<Event> raised = tr.getRaisedEvents().iterator();
		Event raisedEvent = raised.next();
		assertFalse(raised.hasNext());
		assertEquals(FailureSimulationEvent.FS_FAILED, raisedEvent.getType());
		
		//Inject a repair and then a failure at the same time but order the failure before the repair
		Event rep1 = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep0", 190,ordinal++);
		Event fail3 = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep2",190,ordinal++);
		
		tr = new TestResources(Arrays.asList(new Event[] {fail3,rep1}));
		tf.consume(200, tr);
		
		//There should be no raised events since the additional failure prevents a repair
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
	}
	
	protected void testKofN_simple(int k, int n) {
		RandomProvider rp = new RanluxProvider(n * k);
		
		//Init the dependency list
		String[] deps = new String[n];
		for (int i = 0; i < deps.length; i++) {
			deps[i] = "dep" + i;
		}
		
		//Create and initiate the test function
		FunctionKofN tf = newFunctionKofN("testFunction",k,n);
		tf.initiate(new FunctionInfo() {

			@Override
			public Set<String> getDependencies() {
				return new HashSet<>(Arrays.asList(deps));
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
		
		ArrayList<Event> failureEvents = new ArrayList<>();
		Event repairEvent = null;
		for (long i = 1; i < n + 20 ; i++) {
			
			long timeOffset = i * 100;
			
			if (i == 1) {
				TestResources tr = new TestResources(Collections.emptyList());
				tf.consume(timeOffset, tr);
				
				assertEquals(0, tr.getRaisedEvents().size());
				assertEquals(0, tr.getRaisedResponses().size());
			}else if (failureEvents.size() < n - k) {
				//Injecting failure events that should not cause the function to fail
				Event failEvent = new FailSimEvent(FailureSimulationEvent.FS_FAILED,deps[(int) (i-2)],timeOffset - 50,ordinal++);
				TestResources tr = new TestResources(Collections.singletonList(failEvent));
				failureEvents.add(failEvent);
				tf.consume(timeOffset, tr);
				
				assertEquals(0, tr.getRaisedEvents().size());
				assertEquals(0, tr.getRaisedResponses().size());
			}else if (failureEvents.size() == n - k) {
				//Inject the failure event that will cause the function to fail
				Event failEvent = new FailSimEvent(FailureSimulationEvent.FS_FAILED,deps[(int) (i-2)],timeOffset - 50,ordinal++);
				TestResources tr = new TestResources(Collections.singletonList(failEvent));
				failureEvents.add(failEvent);
				tf.consume(timeOffset, tr);
				
				//There should be one raised event
				assertEquals(1, tr.getRaisedEvents().size());
				assertEquals(0, tr.getRaisedResponses().size());
				
				Iterator<Event> raised = tr.getRaisedEvents().iterator();
				Event raisedEvent = raised.next();
				assertFalse(raised.hasNext());
				assertEquals(FailureSimulationEvent.FS_FAILED,raisedEvent.getType());
				
				//There should be an event for each accumulated failure in causedBy()
				ArrayList<Event> causing = new ArrayList<>();
				for (Iterator<Event> causes = raisedEvent.causedBy(); causes.hasNext();) {
					causing.add(causes.next());
				}
				
				assertEquals(n-k + 1, causing.size());
				assertEquals(failureEvents.size(),causing.size());
				assertTrue(failureEvents.equals(causing));
				assertEquals(causing.get(causing.size()-1), failEvent);
				
				//The raisedEvent time should be the same as the finalFailure
				assertEquals(raisedEvent.getTime(),failEvent.getTime());
			}else {
				repairEvent = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,"dep0",timeOffset - 50,ordinal++);
				TestResources tr = new TestResources(Collections.singletonList(repairEvent));
				tf.consume(timeOffset, tr);
				
				assertEquals(1, tr.getRaisedEvents().size());
				assertEquals(0, tr.getRaisedResponses().size());
				
				Iterator<Event> raised = tr.getRaisedEvents().iterator();
				Event raisedEvent = raised.next();
				assertFalse(raised.hasNext());
				
				Iterator<Event>caused = raisedEvent.causedBy();
				Event causedBy = caused.next();
				assertFalse(raised.hasNext());
				
				assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE,raisedEvent.getType());
				
				//Make sure the failure event that was injected is the cause of the generated failure event
				assertEquals(repairEvent,causedBy);
				
				//Make sure the generated failure event has the same time as the causing event
				assertEquals(repairEvent.getTime(), raisedEvent.getTime());
				
				break;
			}	
		}
		
		assertEquals(n-k + 1, failureEvents.size());
		assertNotNull(repairEvent);
	}
	
	@Test
	void testMeanTimeToFailOverStatic_singleWindow() {
		RandomProvider rp = new RanluxProvider(3);

		//Create and initiate the test function
		FunctionKofN tf = newFunctionKofN("testFunction",2,3);
		tf.setMeanTimeToFailOver(25);
		
		tf.initiate(new FunctionInfo() {

			@Override
			public Set<String> getDependencies() {
				return new HashSet<>(Arrays.asList(new String[] {"dep0","dep1","dep2"}));
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
		
		
		Event failEvent = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0",50,ordinal++);
		TestResources tr = new TestResources(Collections.singletonList(failEvent));
		
		tf.consume(100, tr);
		
		assertEquals(2, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		Iterator<Event> events = tr.getRaisedEvents().iterator();
		Event raisedFail = events.next();
		assertEquals(50, raisedFail.getTime());
		assertEquals(FailureSimulationEvent.FS_FAILED,raisedFail.getType());
		assertEquals(0, raisedFail.getResponseTypes().size());
		
		Iterator<Event> causes = raisedFail.causedBy();
		Event cause = causes.next();
		assertEquals(FailureSimulationEvent.FS_FAILING_OVER, cause.getType());
		assertEquals(tf.getId(), cause.getProducerId());
		assertEquals(50, cause.getTime());
		assertEquals(0, cause.getResponseTypes().size());
		assertFalse(causes.hasNext());
		
		Event rts = events.next();
		assertEquals(75, rts.getTime());
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, rts.getType());
		assertEquals(0, rts.getResponseTypes().size());
		assertFalse(rts.causedBy().hasNext());
		
		assertFalse(events.hasNext());		
	}
	
	@Test
	void testMeanTimeToFailOverStatic_splitWindow() {
		RandomProvider rp = new RanluxProvider(3);

		//Create and initiate the test function
		FunctionKofN tf = newFunctionKofN("testFunction",2,3);
		tf.setMeanTimeToFailOver(25);
		
		tf.initiate(new FunctionInfo() {

			@Override
			public Set<String> getDependencies() {
				return new HashSet<>(Arrays.asList(new String[] {"dep0","dep1","dep2"}));
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
		
		
		Event failEvent = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0",50,ordinal++);
		TestResources tr = new TestResources(Collections.singletonList(failEvent));
		
		tf.consume(60, tr);
		
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		Iterator<Event> events = tr.getRaisedEvents().iterator();
		Event raisedFail = events.next();
		assertEquals(50, raisedFail.getTime());
		assertEquals(FailureSimulationEvent.FS_FAILED,raisedFail.getType());
		assertEquals(0, raisedFail.getResponseTypes().size());
		
		Iterator<Event> causes = raisedFail.causedBy();
		Event cause = causes.next();
		assertEquals(FailureSimulationEvent.FS_FAILING_OVER, cause.getType());
		assertEquals(tf.getId(), cause.getProducerId());
		assertEquals(50, cause.getTime());
		assertEquals(0, cause.getResponseTypes().size());
		assertFalse(causes.hasNext());
		
		//Return to service should be in the next window
		tr = new TestResources(Collections.emptySet());
		tf.consume(100, tr);
		
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		events = tr.getRaisedEvents().iterator();
		
		Event rts = events.next();
		assertEquals(75, rts.getTime());
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, rts.getType());
		assertEquals(0, rts.getResponseTypes().size());
		assertFalse(rts.causedBy().hasNext());
		
		assertFalse(events.hasNext());		
	}
	
	@Test
	void testMeanTimeToFailOver_distribution() {
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution dr = dp.exponential(50);
		
		FunctionInfo fi = new TestFunctionInfo() {
			
			@Override
			public Set<String> getDependencies() {
				return new HashSet<>(Arrays.asList(new String[] {"dep0","dep1","dep2"}));
			}
			
			@Override
			public RandomGenerator getRandomGenerator() {
				return new RandomGenerator() {

					private static final long serialVersionUID = 1L;

					@Override
					public double nextDouble() {
						//always return 0.5 so we know what he distribution should be doing
						return 0.5;
					}

					@Override
					public String getRandomSetup() {
						return "constant";
					}

					@Override
					public RandomGenerator copy() {
						throw new IllegalStateException("not implemented");
					}
					
				};
			}

			@Override
			public TimeTranslator getTimeTranslator() {
				
				return null;
			}
			
		};
		
		long mtfo = (long)Math.ceil(dr.sample(0.5));
		FunctionKofN tf = newFunctionKofN("testFunction",2,3);
		tf.setTimeToFailOver(dr);
		
		tf.initiate(fi);
		
		Event failEvent = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0",50,ordinal++);
		TestResources tr = new TestResources(Collections.singletonList(failEvent));
		
		tf.consume(100, tr);
		
		assertEquals(2, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		Iterator<Event> events = tr.getRaisedEvents().iterator();
		Event raisedFail = events.next();
		assertEquals(50, raisedFail.getTime());
		assertEquals(FailureSimulationEvent.FS_FAILED,raisedFail.getType());
		assertEquals(0, raisedFail.getResponseTypes().size());
		
		Iterator<Event> causes = raisedFail.causedBy();
		Event cause = causes.next();
		assertEquals(FailureSimulationEvent.FS_FAILING_OVER, cause.getType());
		assertEquals(tf.getId(), cause.getProducerId());
		assertEquals(50, cause.getTime());
		assertEquals(0, cause.getResponseTypes().size());
		assertFalse(causes.hasNext());
		
		Event rts = events.next();
		assertEquals(50 + mtfo, rts.getTime());
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, rts.getType());
		assertEquals(0, rts.getResponseTypes().size());
		assertFalse(rts.causedBy().hasNext());
		
		assertFalse(events.hasNext());		
	}
	
	@Test
	void testFailOverFault() {
		FunctionInfo fi = new TestFunctionInfo() {
			
			@Override
			public Set<String> getDependencies() {
				return new HashSet<>(Arrays.asList(new String[] {"dep0","dep1","dep2"}));
			}
			
			@Override
			public RandomGenerator getRandomGenerator() {
				return new RandomGenerator() {

					private static final long serialVersionUID = 1L;

					@Override
					public double nextDouble() {
						return 0.01;
					}

					@Override
					public String getRandomSetup() {
						return "constant";
					}

					@Override
					public RandomGenerator copy() {
						throw new IllegalStateException("not implemented");
					}
					
				};
			}

			@Override
			public TimeTranslator getTimeTranslator() {
				
				return null;
			}
			
		};
		
		FunctionKofN tf = newFunctionKofN("testFunction",2,3);
		tf.setRestoreTime(33);
		tf.setFailOverFaultPercentage(0.05d);
		
		tf.initiate(fi);
		
		assertTrue(FailureUtils.processFails(0.05, 0.01));
		
		Event failEvent = new FailSimEvent(FailureSimulationEvent.FS_FAILED,"dep0",50,ordinal++);
		TestResources tr = new TestResources(Collections.singletonList(failEvent));
		
		tf.consume(100, tr);
		
		assertEquals(2, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		Iterator<Event> events = tr.getRaisedEvents().iterator();
		Event raisedFail = events.next();
		assertEquals(50, raisedFail.getTime());
		assertEquals(FailureSimulationEvent.FS_FAILED,raisedFail.getType());
		assertEquals(0, raisedFail.getResponseTypes().size());
		
		Iterator<Event> causes = raisedFail.causedBy();
		Event cause = causes.next();
		assertEquals(FailureSimulationEvent.FS_FAILOVER_FAULT, cause.getType());
		assertEquals(tf.getId(), cause.getProducerId());
		assertEquals(50, cause.getTime());
		assertEquals(0, cause.getResponseTypes().size());
		assertFalse(causes.hasNext());
		
		Event rts = events.next();
		assertEquals(50 + 33, rts.getTime());
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, rts.getType());
		assertEquals(0, rts.getResponseTypes().size());
		assertFalse(rts.causedBy().hasNext());
		
		assertFalse(events.hasNext());			
	}
	
}
