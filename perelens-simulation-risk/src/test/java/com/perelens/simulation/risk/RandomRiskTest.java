/**
 * 
 */
package com.perelens.simulation.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.perelens.engine.TestFunctionInfo;
import com.perelens.engine.TestResources;
import com.perelens.engine.api.EventGenerator;
import com.perelens.simulation.api.Distribution;
import com.perelens.simulation.api.DistributionProvider;
import com.perelens.simulation.api.FunctionInfo;
import com.perelens.simulation.api.RandomGenerator;
import com.perelens.simulation.api.RandomProvider;
import com.perelens.simulation.api.TimeTranslator;
import com.perelens.simulation.core.CoreDistributionProvider;
import com.perelens.simulation.mixed.AbstractEventWindow;
import com.perelens.simulation.random.RanluxProvider;
import com.perelens.simulation.risk.RandomRisk.CONFIG_KEYS;
import com.perelens.simulation.risk.events.RiskEvent;

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
class RandomRiskTest {

	@Test
	void testConstructorArgChecking() {
		
		int good=0,bad = 0;
		@SuppressWarnings("unused")
		RandomRisk event;
		
		try {
			event = new RandomRisk(null, RiskUtils.ZERO_DISTRIBUTION, RiskEvent.RE_CONTACT);
			fail("Should have generated an exception");
		}catch (IllegalArgumentException e){
			
		}
		
		try {
			event = new RandomRisk("test", null, RiskEvent.RE_CONTACT);
			fail("Should have generated an exception");
		}catch (IllegalArgumentException e){
			
		}
		
		try {
			event = new RandomRisk("test", RiskUtils.ZERO_DISTRIBUTION, null);
			fail("Should have generated an exception");
		}catch (IllegalArgumentException e){
			
		}
		
		for (RiskEvent e : RiskEvent.values()) {
			if (e.name().endsWith("_END")) {
				try {
					event = new RandomRisk("test", RiskUtils.ZERO_DISTRIBUTION,e);
					fail("Should have generated an exception for " + e);
				}catch(IllegalArgumentException e1) {
					
				}
				bad++;
			}else {
				event = new RandomRisk("test", RiskUtils.ZERO_DISTRIBUTION,e);
				good++;
			}
		}
		
		assertEquals(good,bad);
	}
	
	
	@Test
	void testNonRandomArrivalTimeWithZeroDuration() {
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution arr = dp.exponential(1000);
		
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
		
		
		RandomRisk re = new RandomRisk("t1",arr, RiskEvent.RE_CONTACT);
		
		re.initiate(fi);
		
		//First event
		long nextArrivalTime = (long)arr.sample(0.5);
		TestResources tr = new TestResources(Collections.emptyList());
		
		re.consume(nextArrivalTime + 10, tr);
		
		assertEquals(2, tr.getRaisedEvents().size());
		assertEquals(0,tr.getRaisedResponses().size());
		
		var i = tr.getRaisedEvents().iterator();
		
		var e1 = i.next();
		var e2 = i.next();
		
		assertFalse(i.hasNext());
		
		assertEquals(RiskEvent.RE_CONTACT, e1.getType());
		assertEquals(RiskEvent.RE_CONTACT_END, e2.getType());
		assertEquals(e1.getTime(),e2.getTime());
		assertEquals(nextArrivalTime,e1.getTime());
		assertTrue(e1.getOrdinal() < e2.getOrdinal());
		
		//Make sure we get a second event
		tr = new TestResources(Collections.emptyList());
		
		re.consume(nextArrivalTime * 2 + 10, tr);
		
		assertEquals(2, tr.getRaisedEvents().size());
		assertEquals(0,tr.getRaisedResponses().size());
		
		i = tr.getRaisedEvents().iterator();
		
		e1 = i.next();
		e2 = i.next();
		
		assertFalse(i.hasNext());
		
		assertEquals(RiskEvent.RE_CONTACT, e1.getType());
		assertEquals(RiskEvent.RE_CONTACT_END, e2.getType());
		assertEquals(e1.getTime(),e2.getTime());
		assertEquals(nextArrivalTime * 2, e1.getTime());
		assertTrue(e1.getOrdinal() < e2.getOrdinal());
	}
	
