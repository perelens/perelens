package com.perelens.simulation.scenarios;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import com.perelens.simulation.api.Distribution;
import com.perelens.simulation.api.DistributionProvider;
import com.perelens.simulation.api.Function;
import com.perelens.simulation.api.RandomProvider;
import com.perelens.simulation.api.ResourcePool;
import com.perelens.simulation.api.Simulation;
import com.perelens.simulation.api.SimulationBuilder;
import com.perelens.simulation.core.CoreDistributionProvider;
import com.perelens.simulation.core.CoreResourcePool;
import com.perelens.simulation.core.CoreSimulationBuilder;
import com.perelens.simulation.failure.ActivePassiveKofN;
import com.perelens.simulation.failure.FunctionKofN;
import com.perelens.simulation.failure.RandomFailureFunction;
import com.perelens.simulation.failure.consumers.AvailabilityConsumer;
import com.perelens.simulation.failure.consumers.AvailabilitySampler;
import com.perelens.simulation.failure.consumers.OutageConsumer;
import com.perelens.simulation.random.RanluxProvider;
import com.perelens.simulation.statistics.SampledStatistic;
import com.perelens.simulation.utils.Relationships;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
   
 * These are rough validation tests.
 * They make sure that all the features of the simulation work and that the answers are in the ballpark, while finishing in
 * a convenient amount of time.
 * See SimulationAccuracyTest for precise validation of simulation results.
 * 
 * @author Steve Branda
 *
 */
class ValidationTest {

	protected SimulationBuilder setupSingleSpare4NodeCluster() {
		SimulationBuilder toReturn = new CoreSimulationBuilder();
		DistributionProvider dp = new CoreDistributionProvider();
		
		double availability = 0.999d;
		double mtr = 2 * 60; //mtr in minutes
		double mtbf = Relationships.getMeanTimeBetweenFailure(availability, mtr);
		int mtfo = 3; //MTFO in minutes
		int restore = 2 * 60; //System restore time in minutes
		
		Distribution failure = dp.exponential(mtbf);
		Distribution repair = dp.exponential(mtr);
		
		Function node1 = new RandomFailureFunction("node.1",failure,repair);
		Function node2 = new RandomFailureFunction("node.2",failure,repair);
		Function node3 = new RandomFailureFunction("node.3",failure,repair);
		Function node4 = new RandomFailureFunction("node.4",failure,repair);
		
		FunctionKofN cluster = new FunctionKofN("cluster",3,4);
		cluster.setRestoreTime(restore);
		cluster.setMeanTimeToFailOver(mtfo);
		
		toReturn.addFunction(node1);
		toReturn.addFunction(node2);
		toReturn.addFunction(node3);
		toReturn.addFunction(node4);
		toReturn.addFunction(cluster);
		
		toReturn.getFunction("cluster")
			.addDependency(toReturn.getFunction("node.1"))
			.addDependency(toReturn.getFunction("node.2"))
			.addDependency(toReturn.getFunction("node.3"))
			.addDependency(toReturn.getFunction("node.4"));
		
		return toReturn;
	}
	
	protected SimulationBuilder setupDualSubsystemModelPR() {
		SimulationBuilder toReturn = new CoreSimulationBuilder();
		DistributionProvider dp = new CoreDistributionProvider();
		
		double availability = 0.999d;
		double mtr = 2 * 60; //mtr in minutes
		double mtbf = Relationships.getMeanTimeBetweenFailure(availability, mtr);
		
		Distribution failure = dp.exponential(mtbf);
		Distribution repair = dp.exponential(mtr);
		
		Function node1 = new RandomFailureFunction("subsystem.1",failure,repair);
		Function node2 = new RandomFailureFunction("subsystem.2",failure,repair);
		
		Function cluster = new FunctionKofN("system",1,2);
		
		toReturn.addFunction(node1);
		toReturn.addFunction(node2);
		toReturn.addFunction(cluster);
		
		toReturn.getFunction("system")
			.addDependency(toReturn.getFunction("subsystem.1"))
			.addDependency(toReturn.getFunction("subsystem.2"));
		
		return toReturn;
	}
	
