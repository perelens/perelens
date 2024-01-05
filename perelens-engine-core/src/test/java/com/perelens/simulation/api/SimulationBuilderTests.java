/**
 * 
 */
package com.perelens.simulation.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import com.perelens.engine.TestFunction;
import com.perelens.engine.TestResourcePool;
import com.perelens.engine.api.ConsumerResources;
import com.perelens.engine.api.EvaluatorResources;
import com.perelens.engine.api.EventConsumer;
import com.perelens.engine.api.EventGenerator;
import com.perelens.simulation.random.RanluxProvider;

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
public abstract class SimulationBuilderTests {

	protected abstract SimulationBuilder getSimulationBuilder();
	
	protected abstract TimeTranslator getTimeTranslator();
	
	@Test
	void testAddFunction() {
		
		SimulationBuilder sb = getSimulationBuilder();
		
		String hash1 = sb.getHashCode();
		assertNull(sb.getFunction("f1"));
		
		Function f1 = new TestFunction("f1", Collections.emptyMap());
		sb.addFunction(f1);
		
		String hash2 = sb.getHashCode();
		
		assertNotNull(sb.getFunction("f1"));
		assertFalse(hash1.equals(hash2));
	}
	
	@Test
	void testAddDependency() {
		
		SimulationBuilder sb = getSimulationBuilder();
		
		assertNull(sb.getFunction("f1"));
		
		Function f1 = new TestFunction("f1", Collections.emptyMap());
		sb.addFunction(f1);
		assertNotNull(sb.getFunction("f1"));
		
		Function f2 = new TestFunction("f2", Collections.emptyMap());
		sb.addFunction(f2);
		assertNotNull(sb.getFunction("f2"));
		
		String hashBefore = sb.getHashCode();
		
		sb.getFunction("f1").addDependency(sb.getFunction("f2"));
		
		String hashAfter = sb.getHashCode();
		
		assertFalse(hashBefore.equals(hashAfter));
		assertEquals(1,sb.getFunction("f1").getDependencies().size());
		assertEquals("f2",sb.getFunction("f1").getDependencies().iterator().next().getId());
	}
	
	@Test
	void testAddRandomProvider() {
		SimulationBuilder sb = getSimulationBuilder();
		
		String hashBefore = sb.getHashCode();
		
		sb.setRandomProvider(new RanluxProvider(1));
		
		String hashA1 = sb.getHashCode();
		
		assertFalse(hashBefore.equals(hashA1));
		
		sb.setRandomProvider(new RanluxProvider(2));
		
		String hashA2 = sb.getHashCode();
		assertFalse(hashA1.equals(hashA2));
		assertFalse(hashBefore.equals(hashA2));
		assertTrue(hashA2.equals(sb.getHashCode()));
		
	}
	
	@Test
	void testAddRemoveResourcePool() {
		SimulationBuilder sb = getSimulationBuilder();
		
		sb.addFunction(new TestFunction("f1", Collections.emptyMap()));
		
		String hashBefore = sb.getHashCode();
		sb.addResourcePool(new TestResourcePool("rp1"));
		
		String hashAfter1 = sb.getHashCode();
		
		assertFalse(hashBefore.equals(hashAfter1));
		
		sb.addResourcePool(new TestResourcePool("rp2"));
		
		String hashAfter2 = sb.getHashCode();
		
		assertFalse(hashBefore.equals(hashAfter2));
		assertFalse(hashAfter1.equals(hashAfter2));
		assertTrue(hashAfter2.equals(sb.getHashCode()));
		
		sb.getFunction("f1").addResourcePool("rp1");
		
		String hashAfter3 = sb.getHashCode();
		assertFalse(hashAfter2.equals(hashAfter3));
		
		Collection<String> rps = sb.getFunction("f1").getResourcePools();
		assertEquals(1,rps.size());
		assertEquals("rp1", rps.iterator().next());
		
		sb.getFunction("f1").removeResourcePool("rp1");
		
		String hashAfter4= sb.getHashCode();
		
		assertEquals(hashAfter2,hashAfter4);
		assertEquals(0, sb.getFunction("f1").getResourcePools().size());
	}
	
	@Test
	void testAddRemoveTimeTranslator() {
		SimulationBuilder sb = getSimulationBuilder();
		
		sb.addFunction(new TestFunction("f1", Collections.emptyMap()));
		
		String hashBefore = sb.getHashCode();
		
		sb.setTimeTranslator(getTimeTranslator());
		
		String hashAfter = sb.getHashCode();
		
		assertFalse(hashBefore.equals(hashAfter));
		
		sb.setTimeTranslator(null);
		hashAfter = sb.getHashCode();
		assertEquals(hashBefore,hashAfter);
	}
	
	@Test
	void testCreateSimulation() throws Throwable {
		ConcurrentHashMap<CountFunction,Boolean> initRecord = new ConcurrentHashMap<>();
		CountFunction f1 = new CountFunction("c1",initRecord);
		
		SimulationBuilder sb = getSimulationBuilder();
		sb.addFunction(f1);
		assertEquals(0,initRecord.size());
		
		Simulation sim1 = sb.createSimulation(4);
		assertEquals(1, initRecord.size());
		CountFunction sim1f = initRecord.keySet().iterator().next();
		
		Simulation sim2 = sb.createSimulation(4);
		assertEquals(2, initRecord.size());
		CountFunction sim2f = null;
		
		for (CountFunction cf : initRecord.keySet()) {
			assertFalse(f1 == cf);
			if (cf != sim1f) {
				sim2f = cf;
			}
		}
		
		//Register at least one event consumer so the simulation will run.
		sim1.registerGlobalConsumer(new EventConsumer() {

			@Override
			public String getId() {
				return "ec1";
			}

			@Override
			public void consume(long timeWindow, ConsumerResources events) {
				
			}});
		
		sim2.registerGlobalConsumer(new EventConsumer() {

			@Override
			public String getId() {
				return "ec1";
			}

			@Override
			public void consume(long timeWindow, ConsumerResources events) {
				
			}});
		
		sim1.start(100_000);
		assertEquals(Simulation.Status.RUNNING,sim1.getStatus());
		sim1.join();
		assertEquals(Simulation.Status.PAUSED,sim1.getStatus());
		
		assertTrue(sim1f.count >= 100_000 && sim1f.count < 200_000);
		
		sim2.start(200_000);
		assertEquals(Simulation.Status.RUNNING,sim2.getStatus());
		sim2.join();
		assertEquals(Simulation.Status.PAUSED,sim2.getStatus());
		
		assertTrue(sim1f.count >= 100_000 && sim1f.count < 200_000);
		assertTrue(sim2f.count >= 200_000);
		
		sim1.destroy();
		sim2.destroy();
		assertEquals(Simulation.Status.DESTROYED,sim1.getStatus());
		assertEquals(Simulation.Status.DESTROYED,sim1.getStatus());
		
		
	}
	
	protected static class CountFunction extends TestFunction{

		private long count = 0;
		private Map<CountFunction,Boolean> initRec;
		
		public CountFunction(String id, Map<CountFunction,Boolean> ir) {
			super(id, Collections.emptyMap());
			initRec = ir;
		}

		@Override
		public void consume(long timeWindow, EvaluatorResources resources) {
			count = timeWindow;
		}

		@Override
		public EventGenerator copy() {
			CountFunction toReturn = new CountFunction(this.getId(),initRec);
			toReturn.count = this.count;
			return toReturn;
		}
		
		@Override
		public void initiate(FunctionInfo info) {
			initRec.put(this, true);
		}
	}

}
