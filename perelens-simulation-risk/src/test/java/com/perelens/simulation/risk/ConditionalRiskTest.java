/**
 * 
 */
package com.perelens.simulation.risk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.perelens.engine.TestFunctionInfo;
import com.perelens.engine.TestResources;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventMagnitude;
import com.perelens.engine.api.EventType;
import com.perelens.simulation.api.FunctionInfo;
import com.perelens.simulation.api.RandomGenerator;
import com.perelens.simulation.api.TimeTranslator;
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
class ConditionalRiskTest extends RealizedRiskTest {

	@Override
	protected RealizedRisk newRealizedRisk(String id, EventType trigger, RiskEvent result) {
		return new ConditionalRisk(id, trigger, 1.0, result);
	}

	@Test
	void testConstructorArgCheck_CR() {
		try {
			new ConditionalRisk("id",RiskEvent.RE_CONTACT,1.0001,RiskEvent.RE_LOSS);
			fail();
		}catch (IllegalArgumentException e) {}
		
		try {
			new ConditionalRisk("id",RiskEvent.RE_CONTACT,-0.00001,RiskEvent.RE_LOSS);
			fail();
		}catch (IllegalArgumentException e) {}
	}
	
	@Test
	void testConditionConfigs() {
		var cr = new ConditionalRisk("id",RiskEvent.RE_THREAT, 0.5, RiskEvent.RE_LOSS);
		
		//Test setting just one
		cr.setCondition(RiskEvent.RE_CONTACT, false);
		
		for (RiskEvent re : RiskEvent.values()) {
			if (re == RiskEvent.RE_CONTACT || re == RiskEvent.RE_CONTACT_END) {
				assertEquals(cr.getConditionConfig(re),ConditionalRisk.COND_FALSE);
			}else {
				assertEquals(cr.getConditionConfig(re),ConditionalRisk.NOT_SET);
			}
		}
		
		//Test setting using the END event
		try {
			cr.setCondition(RiskEvent.RE_CONTACT_END, true);
			fail();
		}catch (IllegalArgumentException e) {}
		
		//Test setting all TRUE
		for (RiskEvent re: RiskEvent.values()) {
			if (!re.name().endsWith(RiskEvent.END)) {
				try {
					cr.setCondition(re, true);
					if (re == RiskEvent.RE_THREAT) {
						fail();
					}
				}catch(IllegalArgumentException e) {
					if (re != RiskEvent.RE_THREAT) {
						fail();
					}
				}
			}
		}
		for (RiskEvent re : RiskEvent.values()) {
			if (re == RiskEvent.RE_THREAT || re == RiskEvent.RE_THREAT_END) {
				assertEquals(cr.getConditionConfig(re),ConditionalRisk.NOT_SET);
			}else {
				assertEquals(cr.getConditionConfig(re),ConditionalRisk.COND_TRUE);
			}
		}
		
		//Test setting all FALSE
		for (RiskEvent re: RiskEvent.values()) {
			if (!re.name().endsWith(RiskEvent.END)) {
				try {
					cr.setCondition(re, false);
					if (re == RiskEvent.RE_THREAT) {
						fail();
					}
				}catch(IllegalArgumentException e) {
					if (re != RiskEvent.RE_THREAT) {
						fail();
					}
				}
			}
		}
		for (RiskEvent re : RiskEvent.values()) {
			if (re == RiskEvent.RE_THREAT || re == RiskEvent.RE_THREAT_END) {
				assertEquals(cr.getConditionConfig(re),ConditionalRisk.NOT_SET);
			}else {
				assertEquals(cr.getConditionConfig(re),ConditionalRisk.COND_FALSE);
			}
		}
	}
	