	protected SimulationBuilder setupDualSubsystemModelSR() {
		SimulationBuilder toReturn = new CoreSimulationBuilder();
		DistributionProvider dp = new CoreDistributionProvider();
		
		double availability = 0.999d;
		double mtr = 2 * 60; //mtr in minutes
		double mtbf = Relationships.getMeanTimeBetweenFailure(availability, mtr);
		
		Distribution failure = dp.exponential(mtbf);
		Distribution repair = dp.exponential(mtr);
		
		Function node1 = new RandomFailureFunction("subsystem.1",failure,repair);
		Function node2 = new RandomFailureFunction("subsystem.2",failure,repair);
		ResourcePool pool = new CoreResourcePool("repair pool", 1); 
		
		Function cluster = new FunctionKofN("system",1,2);
		
		toReturn.addResourcePool(pool);
		toReturn.addFunction(node1);
		toReturn.addFunction(node2);
		toReturn.addFunction(cluster);
		
		toReturn.getFunction("system")
			.addDependency(toReturn.getFunction("subsystem.1"))
			.addDependency(toReturn.getFunction("subsystem.2"));
		
		toReturn.getFunction("subsystem.1").addResourcePool("repair pool");
		toReturn.getFunction("subsystem.2").addResourcePool("repair pool");
		
		return toReturn;
	}
	
	protected SimulationBuilder setupActiveBackupVol2p121() {
		SimulationBuilder toReturn = new CoreSimulationBuilder();
		DistributionProvider dp = new CoreDistributionProvider();
		
		double availability = 0.9999d;
		double mtr = 2 * 60; //mtr in minutes
		double mtbf = Relationships.getMeanTimeBetweenFailure(availability, mtr);
		
		Distribution failure = dp.exponential(mtbf);
		Distribution repair = dp.exponential(mtr);
		
		Function node1 = new RandomFailureFunction("subsystem.1",failure,repair);
		Function node2 = new RandomFailureFunction("subsystem.2",failure,repair);
		
		int restoreTime = 2 * 60;   //System restore in minutes
		int mtfo = 2 * 60; 			//Mean time to fail over in minutes
		ActivePassiveKofN cluster = new ActivePassiveKofN("system",1,2);
		cluster.setRestoreTime(restoreTime);
		cluster.setMeanTimeToFailOver(mtfo);
		cluster.setActiveNodes(1);
		
		toReturn.addFunction(node1);
		toReturn.addFunction(node2);
		toReturn.addFunction(cluster);
		
		toReturn.getFunction("system")
			.addDependency(toReturn.getFunction("subsystem.1"))
			.addDependency(toReturn.getFunction("subsystem.2"));
		
		return toReturn;
	}
	
	protected SimulationBuilder setupActiveActiveVol2p122() {
		SimulationBuilder toReturn = new CoreSimulationBuilder();
		DistributionProvider dp = new CoreDistributionProvider();
		
		double availability = 0.9999d;
		double mtr = 2 * 60 * 60; //mtr in seconds
		double mtbf = Relationships.getMeanTimeBetweenFailure(availability, mtr);
		
		Distribution failure = dp.exponential(mtbf);
		Distribution repair = dp.exponential(mtr);
		
		Function node1 = new RandomFailureFunction("subsystem.1",failure,repair);
		Function node2 = new RandomFailureFunction("subsystem.2",failure,repair);
		
		int restoreTime = 2 * 60 * 60;   //System restore in seconds
		int mtfo = 1; 			//Mean time to fail over in seconds
		FunctionKofN cluster = new FunctionKofN("system",1,2);
		cluster.setRestoreTime(restoreTime);
		cluster.setMeanTimeToFailOver(mtfo);
		
		toReturn.addFunction(node1);
		toReturn.addFunction(node2);
		toReturn.addFunction(cluster);
		
		toReturn.getFunction("system")
			.addDependency(toReturn.getFunction("subsystem.1"))
			.addDependency(toReturn.getFunction("subsystem.2"));
		
		return toReturn;
	}
	
