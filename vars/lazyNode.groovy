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

def call(stage, index, task, dist = null) {
	logger.debug('Retrieving config')
	def config = lazyConfig()

	def name = dist ? "${stage}/${index}/${dist}" : "${stage}/${index}/${task.on}"
	logger.debug(name, "Detected tasks to be run on ${task.on}")
	def label = task.on
	if (config.labels[task.on]) {
		label = config.labels[task.on]
		logger.info(name, "Mapping found for label ${task.on} = ${label}")
	}

	logger.info(name, "Preparing to branch on agent with label = ${label}")
	def branch = [
		(name): {
			node(label: label) {
				logger.info('Started')
				try {
					logger.debug('Checkout SCM')
					checkout scm
					ansiColor('xterm') {
						if (task.pre) {
							logger.debug('Execute pre closure first')
							logger.trace("Post closure = ${task.pre.toString()}")
							task.pre.call()
						}

						logger.trace("Processing dist = ${dist.toString()}")
						if (dist) {
							logger.debug('Docker required - Calling lazyDocker')
							lazyDocker(stage, task.run, dist, task.args)
						} else {
							logger.debug('Docker not required - Calling lazyStep')
							lazyStep(stage, task.run, task.on).each { step ->
								step()
							}
						}

						if (task.post) {
							logger.debug('Execute post closure at the end')
							logger.trace("Post closure = ${task.post.toString()}")
							task.post.call()
						}
					}
				} catch (e) {
					error e.toString()
				} finally {
					step([$class: 'WsCleanup'])
				}
				logger.info('Finished')
			}
		}
	]

	logger.trace(name , "Branch block ready = ${branch.toString()}")
	return branch
}
