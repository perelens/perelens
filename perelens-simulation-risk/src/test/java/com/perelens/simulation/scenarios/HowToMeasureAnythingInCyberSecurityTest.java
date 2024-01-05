/**
 * 
 */
package com.perelens.simulation.scenarios;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;

import org.junit.jupiter.api.Test;

import com.perelens.engine.api.AbstractEventConsumer;
import com.perelens.engine.api.ConsumerResources;
import com.perelens.engine.api.EngineExecutionException;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventConsumer;
import com.perelens.engine.api.EventFilter;
import com.perelens.engine.api.EventType;
import com.perelens.simulation.api.DistributionProvider;
import com.perelens.simulation.api.RandomProvider;
import com.perelens.simulation.api.Simulation;
import com.perelens.simulation.api.SimulationBuilder;
import com.perelens.simulation.core.CoreDistributionProvider;
import com.perelens.simulation.core.CoreSimulationBuilder;
import com.perelens.simulation.failure.events.FailureSimulationEvent;
import com.perelens.simulation.mixed.EventToWindow;
import com.perelens.simulation.random.RanluxProvider;
import com.perelens.simulation.risk.ConditionalRisk;
import com.perelens.simulation.risk.MitigatingControl;
import com.perelens.simulation.risk.RandomRisk;
import com.perelens.simulation.risk.RealizedRisk;
import com.perelens.simulation.risk.WindowImpact;
import com.perelens.simulation.risk.events.RiskEvent;
import com.perelens.simulation.risk.events.RiskUnit;
import com.perelens.simulation.statistics.DistributionSamples;
import com.perelens.simulation.statistics.SampledStatistic;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
   
 * Test the simulation system on the examples from the book
 * How to Measure Anything in Cybersecurity Risk by Douglas Hubbard and Richard Seiersen
 * 
 * @author Steve Branda
 *
 */
class HowToMeasureAnythingInCyberSecurityTest {
	static final long hoursPerYear = 8760;
	
