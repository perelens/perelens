/**
 * 
 */
package com.perelens.simulation.scenarios;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
   
 * Class for keeping the calculations that determine the expected value of the validation scenarios.
 * 
 * @author Steve Branda
 *
 */
public class ValidationCalculations {
	
	/**
	 * @param args
	 */
	public static void main (String[] args) {
		System.out.println("AzureKubernetesServiceScenario:" + AzureKubernetesServiceScenario());
		System.out.println("ActiveActiveVol2p122:" + ActiveActiveVol2p122());
		System.out.println("ActiveBackupVol2p121:" + ActiveBackupVol2p121());
		System.out.println("DualSubsystemParallelRepair:" + DualSubsystemParallelRepair());
		System.out.println("DualSubsystemSequentialRepair:" + DualSubsystemSequentialRepair());
		System.out.println("DualSubsystemConstrainedResourcePoolFailure:" + DualSubsystemConstrainedResourcePoolFailure());
		System.out.println("SingleSpare16NodeCluster:" + SingleSpare16NodeCluster());
		System.out.println("SingleSpare4NodeCluster:" + SingleSpare4NodeCluster());
		System.out.println("OneThousandSingleSpare16NodeCluster:" + OneThousandSingleSpare16NodeCluster());
		System.out.println("Complicated Service:" + ComplicatedService());
	}
	
	public static double AzureKubernetesServiceScenario() {
	
		double r = 1.0;//hours
		double R = 1.0;//hours
		double n = 3; //Total nodes
		double m = 3; //Active Nodes
		double MTFO = 1.0/60.0; //1 minute in hours
		double c = 0;
		double a = 0.999;
		double p = 0.006;
		
		double F = 
				(r/2.0 + R)/(r/2.0) * n*(n-1)/2.0 * Math.pow((1.0-a), 2) +
				R/r*p*n*(1.0-a) +
				R/r*c*n*(1.0-a) +
				R/r*p*n*c +
				MTFO/r*m*(1.0-a);
		
		double containerAvailability = 1.0 - F;
		
		double dataCenterAvailability = 0.9999;
		double loadBalancerAvailability = 0.9999;
		
		return containerAvailability * dataCenterAvailability * loadBalancerAvailability;
		
	}
	
	public static double ActiveActiveVol2p122() {
		
		//See availability analysis framework for variable and formulas
		double r = 2.0;//hours
		double R = 2.0;//hours
		double n = 2; //Total nodes
		double m = 2; //Active nodes
		double MTFO = 1.0/60.0/60.0; //1 second in hours
		double c = 0.0;
		double a = 0.9999;
		double p = 0.0;
		
		//Equation 17 from Availability Analysis Framework
		double F = 
				(r/2.0 + R)/(r/2.0) * n*(n-1)/2.0 * Math.pow((1.0-a), 2) +
				R/r*p*n*(1.0-a) +
				R/r*c*n*(1.0-a) +
				R/r*p*n*c +
				MTFO/r*m*(1.0-a); //Divide term by n since only 1/n users notice a failover
		
		double systemDownToMultipleNodeFailure = (r/2.0 + R)/(r/2.0) * n*(n-1)/2.0 * Math.pow((1.0-a), 2);
		double systemDownDuringFailover = MTFO/r*m*(1.0-a)/n;
		@SuppressWarnings("unused")
		double totalDown = systemDownToMultipleNodeFailure + systemDownDuringFailover;
		
		return 1.0-F;
	}
	
	public static double ActiveBackupVol2p121() {
		double r = 2.0;//hours
		double R = 2.0;//hours
		double n = 2; //Total nodes
		double m = 1; //Active nodes
		double MTFO = 2.0;//hours
		double c = 0.0;
		double a = 0.9999;
		double p = 0.0;
		
		//Equation 17 from Availability Analysis Framework
		double F = 
				(r/2.0 + R)/(r/2.0) * n*(n-1)/2.0 * Math.pow((1.0-a), 2) +
				R/r*p*n*(1.0-a) +
				R/r*c*n*(1.0-a) +
				R/r*p*n*c +
				MTFO/r*m*(1.0-a);
		
		double systemDownToMultipleNodeFailure = (r/2.0 + R)/(r/2.0) * n*(n-1)/2.0 * Math.pow((1.0-a), 2);
		double systemDownDuringFailover = MTFO/r*m*(1.0-a);
		@SuppressWarnings("unused")
		double totalDown = systemDownToMultipleNodeFailure + systemDownDuringFailover;
		
		return 1.0-F;
	}
	
