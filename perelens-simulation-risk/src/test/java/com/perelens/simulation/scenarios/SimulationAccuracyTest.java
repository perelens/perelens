/**
 * 
 */
package com.perelens.simulation.scenarios;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.perelens.simulation.api.RandomProvider;
import com.perelens.simulation.api.Simulation;
import com.perelens.simulation.api.SimulationBuilder;
import com.perelens.simulation.failure.consumers.AvailabilitySampler;
import com.perelens.simulation.random.RanluxProvider;
import com.perelens.simulation.statistics.SampledStatistic;


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
class SimulationAccuracyTest {
	
	private ValidationTest scenarios = getScenarios();
	
	protected ValidationTest getScenarios() {
		return new ValidationTest();
	}
	@Test
	void test1000SingleSpare16NodeCluster() throws Throwable {
		SimulationBuilder builder = scenarios.setup1000SingleSpare16NodeClusters();
		RandomProvider rp = new RanluxProvider(1593636046552L);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		long sampleDuration = 525600; //in minutes
		long timeBetweenSamples = (sampleDuration/7) * 6;
		long firstSampleTime = 0;
		AvailabilitySampler data = new AvailabilitySampler("availability", firstSampleTime,sampleDuration, timeBetweenSamples);
		s.subscribeToEvents(data, Collections.singletonList("main cluster"));

		s.start(3000000000L); //5707 years in minutes
		s.join();
		
		SampledStatistic samp = new SampledStatistic();
		samp.add(data.getSamples());
		
		double simA = samp.getMean();
		
		double expectedA = ValidationCalculations.OneThousandSingleSpare16NodeCluster();
		
		double simPct = (simA - expectedA)/(1.0 - expectedA) * 100.0;
		
		System.out.println("1000 Single Spare 16 Node Cluster\t" + expectedA + "\t" + simA + "\t" + simPct + "%");
		
		assertTrue(Math.abs(simPct) < 1.0);
	}
	
	@Test
	void testActiveActiveVol2p122() throws Throwable{
		SimulationBuilder builder = scenarios.setupActiveActiveVol2p122();
		RandomProvider rp = new RanluxProvider(1593636165491L);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		long sampleDuration = 525600; //in minutes
		long timeBetweenSamples = (sampleDuration/7) * 6;
		long firstSampleTime = 0;
		AvailabilitySampler data = new AvailabilitySampler("availability", firstSampleTime,sampleDuration, timeBetweenSamples);
		s.subscribeToEvents(data, Collections.singletonList("system"));

		s.start(8000000000000L); //15 million years in minutes
		s.join();
		
		SampledStatistic samp = new SampledStatistic();
		samp.add(data.getSamples());
		
		double simA = samp.getMean();
		
		double expectedA = ValidationCalculations.ActiveActiveVol2p122();
		
		double simPct = (simA - expectedA)/(1.0 - expectedA) * 100.0;
		
		System.out.println("Active Active Vol2 p122\t" + expectedA + "\t" + simA + "\t" + simPct + "%");
		
		assertTrue(Math.abs(simPct) < 15.0); //Very little downtime in this scenario.  Simulation can swing quite a bit randomly.
	}
	
	@Test
	void testActiveBackupVol2p121() throws Throwable{
		SimulationBuilder builder = scenarios.setupActiveBackupVol2p121();
		RandomProvider rp = new RanluxProvider(1593636202246L);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		long sampleDuration = 525600; //in minutes
		long timeBetweenSamples = (sampleDuration/7) * 6;
		long firstSampleTime = 0;
		AvailabilitySampler data = new AvailabilitySampler("availability", firstSampleTime,sampleDuration, timeBetweenSamples);
		s.subscribeToEvents(data, Collections.singletonList("system"));

		s.start(300000000000L); //57,000 years in minutes
		s.join();
		
		SampledStatistic samp = new SampledStatistic();
		samp.add(data.getSamples());
		
		double simA = samp.getMean();
		
		double expectedA = ValidationCalculations.ActiveBackupVol2p121();
		
		double simPct = (simA - expectedA)/(1.0 - expectedA) * 100.0;
		
		System.out.println("Active Backup Vol2 p121\t" + expectedA + "\t" + simA + "\t" + simPct + "%");
		
		assertTrue(Math.abs(simPct) < 0.6);
	}
	