	protected SimulationBuilder setupSingleSpare16NodeCluster() {
		SimulationBuilder toReturn = new CoreSimulationBuilder();
		DistributionProvider dp = new CoreDistributionProvider();
		
		double availability = 0.999d;
		double mtr = 4 * 60; //mtr in minutes
		double mtbf = Relationships.getMeanTimeBetweenFailure(availability, mtr);
		int restore = 2 * 60; //System restore time in minutes
		
		Distribution failure = dp.exponential(mtbf);
		Distribution repair = dp.exponential(mtr);
		
		FunctionKofN cluster = new FunctionKofN("cluster",15,16);
		cluster.setRestoreTime(restore);
		cluster.setFailOverFaultPercentage(0.01);
		toReturn.addFunction(cluster);
		
		for (int i = 1; i <= 16; i++) {
			toReturn.addFunction(new RandomFailureFunction("node."+i,failure,repair));
			toReturn.getFunction("cluster").addDependency(toReturn.getFunction("node."+i));
		}
		
		return toReturn;
	}
	
	protected SimulationBuilder setupAzureKubernetesService() {
		SimulationBuilder toReturn = new CoreSimulationBuilder();
		DistributionProvider dp = new CoreDistributionProvider();
		
		double avail = 0.999d;
		double mtr = 1 * 60;	//mtr in minutes
		double mtbf = Relationships.getMeanTimeBetweenFailure(avail, mtr);
		int restore = 1 * 60;	//System restore time in minutes
		double foFault = 0.006;	//Failover fault percentage
		
		Distribution failure = dp.exponential(mtbf);
		Distribution repair = dp.exponential(mtr);
		
		FunctionKofN containers = new FunctionKofN("containers",2,3);
		containers.setFailOverFaultPercentage(foFault);
		containers.setMeanTimeToFailOver(1);
		containers.setRestoreTime(restore);
		toReturn.addFunction(containers);
		
		for (int i = 1; i <=3; i++) {
			String id = "failure domain." + i;
			toReturn.addFunction(new RandomFailureFunction(id,failure,repair));
			toReturn.getFunction("containers").addDependency(toReturn.getFunction(id));
		}
		
		double dc_avail = 0.9999d;
		double dc_mtr = 8 * 60; 		//mtr in minutes
		double dc_mtbf = Relationships.getMeanTimeBetweenFailure(dc_avail,dc_mtr);
		Distribution dc_failure = dp.exponential(dc_mtbf);
		Distribution dc_repair = dp.exponential(dc_mtr);
		
		Function datacenter = new RandomFailureFunction("data center", dc_failure, dc_repair);
		toReturn.addFunction(datacenter);
		
		double lb_avail = 0.9999d;
		double lb_mtr = 1 * 60;
		double lb_mtbf = Relationships.getMeanTimeBetweenFailure(lb_avail, lb_mtr);
		Distribution lb_failure = dp.exponential(lb_mtbf);
		Distribution lb_repair = dp.exponential(lb_mtr);
		
		Function loadBalance = new RandomFailureFunction("load balancer", lb_failure, lb_repair);
		toReturn.addFunction(loadBalance);
		
		FunctionKofN service = new FunctionKofN("service",3,3);
		toReturn.addFunction(service);
		toReturn.getFunction("service")
			.addDependency(toReturn.getFunction("load balancer"))
			.addDependency(toReturn.getFunction("data center"))
			.addDependency(toReturn.getFunction("containers"));
		
		return toReturn;
	}
	
