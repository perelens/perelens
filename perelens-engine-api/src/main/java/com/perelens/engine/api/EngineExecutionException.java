/**
 * 
 */
package com.perelens.engine.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

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
public class EngineExecutionException extends RuntimeException {

	private static final long serialVersionUID = -2486924419870156266L;
	
	Collection<Throwable> causes;
	
	public EngineExecutionException(String message, Collection<Throwable> causes) {
		super(message);
		this.causes = Collections.unmodifiableCollection(new ArrayList<>(causes));
	}

	public Collection<Throwable> getCauses(){
		return causes;
	}
}
