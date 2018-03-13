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

	static final Map levels = [
	    TRACE:		1,
	    DEBUG:		2,
	    INFO:		3,
	    WARNING:	4,
	    ERROR:		5,
	]

	String level = 'INFO'
	private caller = null
	final String name = null
	private String[] extra = []

	def Logger (caller) {
		this.caller = caller
		this.name = caller.class.name
	}

	def Logger (caller, level) {
		this.caller = caller
		this.name = caller.class.name
		this.level = level
	}

	def set(String extra) {
		this.extra = extra
	}

	def push(String extra) {
		this.extra += extra 
	}

	def pop() {
		this.extra = this.extra ? this.extra[0..-2] : null
	}
	
	def reset() {
		this.extra = null
	}

	def trace(msg) {
	    log(msg, 'TRACE')
	}

	def debug(msg) {
	    log(msg, 'DEBUG')
	}

	def info(msg) {
	    log(msg, 'INFO')
	}

	def warning(msg) {
	    log(msg, 'WARNING')
	}

	def error(msg) {
	    log(msg, 'ERROR')
	}

	def log(msg, level) {
	    if (levels[level] >= levels[this.level]) {
	        caller.echo "${level} ${name}" + (extra ? "[${extra.join('/')}]" : "") + " - ${msg}"
	    }
	}
}