	@Test
	void testNonRandomArrivalTimeWithZeroDurationStartOfWindow() {
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution arr = dp.exponential(1000);
		
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
		
		
		RandomRisk re = new RandomRisk("t1",arr, RiskEvent.RE_CONTACT);
		
		re.initiate(fi);
		
		//First event
		long nextArrivalTime = (long)arr.sample(0.5);
		TestResources tr = new TestResources(Collections.emptyList());
		
		re.consume(nextArrivalTime - 1, tr);
		
		assertEquals(0,tr.getRaisedEvents().size());
		assertEquals(0,tr.getRaisedResponses().size());
		
		re.consume(nextArrivalTime + 10, tr);
		assertEquals(2, tr.getRaisedEvents().size());
		
		var i = tr.getRaisedEvents().iterator();
		
		var e1 = i.next();
		var e2 = i.next();
		
		assertFalse(i.hasNext());
		
		assertEquals(RiskEvent.RE_CONTACT, e1.getType());
		assertEquals(RiskEvent.RE_CONTACT_END, e2.getType());
		assertEquals(e1.getTime(),e2.getTime());
		assertTrue(e1.getOrdinal() < e2.getOrdinal());
		
		//Make sure we get a second event
		tr = new TestResources(Collections.emptyList());
		
		re.consume(nextArrivalTime * 2 - 1, tr);
		
		assertEquals(0,tr.getRaisedEvents().size());
		assertEquals(0,tr.getRaisedResponses().size());
		
		re.consume(nextArrivalTime * 2 + 10, tr);
		
		assertEquals(2, tr.getRaisedEvents().size());
		
		i = tr.getRaisedEvents().iterator();
		
		e1 = i.next();
		e2 = i.next();
		
		assertFalse(i.hasNext());
		
		assertEquals(RiskEvent.RE_CONTACT, e1.getType());
		assertEquals(RiskEvent.RE_CONTACT_END, e2.getType());
		assertEquals(e1.getTime(),e2.getTime());
		assertTrue(e1.getOrdinal() < e2.getOrdinal());
	}
	
	@Test
	void testNonRandomArrivalTimeWithZeroDurationEndOfWindow() {
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution arr = dp.exponential(1000);
		
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
		
		
		RandomRisk re = new RandomRisk("t1",arr, RiskEvent.RE_CONTACT);
		
		re.initiate(fi);
		
		//First event
		long nextArrivalTime = (long)arr.sample(0.5);
		TestResources tr = new TestResources(Collections.emptyList());
		
		re.consume(nextArrivalTime, tr);
		
		assertEquals(2, tr.getRaisedEvents().size());
		assertEquals(0,tr.getRaisedResponses().size());
		
		var i = tr.getRaisedEvents().iterator();
		
		var e1 = i.next();
		var e2 = i.next();
		
		assertFalse(i.hasNext());
		
		assertEquals(RiskEvent.RE_CONTACT, e1.getType());
		assertEquals(RiskEvent.RE_CONTACT_END, e2.getType());
		assertEquals(e1.getTime(),e2.getTime());
		assertEquals(nextArrivalTime,e1.getTime());
		assertTrue(e1.getOrdinal() < e2.getOrdinal());
		
		//Make sure we get a second event
		tr = new TestResources(Collections.emptyList());
		
		re.consume(nextArrivalTime * 2, tr);
		
		assertEquals(2, tr.getRaisedEvents().size());
		assertEquals(0,tr.getRaisedResponses().size());
		
		i = tr.getRaisedEvents().iterator();
		
		e1 = i.next();
		e2 = i.next();
		
		assertFalse(i.hasNext());
		
		assertEquals(RiskEvent.RE_CONTACT, e1.getType());
		assertEquals(RiskEvent.RE_CONTACT_END, e2.getType());
		assertEquals(e1.getTime(),e2.getTime());
		assertEquals(nextArrivalTime * 2, e1.getTime());
		assertTrue(e1.getOrdinal() < e2.getOrdinal());
	}
	
