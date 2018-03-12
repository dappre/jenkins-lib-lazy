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

def call (body) {
	def params = [
		name:		null,
		tasks:		[],
		dockerArgs:	'',
	]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = params
	body()

	// Retrieving global config
	def config = lazyConfig()

	// Check parameters and config for possible error
	def err = null
	if (!params.name) {
		err = 'Stage always needs a name'
	} else if (!params.tasks) {
		err = 'Stage always needs some tasks'
	}
	if (err) {
		error err
	}

	// Skip stage if not listed in the config
	if (config.stages && !config.stages.contains(params.name)) {
		echo "Stage ${params.name} will be skipped (config.stages.${params.name} is not set)"
		return 0
	}

	stage(Character.toUpperCase(params.name.charAt(0)).toString() + params.name.substring(1)) {
		if (config.verbose) echo "Stage ${params.name} for ${config.name} begins here"

		// Collect all tasks in a Map of pipeline branches (as Closure) to be run in parallel
		def branches = [:]
		def index = 1

		params.tasks.each { task ->
			// Lookup node label to be used for this task bloc
			def label = config.labels.default
			if (task.on) {
				label = config.labels[task.on]
			} else if (task.inside) {
				label = config.labels.docker
			}

			// Prepare List of dists to be used for this task block
			def dists = []
			if (task.inside == '*') {
				if (config.dists) {
					echo "Expanding '*' with configured dists (${config.dists})"
					dists = config.dists
				} else {
					error "Using '*' as value for inside key requires dists to be configured"
				}
			} else if (task.inside) {
				dists = task.inside
			}

			// FIXME: Must be possible to fold the following conditionnal blocks in a simpler one
			if (dists) {
				// If inside docker, keeps adding each dist as a new branch block
				dists.each { dist ->
					if (!config.labels['docker']) {
						error "Can not find any label value for key = docker"
					}
					if (config.verbose) echo "Stage ${params.name} inside ${dist} will be done on agent with label = ${config.labels.docker}"
					def branch = "${params.name}_${index++}_${dist}"
					branches += [
						(branch): {
							node(label: config.labels.docker) {
								checkout scm
								try {
									lazyDocker(params.name, task, dist, params.dockerArgs)
								} catch (e) {
									error e.toString()
								} finally {
									step([$class: 'WsCleanup'])
								}
							}
						}
					]
				}
			} else {
				// If not, just add the branch block
				def target = task.on ? task.on : 'default'
				if (!config.labels[target]) {
					error "Can not find any node with label = ${target}"
				}
				if (config.verbose) echo "Stage ${params.name} on ${target} will be done on agent with label = ${config.labels[target]}"
				def branch = "${params.name}_${index++}_${target}"
				branches += [
					(branch): {
						node(label: config.labels[target]) {
							checkout scm
							try {
								// Execute each steps
								lazyStep(params.name, task.exec, target).each { step ->
									step ()
								}
							} catch (e) {
								error e.toString()
							} finally {
								step([$class: 'WsCleanup'])
							}
						}
					}
				]
			}
		}

		// Now we can execute block(s) in parallel
		parallel(branches)
		
		if (config.verbose) echo "Stage ${params.name} for ${config.name} ends here"
	}
}