	@Test
	void testAzureKubernetesService() throws Throwable{
		SimulationBuilder builder = scenarios.setupAzureKubernetesService();
		RandomProvider rp = new RanluxProvider(1593636073202l);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		long sampleDuration = 525600; //in minutes
		long timeBetweenSamples = (sampleDuration/7) * 6;
		long firstSampleTime = 0;
		AvailabilitySampler data = new AvailabilitySampler("availability", firstSampleTime,sampleDuration, timeBetweenSamples);
		s.subscribeToEvents(data, Collections.singletonList("service"));

		s.start(30000000000l); //5700 years in minutes
		s.join();
		
		SampledStatistic samp = new SampledStatistic();
		samp.add(data.getSamples());
		
		double simA = samp.getMean();
		
		double expectedA = ValidationCalculations.AzureKubernetesServiceScenario();
		
		double simPct = (simA - expectedA)/(1.0 - expectedA) * 100.0;
		
		System.out.println("Azure Kubernetes Service\t" + expectedA + "\t" + simA + "\t" + simPct + "%");
		assertTrue(Math.abs(simPct) < 0.1);
	}
	
	@Test
	void testDualSubsystemModelPR() throws Throwable{
		SimulationBuilder builder = scenarios.setupDualSubsystemModelPR();
		RandomProvider rp = new RanluxProvider(1593636332193l);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		long sampleDuration = 525600; //in minutes
		long timeBetweenSamples = (sampleDuration/7) * 6;
		long firstSampleTime = 0;
		AvailabilitySampler data = new AvailabilitySampler("availability", firstSampleTime,sampleDuration, timeBetweenSamples);
		s.subscribeToEvents(data, Collections.singletonList("system"));

		s.start(9000000000000l); //17 million years in minutes
		s.join();
		
		SampledStatistic samp = new SampledStatistic();
		samp.add(data.getSamples());
		
		double simA = samp.getMean();
		
		double expectedA = ValidationCalculations.DualSubsystemParallelRepair();
		
		double simPct = (simA - expectedA)/(1.0 - expectedA) * 100.0;
		
		System.out.println("Dual Subsystem Model PR\t" + expectedA + "\t" + simA + "\t" + simPct + "%");
		
		assertTrue(Math.abs(simPct) < 0.7);
	}
	
	@Test
	void testDualSubsystemModelSR() throws Throwable{
		SimulationBuilder builder = scenarios.setupDualSubsystemModelSR();
		RandomProvider rp = new RanluxProvider(1593636394932l);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		long sampleDuration = 525600; //in minutes
		long timeBetweenSamples = (sampleDuration/7) * 6;
		long firstSampleTime = 0;
		AvailabilitySampler data = new AvailabilitySampler("availability", firstSampleTime,sampleDuration, timeBetweenSamples);
		s.subscribeToEvents(data, Collections.singletonList("system"));

		s.start(9000000000000l); //17 million years in minutes
		s.join();
		
		SampledStatistic samp = new SampledStatistic();
		samp.add(data.getSamples());
		
		double simA = samp.getMean();
		
		double expectedA = ValidationCalculations.DualSubsystemSequentialRepair();
		
		double simPct = (simA - expectedA)/(1.0 - expectedA) * 100.0;
		
		System.out.println("Dual Subsystem Model SR\t" + expectedA + "\t" + simA + "\t" + simPct + "%");
		assertTrue(Math.abs(simPct) < 1.0);
	}
	
	@Test
	void testDualSubsystemModelConstrainedResources() throws Throwable{
		SimulationBuilder builder = scenarios.setupDualSubsystemsWithConstrainedRepair();
		RandomProvider rp = new RanluxProvider(1593636471953l);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		long sampleDuration = 525600; //in minutes
		long timeBetweenSamples = (sampleDuration/7) * 6;
		long firstSampleTime = 0;
		AvailabilitySampler data = new AvailabilitySampler("availability a", firstSampleTime,sampleDuration, timeBetweenSamples);
		s.subscribeToEvents(data, Collections.singletonList("a.system"));
		AvailabilitySampler datab = new AvailabilitySampler("availability b", firstSampleTime,sampleDuration, timeBetweenSamples);
		s.subscribeToEvents(datab, Collections.singletonList("b.system"));

		s.start(300000000000l); //570,000 years in minutes
		s.join();
		
		SampledStatistic sampA = new SampledStatistic();
		sampA.add(data.getSamples());
		
		SampledStatistic sampB = new SampledStatistic();
		sampB.add(datab.getSamples());
		
		double simFailure = (1.0 - sampA.getMean()) + (1.0 - sampB.getMean());
		
		double expectedFailure = ValidationCalculations.DualSubsystemConstrainedResourcePoolFailure();
		
		double simPct = (simFailure - expectedFailure)/ expectedFailure * 100.0;
		
		System.out.println("Dual Subsystem Model Constrained Resource Pool\t" + expectedFailure + "\t" + simFailure + "\t" + simPct +  "%");
		
		assertTrue(Math.abs(simPct) < 0.1);
	}
	
