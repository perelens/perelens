/**
 * 
 */
package com.perelens.simulation.failure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.perelens.engine.TestFunctionInfo;
import com.perelens.engine.TestResources;
import com.perelens.engine.TestResources.ResponseEntry;
import com.perelens.engine.api.Event;
import com.perelens.simulation.api.Distribution;
import com.perelens.simulation.api.DistributionProvider;
import com.perelens.simulation.api.FunctionInfo;
import com.perelens.simulation.api.RandomGenerator;
import com.perelens.simulation.api.RandomProvider;
import com.perelens.simulation.api.TimeTranslator;
import com.perelens.simulation.core.CoreDistributionProvider;
import com.perelens.simulation.events.ResourcePoolEvent;
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
   
   
 * @author Steve Branda
 *
 */
class RandomFailureFunctionTest {
	long ordinal = 1;
	
	/**
	 * Use random generators that return a constant number.
	 */
	@Test
	void testNonRandomArrivalTime() {
		
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution df = dp.exponential(1000);
		Distribution dr = dp.exponential(100);
		
		RandomFailureFunction rff = new RandomFailureFunction("rff1",df,dr);
		
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
		
		rff.initiate(fi);
		
		ArrayList<Event> fails = new ArrayList<>();
		ArrayList<Event> repairs = new ArrayList<>();
		
		for (int i = 0; fails.size() < 10 || repairs.size() < 10; i+=100) {
			TestResources tr = new TestResources(Collections.emptyList());
			rff.consume(i, tr);
			
			for (Event e : tr.getRaisedEvents()) {
				if (e.getType() == FailureSimulationEvent.FS_FAILED) {
					fails.add(e);
				}else if (e.getType() == FailureSimulationEvent.FS_RETURN_TO_SERVICE) {
					repairs.add(e);
				}else {
					fail("Unexepcted Event Type" + e.getType());
				}
			}
		}
		
		//Make sure the time to failure and time to repair are consistent.
		assertTrue(fails.size() >= repairs.size());
		Iterator<Event> failures = fails.iterator();
		Iterator<Event> reps = repairs.iterator();
		
		Event curFail = failures.next();
		Event curRepair = reps.next();
		long timeToFail = curFail.getTime();
		long timeToFix = curRepair.getTime() - timeToFail;
		
		while(failures.hasNext()) {
			Event nextFail = failures.next();
			Event nextRepair = null;
			if (reps.hasNext()) {
				nextRepair = reps.next();
			}
			
			assertEquals(timeToFail,nextFail.getTime() - curRepair.getTime());
			if (nextRepair != null) {
				assertEquals(timeToFix, nextRepair.getTime() - nextFail.getTime());
			}
			
			curRepair = nextRepair;
			curFail = nextFail;
		}
	}
	