	@Test
	void testCh3OneForOneSubstituion() {
		SimulationBuilder sb = new CoreSimulationBuilder();
		DistributionProvider dp = new CoreDistributionProvider();
		RandomProvider rp = new RanluxProvider(674329);
		sb.setRandomProvider(rp);
		
		//Run the simulation for 10,000 years
		long totalTime = 10_000 * hoursPerYear;
		
		//Data Breach
		double dbLossLower = 500_000;
		double dbLossUpper = 10_000_000;
		double dbMean = hoursPerYear/0.4;
		var dataBreach = new RandomRisk("Data Breach",dp.exponential(dbMean),RiskEvent.RE_THREAT);
		var dataBreachLoss = new RealizedRisk("Data Breach Loss",RiskEvent.RE_THREAT,RiskEvent.RE_LOSS);
		dataBreachLoss.setResultMagnitude(dp.lognormal90pctCI(dbLossLower, dbLossUpper), RiskUnit.RU_QUANTITY);
		
		var dataBreachControl = new MitigatingControl("Data Breach Controlled",RiskEvent.RE_LOSS,0.35d,dp.constant(1));
		
		sb.addFunction(dataBreach);
		sb.addFunction(dataBreachLoss);
		sb.addFunction(dataBreachControl);
		
		sb.getFunction("Data Breach Controlled").addDependency(sb.getFunction("Data Breach Loss"));
		sb.getFunction("Data Breach Loss").addDependency(sb.getFunction("Data Breach"));
		
		//Malware
		double malLossLower = 2_000_000;
		double malLossUpper = 5_000_000;
		double malMean = hoursPerYear/0.15;
		var malware = new RandomRisk("Malware", dp.exponential(malMean), RiskEvent.RE_THREAT);
		var malwareLoss = new RealizedRisk("Malware Loss", RiskEvent.RE_THREAT,RiskEvent.RE_LOSS);
		malwareLoss.setResultMagnitude(dp.lognormal90pctCI(malLossLower, malLossUpper), RiskUnit.RU_QUANTITY);
		
		var malwareControl = new MitigatingControl("Malware Controlled",RiskEvent.RE_LOSS,0.25d,dp.constant(1));
		
		sb.addFunction(malware);
		sb.addFunction(malwareLoss).addDependency(sb.getFunction("Malware"));
		sb.addFunction(malwareControl).addDependency(sb.getFunction("Malware Loss"));
		
		//Lost laptops
		double llLossLower = 150_000;
		double llLossUpper = 300_000;
		double llMean = hoursPerYear/0.35;
		var lostLaptops = new RandomRisk("Lost Laptops", dp.exponential(llMean), RiskEvent.RE_THREAT);
		var lostLaptopsLoss = new RealizedRisk("Lost Laptops Loss", RiskEvent.RE_THREAT,RiskEvent.RE_LOSS);
		lostLaptopsLoss.setResultMagnitude(dp.lognormal90pctCI(llLossLower, llLossUpper), RiskUnit.RU_QUANTITY);
		
		var lostLaptopsControl = new MitigatingControl("Lost Laptops Controlled",RiskEvent.RE_LOSS,0.5d,dp.constant(1));
		
		sb.addFunction(lostLaptops);
		sb.addFunction(lostLaptopsLoss).addDependency("Lost Laptops");
		sb.addFunction(lostLaptopsControl).addDependency("Lost Laptops Loss");
		
		//Pennance projects
		double ppLossLower = 200_000;
		double ppLossUpper = 500_000;
		double ppMean = hoursPerYear/0.1;
		var pennanceProject = new RandomRisk("Pennance Projects", dp.exponential(ppMean),RiskEvent.RE_THREAT);
		var pennanceProjectLoss = new RealizedRisk("Pennance Projects Loss", RiskEvent.RE_THREAT,RiskEvent.RE_LOSS);
		pennanceProjectLoss.setResultMagnitude(dp.lognormal90pctCI(ppLossLower, ppLossUpper), RiskUnit.RU_QUANTITY);
		
		var pennanceProjectsControl = new MitigatingControl("Pennance Projects Controlled", RiskEvent.RE_LOSS,0.1d,dp.constant(1));
		
		sb.addFunction(pennanceProject);
		sb.addFunction(pennanceProjectLoss).addDependency(sb.getFunction("Pennance Projects"));
		sb.addFunction(pennanceProjectsControl).addDependency(sb.getFunction("Pennance Projects Loss"));
		
		EventCount dataBreachCount = new EventCount("EC",RiskEvent.RE_THREAT);
		
		var residual = new YearlyLossConsumer("residual consumer",hoursPerYear);
		var inherent = new YearlyLossConsumer("inherent consumer",hoursPerYear);
			
		var sim = sb.createSimulation(4);
		sim.subscribeToEvents(residual,
					Arrays.asList(new String[] {
							"Data Breach Controlled","Malware Controlled","Lost Laptops Controlled","Pennance Projects Controlled"})
					);
			
		sim.subscribeToEvents(inherent,
					Arrays.asList(new String[] {
							"Data Breach Loss","Malware Loss","Lost Laptops Loss","Pennance Projects Loss"})
					);
			
		sim.subscribeToEvents(dataBreachCount, "Data Breach");
		sim.start(totalTime);
		try {
			sim.join();
		} catch (EngineExecutionException e) {
			for (Throwable t: e.getCauses()) {
				t.printStackTrace();
				System.err.println();
			}
			sim.destroy();
			fail();
		}catch (Throwable t) {
			t.printStackTrace();
			sim.destroy();
			fail();
		}
		sim.destroy();
		
		//Make sure we are within 1% of the averages from the sample excel spreadsheet

		double expInhLoss = 1_967_614;
		double expResLoss =  1_310_325;

		assertEquals(expInhLoss,inherent.losses.getMean(), expInhLoss * 0.01);
		assertEquals(expResLoss,residual.losses.getMean(), expResLoss * 0.01);
		
	}
	
	@Test
	void testCh6FurtherDecompLevel1() {
		
		long minutesPerYear = hoursPerYear * 60;
		long totalTime = 10_000 * minutesPerYear;
		
		SimulationBuilder sb = new CoreSimulationBuilder();
		DistributionProvider dp = new CoreDistributionProvider();
		RandomProvider rp = new RanluxProvider(1630510946807L);
		sb.setRandomProvider(rp);
		
		//Rate of occurrence info
		double aro = 0.10; //10%
		
		//Risk for the base event
		double baseMean = minutesPerYear/aro;
		var baseRisk = new RandomRisk("Base Event",dp.exponential(baseMean), RiskEvent.RE_THREAT);
		sb.addFunction(baseRisk);
		
		var sim = sb.createSimulation(1);
		
		var c1 = new AbstractEventConsumer("c1") {

			long lastThreat = 0;
			long lastThreatEnd = 0;
			
			@Override
			protected void processEvent(Event currentEvent) {
				if (currentEvent.getType() == RiskEvent.RE_THREAT) {
					assertTrue(currentEvent.getTime() > lastThreat);
					lastThreat = currentEvent.getTime();
				}else if (currentEvent.getType() == RiskEvent.RE_THREAT_END) {
					assertTrue(currentEvent.getTime() > lastThreatEnd);
					lastThreatEnd = currentEvent.getTime();
					assertEquals(lastThreat,lastThreatEnd);
				}else {
					fail("Unexpected event type:" + currentEvent);
				}
			}};
			
		var c2 = new EventCount("c2", RiskEvent.RE_THREAT);
		var c3 = new EventCount("c3", RiskEvent.RE_THREAT_END);
		
		sim.subscribeToEvents(c1, baseRisk.getId());
		sim.subscribeToEvents(c2, baseRisk.getId());
		sim.subscribeToEvents(c3, baseRisk.getId());
		
		runSimulation(sim,totalTime);
		
		assertEquals(10_000 * aro,c2.total, 10_000 * aro * 0.03);
		assertEquals(c2.total,c3.total);
	}
	