	@Test
	void testDualSubsystemModelUnconstrainedResources() throws Throwable{
		SimulationBuilder builder = scenarios.setupDualSubsystemsWithUnconstrainedRepair();
		RandomProvider rp = new RanluxProvider(1593636471953l);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		long sampleDuration = 525600; //in minutes
		long timeBetweenSamples = (sampleDuration/7) * 6;
		long firstSampleTime = 0;
		AvailabilitySampler data = new AvailabilitySampler("availability a", firstSampleTime,sampleDuration, timeBetweenSamples);
		s.subscribeToEvents(data, Collections.singletonList("a.system"));
		AvailabilitySampler datab = new AvailabilitySampler("availability b", firstSampleTime,sampleDuration, timeBetweenSamples);
		s.subscribeToEvents(datab, Collections.singletonList("b.system"));

		s.start(1000000000000l); //1.9 million years in minutes
		s.join();
		
		SampledStatistic sampA = new SampledStatistic();
		sampA.add(data.getSamples());
		
		SampledStatistic sampB = new SampledStatistic();
		sampB.add(datab.getSamples());
		
		double expectedA = ValidationCalculations.DualSubsystemUnconstrainedResourcePool();
		
		double simA1 = sampA.getMean();
		
		double simA2 = sampB.getMean();
		
		double simPct1 = (simA1 - expectedA)/(1.0 - expectedA) * 100.0;
		
		double simPct2 = (simA2 - expectedA)/(1.0 - expectedA) * 100.0;
		
		System.out.println("Dual Subsystem Model Unconstrained Resource Pool 1\t" + expectedA + "\t" + simA1 + "\t" + simPct1 + "%");
		System.out.println("Dual Subsystem Model Unconstrained Resource Pool 2\t" + expectedA + "\t" + simA2 + "\t" + simPct2 + "%");
		
		assertTrue(Math.abs(simPct1) < 3);
		assertTrue(Math.abs(simPct2) < 3);
	}
	
	@Test
	void testSingleSpare16NodeClusterSimulation() throws Throwable{
		SimulationBuilder builder = scenarios.setupSingleSpare16NodeCluster();
		RandomProvider rp = new RanluxProvider(1593636471953l);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		long sampleDuration = 525600; //in minutes
		long timeBetweenSamples = (sampleDuration/7) * 6;
		long firstSampleTime = 0;
		AvailabilitySampler data = new AvailabilitySampler("availability", firstSampleTime,sampleDuration, timeBetweenSamples);
		s.subscribeToEvents(data, Collections.singletonList("cluster"));

		s.start(80000000000l); //152,000 years in minutes
		s.join();
		
		SampledStatistic samp = new SampledStatistic();
		samp.add(data.getSamples());
		
		double simA = samp.getMean();
		
		double expectedA = ValidationCalculations.SingleSpare16NodeCluster();
		
		double simPct = (simA - expectedA)/(1.0 - expectedA) * 100.0;
		
		System.out.println("Single Spare 16 Node Cluster\t" + expectedA + "\t" + simA + "\t" + simPct + "%");
		
		assertTrue(Math.abs(simPct) < 1.0);
	}
	
	@Test
	void testSingleSpare4NodeClusterSimulation() throws Throwable{
		SimulationBuilder builder = scenarios.setupSingleSpare4NodeCluster();
		RandomProvider rp = new RanluxProvider(1593636471953l);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		long sampleDuration = 525600; //in minutes
		long timeBetweenSamples = (sampleDuration/7) * 6;
		long firstSampleTime = 0;
		AvailabilitySampler data = new AvailabilitySampler("availability", firstSampleTime,sampleDuration, timeBetweenSamples);
		s.subscribeToEvents(data, Collections.singletonList("cluster"));

		s.start(80000000000l); //152,000 years in minutes
		s.join();
		
		SampledStatistic samp = new SampledStatistic();
		samp.add(data.getSamples());
		
		double simA = samp.getMean();
		
		double expectedA = ValidationCalculations.SingleSpare4NodeCluster();
		
		double simPct = (simA - expectedA)/(1.0 - expectedA) * 100.0;
		
		System.out.println("Single Spare 4 Node Cluster\t" + expectedA + "\t" + simA + "\t" + simPct +  "%");
		
		assertTrue(Math.abs(simPct) < 0.1);
	}
}
