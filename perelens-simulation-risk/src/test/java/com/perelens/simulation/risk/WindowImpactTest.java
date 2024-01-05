package com.perelens.simulation.risk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.perelens.engine.TestFunctionInfo;
import com.perelens.engine.TestResources;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventMagnitude;
import com.perelens.simulation.api.FunctionInfo;
import com.perelens.simulation.api.RandomGenerator;
import com.perelens.simulation.api.TimeTranslator;
import com.perelens.simulation.core.CoreDistributionProvider;
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
class WindowImpactTest {

	@Test
	void testConstructorArgs() {
		
		try {
			new WindowImpact(null,RiskEvent.RE_VULNERABILITY,RiskEvent.RE_VULNERABILITY_END,RiskEvent.RE_LOSS,100);
			fail();
		}catch (IllegalArgumentException e) {}
		try {
			new WindowImpact("test",null,RiskEvent.RE_VULNERABILITY_END,RiskEvent.RE_LOSS,100);
			fail();
		}catch (IllegalArgumentException e) {}
		try {
			new WindowImpact("test",RiskEvent.RE_VULNERABILITY,null,RiskEvent.RE_LOSS,100);
			fail();
		}catch (IllegalArgumentException e) {}
		try {
			new WindowImpact("test",RiskEvent.RE_VULNERABILITY,RiskEvent.RE_VULNERABILITY_END,null,100);
			fail();
		}catch (IllegalArgumentException e) {}
		
		try {
			new WindowImpact("test",RiskEvent.RE_VULNERABILITY,RiskEvent.RE_VULNERABILITY_END,RiskEvent.RE_LOSS_END,100);
			fail();
		}catch (IllegalArgumentException e) {}
		
		try {
			new WindowImpact("test",RiskEvent.RE_VULNERABILITY,RiskEvent.RE_VULNERABILITY_END,RiskEvent.RE_LOSS_END,0);
			fail();
		}catch (IllegalArgumentException e) {}
	}
	
	@Test
	void testSetMagnitudeArgs() {
		var dp = new CoreDistributionProvider();
		
		try {
			var wi = new WindowImpact("test",RiskEvent.RE_VULNERABILITY,RiskEvent.RE_VULNERABILITY_END,RiskEvent.RE_LOSS,100);
			wi.setMagnitude(null, dp.constant(10));
			fail();
		}catch (IllegalArgumentException e) {}
		
		try {
			var wi = new WindowImpact("test",RiskEvent.RE_VULNERABILITY,RiskEvent.RE_VULNERABILITY_END,RiskEvent.RE_LOSS,100);
			wi.setMagnitude(RiskUnit.RU_QUANTITY, null);
			fail();
		}catch (IllegalArgumentException e) {}
		
	}
	
	@Test
	void testWindowAndSliceSameSizeNoMag() {
		
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
		
		long ss = 200;
		
		var wi = new WindowImpact("test",RiskEvent.RE_LOSS, RiskEvent.RE_LOSS_END, RiskEvent.RE_LOSS,ss);
		wi.initiate(fi);
		
		
		var start = new RiskSimEvent("ES1",RiskEvent.RE_LOSS,100,1);
		var end = new RiskSimEvent("ES1",RiskEvent.RE_LOSS_END, 300,2);
		
		var tr = new TestResources(Arrays.asList(new Event[] {start,end}));
		
		wi.consume(400, tr);
		
		assertEquals(1, tr.getRaisedEvents().size());
		
		var i = tr.getRaisedEvents().iterator();
		
		var e1 = i.next();
		assertFalse(i.hasNext());
		
		assertEquals(RiskEvent.RE_LOSS,e1.getType());
		assertEquals(start.getTime() + 200, e1.getTime());
		assertEquals(EventMagnitude.NO_MAGNITUDE, e1.getMagnitude());
		assertEquals(wi.getId(), e1.getProducerId());
		
		var causes = e1.causedBy();
		
		var c1 = causes.next();
		assertFalse(causes.hasNext());
		
		assertEquals(start,c1);
	}
	