	@Test
	void testConditionsMetInstantanious() {
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
		
		var cr = new ConditionalRisk("ev1",RiskEvent.RE_THREAT,1.0,RiskEvent.RE_LOSS);
		cr.setCondition(RiskEvent.RE_VULNERABILITY, true);
		cr.initiate(fi);
		
		var trigger = new RiskSimEvent("d1",RiskEvent.RE_THREAT,100,1);
		var cStart = new RiskSimEvent("c1", RiskEvent.RE_VULNERABILITY,100,1);
		var cEnd = new RiskSimEvent("c1", RiskEvent.RE_VULNERABILITY_END,100,2);
		
		var tr = new TestResources(Arrays.asList(new Event[] {cStart,trigger,cEnd}));
		
		cr.consume(200, tr);
		
		assertEquals(2,tr.getRaisedEvents().size());
		
		var i = tr.getRaisedEvents().iterator();
		
		var e1 = i.next();
		assertTrue(i.hasNext());
		
		assertEquals(RiskEvent.RE_LOSS,e1.getType());
		assertEquals(trigger.getTime(), e1.getTime());
		assertEquals(EventMagnitude.NO_MAGNITUDE, e1.getMagnitude());
		assertEquals(cr.getId(), e1.getProducerId());
		
		var causes = e1.causedBy();
		
		var c1 = causes.next();
		assertFalse(causes.hasNext());
		
		assertEquals(trigger,c1);
		
		e1 = i.next();
		assertFalse(i.hasNext());
		
		assertEquals(RiskEvent.RE_LOSS_END,e1.getType());
		assertEquals(trigger.getTime(), e1.getTime());
		assertEquals(EventMagnitude.NO_MAGNITUDE, e1.getMagnitude());
		assertEquals(cr.getId(), e1.getProducerId());
	}
	
	@Test
	void testConditionsNotMetInstantanious() {
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
		
		var cr = new ConditionalRisk("ev1",RiskEvent.RE_THREAT,1.0,RiskEvent.RE_LOSS);
		cr.setCondition(RiskEvent.RE_VULNERABILITY, false);
		cr.initiate(fi);
		
		var trigger = new RiskSimEvent("d1",RiskEvent.RE_THREAT,100,1);
		var cStart = new RiskSimEvent("c1", RiskEvent.RE_VULNERABILITY,100,1);
		var cEnd = new RiskSimEvent("c1", RiskEvent.RE_VULNERABILITY_END,100,2);
		
		var tr = new TestResources(Arrays.asList(new Event[] {cStart,trigger,cEnd}));
		
		cr.consume(200, tr);
		
		assertEquals(0,tr.getRaisedEvents().size());
		
		var i = tr.getRaisedEvents().iterator();
		assertFalse(i.hasNext());
	}
	
	@Test
	void testConditionsMetWindow() {
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
		
		var cr = new ConditionalRisk("ev1",RiskEvent.RE_THREAT,1.0,RiskEvent.RE_LOSS);
		cr.setCondition(RiskEvent.RE_VULNERABILITY, true);
		cr.initiate(fi);
		
		var trigger = new RiskSimEvent("d1",RiskEvent.RE_THREAT,100,1);
		var cStart = new RiskSimEvent("c1", RiskEvent.RE_VULNERABILITY,90,1);
		var cEnd = new RiskSimEvent("c1", RiskEvent.RE_VULNERABILITY_END,110,2);
		
		var tr = new TestResources(Arrays.asList(new Event[] {cStart,trigger,cEnd}));
		
		cr.consume(200, tr);
		
		assertEquals(2,tr.getRaisedEvents().size());
		
		var i = tr.getRaisedEvents().iterator();
		
		var e1 = i.next();
		assertTrue(i.hasNext());
		
		assertEquals(RiskEvent.RE_LOSS,e1.getType());
		assertEquals(trigger.getTime(), e1.getTime());
		assertEquals(EventMagnitude.NO_MAGNITUDE, e1.getMagnitude());
		assertEquals(cr.getId(), e1.getProducerId());
		
		var causes = e1.causedBy();
		
		var c1 = causes.next();
		assertFalse(causes.hasNext());
		
		assertEquals(trigger,c1);
		
		e1 = i.next();
		assertFalse(i.hasNext());
		
		assertEquals(RiskEvent.RE_LOSS_END,e1.getType());
		assertEquals(trigger.getTime(), e1.getTime());
		assertEquals(EventMagnitude.NO_MAGNITUDE, e1.getMagnitude());
		assertEquals(cr.getId(), e1.getProducerId());
	}
	