	protected SimulationBuilder setupDualSubsystemsWithUnconstrainedRepair() {
		SimulationBuilder toReturn = new CoreSimulationBuilder();
		DistributionProvider dp = new CoreDistributionProvider();
		
		double availability = 0.999d;
		double mtr = 2 * 60; //mtr in minutes
		double mtbf = Relationships.getMeanTimeBetweenFailure(availability, mtr);
		
		Distribution failure = dp.exponential(mtbf);
		Distribution repair = dp.exponential(mtr);
		
		Function node1 = new RandomFailureFunction("a.system.subsystem.1",failure,repair);
		Function node2 = new RandomFailureFunction("a.system.subsystem.2",failure,repair);
		
		Function cluster = new FunctionKofN("a.system",1,2);
		
		toReturn.addFunction(node1);
		toReturn.addFunction(node2);
		toReturn.addFunction(cluster);
		
		toReturn.getFunction("a.system")
			.addDependency(toReturn.getFunction("a.system.subsystem.1"))
			.addDependency(toReturn.getFunction("a.system.subsystem.2"));
		
		node1 = new RandomFailureFunction("b.system.subsystem.1",failure,repair);
		node2 = new RandomFailureFunction("b.system.subsystem.2",failure,repair);
		
		cluster = new FunctionKofN("b.system",1,2);
		
		toReturn.addFunction(node1);
		toReturn.addFunction(node2);
		toReturn.addFunction(cluster);
		
		toReturn.getFunction("b.system")
			.addDependency(toReturn.getFunction("b.system.subsystem.1"))
			.addDependency(toReturn.getFunction("b.system.subsystem.2"));
		
		ResourcePool pool = new CoreResourcePool("repair pool",4);
		toReturn.addResourcePool(pool);
		
		toReturn.getFunction("a.system.subsystem.1").addResourcePool(pool.getId());
		toReturn.getFunction("a.system.subsystem.2").addResourcePool(pool.getId());
		toReturn.getFunction("b.system.subsystem.1").addResourcePool(pool.getId());
		toReturn.getFunction("b.system.subsystem.2").addResourcePool(pool.getId());
		
		return toReturn;
	}
	
	protected SimulationBuilder setupDualSubsystemsWithConstrainedRepair() {
		SimulationBuilder toReturn = new CoreSimulationBuilder();
		DistributionProvider dp = new CoreDistributionProvider();
		
		double availability = 0.999d;
		double mtr = 2 * 60; //mtr in minutes
		double mtbf = Relationships.getMeanTimeBetweenFailure(availability, mtr);
		
		Distribution failure = dp.exponential(mtbf);
		Distribution repair = dp.exponential(mtr);
		
		ResourcePool pool = new CoreResourcePool("repair pool",1);
		toReturn.addResourcePool(pool);
		
		Function node1 = new RandomFailureFunction("a.system.subsystem.1",failure,repair);
		Function node2 = new RandomFailureFunction("a.system.subsystem.2",failure,repair);
		
		Function cluster = new FunctionKofN("a.system",1,2);
		
		toReturn.addFunction(node1);
		toReturn.addFunction(node2);
		toReturn.addFunction(cluster);
		
		toReturn.getFunction("a.system")
			.addDependency(toReturn.getFunction("a.system.subsystem.1"))
			.addDependency(toReturn.getFunction("a.system.subsystem.2"));
		
		toReturn.getFunction("a.system.subsystem.1").addResourcePool(pool.getId());
		toReturn.getFunction("a.system.subsystem.2").addResourcePool(pool.getId());
		
		node1 = new RandomFailureFunction("b.system.subsystem.1",failure,repair);
		node2 = new RandomFailureFunction("b.system.subsystem.2",failure,repair);
		
		cluster = new FunctionKofN("b.system",1,2);
		
		toReturn.addFunction(node1);
		toReturn.addFunction(node2);
		toReturn.addFunction(cluster);
		
		toReturn.getFunction("b.system")
			.addDependency(toReturn.getFunction("b.system.subsystem.1"))
			.addDependency(toReturn.getFunction("b.system.subsystem.2"));
		
		
		toReturn.getFunction("b.system.subsystem.1").addResourcePool(pool.getId());
		toReturn.getFunction("b.system.subsystem.2").addResourcePool(pool.getId());
		
		return toReturn;
	}
	
