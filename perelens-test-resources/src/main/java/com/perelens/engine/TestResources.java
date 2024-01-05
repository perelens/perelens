package com.perelens.engine;

import java.util.ArrayList;
import java.util.Collection;

import com.perelens.engine.api.EvaluatorResources;
import com.perelens.engine.api.Event;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
   
 * 
 */
public class TestResources implements EvaluatorResources{

	private Collection<Event> events;
	private ArrayList<Event> raised = new ArrayList<>();
	
	public static class ResponseEntry{
		private Event response;
		private Event inResponseTo;
		
		private ResponseEntry(Event response, Event inResponseTo) {
			this.response = response;
			this.inResponseTo = inResponseTo;
		}
		
		public Event getResponse() {
			return response;
		}
		
		public Event getInResponseTo() {
			return inResponseTo;
		}
	}
	
	private ArrayList<ResponseEntry> responses = new ArrayList<>();
	
	public TestResources(Collection<Event> events) {
		this.events = events;
	}
	
	
	@Override
	public Iterable<Event> getEvents() {
		return events;
	}

	@Override
	public void raiseResponse(Event toRaise, Event inResponseTo) {
		responses.add(new ResponseEntry(toRaise,inResponseTo));
	}

	@Override
	public void raiseEvent(Event toRaise) {
		raised.add(toRaise);
	}
	
	public Collection<Event> getRaisedEvents(){
		return raised;
	}
	
	public Collection<ResponseEntry> getRaisedResponses(){
		return responses;
	}
}