	@Test
	void testNonRandomArrivalTimeWithNonZeroDuration() {
		
		int duration = 100;
		
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution arr = dp.exponential(1000);
		Distribution dur = dp.constant(duration);
		
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
		
		
		RandomRisk re = new RandomRisk("t1",arr, RiskEvent.RE_CONTACT);
		re.setEventDuration(dur);
		
		re.initiate(fi);
		
		//First event
		long nextArrivalTime = (long)arr.sample(0.5);
		TestResources tr = new TestResources(Collections.emptyList());
		
		re.consume(nextArrivalTime + duration + 1, tr);
		
		assertEquals(2, tr.getRaisedEvents().size());
		assertEquals(0,tr.getRaisedResponses().size());
		
		var i = tr.getRaisedEvents().iterator();
		
		var e1 = i.next();
		var e2 = i.next();
		
		assertFalse(i.hasNext());
		
		assertEquals(RiskEvent.RE_CONTACT, e1.getType());
		assertEquals(RiskEvent.RE_CONTACT_END, e2.getType());
		assertEquals(e1.getTime() + duration,e2.getTime());
		assertEquals(nextArrivalTime,e1.getTime());
		assertTrue(e1.getOrdinal() < e2.getOrdinal());
		
		//Make sure we get a second event
		tr = new TestResources(Collections.emptyList());
		
		re.consume(nextArrivalTime * 2 + duration * 2 + 1, tr);
		
		assertEquals(2, tr.getRaisedEvents().size());
		assertEquals(0,tr.getRaisedResponses().size());
		
		i = tr.getRaisedEvents().iterator();
		
		e1 = i.next();
		e2 = i.next();
		
		assertFalse(i.hasNext());
		
		assertEquals(RiskEvent.RE_CONTACT, e1.getType());
		assertEquals(RiskEvent.RE_CONTACT_END, e2.getType());
		assertEquals(e1.getTime() + duration,e2.getTime());
		assertEquals(nextArrivalTime * 2 + duration, e1.getTime());
		assertTrue(e1.getOrdinal() < e2.getOrdinal());
	}
	
	@Test
	void testNonRandomArrivalTimeWithNonZeroDurationSplitWindow() {
		
		int duration = 100;
		
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution arr = dp.exponential(1000);
		Distribution dur = dp.constant(duration);
		
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
		
		
		RandomRisk re = new RandomRisk("t1",arr, RiskEvent.RE_CONTACT);
		re.setEventDuration(dur);
		
		re.initiate(fi);
		
		//Split across windows
		long nextArrivalTime = (long)arr.sample(0.5);
		TestResources tr = new TestResources(Collections.emptyList());
		
		re.consume(nextArrivalTime, tr);
		
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0,tr.getRaisedResponses().size());
		
		var i = tr.getRaisedEvents().iterator();
		
		var e1 = i.next();
		
		assertFalse(i.hasNext());
		assertEquals(RiskEvent.RE_CONTACT, e1.getType());
		assertEquals(nextArrivalTime,e1.getTime());
		
		tr = new TestResources(Collections.emptyList());
		
