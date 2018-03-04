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

import org.jenkins.ci.lazy.plConfig

// Step for Linux distribution on Docker
def stepDocker(name, config, dist, args = '', tasks = [ 'inside.sh' ]) {
	if (config.verbose) echo "Docker step ${name} on ${dist}: Startup"

	// Checkout the source
	checkout scm

	try {
		// Prepare all shell scripts in workspace to be run later
		def shTasks = prepareShTasks(name, config, dist, tasks)
		if (config.verbose) {
			echo "Found ${shTasks.size()} shell tasks:"
			shTasks.eachWithIndex { shTask, i ->
				echo "${i + 1}.\t${shTask}"
			}
		}

		// Build the relavent Docker image
		def imgDocker = buildDockerImg(name, config, dist)

		// Run each shell scripts as task inside the Docker
		imgDocker.inside(args) {
			ansiColor('xterm') {
				withEnv(["DIST=${dist}"]) {
					shTasks.each { shTask ->
						sh "${config.sdir}/${name}/${shTask}"
					}
				}
			}
		}
	} finally {
		if (config.verbose) echo "Docker step ${name} on ${dist}: Cleanup"
		step([$class: 'WsCleanup'])
	}
}

// Generates a Map of nodes to execute one Docker step for each distribution
Map distNode(name, config, args = '', tasks = [ 'inside.sh' ]) {
	Map mDists = [:]

	config.dists.each { dist ->
		if (config.verbose) echo "Stage ${name} on ${dist} will be done using Docker (label = ${config.labels.docker})"
		mDists += [
			(dist): {
				node(label: config.labels.docker) {
					stepDocker(name, config, dist, args, tasks)
				}
			}
		]
	}

	return mDists
}

// Default method to run shell tasks in parallel Docker containers
def call(name, config, args = '', tasks = [ 'inside.sh' ]) {
	if (config.stages.contains(name)) {
		stage(Character.toUpperCase(name.charAt(0)).toString() + name.substring(1)) {
			if (config.verbose) echo "Stage ${name} for ${config.name} begins here"
			parallel(distNode(name, config, args, tasks))
			if (config.verbose) echo "Stage ${name} for ${config.name} ends here"
		}
	} else {
		echo "Stage ${name} will be skipped (config.stages.${name} is not set)"
	}
}