	public static double DualSubsystemParallelRepair() {
		
		double a = 0.999;
		double zeroFailed = a * a;
		double oneFailed = 2 * a * (1.0-a);
		
		
		return zeroFailed + oneFailed;
	}
	
	public static double DualSubsystemSequentialRepair() {
		double a = 0.999;
		
		//State P2 solution from "Dual Subsystem with Sequential Repair" Markov Chain solution
		double F = (2.0 * (1.0 - 2.0*a + a*a))/(2.0 - 2.0 * a +a*a);
		
		return 1.0 - F;
	}
	
	/**
	 * This method returns the cumulative probability of failure across the two systems that share a constrained
	 * resource pool in this scenario.
	 * This cumulative failure may be split unevenly between the two systems in any given simulation, but the total
	 * value should be observed between the two systems.
	 * 
	 * @return
	 */
	public static double DualSubsystemConstrainedResourcePoolFailure() {
		double a = 0.999;
		
		//F solution from "Dual Subsystem with Constrained Resource Pool" Markov Chain Solution
		//F = 2 * p5 + p4 + p3
		//In state p5, both systems are down, so we need to double the probability of that state.
		//In state p4 and p3, only one system is down.
		double F = 
				(4.0*(-12.0 + 48.0 * a - 78.0*a*a + 65.0 * Math.pow(a, 3) - 28.0 * Math.pow(a, 4) + 5*Math.pow(a, 5))) /
				(-24.0 + 84.0*a - 132.0*a*a + 112.0*Math.pow(a, 3) - 50.0 * Math.pow(a, 4) + 9.0 * Math.pow(a, 5));
		
		return F;	  
	}
	
	/**
	 * The purpose of this scenario is to ensure that an unconstrained resource pool does not affect the accuracy
	 * of the simulation.
	 * If it did this would indicate some sort of bug in the simulation code.
	 * 
	 * @return
	 */
	public static double DualSubsystemUnconstrainedResourcePool() {
		return DualSubsystemParallelRepair();
	}
	
	
	/**
	 * Equation 17 is not used for this case because of the number of nodes.
	 * We know from the 1000 single spare 16 node cluster scenario that each additional node
	 * results in a greater percentage of underestimation from Equation 17.
	 * With 16 nodes the underestimation is minimal, but not negligible.
	 * 
	 * @return
	 */
	public static double SingleSpare16NodeCluster(){
		double r = 4.0;//hours
		double R = 2.0;//hours
		double n = 16; //Total nodes
		double a = 0.999;
		double p = 0.01;
		
		double allNodesWorking = Math.pow(a, n);
		double oneNodeFailed = n * Math.pow(a, n-1) * (1.0-a);
		
		double twoNodeFailChance = (1.0 - allNodesWorking - oneNodeFailed);
		
		double failoverFaultChance =  p * n * (1-a);
		
		double F = (r/2.0 + R)/(r/2.0) * twoNodeFailChance + (R/r) * failoverFaultChance;
		
		return 1.0 - F;
	}
	
	public static double OneThousandSingleSpare16NodeCluster() {
		double a = 0.999;
		double n = 16;
		double clusterCount = 1000;
		
		double cluster_a = Math.pow(a, n) + Math.pow(a, n-1)*(1.0-a) * n;
		
		double allClustersWorking = Math.pow(cluster_a, clusterCount);
		
		double oneClusterFailed = Math.pow(cluster_a, clusterCount -1) * (1.0-cluster_a) * clusterCount;
		
		return allClustersWorking + oneClusterFailed;
	}
	
	public static double SingleSpare4NodeCluster() {
		double r = 2.0;//hours
		double R = 2.0;//hours
		double n = 4; //Total nodes
		double a = 0.999;
		double MTFO = 0.05;//hours
		
		double allNodesWorking = Math.pow(a, n);
		double oneNodeFailed = n * Math.pow(a, n-1) * (1.0-a);
		
		double twoNodeFailChance = (1.0 - allNodesWorking - oneNodeFailed);
		
		double downDuringFO =  MTFO/r*n*(1.0-a);
		
		double F = (r/2.0 + R)/(r/2.0) * twoNodeFailChance + downDuringFO;
		
		return 1.0 - F;
	}
	
	public static double ComplicatedService() {
		
		double allAs =  AzureKubernetesServiceScenario() * SingleSpare4NodeCluster() * SingleSpare16NodeCluster() * ActiveActiveVol2p122() * ActiveBackupVol2p121();
		double constrainedRepair = 1 - DualSubsystemConstrainedResourcePoolFailure() + Math.pow(DualSubsystemConstrainedResourcePoolFailure()/2.0, 2);
		
		return allAs * constrainedRepair;
	}

}