	@Test
	void testFailureAndRepairAtEndOfWindow(){
		
		//Use non random arrival again
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution df = dp.exponential(1000);
		Distribution dr = dp.exponential(100);
		
		RandomFailureFunction rff = new RandomFailureFunction("rff1",df,dr);
		
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
		
		rff.initiate(fi);
		
		long nextFailureTime = (long)df.sample(0.5);
		TestResources tr = new TestResources(Collections.emptyList());
		
		rff.consume(nextFailureTime, tr);
		
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		Iterator<Event> events = tr.getRaisedEvents().iterator();
		Event raisedEvent = events.next();
		assertEquals(nextFailureTime, raisedEvent.getTime());
		assertEquals(FailureSimulationEvent.FS_FAILED, raisedEvent.getType());
		assertEquals("rff1", raisedEvent.getProducerId());
		assertFalse(events.hasNext());
		
		
		//now have the repair happen at the end of the next window
		long nextRepairTime = nextFailureTime + (long)Math.ceil(dr.sample(0.5));
		tr = new TestResources(Collections.emptyList());
		
		rff.consume(nextRepairTime, tr);
		
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		events = tr.getRaisedEvents().iterator();
		raisedEvent = events.next();
		assertEquals(nextRepairTime, raisedEvent.getTime());
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, raisedEvent.getType());
		assertEquals("rff1", raisedEvent.getProducerId());
		assertFalse(events.hasNext());
	}
	
	@Test
	void testFailureAndRepairAtStartOfWindow(){
		
		//Use non random arrival again
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution df = dp.exponential(1000);
		Distribution dr = dp.exponential(100);
		
		RandomFailureFunction rff = new RandomFailureFunction("rff1",df,dr);
		
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
		
		rff.initiate(fi);
		
		long nextFailureTime = (long)df.sample(0.5);
		long nextRepairTime = nextFailureTime + (long)Math.ceil(dr.sample(0.5));
		TestResources tr = new TestResources(Collections.emptyList());
		
		rff.consume(nextFailureTime - 1, tr);
		
		assertEquals(0, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		rff.consume(nextRepairTime - 1, tr);
		Iterator<Event> events = tr.getRaisedEvents().iterator();
		Event raisedEvent = events.next();
		assertEquals(nextFailureTime, raisedEvent.getTime());
		assertEquals(FailureSimulationEvent.FS_FAILED, raisedEvent.getType());
		assertEquals("rff1", raisedEvent.getProducerId());
		assertFalse(events.hasNext());
		
		
		//now have the repair happen at the end of the next window
		
		tr = new TestResources(Collections.emptyList());
		rff.consume(nextRepairTime + 100, tr);
		
		assertEquals(1, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		events = tr.getRaisedEvents().iterator();
		raisedEvent = events.next();
		assertEquals(nextRepairTime, raisedEvent.getTime());
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, raisedEvent.getType());
		assertEquals("rff1", raisedEvent.getProducerId());
		assertFalse(events.hasNext());
	}
	
	@Test
	void testRandomExponentialFailure() {
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution df = dp.exponential(1000);
		Distribution dr = dp.exponential(100);
		RandomProvider rp = new RanluxProvider(System.currentTimeMillis());
		
		RandomFailureFunction rff = new RandomFailureFunction("rff1",df,dr);
		
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
		
		rff.initiate(fi);
		
		ArrayList<Long> fails = new ArrayList<>();
		ArrayList<Long> repairs = new ArrayList<>();
		Event lastFailEvent = null;
		Event lastRepairEvent = null;
		
		for (int i = 0; ; i+=100) {
			TestResources tr = new TestResources(Collections.emptyList());
			rff.consume(i, tr);
			
			for (Event e : tr.getRaisedEvents()) {
				if (e.getType() == FailureSimulationEvent.FS_FAILED) {
					long lastTime = 0;
					if (lastRepairEvent != null) {
						lastTime = lastRepairEvent.getTime();
					}
					fails.add(e.getTime() - lastTime);
					lastFailEvent = e;
				}else if (e.getType() == FailureSimulationEvent.FS_RETURN_TO_SERVICE) {
					repairs.add(e.getTime() - lastFailEvent.getTime());
					lastRepairEvent = e;
				}else {
					fail("Unexepcted Event Type" + e.getType());
				}
			}
			
			if (fails.size() > 100_000 && fails.size() == repairs.size() && fails.size()%2 == 0) {
				//stop when we have an even number of failures and repairs, greater than 100,000 total
				break;
			}
		}
		
		//Check the mean values
		long total = 0;
		for (long failTime : fails) {
			total += failTime;
		}
		
		double failMean = total/(double)fails.size();
		assertEquals(1000,failMean, 1000 * 0.01); //check within 1%
		
		total = 0;
		for (long repairTime : repairs) {
			total += repairTime;
		}
		
		double repairMean = total/(double)repairs.size();
		assertEquals(100, repairMean, 100 * 0.012);  //Check within 1.2%
		
		//check the median values
		Collections.sort(fails);
		double failMedian = Math.log(2)/(1d/1000);
		double foundMedian = (fails.get((fails.size()/2)) + fails.get((fails.size()/2 + 1)))/2d;
		
		assertEquals(failMedian,foundMedian, failMedian * 0.02); //check within 2%
	}
	
	@Test
	void testRestoreTime() {
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution df = dp.exponential(2000);
		Distribution dr = dp.exponential(200);
		
		RandomFailureFunction rff = new RandomFailureFunction("rff1",df,dr);
		rff.setRestoreTime(100);
		
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
		
		rff.initiate(fi);
		
		int failCount = 0;
		long lastFailTime = 0;
		long repairInterval = (long)Math.ceil(dr.sample(0.5)) + 100;
		
		for (int i = 0; failCount < 1000 ; i+=100) {
			TestResources tr = new TestResources(Collections.emptyList());
			rff.consume(i, tr);
			
			for (Event e : tr.getRaisedEvents()) {
				if (e.getType() == FailureSimulationEvent.FS_FAILED) {
					failCount++;
					lastFailTime = e.getTime();
				}else if (e.getType() == FailureSimulationEvent.FS_RETURN_TO_SERVICE) {
					assertEquals(lastFailTime + repairInterval, e.getTime());
				}else {
					fail("Unexepcted Event Type" + e.getType());
				}
			}
		}
		
		assertEquals(1000, failCount);
	}
	
	@Test
	void testResourceRequestGrantInSameWindow() {
		//Use non random arrival again
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution df = dp.exponential(1000);
		Distribution dr = dp.exponential(100);

		RandomFailureFunction rff = new RandomFailureFunction("rff1",df,dr);

		FunctionInfo fi = new TestFunctionInfo() {
			
			

			@Override
			public Set<String> getResourcePools() {
				return Collections.singleton("pool1");
			}

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

		rff.initiate(fi);

		long nextFailureTime = (long)df.sample(0.5);
		//Make the window big enough to contain the entire resource pool interaction
		//but not big enough to contain two failures
		long nextWindowTime = nextFailureTime + nextFailureTime;
		
		TestResources tr = new TestResources(Collections.emptyList());
		rff.consume(nextWindowTime, tr);
		
		assertEquals(2,tr.getRaisedEvents().size()); //Fail event an resource pool request
		assertEquals(0,tr.getRaisedResponses().size());
		
		Iterator<Event> events = tr.getRaisedEvents().iterator();
		Event failEvent = events.next();
		assertEquals(FailureSimulationEvent.FS_FAILED, failEvent.getType());
		assertEquals(nextFailureTime,failEvent.getTime());
		assertEquals(rff.getId(), failEvent.getProducerId());
		
		Event resourceRequest = events.next();
		assertEquals(ResourcePoolEvent.RP_REQUEST,resourceRequest.getType());
		assertEquals(nextFailureTime,resourceRequest.getTime());
		assertEquals(rff.getId(), resourceRequest.getProducerId());
		
		assertFalse(events.hasNext());
		
		//Inject the RP_GRANTED request so repair can start
		long grantTime = nextFailureTime + 43;
		long rtsTime =  grantTime + (long)Math.ceil(dr.sample(0.5));
		Event grantedEvent = new ResPoolEvent("pool1",ResourcePoolEvent.RP_GRANT,grantTime,ordinal++,ResourcePoolEvent.GRANT_RESPONSE_TYPES);
		tr = new TestResources(Collections.singletonList(grantedEvent));
		
		rff.consume(nextWindowTime, tr);
		
		assertEquals(1, tr.getRaisedEvents().size()); 		//The RETURN_TO_SERVICE event
		assertEquals(1, tr.getRaisedResponses().size()); 	//The RP_RETURN response event
		
		events = tr.getRaisedEvents().iterator();
		Event rtsEvent = events.next();
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, rtsEvent.getType());
		assertEquals(rtsTime, rtsEvent.getTime());
		assertEquals(rff.getId(),rtsEvent.getProducerId());
		assertFalse(events.hasNext());
		
		Iterator<ResponseEntry> re = tr.getRaisedResponses().iterator();
		ResponseEntry ree = re.next();
		Event retEvent = ree.getResponse();
		assertEquals(grantedEvent,ree.getInResponseTo());
		assertEquals(ResourcePoolEvent.RP_RETURN, retEvent.getType());
		assertEquals(rtsTime,retEvent.getTime());
		assertEquals(rff.getId(),retEvent.getProducerId());
		assertFalse(re.hasNext());
	
	}
	
	@Test
	void testResourceRequestGrantAndReturnInDifferentWindows() {
		//Use non random arrival again
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution df = dp.exponential(1000);
		Distribution dr = dp.exponential(100);

		RandomFailureFunction rff = new RandomFailureFunction("rff1",df,dr);

		FunctionInfo fi = new TestFunctionInfo() {
			
			

			@Override
			public Set<String> getResourcePools() {
				return Collections.singleton("pool1");
			}

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

		rff.initiate(fi);

		long nextFailureTime = (long)df.sample(0.5);
		long repairInterval = (long)Math.ceil(dr.sample(0.5));
		//Size the window so the repair time pushes the resource return into the next window
		long nextWindowTime = nextFailureTime + repairInterval/2;
		
		TestResources tr = new TestResources(Collections.emptyList());
		rff.consume(nextWindowTime, tr);
		
		assertEquals(2,tr.getRaisedEvents().size()); //Fail event an resource pool request
		assertEquals(0,tr.getRaisedResponses().size());
		
		Iterator<Event> events = tr.getRaisedEvents().iterator();
		Event failEvent = events.next();
		assertEquals(FailureSimulationEvent.FS_FAILED, failEvent.getType());
		assertEquals(nextFailureTime,failEvent.getTime());
		assertEquals(rff.getId(), failEvent.getProducerId());
		
		Event resourceRequest = events.next();
		assertEquals(ResourcePoolEvent.RP_REQUEST,resourceRequest.getType());
		assertEquals(nextFailureTime,resourceRequest.getTime());
		assertEquals(rff.getId(), resourceRequest.getProducerId());
		
		assertFalse(events.hasNext());
		
		//Inject the RP_GRANTED request so repair can start
		long grantTime = nextFailureTime + 3;
		long rtsTime =  grantTime + repairInterval;
		Event grantedEvent = new ResPoolEvent("pool1",ResourcePoolEvent.RP_GRANT,grantTime,ordinal++,ResourcePoolEvent.GRANT_RESPONSE_TYPES);
		tr = new TestResources(Collections.singletonList(grantedEvent));
		
		rff.consume(nextWindowTime, tr);
		
		assertEquals(0, tr.getRaisedEvents().size()); 		
		assertEquals(1, tr.getRaisedResponses().size()); 	//The RP_DEFER response event
		
		Iterator<ResponseEntry> re = tr.getRaisedResponses().iterator();
		ResponseEntry ree = re.next();
		Event retEvent = ree.getResponse();
		assertEquals(grantedEvent,ree.getInResponseTo());
		assertEquals(ResourcePoolEvent.RP_DEFER, retEvent.getType());
		assertEquals(grantTime,retEvent.getTime());
		assertEquals(rff.getId(),retEvent.getProducerId());
		assertFalse(re.hasNext());
		
		//invoke the next window where the repair and return should happen
		tr = new TestResources(Collections.emptyList());
		
		rff.consume(nextWindowTime * 2, tr);
		assertEquals(2, tr.getRaisedEvents().size()); 		//The RETURN_TO_SERVICE and RP_RETURN event
		assertEquals(0, tr.getRaisedResponses().size());
		
		events = tr.getRaisedEvents().iterator();
		
		retEvent = events.next();
		assertEquals(ResourcePoolEvent.RP_RETURN, retEvent.getType());
		assertEquals(rtsTime,retEvent.getTime());
		assertEquals(rff.getId(),retEvent.getProducerId());
		
		Event rtsEvent = events.next();
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, rtsEvent.getType());
		assertEquals(rtsTime, rtsEvent.getTime());
		assertEquals(rff.getId(),rtsEvent.getProducerId());
		
		assertFalse(events.hasNext());
	}
	
	@Test
	void testResourceRenewOverOneWindow() {
		//Use non random arrival again
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution df = dp.exponential(1000);
		Distribution dr = dp.exponential(100);

		RandomFailureFunction rff = new RandomFailureFunction("rff1",df,dr);

		FunctionInfo fi = new TestFunctionInfo() {
			@Override
			public Set<String> getResourcePools() {
				return Collections.singleton("pool1");
			}

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

		rff.initiate(fi);

		long nextFailureTime = (long)df.sample(0.5);
		long repairInterval = (long)Math.ceil(dr.sample(0.5));
		//Size the window so a failure will occur
		long nextWindowTime = nextFailureTime + 100;

		TestResources tr = new TestResources(Collections.emptyList());
		rff.consume(nextWindowTime, tr);
		
		assertEquals(2,tr.getRaisedEvents().size()); //Fail event an resource pool request
		assertEquals(0,tr.getRaisedResponses().size());
		
		Iterator<Event> events = tr.getRaisedEvents().iterator();
		Event failEvent = events.next();
		assertEquals(FailureSimulationEvent.FS_FAILED, failEvent.getType());
		assertEquals(nextFailureTime,failEvent.getTime());
		assertEquals(rff.getId(), failEvent.getProducerId());
		
		Event resourceRequest = events.next();
		assertEquals(ResourcePoolEvent.RP_REQUEST,resourceRequest.getType());
		assertEquals(nextFailureTime,resourceRequest.getTime());
		assertEquals(rff.getId(), resourceRequest.getProducerId());
		
		assertFalse(events.hasNext());
		
		//Inject the RP_DEFER request so the repair needs to wait until the next window
		long deferTime = nextFailureTime + 3;
		Event grantedEvent = new ResPoolEvent("pool1",ResourcePoolEvent.RP_DEFER,deferTime,ordinal++,ResourcePoolEvent.GRANT_RESPONSE_TYPES);
		tr = new TestResources(Collections.singletonList(grantedEvent));
		
		rff.consume(nextWindowTime, tr);
		
		//RandomFailureFunction should send RP_RENEW at the start of the next window
		long windowStart = nextWindowTime;
		nextWindowTime += repairInterval * 2;
		
		tr = new TestResources(Collections.emptyList());
		rff.consume(nextWindowTime, tr);
		
		assertEquals(1, tr.getRaisedEvents().size()); //RP_RENEW request
		assertEquals(0,tr.getRaisedResponses().size());
		
		events = tr.getRaisedEvents().iterator();
		Event renewEvent = events.next();
		assertEquals(ResourcePoolEvent.RP_RENEW, renewEvent.getType());
		assertEquals(windowStart + 1, renewEvent.getTime());
		assertEquals(rff.getId(), renewEvent.getProducerId());
		assertEquals(ResourcePoolEvent.REQUEST_RESPONSE_TYPES,renewEvent.getResponseTypes());
		
		assertFalse(events.hasNext());
		
		//Inject the RP_GRANTED event so the repair can happen
		long grantTime = windowStart + repairInterval/2;
		long rtsTime = grantTime + repairInterval;
		Event grantEvent = new ResPoolEvent("pool1",ResourcePoolEvent.RP_GRANT,grantTime,ordinal++,ResourcePoolEvent.GRANT_RESPONSE_TYPES);
		tr = new TestResources(Arrays.asList(new Event[] {grantEvent}));
		
		rff.consume(nextWindowTime, tr);
		
		assertEquals(1, tr.getRaisedEvents().size()); 		//The RETURN_TO_SERVICE event
		assertEquals(1, tr.getRaisedResponses().size());	//The RP_RETURN response
		
		events = tr.getRaisedEvents().iterator();
		Iterator<ResponseEntry> responses = tr.getRaisedResponses().iterator();
		
		ResponseEntry retEntry = responses.next();
		Event retEvent = retEntry.getResponse();
		assertEquals(ResourcePoolEvent.RP_RETURN, retEvent.getType());
		assertEquals(rtsTime,retEvent.getTime());
		assertEquals(rff.getId(),retEvent.getProducerId());
		assertEquals(grantEvent,retEntry.getInResponseTo());
		
		assertFalse(responses.hasNext());
		
		Event rtsEvent = events.next();
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, rtsEvent.getType());
		assertEquals(rtsTime, rtsEvent.getTime());
		assertEquals(rff.getId(),rtsEvent.getProducerId());
		
		assertFalse(events.hasNext());
	}
	
	@Test
	void testResourceRenewOverMultipleWindow() {
		//Use non random arrival again
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution df = dp.exponential(1000);
		Distribution dr = dp.exponential(100);

		RandomFailureFunction rff = new RandomFailureFunction("rff1",df,dr);

		FunctionInfo fi = new TestFunctionInfo() {
			@Override
			public Set<String> getResourcePools() {
				return Collections.singleton("pool1");
			}

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

		rff.initiate(fi);

		long nextFailureTime = (long)df.sample(0.5);
		long repairInterval = (long)Math.ceil(dr.sample(0.5));
		//Size the window so a failure will occur
		long nextWindowTime = nextFailureTime + 100;

		TestResources tr = new TestResources(Collections.emptyList());
		rff.consume(nextWindowTime, tr);
		
		assertEquals(2,tr.getRaisedEvents().size()); //Fail event an resource pool request
		assertEquals(0,tr.getRaisedResponses().size());
		
		Iterator<Event> events = tr.getRaisedEvents().iterator();
		Event failEvent = events.next();
		assertEquals(FailureSimulationEvent.FS_FAILED, failEvent.getType());
		assertEquals(nextFailureTime,failEvent.getTime());
		assertEquals(rff.getId(), failEvent.getProducerId());
		
		Event resourceRequest = events.next();
		assertEquals(ResourcePoolEvent.RP_REQUEST,resourceRequest.getType());
		assertEquals(nextFailureTime,resourceRequest.getTime());
		assertEquals(rff.getId(), resourceRequest.getProducerId());
		
		assertFalse(events.hasNext());
		
		//Inject the RP_DEFER request so the repair needs to wait until the next window
		long deferTime = nextFailureTime + 3;
		Event grantedEvent = new ResPoolEvent("pool1",ResourcePoolEvent.RP_DEFER,deferTime,ordinal++,ResourcePoolEvent.GRANT_RESPONSE_TYPES);
		tr = new TestResources(Collections.singletonList(grantedEvent));
		
		rff.consume(nextWindowTime, tr);
		
		//RandomFailureFunction should send RP_RENEW at the start of the next window
		long windowStart = nextWindowTime;
		nextWindowTime += repairInterval * 2;
		
		tr = new TestResources(Collections.emptyList());
		rff.consume(nextWindowTime, tr);
		
		assertEquals(1, tr.getRaisedEvents().size()); //RP_RENEW request
		assertEquals(0,tr.getRaisedResponses().size());
		
		events = tr.getRaisedEvents().iterator();
		Event renewEvent = events.next();
		assertEquals(ResourcePoolEvent.RP_RENEW, renewEvent.getType());
		assertEquals(windowStart + 1, renewEvent.getTime());
		assertEquals(rff.getId(), renewEvent.getProducerId());
		assertEquals(ResourcePoolEvent.REQUEST_RESPONSE_TYPES,renewEvent.getResponseTypes());
		
		assertFalse(events.hasNext());
		
		//Inject another RP_DEFER request to push the repair into the next window
		deferTime = windowStart + repairInterval;
		grantedEvent = new ResPoolEvent("pool1",ResourcePoolEvent.RP_DEFER,deferTime,ordinal++,ResourcePoolEvent.GRANT_RESPONSE_TYPES);
		tr = new TestResources(Collections.singletonList(grantedEvent));
		
		rff.consume(nextWindowTime, tr);
		
		//RandomFailureFunction should RENEW again
		
		windowStart = nextWindowTime;
		nextWindowTime += repairInterval * 2;
		
		tr = new TestResources(Collections.emptyList());
		rff.consume(nextWindowTime, tr);
		
		assertEquals(1, tr.getRaisedEvents().size()); //RP_RENEW request
		assertEquals(0,tr.getRaisedResponses().size());
		
		events = tr.getRaisedEvents().iterator();
		renewEvent = events.next();
		assertEquals(ResourcePoolEvent.RP_RENEW, renewEvent.getType());
		assertEquals(windowStart + 1, renewEvent.getTime());
		assertEquals(rff.getId(), renewEvent.getProducerId());
		assertEquals(ResourcePoolEvent.REQUEST_RESPONSE_TYPES,renewEvent.getResponseTypes());
		
		assertFalse(events.hasNext());
		
		//Inject the GRANT request so repair can occur
		long grantTime = windowStart + repairInterval/2;
		long rtsTime = grantTime + repairInterval;
		Event grantEvent = new ResPoolEvent("pool1",ResourcePoolEvent.RP_GRANT,grantTime,ordinal++,ResourcePoolEvent.GRANT_RESPONSE_TYPES);
		tr = new TestResources(Arrays.asList(new Event[] {grantEvent}));
		
		rff.consume(nextWindowTime, tr);
		
		assertEquals(1, tr.getRaisedEvents().size()); 		//The RETURN_TO_SERVICE event
		assertEquals(1, tr.getRaisedResponses().size());	//The RP_RETURN response
		
		events = tr.getRaisedEvents().iterator();
		Iterator<ResponseEntry> responses = tr.getRaisedResponses().iterator();
		
		ResponseEntry retEntry = responses.next();
		Event retEvent = retEntry.getResponse();
		assertEquals(ResourcePoolEvent.RP_RETURN, retEvent.getType());
		assertEquals(rtsTime,retEvent.getTime());
		assertEquals(rff.getId(),retEvent.getProducerId());
		assertEquals(grantEvent,retEntry.getInResponseTo());
		
		assertFalse(responses.hasNext());
		
		Event rtsEvent = events.next();
		assertEquals(FailureSimulationEvent.FS_RETURN_TO_SERVICE, rtsEvent.getType());
		assertEquals(rtsTime, rtsEvent.getTime());
		assertEquals(rff.getId(),rtsEvent.getProducerId());
		
		assertFalse(events.hasNext());
	}
	
	@Test
	void testMultipleFailuresAndRepairsInOneWindow() {
		//Use non random arrival again
		DistributionProvider dp = new CoreDistributionProvider();
		Distribution df = dp.exponential(1000);
		Distribution dr = dp.exponential(100);

		RandomFailureFunction rff = new RandomFailureFunction("rff1",df,dr);
		rff.setRestoreTime(101);

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

		rff.initiate(fi);

		long failureInterval = (long)df.sample(0.5);
		long repairInterval = (long)Math.ceil(dr.sample(0.5)) + 101;
		long cycleTime = failureInterval + repairInterval;
		TestResources tr = new TestResources(Collections.emptyList());

		rff.consume(cycleTime * 100, tr);
		
		assertEquals(200, tr.getRaisedEvents().size());
		assertEquals(0, tr.getRaisedResponses().size());
		
		int failCount = 0;
		int repairCount = 0;
		long lastFailTime = 0;
		long lastRepairTime = 0;
		
		for (Event e: tr.getRaisedEvents()) {
			if (FailureSimulationEvent.FS_FAILED == e.getType()) {
				assertEquals(lastRepairTime + failureInterval, e.getTime());
				lastFailTime = e.getTime();
				failCount++;
			}else if (FailureSimulationEvent.FS_RETURN_TO_SERVICE == e.getType()) {
				assertEquals(lastFailTime + repairInterval, e.getTime());
				lastRepairTime = e.getTime();
				repairCount++;
			}else {
				fail("Unexepected event type");
			}
			
			assertEquals(rff.getId(),e.getProducerId());
			assertEquals(Collections.emptyList(),e.getResponseTypes());
			
		}
		
		assertEquals(100,failCount);
		assertEquals(100,repairCount);
	}
	
}