	protected SimulationBuilder setup1000SingleSpare16NodeClusters() {
		
		int numClusters = 1000;
		
		SimulationBuilder toReturn = new CoreSimulationBuilder();
		DistributionProvider dp = new CoreDistributionProvider();
		
		double availability = 0.999d;
		double mtr = 4 * 60; //mtr in minutes
		double mtbf = Relationships.getMeanTimeBetweenFailure(availability, mtr);
		
		Distribution failure = dp.exponential(mtbf);
		Distribution repair = dp.exponential(mtr);
		
		for (int i = 1; i <= numClusters; i++) {
			FunctionKofN cluster = new FunctionKofN("cluster." + i,15,16);
			toReturn.addFunction(cluster);

			for (int j = 1; j <= 16; j++) {
				toReturn.addFunction(new RandomFailureFunction(cluster.getId()+ ".node." + j,failure,repair));
				toReturn.getFunction("cluster."+i).addDependency(toReturn.getFunction(cluster.getId()+ ".node." + j));
			}
		}
		
		FunctionKofN cluster = new FunctionKofN("main cluster", numClusters-1,numClusters);
		toReturn.addFunction(cluster);
		for (int i = 1; i <= numClusters; i++) {
			toReturn.getFunction("main cluster").addDependency(toReturn.getFunction("cluster."+i));
		}
		
		return toReturn;
		
	}
	
	@Test
	void testSingleSpare4NodeCluster() throws Throwable {
		
		SimulationBuilder builder = setupSingleSpare4NodeCluster();
		RandomProvider rp = new RanluxProvider(8L);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		AvailabilityConsumer avail = new AvailabilityConsumer("availability");
		s.subscribeToEvents(avail, Collections.singletonList("cluster"));
		
		s.start(3_000_000_000l); //5700 years in minutes
		s.join();
		assertEquals(0.999882, avail.getAvailability(), 0.00005);
		System.out.println("Single Spare 4 Node Cluster: " + 0.999882 + ", " + avail.getAvailability());
		
		s.destroy();
	}
	
	/**
	 * Parallel Repair
	 * 
	 * @throws Throwable
	 */
	@Test
	void testDualSubsystemModelPR() throws Throwable {
		
		SimulationBuilder builder = setupDualSubsystemModelPR();
		RandomProvider rp = new RanluxProvider(1571435486293L);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		AvailabilityConsumer avail = new AvailabilityConsumer("availability");
		s.subscribeToEvents(avail, Collections.singletonList("system"));
		
		s.start(3_000_000_000l); //5700 years in minutes
		s.join();
		assertEquals(0.999999,avail.getAvailability(), 0.0000005);
		System.out.println("Dual Subsystem Model PR: " + 0.999999 + ", " + avail.getAvailability());
		
		s.destroy();
	}
	
	/**
	 * Sequential Repair
	 * 
	 * @throws Throwable
	 */
	@Test
	void testDualSubsystemModelSR() throws Throwable {
		
		SimulationBuilder builder = setupDualSubsystemModelSR();
		RandomProvider rp = new RanluxProvider(1571435486293L);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		AvailabilityConsumer avail = new AvailabilityConsumer("availability");
		s.subscribeToEvents(avail, Collections.singletonList("system"));
		
		s.start(3_000_000_000l); //5700 years in minutes
		s.join();
		assertEquals(0.999998,avail.getAvailability(), 0.000005);
		System.out.println("Dual Subsystem Model SR: " + 0.999998 + ", " + avail.getAvailability());
		
		s.destroy();
	}
	
