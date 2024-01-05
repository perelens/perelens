/**
 * 
 */
package com.perelens.simulation.core;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.perelens.engine.api.ConfigKey;
import com.perelens.engine.api.EventGenerator;
import com.perelens.engine.core.CoreEngine;
import com.perelens.simulation.api.BasicInfo;
import com.perelens.simulation.api.Function;
import com.perelens.simulation.api.FunctionInfo;
import com.perelens.simulation.api.FunctionReference;
import com.perelens.simulation.api.RandomGenerator;
import com.perelens.simulation.api.RandomProvider;
import com.perelens.simulation.api.ResourcePool;
import com.perelens.simulation.api.Simulation;
import com.perelens.simulation.api.SimulationBuilder;
import com.perelens.simulation.api.TimeTranslator;

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
public class CoreSimulationBuilder implements SimulationBuilder {

	private TreeMap<String,SimRecord> simObjects = new TreeMap<>();
	private RandomProvider rProvider;
	private boolean destroyed = false;
	private TimeTranslator tTranslator;
	
	private static class SimRecord{
		
		EventGenerator simObject;
		TreeSet<String> outgoingDeps;
		TreeSet<String> incomingDeps;
		
		SimRecord(EventGenerator o){
			if (o==null) throw new IllegalStateException(SimMsgs.badState());
			simObject = o;
		}
		
		
		EventGenerator getSimObject() {
			return simObject;
		}
		
		TreeSet<String> getIncomingDeps(){
			if (incomingDeps == null) {
				incomingDeps = new TreeSet<String>();
			}
			return incomingDeps;
		}
		
		TreeSet<String> getOutgoingDeps(){
			if (outgoingDeps == null) {
				outgoingDeps = new TreeSet<String>();
			}
			return outgoingDeps;
		}
	}
	
	private class FunctionReferenceImpl implements FunctionReference{

		SimRecord rec;
		
		FunctionReferenceImpl(SimRecord r){
			if (r == null) throw new IllegalArgumentException(SimMsgs.argNotNull());
			if (!(r.getSimObject() instanceof Function)) {
				throw new IllegalArgumentException(SimMsgs.wrongType(r.getSimObject().getId(), Function.class,r.getSimObject().getClass()));
			}
			rec = r;
		}
		
		@Override
		public FunctionReference addDependency(FunctionReference dependency) {
			if (dependency == null) throw new IllegalArgumentException(SimMsgs.argNotNull());
			if (!(dependency instanceof FunctionReferenceImpl)) {
				throw new IllegalArgumentException(SimMsgs.wrongType(FunctionReferenceImpl.class, dependency.getClass()));
			}
			
			SimRecord target = ((FunctionReferenceImpl)dependency).getRec();
			SimRecord source = getRec();
			String targetId = target.getSimObject().getId();
			String sourceId = source.getSimObject().getId();
			
			target.getIncomingDeps().add(sourceId);
			source.getOutgoingDeps().add(targetId);
			
			return this;
		}

		@Override
		public FunctionReference removeDependency(FunctionReference dependency) {
			if (dependency == null) throw new IllegalArgumentException(SimMsgs.argNotNull());
			if (!(dependency instanceof FunctionReferenceImpl)) {
				throw new IllegalArgumentException(SimMsgs.wrongType(FunctionReferenceImpl.class, dependency.getClass()));
			}
			
			SimRecord target = ((FunctionReferenceImpl)dependency).getRec();
			SimRecord source = getRec();
			String targetId = target.getSimObject().getId();
			String sourceId = source.getSimObject().getId();
			
			target.getIncomingDeps().remove(sourceId);
			source.getOutgoingDeps().remove(targetId);
			
			return this;
		}

		@Override
		public Set<FunctionReference> getDependencies() {
			TreeSet<String> outgoing = getRec().getOutgoingDeps();
			if (outgoing.size() == 0) {
				return Collections.emptySet();
			}else {
				HashSet<FunctionReference> toReturn = new HashSet<>(outgoing.size());
				for (String cur: outgoing) {
					SimRecord curRec = simObjects.get(cur);
					toReturn.add(new FunctionReferenceImpl(curRec));
				}
				
				return Collections.unmodifiableSet(toReturn);
			}
		}
		
		SimRecord getRec() {
			return rec;
		}

