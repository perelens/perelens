# perelens

## Description

Perelens is a discrete event simulation engine (DES) with support for risk analysis via Monte Carlo simulation.
It was originally developed to solve the challenge of creating quantitative risk models for complex information technology systems.
Large organizations have hundreds or thousands of IT systems that are further assembled into systems of systems.
In such an environment, there will always be another dollar to spend on decreasing downtime and security risk, but without a quantitative risk model, it is nearly impossible to know where to spend the limited number of dollars the organization can afford.
In theory, a small army of skilled analysts could build and maintain mathematical models in systems like R or Excel, but this is not practical for most organizations.
The goal of perelens is to automate the mathematical analysis through Monte Carlo simulation in order to make the creation of comprehensive quantitative risk models practical for most organizations.

Perelens makes this possible by providing a novel DES engine implemented in a very fast runtime (Java).
The engine optimally parallelizes large simulations on commodity hardware while remaining deterministic.
This deterministic behavior is necessary to ensure the accuracy of the simulation output.
The same model with the same random seeding should generate the exact same simulation each time it is run, whether the engine is running on a single core or multiple cores.
The ultimate goal being to create a pipeline where models of individual systems are developed as part of the life-cycle of those systems, and then those hundreds of individual models are composed into a comprehensive risk model for the organization.

The general purpose DES engine components are located in the **perelens-engine-api** and **perelens-engine-core** components.
Perelens can be extended to address additional problem domains by extending and using the classes in these two components.

## Installation

Perelens requires Java 17 or higher and is most thoroughly tested on [OpenJDK 17](https://adoptium.net/temurin/releases/?version=17).
The next major milestone for the project will be to develop a front-end that allows simulations to be concisely configured in XML or Script.
In the meantime the best way to consume the project is as a Maven dependency.
Use the **perelens-complete** artifact for Maven projects.

```xml
<dependency>
    <groupId>com.perelens</groupId>
    <artifactId>perelens-complete</artifactId>
    <version>1.1</version>
    <type>pom</type>
</dependency>
```

For legacy classpath usage, it is best to download the [Uber Jar](https://repo1.maven.org/maven2/com/perelens/perelens-uberjar/1.1/)

## Usage

Sample code for multiple scenarios can be found in the **com.perelens.simulation.scenarios** package located in the **perelens-simulation-risk** artifact.
Below is a simple, annotated, example of how to construct and run a simulation to predict the availability of a 4 node compute cluster.
The sample simulation is configured to run with 4 concurrent threads, so on a 4 core system it will go to 100% CPU until the simulation completes.
This is by design, the goal being to maximize the capacity for a given system to run simulations in a manageable amount of wall clock time,
rather than consume minimal resources to promote multi-tasking on the system.

```java
SimulationBuilder builder = new CoreSimulationBuilder();
DistributionProvider dp = new CoreDistributionProvider();

double availability = 0.999d; //each cluster node is 99.9% available
double mtr = 2 * 60; //2 hour mean time to repair in minutes

//Calculate mean time before failure
double mtbf = Relationships.getMeanTimeBetweenFailure(availability, mtr); 
int mtfo = 3; //Mean Time to Fail Over in minutes
int restore = 2 * 60; //System restore time in minutes

//Use exponential distributions for failure and repair arrival times
Distribution failure = dp.exponential(mtbf);
Distribution repair = dp.exponential(mtr);

//Create the cluster nodes
Function node1 = new RandomFailureFunction("node.1",failure,repair);
Function node2 = new RandomFailureFunction("node.2",failure,repair);
Function node3 = new RandomFailureFunction("node.3",failure,repair);
Function node4 = new RandomFailureFunction("node.4",failure,repair);

//Create the cluster setting k=3 and n=4, resulting in a single spare.
//The cluster is available if 3 of the 4 nodes are operational.
FunctionKofN cluster = new FunctionKofN("cluster",3,4);
//Also count restore operations, like restoring an OS image
//and the duration of failover processes as downtime.
cluster.setRestoreTime(restore);
cluster.setMeanTimeToFailOver(mtfo);

//Add all the components to the Simulation
builder.addFunction(node1);
builder.addFunction(node2);
builder.addFunction(node3);
builder.addFunction(node4);
builder.addFunction(cluster);


//Add the individual nodes to the Cluster as dependencies
builder.getFunction("cluster")
	.addDependency(builder.getFunction("node.1"))
	.addDependency(builder.getFunction("node.2"))
	.addDependency(builder.getFunction("node.3"))
	.addDependency(builder.getFunction("node.4"));

//Create a random provider and associate it with the Simulation
RandomProvider rp = new RanluxProvider(1593636471953l/*random seed*/);
builder.setRandomProvider(rp);

//Create a runnable Simulation
Simulation s = builder.createSimulation(4/*concurrent threads*/);

//Capture the simulated availability of the Cluster by taking 1 year samples during the simulation
long sampleDuration = 525600; //1 year in minutes
long timeBetweenSamples = (sampleDuration/7) * 6;
long firstSampleTime = 0;
AvailabilitySampler data = new AvailabilitySampler("availability", firstSampleTime,sampleDuration, timeBetweenSamples);
s.subscribeToEvents(data, Collections.singletonList("cluster"));

s.start(80000000000l); //Start the simulation and stop after simulating 152,000 years in minutes
s.join(); //wait for the simulation to complete

//Extract the sample data from the output of the simulation
SampledStatistic samp = new SampledStatistic();
samp.add(data.getSamples());

//Take the mean availability from all the samples
double simA = samp.getMean();

System.out.println("Single Spare 4 Node Cluster\t" + simA);

//Cleanup the underlying resources and thread pools of the simulation
s.destroy();
```

## Credits

* Mayo Clinic Office of the CTO for sponsoring this work and allowing it to be open-sourced.
* Dr. Bill Highleyman, Paul Holenstein, and Dr. Bruce Holenstein who co-wrote the [Breaking the Avalability Barrier](https://www.availabilitydigest.com/book.htm) book series which inspired the initial investigations into automating reliability analysis and provided mathematical models and examples used to validate perelens.
* Zach Cramer, whose Monte Carlo Simulation supplement to his [Reliability Analysis and Artificial Intelligence](https://www.ewh.ieee.org/r6/san_francisco/pes/past_presentations.htm) presentation to the San Franciso Power & Energy Society provided the necessary foundation and inspiration for the creation of perelens as a general purpose solution for Reliabiltiy Analysis.
* Paul Houle and Sean Luke who created the Java implementation of the [Ranlux random number generator (M. Lüscher)](https://luscher.web.cern.ch/luscher/ranlux/) utilized by perelens.
* Douglas Hubbard and Richard Seirsen who co-wrote [How to Measure Anything in Cybersecurity Risk](https://www.howtomeasureanything.com/cybersecurity/), which inspired the conditional risk extensions to perelens and provided examples used to validate those extensions.