	@Test
	void testCh6FurtherDecompLevel2() {
		
		long minutesPerYear = hoursPerYear * 60;
		long totalTime = 10_000 * minutesPerYear;
		
		SimulationBuilder sb = new CoreSimulationBuilder();
		DistributionProvider dp = new CoreDistributionProvider();
		RandomProvider rp = new RanluxProvider(1630510946807L);
		sb.setRandomProvider(rp);
		
		//Rate of occurrence info
		double aro = 0.10; //10%
		double confOnly = 0.2; //20%
		double availOnly = 0.5; //50%
		double both = 1.0d - confOnly - availOnly;
		
		//Risk for the base event
		double baseMean = minutesPerYear/aro;
		var baseRisk = new RandomRisk("Base Event",dp.exponential(baseMean), RiskEvent.RE_THREAT);
		sb.addFunction(baseRisk);
		
		//Risk of both Confidentiality and Availability Events
		var bothRisk = new ConditionalRisk("both",RiskEvent.RE_THREAT,both,RiskEvent.RE_LOSS);
		sb.addFunction(bothRisk)
			.addDependency(baseRisk.getId());
		
		var sim = sb.createSimulation(1);
		
		var c1 = new AbstractEventConsumer("c1") {

			long lastThreat = 0;
			long lastThreatEnd = 0;
			
			@Override
			protected void processEvent(Event currentEvent) {
				if (currentEvent.getType() == RiskEvent.RE_LOSS) {
					assertTrue(currentEvent.getTime() > lastThreat);
					lastThreat = currentEvent.getTime();
				}else if (currentEvent.getType() == RiskEvent.RE_LOSS_END) {
					assertTrue(currentEvent.getTime() > lastThreatEnd);
					lastThreatEnd = currentEvent.getTime();
					assertEquals(lastThreat,lastThreatEnd);
				}else {
					fail("Unexpected event type:" + currentEvent);
				}
			}};
			
		var c2 = new EventCount("c2", RiskEvent.RE_LOSS);
		var c3 = new EventCount("c3", RiskEvent.RE_LOSS_END);
		
		sim.subscribeToEvents(c1, bothRisk.getId());
		sim.subscribeToEvents(c2, bothRisk.getId());
		sim.subscribeToEvents(c3, bothRisk.getId());
		
		runSimulation(sim,totalTime);
		
		assertEquals(10_000 * aro * both,c2.total, 10_000 * aro * both * 0.03);
		assertEquals(c2.total,c3.total);
	}
	