		private CoreSimulationBuilder getEnclosingInstance() {
			return CoreSimulationBuilder.this;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getEnclosingInstance().hashCode();
			result = prime * result + ((rec == null) ? 0 : rec.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			FunctionReferenceImpl other = (FunctionReferenceImpl) obj;
			if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
				return false;
			if (rec == null) {
				if (other.rec != null)
					return false;
			} else if (!rec.equals(other.rec))
				return false;
			return true;
		}

		@Override
		public FunctionReference addResourcePool(String poolId) {
			SimRecord poolRec = getSimRecord(poolId, ResourcePool.class);
			
			String sourceId = getRec().getSimObject().getId();
			String targetId = poolRec.getSimObject().getId();
			
			getRec().getIncomingDeps().add(targetId);
			poolRec.getOutgoingDeps().add(sourceId);
			
			return this;
		}

		@Override
		public FunctionReference removeResourcePool(String poolId) {
			SimRecord poolRec = getSimRecord(poolId, ResourcePool.class);
			
			String sourceId = getRec().getSimObject().getId();
			String targetId = poolRec.getSimObject().getId();
			
			getRec().getIncomingDeps().remove(targetId);
			poolRec.getOutgoingDeps().remove(sourceId);
			
			return this;
		}

		@Override
		public String getId() {
			return getRec().getSimObject().getId();
		}

		@Override
		public Set<String> getResourcePools() {
			HashSet<String> toReturn = new HashSet<>();
			for (String rec: getRec().getIncomingDeps()) {
				SimRecord curRec = simObjects.get(rec);
				if (curRec.getSimObject() instanceof ResourcePool) {
					toReturn.add(rec);
				}
			}
			return toReturn;
		}

		@Override
		public FunctionReference addDependency(String functionId) {
			return addDependency(getFunction(functionId));
		}

		@Override
		public FunctionReference removeDependency(String functionId) {
			return addDependency(getFunction(functionId));
		}
		
	}
	
	private void addEventGenerator(EventGenerator simulationObject) {
		checkIfDestroyed();
		if (simulationObject == null) throw new IllegalArgumentException(SimMsgs.argNotNull());
		if (simulationObject.getId() == null || simulationObject.getId().trim().length() == 0) {
			throw new IllegalArgumentException(SimMsgs.validId());
		}
		
		SimRecord prev = simObjects.get(simulationObject.getId());
		if (prev != null) {
			throw new IllegalArgumentException(SimMsgs.duplicateSimObject(simulationObject.getId(),prev.getSimObject().getClass()));
		}
		
		SimRecord toInsert = new SimRecord(simulationObject);
		simObjects.put(simulationObject.getId(), toInsert);
	}

	@Override
	public FunctionReference getFunction(String id) {
		SimRecord toReturn = simObjects.get(id);
		
		if (toReturn == null) {
			return null;
		}
		return new FunctionReferenceImpl(toReturn);
	}

	@Override
	public Simulation createSimulation(int parallelism) {
		return createSimulation(parallelism, true);
	}
	
	@Override
	public Simulation createSimulationAndDestroy(int parallelism) {
		Simulation toReturn = createSimulation(parallelism,false);
		destroyed = true;
		simObjects = null;
		rProvider = null;
		return toReturn;
	}
	
	private void checkIfDestroyed() {
		if (destroyed) {
			throw new IllegalStateException(SimMsgs.builderIsDestroyed());
		}
	}
	
	private class BasicInfoImpl implements BasicInfo{
		
		SimRecord rec;
		RandomProvider rp;
		TimeTranslator tt;
		
		BasicInfoImpl(SimRecord rec, RandomProvider rp, TimeTranslator tt) {
			super();
			this.rec = rec;
			this.rp = rp;
			this.tt = tt;
		}

		@Override
		public Set<String> getDependencies() {
			return Collections.unmodifiableSet(rec.getOutgoingDeps());
		}

		@Override
		public TimeTranslator getTimeTranslator() {
			if (tt == null) {
				throw new IllegalStateException(SimMsgs.timeTranslationNotEnabled());
			}
			return tt;
		}

		@Override
		public RandomGenerator getRandomGenerator() {
			if (rp == null) {
				throw new IllegalStateException(SimMsgs.randomGeneratorNotSet());
			}else {
				return rp.createGenerator();
			}
		}
	}
	
	private class FunctionInfoImpl extends BasicInfoImpl implements FunctionInfo{

		FunctionInfoImpl(SimRecord rec, RandomProvider rp, TimeTranslator tt) {
			super(rec, rp, tt);
		}
		
		@Override
		public Set<String> getResourcePools() {
			FunctionReferenceImpl ref = new FunctionReferenceImpl(rec);
			return new HashSet<String>(ref.getResourcePools());
		}
		
	}
	
	private Simulation createSimulation(int parallelism, boolean copy) {
		CoreEngine engine = new CoreEngine(parallelism);
		
		final RandomProvider rp = rProvider;
		final TimeTranslator tt = tTranslator;
		
		//First register a COPY of all the objects into the simulation and run their initialization code
		for (SimRecord rec : simObjects.values()) {
			if (rec.getSimObject() instanceof Function) {
				Function f;
				if (copy) {
					f = (Function)rec.getSimObject().copy();
				}else {
					f = (Function)rec.getSimObject();
				}
				
				f.initiate(new FunctionInfoImpl(rec,rp,tt));
				
				engine.registerEvaluator(f);
			}else if (rec.getSimObject() instanceof ResourcePool) {
				ResourcePool pool;
				if (copy) {
					pool = (ResourcePool)rec.getSimObject().copy();
				}else {
					pool = (ResourcePool)rec.getSimObject();
				}
				pool.initiate(new BasicInfoImpl(rec,rp,tt));
				
				engine.registerResponder(pool);
			}
		}
		
		//Now create all the subscriptions.
		for (SimRecord rec : simObjects.values()) {
			for (String dep : rec.getOutgoingDeps()) {
				engine.registerSubscription(dep, rec.getSimObject().getId());
			}
		}
		
		return new CoreSimulation(engine);
	}
	