	@Test
	void testConditionsNotMetWindow() {
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
		
		var cr = new ConditionalRisk("ev1",RiskEvent.RE_THREAT,1.0,RiskEvent.RE_LOSS);
		cr.setCondition(RiskEvent.RE_VULNERABILITY, false);
		cr.initiate(fi);
		
		var trigger = new RiskSimEvent("d1",RiskEvent.RE_THREAT,100,1);
		var cStart = new RiskSimEvent("c1", RiskEvent.RE_VULNERABILITY,90,1);
		var cEnd = new RiskSimEvent("c1", RiskEvent.RE_VULNERABILITY_END,110,2);
		
		var tr = new TestResources(Arrays.asList(new Event[] {cStart,trigger,cEnd}));
		
		cr.consume(200, tr);
		
		assertEquals(0,tr.getRaisedEvents().size());
		
		var i = tr.getRaisedEvents().iterator();
		assertFalse(i.hasNext());
	}
	
	@Test
	void testConditionsMetThenNotMet() {
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
		
		var cr = new ConditionalRisk("ev1",RiskEvent.RE_THREAT,1.0,RiskEvent.RE_LOSS);
		cr.setCondition(RiskEvent.RE_VULNERABILITY, true);
		cr.initiate(fi);
		
		var trigger = new RiskSimEvent("d1",RiskEvent.RE_THREAT,100,1);
		var cStart = new RiskSimEvent("c1", RiskEvent.RE_VULNERABILITY,90,1);
		var cEnd = new RiskSimEvent("c1", RiskEvent.RE_VULNERABILITY_END,110,2);
		var trigger2 = new RiskSimEvent("d1",RiskEvent.RE_THREAT,150,3);
		
		var tr = new TestResources(Arrays.asList(new Event[] {cStart,trigger,cEnd,trigger2}));
		
		cr.consume(200, tr);
		
		assertEquals(2,tr.getRaisedEvents().size());
		
		var i = tr.getRaisedEvents().iterator();
		
		var e1 = i.next();
		assertTrue(i.hasNext());
		
		assertEquals(RiskEvent.RE_LOSS,e1.getType());
		assertEquals(trigger.getTime(), e1.getTime());
		assertEquals(EventMagnitude.NO_MAGNITUDE, e1.getMagnitude());
		assertEquals(cr.getId(), e1.getProducerId());
		
		var causes = e1.causedBy();
		
		var c1 = causes.next();
		assertFalse(causes.hasNext());
		
		assertEquals(trigger,c1);
		
		e1 = i.next();
		assertFalse(i.hasNext());
		
		assertEquals(RiskEvent.RE_LOSS_END,e1.getType());
		assertEquals(trigger.getTime(), e1.getTime());
		assertEquals(EventMagnitude.NO_MAGNITUDE, e1.getMagnitude());
		assertEquals(cr.getId(), e1.getProducerId());
	}
	
	@Test
	void testComparatorLogic() {
		var cr = new ConditionalRisk("ev1",RiskEvent.RE_THREAT,1.0,RiskEvent.RE_LOSS);
		
		EventType[] events = new EventType[] {RiskEvent.RE_VULNERABILITY_END,RiskEvent.RE_VULNERABILITY,RiskEvent.RE_THREAT};
		Arrays.sort(events,cr);
		
		assertEquals(RiskEvent.RE_VULNERABILITY, events[0]);
		assertEquals(RiskEvent.RE_THREAT, events[1]);
		assertEquals(RiskEvent.RE_VULNERABILITY_END, events[2]);
		
	}
}
