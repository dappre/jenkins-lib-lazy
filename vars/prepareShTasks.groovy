#!groovyy

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

import org.jenkins.ci.lazy.plConfig

// Function to copy shell scripts from lib to workspace if needed
def call(name, config, dist, tasks = [ 'main.sh' ]) {
	def shTasksLst = []

	// Enter sub-folder where Dockerfiles and scripts are located
	dir("${config.sdir}/${name}") {
		tasks.each { shTask ->
			def dstShTask = "${shTask}"	// Default main script location
			// Lookup fo the relevant main script in sub workspace first
			// TODO: Rework to use fileExists?
			def srcShTask = sh(
				returnStdout: true,
				script: "ls -1 ${dist}.${shTask} 2> /dev/null || ls -1 ${shTask} 2> /dev/null || echo"
			).trim()

			if (srcShTask != null && srcShTask != '') {
				// Use main script from workspace if existing
				dstShTask = srcShTask
			} else {
				// Extract main script from shared lib
				def contentShTask = ''
				try {
					contentShTask = libraryResource("${config.sdir}/${name}/${dist}.${shTask}")
				} catch (hudson.AbortException e) {
					contentShTask = libraryResource("${config.sdir}/${name}/${shTask}")
				}

				// Write the selected Dockerfile to workspace sub-folder
				writeFile(
					file: dstShTask,
					text: contentShTask
				)

				// Allow execution
				sh "chmod +x ${dstShTask}"
			}

			// Add the path to the script in the final list to be returned
			shTasksLst += dstShTask
		}
	}

	return shTasksLst
}