	@Test
	void testActiveBackupVol2p121() throws Throwable {
		
		SimulationBuilder builder = setupActiveBackupVol2p121();
		RandomProvider rp = new RanluxProvider(1572454958133L);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		AvailabilityConsumer avail = new AvailabilityConsumer("availability");
		s.subscribeToEvents(avail, Collections.singletonList("system"));
		
		s.start(3_000_000_000l); //5700 years in minutes
		s.join();
		assertEquals(0.9999,avail.getAvailability(), 0.00005);
		
		System.out.println("Active Backup Vol2 p121: " + 0.9999 + ", " + avail.getAvailability());
		
		s.destroy();
	}
	
	@Test
	void testActiveActiveVol2p122() throws Throwable {
		
		SimulationBuilder builder = setupActiveActiveVol2p122();
		RandomProvider rp = new RanluxProvider(1572454958139L);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		AvailabilityConsumer avail = new AvailabilityConsumer("availability");
		s.subscribeToEvents(avail, Collections.singletonList("system"));
		
		s.start(3_000_000_000l * 60); //5700 years in seconds
		s.join();
		assertEquals(0.99999995,avail.getAvailability(), 0.00000005);
		
		System.out.println("Active Active Vol2 p122: " + 0.99999995 + ", " + avail.getAvailability());
		
		s.destroy();
	}
	
	@Test
	void testSingleSpare16NodeCluster() throws Throwable {
		
		SimulationBuilder builder = setupSingleSpare16NodeCluster();
		RandomProvider rp = new RanluxProvider(1572558021318L);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		AvailabilityConsumer avail = new AvailabilityConsumer("availability");
		s.subscribeToEvents(avail, Collections.singletonList("cluster"));
		
		s.start(3_000_000_000l); //5700 years in minutes
		s.join();
		assertEquals(0.99968,avail.getAvailability(), 0.0005);
		
		System.out.println("Single Spare 16 Node Cluster: " + 0.99968 + ", " + avail.getAvailability());
		
		s.destroy();
	}
	
	@Test
	void testAzureKubernetesService() throws Throwable {
		
		SimulationBuilder builder = setupAzureKubernetesService();
		RandomProvider rp = new RanluxProvider(1572624226970L);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		AvailabilityConsumer avail = new AvailabilityConsumer("availability");
		AvailabilitySampler samp = new AvailabilitySampler("avsamp",0,43800,6500);
		OutageConsumer outage = new OutageConsumer("outages",2);
		s.subscribeToEvents(avail, Collections.singletonList("service"));
		s.subscribeToEvents(outage, Collections.singletonList("service"));
		s.subscribeToEvents(samp,Collections.singletonList("service"));
		
		s.start(3_000_000_000l); //5700 years in minutes
		s.join();
		
		System.out.println("Azure Kubernetes Service - BB: " + 0.99973 + ", " + avail.getAvailability());
		assertEquals(0.99973,avail.getAvailability(), 0.0005);
		
		SampledStatistic av = new SampledStatistic();
		av.add(samp.getSamples());
		
		System.out.println(av.getN());
		System.out.println(av.getMean());
		System.out.println(av.getHigh());
		System.out.println(av.getLow());
		System.out.println(av.getConfidenceInterval(0.83));
		
		s.destroy();
	}
	