	@Test
	void testCh6FurtherDecompLevel3() {
		
		long minutesPerYear = hoursPerYear * 60;
		long totalTime = 10_000 * minutesPerYear;
		
		SimulationBuilder sb = new CoreSimulationBuilder();
		DistributionProvider dp = new CoreDistributionProvider();
		RandomProvider rp = new RanluxProvider(1630510946807L);
		sb.setRandomProvider(rp);
		
		//Rate of occurrence info
		double aro = 0.10; //10%
		double confOnly = 0.2; //20%
		double availOnly = 0.5; //50%
		double both = 1.0d - confOnly - availOnly;
		
		//Risk for the base event
		double baseMean = minutesPerYear/aro;
		var baseRisk = new RandomRisk("Base Event",dp.exponential(baseMean), RiskEvent.RE_THREAT);
		sb.addFunction(baseRisk);
		
		//Risk of both Confidentiality and Availability Events
		var bothRisk = new ConditionalRisk("both",RiskEvent.RE_THREAT,both,RiskEvent.RE_LOSS);
		sb.addFunction(bothRisk)
			.addDependency(baseRisk.getId());
		
		//Risk of Confidentiality only if not both
		var confOnlyNotBoth = confOnly/(availOnly + confOnly);
		var cNotB = new ConditionalRisk("cNotB", RiskEvent.RE_THREAT,confOnlyNotBoth,RiskEvent.RE_LOSS);
		cNotB.setCondition(RiskEvent.RE_LOSS, false);
		sb.addFunction(cNotB)
			.addDependency(baseRisk.getId()) //Source of THREAT event
			.addDependency(bothRisk.getId());//Source of LOSS event
		
		var sim = sb.createSimulation(1);
		
		var c1 = new AbstractEventConsumer("c1") {

			long lastThreat = 0;
			long lastThreatEnd = 0;
			
			@Override
			protected void processEvent(Event currentEvent) {
				if (currentEvent.getType() == RiskEvent.RE_LOSS) {
					assertTrue(currentEvent.getTime() > lastThreat);
					lastThreat = currentEvent.getTime();
				}else if (currentEvent.getType() == RiskEvent.RE_LOSS_END) {
					assertTrue(currentEvent.getTime() > lastThreatEnd);
					lastThreatEnd = currentEvent.getTime();
					assertEquals(lastThreat,lastThreatEnd);
				}else {
					fail("Unexpected event type:" + currentEvent);
				}
			}};
			
		var c2 = new EventCount("c2", RiskEvent.RE_LOSS);
		var c3 = new EventCount("c3", RiskEvent.RE_LOSS_END);
		
		sim.subscribeToEvents(c1, cNotB.getId());
		sim.subscribeToEvents(c2, cNotB.getId());
		sim.subscribeToEvents(c3, cNotB.getId());
		
		runSimulation(sim,totalTime);
		
		assertEquals(10_000 * aro * confOnly,c2.total, 10_000 * aro * confOnly * 0.03);
		assertEquals(c2.total,c3.total);
	}
	
