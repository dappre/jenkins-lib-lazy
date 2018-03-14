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

import groovy.transform.Field
import org.jenkins.ci.lazy.Logger

@Field private logger = new Logger(this)

def call(stage, index, task, target, dist = null) {
	logger.debug('Retrieving config')
	def config = lazyConfig()

	logger.debug("Detected tasks to be run on ${target}")
	def label = target
	if (config.labels[target]) {
		label = config.labels[target]
		logger.info("Mapping found for label ${target} = ${label}")
	}

	def name = "${stage}/${index}/${target}"
	logger.info(name, "Preparing to branch on agent with label = ${label}")
	def branch = [
		(name): {
			node(label: label) {
				try {
					checkout scm

					ansiColor('xterm') {
						// Execute pre closure first
						if (task.pre) task.pre.call()

						if (dist) {
							lazyDocker(stage, task.run, dist, task.args)
						} else {
							lazyStep(stage, task.run, target).each { step ->
								step()
							}
						}

						// Execute post closure at the end
						if (task.post) task.post.call()
					}
				} catch (e) {
					error e.toString()
				} finally {
					step([$class: 'WsCleanup'])
				}
			}
		}
	]

	return branch
}