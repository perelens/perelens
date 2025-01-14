/**
 * 
 */
package com.perelens.engine.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import com.perelens.engine.utils.Utils;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License

 * 
 * @author Steve Branda
 *
 */
public abstract class AbstractEvent implements Event {

	private String producerId;
	private long time;
	private Event[] causedBy = Utils.EMPTY_QUEUE;
	private EventType type;
	private Collection<EventType> responseTypes = Collections.emptyList();
	private long ordinal = 0;

	@Override
	public EventType getType() {
		return type;
	}
	
	protected AbstractEvent(String producerId, EventType type, long time, long ordinal) {
		Utils.checkId(producerId);
		Utils.checkNull(type);
		this.producerId = producerId;
		this.time = time;
		this.type = type;
		setOrdinal(ordinal);
	}
	
	protected AbstractEvent(String producerId, EventType type, long time, long ordinal, Event causedBy) {
		this(producerId,type,time,ordinal);
		Utils.checkNull(causedBy);
		this.causedBy = new Event[] {causedBy};
	}
	
	protected AbstractEvent(String producerId, EventType type,long time, long ordinal, Event[] causedBy) {
		this(producerId,type,time, ordinal);
		Utils.checkNull(causedBy);
		this.causedBy = Arrays.copyOf(causedBy, causedBy.length);
	}
	
	protected AbstractEvent(String producerId, EventType type,long time, long ordinal, Event[] causedBy, int causeLimit) {
		this(producerId,type,time, ordinal);
		Utils.checkNull(causedBy);
		this.causedBy = Arrays.copyOf(causedBy, causeLimit);
	}
	
	protected void setResponseTypes(Collection<EventType> responseTypes) {
		Utils.checkNull(responseTypes);
		this.responseTypes = responseTypes;
	}
	
	protected void setOrdinal(long ordinal) {
		if (ordinal < 1) {
			throw new IllegalArgumentException("Ordinal Must Be Greater Than 0");
		}
		
		if (this.ordinal != 0) {
			throw new IllegalStateException("Ordinal is not mutable");
		}
		
		this.ordinal = ordinal;
	}
	
	@Override
	public String getProducerId() {
		return producerId;
	}

	@Override
	public long getTime() {
		return time;
	}

	@Override
	public Iterator<Event> causedBy() {
		return new Iterator<Event>() {
			int index = 0;
			
			@Override
			public boolean hasNext() {
				return index < causedBy.length;
			}

			@Override
			public Event next() {
				return causedBy[index++];
			}
			
		};
	}

	@Override
	public Collection<EventType> getResponseTypes() {
		return responseTypes;
	}
	
	@Override
	public String toString() {
		StringBuilder toReturn = new StringBuilder();
		
		writeEntry(toReturn,"",this);
		
		return toReturn.toString();
	}
	
	private void writeEntry(StringBuilder builder, String prefix, Event toWrite) {
		builder.append(prefix).append(toWrite.getClass().getSimpleName()).append('\n')
	           .append(prefix).append("  getType() = ").append(toWrite.getType()).append('\n')
	           .append(prefix).append("  getProducerId() = ").append(toWrite.getProducerId()).append('\n')
	           .append(prefix).append("  getTime() = ").append(toWrite.getTime()).append('\n')
	           .append(prefix).append("  getTimeOptimization = ").append(toWrite.getTimeOptimization()).append('\n')
	           .append(prefix).append("  getOrdinal() = ").append(toWrite.getOrdinal()).append('\n')
	           .append(prefix).append("  causedBy() = ").append('\n');
		
		String nextPrefix = prefix + "    ";
		
		for (Iterator<Event> iter = toWrite.causedBy(); iter.hasNext();) {
			writeEntry(builder,nextPrefix,iter.next());
		}
		
		builder.append('\n');
	}

	@Override
	public long getOrdinal() {
		return ordinal;
	}

	@Override
	public EventMagnitude getMagnitude() {
		return EventMagnitude.NO_MAGNITUDE;
	}
}