	@Test
	void testCh6FurtherDecompLevel4() {
		
		long minutesPerYear = hoursPerYear * 60;
		long totalTime = 10_000 * minutesPerYear;
		
		SimulationBuilder sb = new CoreSimulationBuilder();
		DistributionProvider dp = new CoreDistributionProvider();
		RandomProvider rp = new RanluxProvider(1630510946807L);
		sb.setRandomProvider(rp);
		
		//Rate of occurrence info
		double aro = 0.10; //10%
		double confOnly = 0.2; //20%
		double availOnly = 0.5; //50%
		double both = 1.0d - confOnly - availOnly;
		
		//Risk for the base event
		double baseMean = minutesPerYear/aro;
		var baseRisk = new RandomRisk("Base Event",dp.exponential(baseMean), RiskEvent.RE_THREAT);
		sb.addFunction(baseRisk);
		
		//Risk of both Confidentiality and Availability Events
		var bothRisk = new ConditionalRisk("both",RiskEvent.RE_THREAT,both,RiskEvent.RE_LOSS);
		sb.addFunction(bothRisk)
			.addDependency(baseRisk.getId());
		
		//Risk of Confidentiality only if not both
		var confOnlyNotBoth = confOnly/(availOnly + confOnly);
		var cNotB = new ConditionalRisk("cNotB", RiskEvent.RE_THREAT,confOnlyNotBoth,RiskEvent.RE_LOSS);
		cNotB.setCondition(RiskEvent.RE_LOSS, false);
		sb.addFunction(cNotB)
			.addDependency(baseRisk.getId()) //Source of THREAT event
			.addDependency(bothRisk.getId());//Source of LOSS event
		
		//Availability if not confidentiality or both
		var aNotBNotC = new ConditionalRisk("aNotBNotC", RiskEvent.RE_THREAT, 1.0,RiskEvent.RE_LOSS);
		aNotBNotC.setCondition(RiskEvent.RE_LOSS, false);
		sb.addFunction(aNotBNotC)
			.addDependency(cNotB.getId()) //Source of LOSS Event
			.addDependency(bothRisk.getId()) //Source of LOSS Event
			.addDependency(baseRisk.getId()); //Source of THREAT event
		
		var sim = sb.createSimulation(1);
		
		var c1 = new AbstractEventConsumer("c1") {

			long lastThreat = 0;
			long lastThreatEnd = 0;
			
			@Override
			protected void processEvent(Event currentEvent) {
				if (currentEvent.getType() == RiskEvent.RE_LOSS) {
					assertTrue(currentEvent.getTime() > lastThreat);
					lastThreat = currentEvent.getTime();
				}else if (currentEvent.getType() == RiskEvent.RE_LOSS_END) {
					assertTrue(currentEvent.getTime() > lastThreatEnd);
					lastThreatEnd = currentEvent.getTime();
					assertEquals(lastThreat,lastThreatEnd);
				}else {
					fail("Unexpected event type:" + currentEvent);
				}
			}};
			
		var c2 = new EventCount("c2", RiskEvent.RE_LOSS);
		var c3 = new EventCount("c3", RiskEvent.RE_LOSS_END);
		
		//Ensure that the RANDOM threat event does not generate
		//additional LOSS events
		var c4 = new AbstractEventConsumer("c4") {

			private IdentityHashMap<Event,Event> mapping = new IdentityHashMap<>();
			
			@Override
			protected void processEvent(Event currentEvent) {
				
				if (currentEvent.getType() == RiskEvent.RE_LOSS) {
					var causes = currentEvent.causedBy();
					Event cause = null;
					while (causes.hasNext()) {
						cause = causes.next();
						causes = cause.causedBy();
					}
					assertEquals(RiskEvent.RE_THREAT, cause.getType());
					
					//Make sure there are no other loss events with this THREAT event at the root
					Event prev = mapping.put(cause, currentEvent);
					if (prev != null) {
						fail("Events should be exclusive: " + prev + "\n\n" + currentEvent);
					}
				}else if (currentEvent.getType() != RiskEvent.RE_LOSS_END) {
					fail("Unexepcted event type: " + currentEvent);
				}
				
			}
			
		};
		
		sim.subscribeToEvents(c1, aNotBNotC.getId());
		sim.subscribeToEvents(c2, aNotBNotC.getId());
		sim.subscribeToEvents(c3, aNotBNotC.getId());
		
		sim.subscribeToEvents(c4, Arrays.asList(new String[] {aNotBNotC.getId(),cNotB.getId(),bothRisk.getId()}));
		
		runSimulation(sim,totalTime);
		
		assertEquals(10_000 * aro * availOnly,c2.total, 10_000 * aro * availOnly * 0.03);
		assertEquals(c2.total,c3.total);
	}

	
	@Test
	void testCh6FurtherDecomposition() {
		
		SimulationBuilder sb = new CoreSimulationBuilder();
		DistributionProvider dp = new CoreDistributionProvider();
		RandomProvider rp = new RanluxProvider(1628086431547L);
		sb.setRandomProvider(rp);
		
		//Run the simulation long enough to hit the accuracy target
		//1% accuracy took 18_000_000 years
		//2% accuracy took 3_000_000 years
		
		long minutesPerYear = hoursPerYear * 60;
		long totalTime = 1_000_000 /*years*/ * minutesPerYear;
		double accuracyTarget = 0.03; //3%
		
		double[][] args = new double[][] {
				new double[]  {1,  0.02, 0.2, 0.5,110_000,2_200_000,2,4,6100,61_000,7446.96,1140.30}
				,new double[] {2,  0.05, 0.2, 0.3,10_000,50_000,0.5,2,150,450,882.12,12.01}
				,new double[] {3,  0.1, 0.5, 0.1,20_000,400_000,0.25,1.25,3500,17500,12185.94,277.91}
				,new double[] {4,  0.15, 0.4, 0.6, 10_000, 50_000, 0.25, 0.25, 300, 900, 1512.20, 12.36}
				,new double[] {5,  0.20, 0.1, 0.6, 10_000, 200_000, 7.75, 7.75, 350, 1050, 5415.97, 894.17}
				,new double[] {6,  0.12, 0, 0.8, 10_000, 50_000, 0.25, 0.75, 8400, 42_000, 604.88, 1163.16}
				,new double[] {7,  0.08, 0.1, 0.5, 70_000, 1_400_000, 18, 72, 500, 2_500, 18955.91, 3569.65}
				,new double[] {8,  0.11, 0.5, 0, 20_000, 100_000, 0.75, 3.75, 350, 1750, 5544.75, 91.71}
				,new double[] {9,  0.4, 0.1, 0.5, 20_000, 100_000, 0.75, 3.0, 300, 1500, 10081.36, 446.21}
				,new double[] {10, 0.02, 0.4, 0.6, 10_000, 50_000, 11.75, 47, 600, 1800, 201.63, 338.64}
				,new double[] {11, 0.25, 0.2, 0.2, 30_000, 150_000, 8.5, 34, 4000, 40_000, 15_122.04, 60_045.80}
				,new double[] {12, 0.02, 0.2, 0.1, 20_000, 100_000, 14.50, 14.50, 150, 750, 907.32, 87.71}
				,new double[] {13, 0.09, 0.3, 0.3, 10_000, 50_000, 7.75, 23.25, 2500, 7500, 1587.81, 4093.91}
				,new double[] {14, 0.04, 0.1, 0.3, 10_000, 200_000, 0.5, 2.0, 1400, 7000, 1895.59, 138.82}
				,new double[] {15, 0.05, 0.1, 0.6, 40_000, 200_000, 0.5, 2.0, 2300, 11500, 2016.27, 285.08}
				,new double[] {16, 0.06, 0.2, 0.3, 40_000, 800_000, 5.5, 16.5, 1700, 5100, 11373.54, 1505.25}
				,new double[] {17, 0.12, 0, 0.4, 20_000, 100_000, 0.25, 0.50, 1300, 6500, 3629.29, 142.13}
				,new double[] {18, 0.02, 0.2, 0.5, 40_000, 200_000, 0.25, 0.25, 4900, 24_500, 1008.14, 49.40}
				,new double[] {19, 0.03, 0.4, 0.2, 200_000, 4_000_000, 24.75, 99, 1700,5100, 32_495.84, 3031.56}
				,new double[] {20, 0.11, 0.4, 0.6, 80_000, 400_000, 1, 3, 200, 600, 8871.60, 44.27}
				,new double[] {21, 0.23, 0.4, 0.1, 10_000, 50_000, 0.5, 1.50, 450, 4500, 5217.10, 229.73}
				,new double[] {22, 0.34, 0.5, 0.3, 10_000, 50_000, 2, 2, 1100, 5500, 5998.41, 942.61}
				,new double[] {23, 0.21, 0.4, 0.3, 10_000, 50_000, 0.25, 0.75, 9200, 46000, 3704.90, 1337.63}
				,new double[] {24, 0.13, 0.1, 0.1, 20_000, 400_000, 2.25, 4.5, 600, 1800, 15841, 418.27}
				,new double[] {25, 0.02, 0.2, 0.8, 10_000, 50_000, 0.5, 1, 300, 1500, 100.81, 8.75}
				,new double[] {26, 0.07, 0.4, 0.2, 10_000, 200_000,0.25, 2, 200, 2000, 3791.18, 29.30}
				,new double[] {27, 0.05, 0.1,0.8, 20_000, 400_000, 0.5, 2.5, 8400, 42_000, 1353.00, 1200.55}
				,new double[] {28, 0.02, 0, 0.5, 10_000, 200_000, 0.5, 1.5, 4200, 21_000, 677, 193.86}
				,new double[] {29, 0.45, 0.1, 0.9, 40_000, 200_000, 48.50, 48.50, 1300, 13_000, 4536.61, 103_162.70}
				,new double[] {30, 0.35, 0.2, 0.6, 260_000, 1_300_000, 0.75, 1.5, 1000, 5000, 91740.38, 765.30}
				,new double[] {31, 0.09, 0, 0.9, 10_000, 200_000, 1.5, 3, 1400, 4200, 609.3, 500.49}
				,new double[] {32, 0.04, 0.4, 0.6, 20_000, 100_000, 0.5, 1.0, 700, 2100, 806.51, 22.24}
				,new double[] {33, 0.05, 0.2, 0.2, 20_000, 400_000, 2.5, 10, 200, 600, 5415.97, 80.06}
				,new double[] {34, 0.06, 0.2, 0.8, 10_000, 50_000, 4, 12, 1100, 11_000, 302.44, 1562.62}
				,new double[] {35, 0.12, 0, 0.7, 10_000, 50_000, 2.25, 6.75, 300, 900, 907.32, 271.67}
				,new double[] {36, 0.02, 0.4, 0.5, 10_000, 50_000, 1, 6, 7300, 21_900, 252.03, 455.80}
				,new double[] {37, 0.03, 0.5, 0.5, 10_000, 200_000, 0.75, 3, 2700, 13_500, 1015.50, 167.33}
				,new double[] {38, 0.15, 0.2, 0.8, 10_000, 50_000, 0.25, 1, 200, 2000, 756.10, 52.98}
				,new double[] {39, 0.23, 0.4, 0.6, 30_000, 150_000, 0.5, 1, 4100, 20_500, 6956.14, 1030.97}
				,new double[] {40, 0.34, 0.4, 0.6, 10_000, 200_000, 19.75, 79, 3800, 38000, 9207.15, 135_193.12}
				,new double[] {41, 0.21, 0, 0.7, 20_000, 400_000, 2, 4, 1200, 6000, 8530.16, 1836.73}
				,new double[] {42, 0.02, 0.4, 0.2, 10_000, 50_000, 0.25, 2, 1500, 4500, 403.25, 28.46}
				,new double[] {43, 0.03, 0, 0, 20_000, 400_000, 3.5, 14, 1900, 9500, 4061.98, 1098.99}
				,new double[] {44, 0.11, 0.3, 0.5, 70_000, 350_000, 2, 4, 600, 3000, 9703.31, 336.73}
				,new double[] {45, 0.23, 0.1, 0.5, 40_000, 800_000, 0.75, 1.5, 37_600, 188_000, 31_141.85, 21_273.25}
				,new double[] {46, 0.34, 0.5, 0.5, 10_000, 50_000, 0.75, 3, 200, 600, 4284.58, 102.07} //Comment here for failure
				,new double[] {47, 0.21, 0.4, 0.6, 20_000, 100_000, 5.25, 21, 600, 1800, 4234.17, 1588.73}
				,new double[] {48, 0.13, 0.2, 0.2, 40_000, 200_000, 0.75, 3.75, 200, 600, 10_484.62, 72}
				,new double[] {49, 0.02, 0.3, 0.2, 20_000, 100_000, 0.5, 0.5, 350, 1750, 806.51, 6.17}
				,new double[] {50, 0.07, 0.4, 0.6, 10_000, 50_000, 0.75, 3, 600, 1800, 705.70, 75.65}
		};
		
		var totalLosses = new HashMap<String, YearlyLossConsumer>();
		var confLosses = new HashMap<String, YearlyLossConsumer>();
		var availLosses = new HashMap<String, YearlyLossConsumer>();
		
		for (double[] curArgs: args) {
			
			var name = "Event " + (int)curArgs[0];
			
			//Rate of occurrence info
			double aro = curArgs[1];
			double confOnly = curArgs[2];
			double availOnly = curArgs[3];
			double both = 1.0d - confOnly - availOnly;
			
			//Loss magnitude info
			double confLossLower = curArgs[4];
			double confLossUpper = curArgs[5];
			
			double outageDurLower = curArgs[6];
			double outageDurUpper = curArgs[7];
			double outageLossLower = curArgs[8];
			double outageLossUpper = curArgs[9];
			
			//Risk for the base event
			double baseMean = minutesPerYear/aro;
			var baseRisk = new RandomRisk(name,dp.exponential(baseMean), RiskEvent.RE_THREAT);
			sb.addFunction(baseRisk);
			
			//Risk of both Confidentiality and Availability Events
			var bothRisk = new ConditionalRisk(name + " both",RiskEvent.RE_THREAT,both,RiskEvent.RE_LOSS);
			sb.addFunction(bothRisk)
				.addDependency(baseRisk.getId());
			
			//Risk of Confidentiality only if not both
			var confOnlyNotBoth = confOnly/(availOnly + confOnly);
			var cNotB = new ConditionalRisk(name + " cNotB", RiskEvent.RE_THREAT,confOnlyNotBoth,RiskEvent.RE_LOSS);
			cNotB.setCondition(RiskEvent.RE_LOSS, false);
			sb.addFunction(cNotB)
				.addDependency(baseRisk.getId()) //Source of THREAT event
				.addDependency(bothRisk.getId());//Source of LOSS event
			
			//Availability if not confidentiality or both
			var aNotBNotC = new ConditionalRisk(name + " aNotBNotC", RiskEvent.RE_THREAT, 1.0,RiskEvent.RE_LOSS);
			aNotBNotC.setCondition(RiskEvent.RE_LOSS, false);
			sb.addFunction(aNotBNotC)
				.addDependency(cNotB.getId()) //Source of LOSS Event
				.addDependency(bothRisk.getId()) //Source of LOSS Event
				.addDependency(baseRisk.getId()); //Source of THREAT event
			
			
			//Confidentiality impact
			var ci = new RealizedRisk(name + " CI", RiskEvent.RE_LOSS, RiskEvent.RE_LOSS);
			ci.setResultMagnitude(dp.lognormal90pctCI(confLossLower, confLossUpper), RiskUnit.RU_QUANTITY);
			sb.addFunction(ci)
				.addDependency(bothRisk.getId())
				.addDependency(cNotB.getId());
			
			//Availability impact
			var ai = new RealizedRisk(name + " AI", RiskEvent.RE_LOSS, RiskEvent.RE_LOSS);
			sb.addFunction(ai)
				.addDependency(bothRisk.getId())
				.addDependency(aNotBNotC.getId());
			
			//Availability outage
			var ao = new EventToWindow(name + " AO", RiskEvent.RE_LOSS,FailureSimulationEvent.FS_FAILED,FailureSimulationEvent.FS_RETURN_TO_SERVICE);
			if (outageDurLower == outageDurUpper) {
				ao.setEventDuration(dp.constant(outageDurUpper * 60));
			}else{
				ao.setEventDuration(dp.lognormal90pctCI(outageDurLower*60, outageDurUpper*60));
			}
			sb.addFunction(ao)
				.addDependency(ai.getId());
			
			//Availability loss
			var al = new WindowImpact(name + " AL",FailureSimulationEvent.FS_FAILED,FailureSimulationEvent.FS_RETURN_TO_SERVICE,RiskEvent.RE_LOSS,60);
			al.setMagnitude(RiskUnit.RU_QUANTITY, dp.lognormal90pctCI(outageLossLower, outageLossUpper));
			sb.addFunction(al)
				.addDependency(ao.getId());
			
			
			totalLosses.put(name,new YearlyLossConsumer(name + " Losses",minutesPerYear));
			confLosses.put(name,new YearlyLossConsumer(name + " cLosses",minutesPerYear));
			availLosses.put(name,new YearlyLossConsumer(name + " aLosses", minutesPerYear));
		}
		
		var sim = sb.createSimulation(4);
		
		//Register the consumers
		for (double[] curArgs: args) {
			
			var name = "Event " + (int)curArgs[0];
			
			sim.subscribeToEvents(availLosses.get(name), name + " AL");
			sim.subscribeToEvents(confLosses.get(name), name + " CI");
			sim.subscribeToEvents(totalLosses.get(name), name + " AL");
			sim.subscribeToEvents(totalLosses.get(name), name + " CI");
		}
		
		runSimulation(sim,totalTime);
		
		//Check the results
		for (double[] curArgs: args) {
			
			var name = "Event " + (int)curArgs[0];
			
			double confExpected = curArgs[10];
			double availExpected = curArgs[11];
			
			double confReal = confLosses.get(name).losses.getMean();
			double availReal = availLosses.get(name).losses.getMean();
			double totalReal = totalLosses.get(name).losses.getMean();
			
			assertEquals(totalReal, confReal + availReal, totalReal * accuracyTarget);
			assertEquals(confExpected, confReal, confExpected * accuracyTarget);
			assertEquals(availExpected, availReal, availExpected * accuracyTarget);
		}
		
	}
	
