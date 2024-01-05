/**
 * 
 */
package com.perelens.simulation.risk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.perelens.engine.TestFunctionInfo;
import com.perelens.engine.TestResources;
import com.perelens.engine.api.EventMagnitude;
import com.perelens.engine.api.EventType;
import com.perelens.simulation.api.Distribution;
import com.perelens.simulation.api.DistributionProvider;
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
class RealizedRiskTest {
	
	protected RealizedRisk newRealizedRisk(String id, EventType trigger, RiskEvent result){
		return new RealizedRisk(id,trigger,result);
	}

	@Test
	void testConstructorArgCheck() {
		
		try {
			newRealizedRisk(null, RiskEvent.RE_CONTACT,RiskEvent.RE_LOSS).notify();
			fail();
		}catch (IllegalArgumentException e) {
		}
		
		try {
			newRealizedRisk("t1",null,RiskEvent.RE_LOSS).notify();
			fail();
		}catch (IllegalArgumentException e) {
		}
		
		try {
			newRealizedRisk("t1",RiskEvent.RE_CONTACT,null).notify();
			fail();
		}catch (IllegalArgumentException e) {
		}	
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
		
		RealizedRisk ci = newRealizedRisk("t1",RiskEvent.RE_THREAT, RiskEvent.RE_LOSS);
		ci.initiate(fi);
		
		RiskSimEvent trigger = new RiskSimEvent("d1",RiskEvent.RE_THREAT,100,1);
		
		var tr = new TestResources(Collections.singletonList(trigger));
		
		ci.consume(200, tr);
		
		assertEquals(2,tr.getRaisedEvents().size());
		
		var i = tr.getRaisedEvents().iterator();
		
		var e1 = i.next();
		assertTrue(i.hasNext());
		
		assertEquals(RiskEvent.RE_LOSS,e1.getType());
		assertEquals(trigger.getTime(), e1.getTime());
		assertEquals(EventMagnitude.NO_MAGNITUDE, e1.getMagnitude());
		assertEquals(ci.getId(), e1.getProducerId());
		
		var causes = e1.causedBy();
		
		var c1 = causes.next();
		assertFalse(causes.hasNext());
		
		assertEquals(trigger,c1);
		
		e1 = i.next();
		assertFalse(i.hasNext());
		
		assertEquals(RiskEvent.RE_LOSS_END,e1.getType());
		assertEquals(trigger.getTime(), e1.getTime());
		assertEquals(EventMagnitude.NO_MAGNITUDE, e1.getMagnitude());
		assertEquals(ci.getId(), e1.getProducerId());
	}
	
	@Test
	void testEventWithMagnitude() {
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution mag = dp.constant(1000);
		
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
		
		RealizedRisk ci = newRealizedRisk("t1",RiskEvent.RE_THREAT, RiskEvent.RE_LOSS);
		ci.setResultMagnitude(mag, RiskUnit.RU_QUANTITY);
		ci.initiate(fi);
		
		RiskSimEvent trigger = new RiskSimEvent("d1",RiskEvent.RE_THREAT,100,1);
		
		var tr = new TestResources(Collections.singletonList(trigger));
		
		ci.consume(200, tr);
		
		assertEquals(2,tr.getRaisedEvents().size());
		
		var i = tr.getRaisedEvents().iterator();
		
		var e1 = i.next();
		assertTrue(i.hasNext());
		
		assertEquals(RiskEvent.RE_LOSS,e1.getType());
		assertEquals(trigger.getTime(), e1.getTime());
		assertEquals(1000,e1.getMagnitude().magnitude());
		assertEquals(RiskUnit.RU_QUANTITY, e1.getMagnitude().unit());
		assertEquals(ci.getId(), e1.getProducerId());
		
		var causes = e1.causedBy();
		
		var c1 = causes.next();
		assertFalse(causes.hasNext());
		
		assertEquals(trigger,c1);
		
		e1 = i.next();
		assertFalse(i.hasNext());
		
		assertEquals(RiskEvent.RE_LOSS_END,e1.getType());
		assertEquals(trigger.getTime(), e1.getTime());
		assertEquals(EventMagnitude.NO_MAGNITUDE, e1.getMagnitude());
		assertEquals(ci.getId(), e1.getProducerId());
	}
	
	@Test
	void testGetConfiguration() {
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution mag = dp.constant(1000);
		
		RealizedRisk ci = newRealizedRisk("t1",RiskEvent.RE_THREAT, RiskEvent.RE_LOSS);
		ci.setResultMagnitude(mag, RiskUnit.RU_QUANTITY);
		
		var config = ci.getConfiguration();
		
		assertEquals(RiskEvent.RE_LOSS.name(),config.get(RealizedRisk.CONFIG_KEYS.RR_RESULT));
		assertEquals(RiskEvent.RE_THREAT.name(),config.get(RealizedRisk.CONFIG_KEYS.RR_TRIGGER));
		assertEquals(mag.getSetup(),config.get(RealizedRisk.CONFIG_KEYS.RR_RESULT_MAGNITUDE));
		assertEquals(RiskUnit.RU_QUANTITY.name(),config.get(RealizedRisk.CONFIG_KEYS.RR_RESULT_UNIT));	
	}

}