		re.consume(nextArrivalTime + duration, tr);
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0,tr.getRaisedResponses().size());
		
		i = tr.getRaisedEvents().iterator();
		
		var e2 = i.next();
		assertEquals(RiskEvent.RE_CONTACT_END, e2.getType());
		assertEquals(nextArrivalTime + duration,e2.getTime());
		
		assertTrue(e1.getOrdinal() < e2.getOrdinal());
		
		//Make sure we get a second event
		tr = new TestResources(Collections.emptyList());
		
		re.consume(nextArrivalTime * 2 + duration * 2 + 1, tr);
		
		assertEquals(2, tr.getRaisedEvents().size());
		assertEquals(0,tr.getRaisedResponses().size());
		
		i = tr.getRaisedEvents().iterator();
		
		e1 = i.next();
		e2 = i.next();
		
		assertFalse(i.hasNext());
		
		assertEquals(RiskEvent.RE_CONTACT, e1.getType());
		assertEquals(RiskEvent.RE_CONTACT_END, e2.getType());
		assertEquals(e1.getTime() + duration,e2.getTime());
		assertEquals(nextArrivalTime * 2 + duration, e1.getTime());
		assertTrue(e1.getOrdinal() < e2.getOrdinal());
	}
	
	@Test
	void testMultipleEventArrivalsInOneWindow() {
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution arr = dp.exponential(1000);
		
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
		
		
		RandomRisk re = new RandomRisk("t1",arr, RiskEvent.RE_CONTACT);
		
		re.initiate(fi);
		
		//First event
		long nextArrivalTime = (long)arr.sample(0.5);
		TestResources tr = new TestResources(Collections.emptyList());
		
		re.consume(nextArrivalTime * 2 + 10, tr);
		
		assertEquals(4, tr.getRaisedEvents().size());
		assertEquals(0,tr.getRaisedResponses().size());
		
		var i = tr.getRaisedEvents().iterator();
		
		var e1 = i.next();
		var e2 = i.next();
		
		assertTrue(i.hasNext());
		
		assertEquals(RiskEvent.RE_CONTACT, e1.getType());
		assertEquals(RiskEvent.RE_CONTACT_END, e2.getType());
		assertEquals(e1.getTime(),e2.getTime());
		assertEquals(nextArrivalTime,e1.getTime());
		assertTrue(e1.getOrdinal() < e2.getOrdinal());
		
		
		e1 = i.next();
		e2 = i.next();
		
		assertFalse(i.hasNext());
		
		assertEquals(RiskEvent.RE_CONTACT, e1.getType());
		assertEquals(RiskEvent.RE_CONTACT_END, e2.getType());
		assertEquals(e1.getTime(),e2.getTime());
		assertEquals(nextArrivalTime * 2, e1.getTime());
		assertTrue(e1.getOrdinal() < e2.getOrdinal());
	}
	
	
	@Test
	void testGetConfiguration() {
		
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution arr = dp.exponential(100);
		
		RandomRisk tt = new RandomRisk("test1",arr,RiskEvent.RE_THREAT);
		var config = tt.getConfiguration();
		assertEquals(arr.getSetup(),config.get(CONFIG_KEYS.RR_EVENT_ARRIVAL_DIST));
		assertEquals(RiskUtils.ZERO_DISTRIBUTION.getSetup(),config.get(AbstractEventWindow.CONFIG_KEYS.EW_EVENT_DURATION_DIST));
		assertEquals(tt.getId(),config.get(EventGenerator.CONFIG_KEYS.EG_ID));	
	}
	
	@Test
	void testCopy() {
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution arr = dp.exponential(100);
		Distribution dur = dp.constant(50);
		
		RandomProvider rp = new RanluxProvider(111);
		
		FunctionInfo fi = new TestFunctionInfo() {

			@Override
			public RandomGenerator getRandomGenerator() {
				return rp.createGenerator();
			}

			@Override
			public TimeTranslator getTimeTranslator() {
				return null;
			}
		};
		
		
		RandomRisk re = new RandomRisk("t1",arr, RiskEvent.RE_CONTACT);
		re.setEventDuration(dur);
		
		re.initiate(fi);
		
		RandomRisk re2 = re.copy();
		
		TestResources tr = new TestResources(Collections.emptyList());
		
		int window = 1;
		while(tr.getRaisedEvents().size() < 2) {
			re.consume(window++, tr);
		}
		
		//Should be a start and end event
		assertEquals(2, tr.getRaisedEvents().size());
		var i = tr.getRaisedEvents().iterator();
		
		var e1 = i.next();
		var e2 = i.next();
		
		assertFalse(i.hasNext());
		
		//Do the same for the copy
		tr = new TestResources(Collections.emptyList());
		
		window = 1;
		while(tr.getRaisedEvents().size() < 2) {
			re2.consume(window++, tr);
		}
		
		//Should be a start and end event
		assertEquals(2, tr.getRaisedEvents().size());
		i = tr.getRaisedEvents().iterator();
				
		var e1b = i.next();
		var e2b = i.next();
		
		assertFalse(e1 == e1b);
		assertFalse(e2 == e2b);
		
		assertEquals(e1.getOrdinal(),e1b.getOrdinal());
		assertEquals(e1.getProducerId(),e1b.getProducerId());
		assertEquals(e1.getTime(), e1b.getTime());
		assertEquals(e1.getType(), e1b.getType());
		assertEquals(e1.getMagnitude(), e1b.getMagnitude());
		
		assertEquals(e2.getOrdinal(),e2b.getOrdinal());
		assertEquals(e2.getProducerId(),e2b.getProducerId());
		assertEquals(e2.getTime(), e2b.getTime());
		assertEquals(e2.getType(), e2b.getType());
		assertEquals(e2.getMagnitude(), e2b.getMagnitude());
	}
	
}
