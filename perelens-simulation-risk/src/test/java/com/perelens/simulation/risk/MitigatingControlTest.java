/**
 * 
 */
package com.perelens.simulation.risk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Iterator;

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
import com.perelens.simulation.api.DistributionProvider.Interval;
import com.perelens.simulation.core.CoreDistributionProvider;
import com.perelens.simulation.random.RanluxProvider;
import com.perelens.simulation.risk.events.RiskEvent;
import com.perelens.simulation.risk.events.RiskUnit;

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
class MitigatingControlTest {

	DistributionProvider dp = new CoreDistributionProvider();
	
	@Test
	void testConstructorArgCheck() {
		try {
			new MitigatingControl(null,RiskEvent.RE_LOSS, 0.5, dp.constant(0.5));
			fail();
		}catch(IllegalArgumentException e) {}
		
		try {
			new MitigatingControl("test1",null, 0.5, dp.constant(0.5));
			fail();
		}catch(IllegalArgumentException e) {}
		
		try {
			new MitigatingControl("test1",RiskEvent.RE_LOSS, -0.1d, dp.constant(0.5));
			fail();
		}catch(IllegalArgumentException e) {}
		
		try {
			new MitigatingControl("test1",RiskEvent.RE_LOSS, 1.5d, dp.constant(0.5));
			fail();
		}catch(IllegalArgumentException e) {}
		
		try {
			new MitigatingControl("test1",RiskEvent.RE_LOSS, 1.5d, null);
			fail();
		}catch(IllegalArgumentException e) {}
	}
	
