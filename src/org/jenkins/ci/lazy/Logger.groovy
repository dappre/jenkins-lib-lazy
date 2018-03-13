#!groovy
 
/*
 * This work is protected under copyright law in the Kingdom of
 * The Netherlands. The rules of the Berne Convention for the
 * Protection of Literary and Artistic Works apply.
 * Digital Me B.V. is the copyright owner.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.jenkins.ci.lazy
 
class Logger implements Serializable { 

	// Define property for log levels (default first for parameter choice)
	private static final Map levels = [
	    INFO:	2,
	    TRACE:	0,
	    DEBUG:	1,
	    WARN:	3,
	    ERROR:	4,
		FATAL:	5,
	]
	private static String globalLevel = 'INFO'
	private final String level = null

	// Define field for caller class and its name
	private caller = null
	final String name = null

	public Logger (caller, level = null) {
		this.caller = caller
		this.name = caller.class.name
		if (level) {
			this.level = level
		}
	}

  	// Getter for all levels
  	public List getLevels() {
    	return levels.keySet() as List
    }

	// Setter for global shared level
	public void setLevel(level) {
		this.globalLevel = level
	}

	// Getter for active level
	public String getLevel() {
		return this.level ? this.level : globalLevel
	}

	public void trace(sub = null, msg) {
	    log(sub, msg, 'TRACE')
	}

	public void debug(sub = null, msg) {
	    log(sub, msg, 'DEBUG')
	}

	public void info(sub = null, msg) {
	    log(sub, msg, 'INFO')
	}

	public void warn(sub = null, msg) {
	    log(sub, msg, 'WARN')
	}

	public void error(sub = null, msg) {
	    log(sub, msg, 'ERROR')
	}

	public void fatal(sub = null, msg) {
	    log(sub, msg, 'FATAL')
	}

	private void log(sub = null, msg, level) {
	    if (levels[level] >= levels[getLevel()]) {
	        caller.echo "${level} ${name}" + (sub ? "[${sub}]" : "") + " - ${msg}"
	    }
	}
}
