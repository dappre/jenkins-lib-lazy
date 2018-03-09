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

def call (body) {
	def params = [
		config:		null,
		name:		null,
		tasks:		[],
		dockerArgs:	'',
	]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = params
	body()

	def config = lazyConfig()
	echo "Config from lazyStage = ${config}"
	
	def err = null
	if (!params.config) {
		err = 'No config found. Initialize first!'
	}
	if (!params.name) {
		err = 'Stage always needs a name'
	} else if (!params.tasks) {
		err = 'Stage always needs some tasks'
	}

	if (err) {
		error err
	}

	stage(Character.toUpperCase(params.name.charAt(0)).toString() + params.name.substring(1)) {
		params.tasks.each { task ->
			// Prepare dists to be used for this task bloc
			def dists = []
			if (task.on == '*') {
				// TODO: populate from config 
				dists = [ 'all', 'dist', 'available', ]
			} else {
				dists += task.on
			}
	
			// Execute steps inside or outside dists
			dists.each { dist ->
				node('master') {
					// Checkout the source
					checkout scm
					
					// Collect steps to be executed
					def steps = []
					if (task.exec instanceof Closure) {
						// If task is a Closuer, just add it in the step list
						steps += task.exec
					} else {
						// Prepare shell scripts from list or string
						def shTasks = null
						if (task.exec instanceof List) {
							shTasks = prepareShTasks {
								name	= params.name
								config	= params.config
								dist	= params.dist
								tasks	= task.exec
							}
						} else if (task.exec instanceof String) {
							shTasks = prepareShTasks
							shTasks = prepareShTasks {
								name	= params.name
								config	= params.config
								dist	= params.dist
								tasks	= [ task.exec ]
							}
						} else {
							// Give up if not a Closure, not a List and not a String!
							error "No idea what to do with ${task.exec}"
						}
						// Collect all shel steps
						shTasks.each { shTask ->
							steps += { sh "${params.config.sdir}/${params.name}/${shTask}" }
						}
					}

					echo "Need to exec the following steps inside (${dist})"
					steps.each { step ->
						step()
					}
					echo "Done"
				}
			}
		}
	}
}