	static void runSimulation(Simulation sim, long totalTime) {
		sim.start(totalTime);
		try {
			sim.join();
		} catch (EngineExecutionException e) {
			for (Throwable t: e.getCauses()) {
				t.printStackTrace();
				System.err.println();
			}
			sim.destroy();
			fail();
		}catch (Throwable t) {
			t.printStackTrace();
			sim.destroy();
			fail();
		}
		sim.destroy();
	}
	
	static class YearlyLossConsumer extends AbstractEventConsumer implements EventFilter{

		double total = 0;
		long yearMarker;
		final long yearUnits;
		SampledStatistic losses = new SampledStatistic();
		
		protected YearlyLossConsumer(String id, long yearUnits) {
			super(id);
			this.yearUnits = yearUnits;
			this.yearMarker = yearUnits;
		}
		
		@Override
		protected void processEvent(Event currentEvent) {
			while (currentEvent.getTime() >= yearMarker) {
//				if (getId().startsWith("residual")){
//					System.out.println(total);
//				}
				losses.add(total);
				total = 0;
				yearMarker += yearUnits;
			}
			
			total += currentEvent.getMagnitude().magnitude();
		}

		@Override
		public boolean filter(Event event) {
			return event.getType() == RiskEvent.RE_LOSS;
		}
		@Override
		public EventFilter getEventFilter() {
			return this;
		}
	}
	
	static class EventCount implements EventConsumer, EventFilter{

		long total = 0;
		DistributionSamples arrivalDuration = new DistributionSamples();
		long lastEventTime = 0;
		private EventType toCount;
		private String id;

		public EventCount(String id, EventType toCount) {
			assertNotNull(id);
			assertNotNull(toCount);
			this.toCount = toCount;
			this.id = id;
		}
		
		@Override
		public String getId() {
			return id;
		}

		@Override
		public EventFilter getEventFilter() {
			return this;
		}

		@Override
		public void consume(long timeWindow, ConsumerResources resources) {
			for (Event e : resources.getEvents()) {
				total++;
				long ad = e.getTime() - lastEventTime;
				arrivalDuration.addData(ad);
				lastEventTime = e.getTime();
			}
		}

		@Override
		public boolean filter(Event event) {
			return event.getType() == toCount;
		}
	}
}
