package com.perelens.engine.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import com.perelens.engine.TestEvent;
import com.perelens.engine.TestEventType;
import com.perelens.engine.api.Event;
import com.perelens.simulation.core.CoreTimeTranslator;

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
class CoreTimeTranslatorTest {

	@Test
	void test() {
		
		ZonedDateTime monday6am = ZonedDateTime.of(2021, 5, 17, 6, 0,0,0,ZoneId.of("GMT"));
	
		CoreTimeTranslator ttMin = new CoreTimeTranslator(monday6am.toInstant(), ChronoUnit.MINUTES);
		Event e1 = new TestEvent("p1",TestEventType.TE_EVENT1,0,1);
		
		//Should be no change
		assertEquals(monday6am.toInstant(), ttMin.getInstant(e1));
		
		int min24hr = 24 * 60;
		Event e2 = new TestEvent("p1",TestEventType.TE_EVENT1,min24hr,1);
		
		//Should be 24 hour change
		assertEquals(monday6am.plus(24,ChronoUnit.HOURS).toInstant(), ttMin.getInstant(e2));
		
		CoreTimeTranslator ttHr = new CoreTimeTranslator(monday6am.toInstant(), ChronoUnit.HOURS);
		Event e1h = new TestEvent("p1",TestEventType.TE_EVENT1,0,1);
		
		//Should be no change
		assertEquals(monday6am.toInstant(), ttHr.getInstant(e1h));
		
		int h24hr = 24;
		Event e2h = new TestEvent("p1",TestEventType.TE_EVENT1,h24hr,1);
		
		//Should be 24 hour change
		assertEquals(monday6am.plus(24,ChronoUnit.HOURS).toInstant(), ttHr.getInstant(e2h));
	}

}