	@Test
	void testWindowAndSliceSameSizeMag() {
		
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
		
		var dp = new CoreDistributionProvider();
		
		long ss = 200;
		
		var wi = new WindowImpact("test",RiskEvent.RE_LOSS, RiskEvent.RE_LOSS_END, RiskEvent.RE_LOSS,ss);
		wi.setMagnitude(RiskUnit.RU_QUANTITY, dp.constant(123));
		wi.initiate(fi);
		
		
		var start = new RiskSimEvent("ES1",RiskEvent.RE_LOSS,100,1);
		var end = new RiskSimEvent("ES1",RiskEvent.RE_LOSS_END, 300,2);
		
		var tr = new TestResources(Arrays.asList(new Event[] {start,end}));
		
		wi.consume(400, tr);
		
		assertEquals(1, tr.getRaisedEvents().size());
		
		var i = tr.getRaisedEvents().iterator();
		
		var e1 = i.next();
		assertFalse(i.hasNext());
		
		assertEquals(RiskEvent.RE_LOSS,e1.getType());
		assertEquals(start.getTime() + 200, e1.getTime());
		assertEquals(123.0, e1.getMagnitude().magnitude());
		assertEquals(RiskUnit.RU_QUANTITY, e1.getMagnitude().unit());
		assertEquals(wi.getId(), e1.getProducerId());
		
		var causes = e1.causedBy();
		
		var c1 = causes.next();
		assertFalse(causes.hasNext());
		
		assertEquals(start,c1);
	}
	
	@Test
	void testSlicesAcrossMultipleWindows() {
		
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
		
		var dp = new CoreDistributionProvider();
		
		long ss = 200;
		
		var wi = new WindowImpact("test",RiskEvent.RE_LOSS, RiskEvent.RE_LOSS_END, RiskEvent.RE_LOSS,ss);
		wi.setMagnitude(RiskUnit.RU_QUANTITY, dp.constant(123));
		wi.initiate(fi);
		
		
		var start = new RiskSimEvent("ES1",RiskEvent.RE_LOSS,100,1);
		var end = new RiskSimEvent("ES1",RiskEvent.RE_LOSS_END, 1000,2);
		
		var tr = new TestResources(Arrays.asList(new Event[] {start}));
		
		//First window
		wi.consume(200, tr);
		
		assertEquals(0,tr.getRaisedEvents().size());
		
		long nextImpact = start.getTime() + ss;
		
		//second,third,fourth window
		for (int win = 400; win < 1000; win += 200) {
			tr = new TestResources(Arrays.asList(new Event[] {}));

			wi.consume(win, tr);
			assertEquals(1, tr.getRaisedEvents().size());

			var i = tr.getRaisedEvents().iterator();

			var e1 = i.next();
			assertFalse(i.hasNext());

			assertEquals(RiskEvent.RE_LOSS,e1.getType());
			assertEquals(nextImpact, e1.getTime());
			assertEquals(123.0, e1.getMagnitude().magnitude());
			assertEquals(RiskUnit.RU_QUANTITY, e1.getMagnitude().unit());
			assertEquals(wi.getId(), e1.getProducerId());

			var causes = e1.causedBy();

			var c1 = causes.next();
			assertFalse(causes.hasNext());

			assertEquals(start,c1);
			
			nextImpact = e1.getTime() + ss;
		}
		
		//Final window should have a partial impact
		tr = new TestResources(Arrays.asList(new Event[] {end}));

		wi.consume(1000, tr);
		assertEquals(2, tr.getRaisedEvents().size());

		var i = tr.getRaisedEvents().iterator();

		var e1 = i.next();
		assertTrue(i.hasNext());

		assertEquals(RiskEvent.RE_LOSS,e1.getType());
		assertEquals(nextImpact, e1.getTime());
		assertEquals(123.0, e1.getMagnitude().magnitude());
		assertEquals(RiskUnit.RU_QUANTITY, e1.getMagnitude().unit());
		assertEquals(wi.getId(), e1.getProducerId());

		var causes = e1.causedBy();

		var c1 = causes.next();
		assertFalse(causes.hasNext());

		assertEquals(start,c1);
		
		
		e1 = i.next();
		assertFalse(i.hasNext());
		
		assertEquals(RiskEvent.RE_LOSS,e1.getType());
		assertEquals(end.getTime(), e1.getTime());
		assertEquals(100.0/ss * 123.0, e1.getMagnitude().magnitude());
		assertEquals(RiskUnit.RU_QUANTITY, e1.getMagnitude().unit());
		assertEquals(wi.getId(), e1.getProducerId());

		causes = e1.causedBy();

		c1 = causes.next();
		assertFalse(causes.hasNext());

		assertEquals(start,c1);
		
	}

}