	@Test
	void testEventWithNoMagnitude() {
		
		FunctionInfo fi = new TestFunctionInfo() {

			@Override
			public RandomGenerator getRandomGenerator() {
				return new RandomGenerator() {

					private static final long serialVersionUID = 1L;

					@Override
					public double nextDouble() {
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
		
		MitigatingControl mc = new MitigatingControl("mc1",RiskEvent.RE_THREAT,0.3d,dp.uniform(0.1, 0.4, Interval.CLOSED));
		mc.initiate(fi);
		
		RiskSimEvent toControl = new RiskSimEvent("tt1",RiskEvent.RE_THREAT,100,1);
		
		var tr = new TestResources(Collections.singletonList(toControl));
		
		try {
			mc.consume(200, tr);
			fail();
		}catch(IllegalStateException e) {
		}
	}
	
	@Test
	void testEventWithMagnitude() {
		
		FunctionInfo fi = new TestFunctionInfo() {

			@Override
			public RandomGenerator getRandomGenerator() {
				return new RandomGenerator() {

					private static final long serialVersionUID = 1L;

					@Override
					public double nextDouble() {
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
		
		double mitFactor = 0.5d;
		double mag = 100;
		MitigatingControl mc = new MitigatingControl("mc1",RiskEvent.RE_THREAT,1.0d,dp.constant(mitFactor));
		mc.initiate(fi);
		
		RiskSimEvent toControl = new RiskSimEvent("tt1",RiskEvent.RE_THREAT,100,1);
		toControl.setMagnitude(mag, RiskUnit.RU_QUANTITY);
		
		var tr = new TestResources(Collections.singletonList(toControl));
		
		mc.consume(200, tr);
		
		assertEquals(1,tr.getRaisedEvents().size());
		
		Iterator<Event> i = tr.getRaisedEvents().iterator();
		Event raised = i.next();
		assertFalse(i.hasNext());
		
		assertEquals("mc1",raised.getProducerId());
		assertEquals(toControl.getTime(),raised.getTime());
		assertEquals(toControl.getType(),raised.getType());
		assertEquals(mag - mitFactor * mag, raised.getMagnitude().magnitude());
		assertEquals(toControl.getMagnitude().unit(),raised.getMagnitude().unit());
		
		Iterator<Event> c = raised.causedBy();
		assertEquals(toControl,c.next());
		assertFalse(c.hasNext());
	}
	
	@Test
	void testMultipleEventsWithMagnitude() {
		
		FunctionInfo fi = new TestFunctionInfo() {

			RandomProvider rp = new RanluxProvider(System.currentTimeMillis());
			@Override
			public RandomGenerator getRandomGenerator() {
				return rp.createGenerator();
			}

			@Override
			public TimeTranslator getTimeTranslator() {
				return null;
			}
		};
		
		double mitFactorLow = 0.1;
		double mitFactorHigh = 0.4;
		double mitRate = 0.5;
		
		Distribution mitFactor = dp.uniform(mitFactorLow, mitFactorHigh, Interval.OPEN);
		double mag = 1000;
		
		MitigatingControl mc = new MitigatingControl("mc1",RiskEvent.RE_THREAT,mitRate,mitFactor);
		mc.initiate(fi);
		
		long window = 100;
		int ordinal = 1;
		
		long events = 0;
		long mitigated = 0;
		
		for (int i = 0; i < 100000; i+=100) {
			
			long eventTime = i + window/2;
			
			RiskSimEvent toControl = new RiskSimEvent("tt1",RiskEvent.RE_THREAT,eventTime,ordinal++);
			toControl.setMagnitude(mag, RiskUnit.RU_QUANTITY);
			
			var tr = new TestResources(Collections.singletonList(toControl));
			
			mc.consume(i + window, tr);
			
			assertEquals(1,tr.getRaisedEvents().size());
			
			Iterator<Event> iter = tr.getRaisedEvents().iterator();
			Event raised = iter.next();
			assertFalse(iter.hasNext());
			
			assertEquals("mc1",raised.getProducerId());
			assertEquals(toControl.getTime(),raised.getTime());
			assertEquals(toControl.getType(),raised.getType());
			assertEquals(toControl.getMagnitude().unit(),raised.getMagnitude().unit());
			
			Iterator<Event> c = raised.causedBy();
			assertEquals(toControl,c.next());
			assertFalse(c.hasNext());
			
			//see if the event was mitigated or not
			events++;
			if (raised.getMagnitude().magnitude() < toControl.getMagnitude().magnitude()) {
				mitigated++;
				
				//Calculate the mitigation factor
				double orig = toControl.getMagnitude().magnitude();
				double after = raised.getMagnitude().magnitude();
				
				double mf = (orig - after)/orig;
				
				if (mf <= mitFactorLow) {
					fail("Random mitigation factor is too low. Lower Bound = " + mitFactorLow + ". Calculated = " + mf);
				}
				
				if (mf >= mitFactorHigh) {
					fail("Random mitigation factor is too high. Upper Bound = " + mitFactorHigh + ". Calculated = " + mf);
				}
			}
		}
		
		assertEquals(events, 1000);
		assertEquals(events * mitRate, mitigated,60, "Random mitigation rate is too far from " + mitRate + ". Mitigated " + mitigated + " out of " + events);
	}
	
	@Test
	void testGetConfiguration() {
		double mitFactorLow = 0.1;
		double mitFactorHigh = 0.4;
		double mitRate = 0.5;
		
		Distribution mitFactor = dp.uniform(mitFactorLow, mitFactorHigh, Interval.OPEN);
		
		MitigatingControl mc = new MitigatingControl("mc1",RiskEvent.RE_THREAT,mitRate,mitFactor);
		
		var config = mc.getConfiguration();
		
		assertEquals(mitFactor.getSetup(),config.get(MitigatingControl.CONFIG_KEYS.MC_MITIGATION_FACTOR));
		assertEquals(Double.toString(mitRate),config.get(MitigatingControl.CONFIG_KEYS.MC_SUCCESS_RATE));
		assertEquals(RiskEvent.RE_THREAT.name(),config.get(MitigatingControl.CONFIG_KEYS.MC_TO_CONTROL));
	}

}