	@Override
	public String getHashCode() {
		checkIfDestroyed();
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			Charset utf8 = Charset.forName("UTF-8");
			
			for (SimRecord cur : simObjects.values()) {
				EventGenerator ee = cur.getSimObject();
				//Add all relevant configuration data to the MD5 hash
				md.update(ee.getId().getBytes(utf8));				
				TreeMap<String,String> sortedConfig = new TreeMap<>();
				Map<ConfigKey, String> config = ee.getConfiguration();
				for (Map.Entry<ConfigKey,String> e : config.entrySet()) {
					sortedConfig.put(e.getKey().toString(), e.getValue());
				}
				
				for (Map.Entry<String,String> e : sortedConfig.entrySet()) {
					md.update(e.getKey().getBytes(utf8));
					md.update(e.getValue().getBytes(utf8));
				}
				
				for (String out1:cur.getOutgoingDeps()) {
					md.update(out1.getBytes(utf8));
				}
				
				for (String in1:cur.getIncomingDeps()) {
					md.update(in1.getBytes(utf8));
				}
				
			}
			
			if (rProvider != null) {
				md.update(rProvider.getSetup().getBytes(utf8));
			}
			
			if (tTranslator != null) {
				md.update(tTranslator.getSetup().getBytes(utf8));
			}
			
			byte[] d = md.digest();
			
			Formatter format = new Formatter();
			format.format("%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x",
					d[0],d[1],d[2],d[3],d[4],d[5],d[6],d[7],d[8],d[9],d[10],d[11],d[12],d[13],d[14],d[15]);
			String toReturn = format.toString();
			format.close();
			return toReturn;
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(SimMsgs.badState(),e);
		}
	}

	@Override
	public FunctionReference addFunction(Function f) {
		addEventGenerator(f);
		return getFunction(f.getId());
	}

	@Override
	public SimulationBuilder addResourcePool(ResourcePool p) {
		addEventGenerator(p);
		return this;
	}

	@Override
	public SimulationBuilder setRandomProvider(RandomProvider p) {
		checkIfDestroyed();
		this.rProvider = p;
		return this;
	}

	private SimRecord getSimRecord(String id, Class<? extends EventGenerator> required) {
		SimRecord toReturn = simObjects.get(id);
		
		if (toReturn == null) {
			throw new IllegalArgumentException(SimMsgs.noSuchSimObject(id, Function.class));
		}
		
		if (required != null) {
			if (!required.isInstance(toReturn.getSimObject())) {
				throw new IllegalArgumentException(SimMsgs.wrongType(id, required, toReturn.getSimObject().getClass()));
			}
		}
		
		return toReturn;
	}

	@Override
	public Map<ConfigKey,String> getConfig(String id) {
		SimRecord tr = simObjects.get(id);
		
		if (tr == null) {
			return null;
		}
		
		if (tr.getSimObject() instanceof EventGenerator) {
			return ((EventGenerator)tr.getSimObject()).getConfiguration();
		}else {
			throw new IllegalArgumentException(SimMsgs.wrongType(id,EventGenerator.class,tr.getSimObject().getClass()));
		}
	}
	
	@Override
	public String toString() {
		checkIfDestroyed();
		StringBuilder toReturn = new StringBuilder();

		for (SimRecord cur : simObjects.values()) {
			EventGenerator ee = cur.getSimObject();

			toReturn.append(ee.getId()).append('\n');

			TreeMap<String,String> sortedConfig = new TreeMap<>();
			Map<ConfigKey, String> config = ee.getConfiguration();
			for (Map.Entry<ConfigKey,String> e : config.entrySet()) {
				sortedConfig.put(e.getKey().toString(), e.getValue());
			}

			for (Map.Entry<String,String> e : sortedConfig.entrySet()) {
				toReturn.append(e.getKey()).append('=').append(e.getValue()).append('\n');
			}

			toReturn.append("**outgoing dependencies**").append('\n');
			for (String out1:cur.getOutgoingDeps()) {
				toReturn.append("  ").append(out1).append('\n');
			}

			toReturn.append("**incoming dependencies**").append('\n');
			for (String in1:cur.getIncomingDeps()) {
				toReturn.append("  ").append(in1).append('\n');
			}
			
			toReturn.append('\n');
		}

		if (rProvider != null) {
			toReturn.append("**random provider**").append('\n');
			toReturn.append("  ").append(rProvider.getSetup());
		}

		return toReturn.toString();
	}

	@Override
	public boolean resourcePoolExists(String poolId) {
		SimRecord rec = simObjects.get(poolId);
		return (rec != null && rec.getSimObject() instanceof ResourcePool);
	}

	@Override
	public SimulationBuilder setTimeTranslator(TimeTranslator t) {
		checkIfDestroyed();
		this.tTranslator = t;
		return this;
	}
}