	@Test
	void testDualSubsystemsWithUnconstrainedRepair() throws Throwable {
		
		SimulationBuilder builder = setupDualSubsystemsWithUnconstrainedRepair();
		RandomProvider rp = new RanluxProvider(1572640114145L);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		AvailabilityConsumer avail_A = new AvailabilityConsumer("availability a");
		s.subscribeToEvents(avail_A, Collections.singletonList("a.system"));
		
		AvailabilityConsumer avail_B = new AvailabilityConsumer("availability b");
		s.subscribeToEvents(avail_B, Collections.singletonList("b.system"));
		
		s.start(3_000_000_000l); //5700 years in minutes
		s.join();
		
		assertEquals(0.999999,avail_A.getAvailability(), 0.0000005);
		assertEquals(0.999999,avail_B.getAvailability(), 0.0000005);
		
		System.out.println("2x Dual Subsystem with Unconstrained Resource Pool A" + 0.999999 + ", " + avail_A.getAvailability());
		System.out.println("2x Dual Subsystem with Unconstrained Resource Pool B" + 0.999999 + ", " + avail_B.getAvailability());
		
		s.destroy();
	}
	
	@Test
	void testDualSubsystemsWithConstrainedRepair() throws Throwable {
		
		SimulationBuilder builder = setupDualSubsystemsWithConstrainedRepair();
		RandomProvider rp = new RanluxProvider(2L);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		AvailabilityConsumer avail_1a = new AvailabilityConsumer("a1a");
		AvailabilityConsumer avail_2a = new AvailabilityConsumer("a2a");
		
		AvailabilityConsumer avail_1b = new AvailabilityConsumer("a1b");
		AvailabilityConsumer avail_2b = new AvailabilityConsumer("a2b");
		
		AvailabilityConsumer avail_A = new AvailabilityConsumer("availability a");
		s.subscribeToEvents(avail_A, Collections.singletonList("a.system"));
		s.subscribeToEvents(avail_1a, Collections.singletonList("a.system.subsystem.1"));
		s.subscribeToEvents(avail_2a, Collections.singletonList("a.system.subsystem.2"));
		
		AvailabilityConsumer avail_B = new AvailabilityConsumer("availability b");
		s.subscribeToEvents(avail_B, Collections.singletonList("b.system"));
		s.subscribeToEvents(avail_1b, Collections.singletonList("b.system.subsystem.1"));
		s.subscribeToEvents(avail_2b, Collections.singletonList("b.system.subsystem.2"));
		
		s.start(60_000_000_000l); //114000 years in minutes
		s.join();
		
		assertTrue(avail_A.getAvailability() < 0.9999985);
		assertTrue(avail_B.getAvailability() < 0.9999985);
		
		assertTrue(avail_A.getAvailability() > 0.999996);
		assertTrue(avail_B.getAvailability() > 0.999996);
		
		double totalFailure = (1 - avail_A.getAvailability()) + (1 - avail_B.getAvailability());
		
		assertEquals(0.0000040015, totalFailure, 0.0000005);
		
		s.destroy();
	}
	
	@Test
	void test1000SingleSpare16NodeClusters() throws Throwable {
		
		int numClusters = 1000;
		
		SimulationBuilder builder = setup1000SingleSpare16NodeClusters();
		RandomProvider rp = new RanluxProvider(1572892611390L);
		builder.setRandomProvider(rp);
		
		Simulation s = builder.createSimulation(4);
		
		AvailabilityConsumer avail = new AvailabilityConsumer("availability main");
		OutageConsumer outage = new OutageConsumer("outages",10);
		s.subscribeToEvents(avail, Collections.singletonList("main cluster"));
		s.subscribeToEvents(outage, Collections.singletonList("main cluster"));
		
		AvailabilityConsumer[] a_clusters = new AvailabilityConsumer[numClusters];
		for (int i = 1; i <= numClusters; i++) {
			a_clusters[i-1] = new AvailabilityConsumer("availability " + i);
			s.subscribeToEvents(a_clusters[i-1], Collections.singletonList("cluster." + i));
		}
		
		
		s.start(3_000_000_000l); //5700 years in minutes
		s.join();
		
		assertEquals(0.99347,avail.getAvailability(), 0.0005);
		
		System.out.println("1000 Single Spare 16 Node Clusters: " + 0.99347 + ", " + avail.getAvailability());
		
		s.destroy();
	}
}
