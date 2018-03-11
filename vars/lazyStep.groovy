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

// Function to prepare and list shell scripts (copy from lib to workspace if needed)
def listScripts(stage, scripts, target) {
	// Retrieving global config
	def config = lazyConfig()

	def scriptsLst = []

	// Enter sub-folder where Dockerfiles and scripts are located
	dir("${config.sdir}/${stage}") {
		scripts.each { script ->
			def dstScript = "${script}"	// Default main script location
			// Lookup fo the relevant main script in sub workspace first
			// TODO: Rework to use fileExists?
			def srcScript = sh(
				returnStdout: true,
				script: "ls -1 ${target}.${script} 2> /dev/null || ls -1 ${script} 2> /dev/null || echo"
			).trim()

			if (srcScript != null && srcScript != '') {
				// Use main script from workspace if existing
				dstScript = srcScript
			} else {
				// Extract main script from shared lib
				def contentscript = ''
				try {
					contentscript = libraryResource("${config.sdir}/${stage}/${target}.${script}")
				} catch (hudson.AbortException e) {
					contentscript = libraryResource("${config.sdir}/${stage}/${script}")
				}

				// Write the selected Dockerfile to workspace sub-folder
				writeFile(
					file: dstScript,
					text: contentscript
				)

				// Allow execution
				sh "chmod +x ${dstScript}"
			}

			// Add the path to the script in the final list to be returned
			scriptsLst += dstScript
		}
	}

	if (config.verbose) {
		echo "Found ${scriptsLst.size()} shell tasks:"
		scriptsLst.eachWithIndex { script, i ->
			echo "${i + 1}.\t${script}"
		}
	}
	
	return scriptsLst
}

// Parse task as Closure of (list of) String and return a List of step(s)
def call (stage, task, target) {
	// Retrieving global config
	def config = lazyConfig()

	// Collect steps to be executed
	def steps = []
	if (task instanceof Closure) {
		if (config.verbose > 2) echo "Task is a Closure (${task})"
		// If task is a Closure, just add it in the step list
		steps += task
	} else {
		// Prepare shell scripts from (list of) String
		def scripts = []
		if (task instanceof List) {
			if (config.verbose > 2) echo "Task is a List (${task})"
			scripts = listScripts(stage, task, target)
		} else if (task instanceof String) {
			if (config.verbose > 2) echo "Task is a String (${task})"
			scripts = listScripts(stage, [ task ], target)
		} else {
			// Give up if not a Closure, not a List and not a String!
			error "No idea what to do with ${task}".toString()
		}

		// Collect all scripts as shell steps
		if (config.verbose > 2) echo "Task was referring to {scripts.size()} scripts (${scripts})"
		scripts.each { script ->
			steps += { sh "${config.sdir}/${stage}/${script}" }
		}
	}

	// Execute each collected steps
	if (config.verbose > 2) echo "Task has been converted in {steps.size()} steps (${steps})"
	
	return steps
}
