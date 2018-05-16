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

// Parse task as Closure of (list of) String and return a List of step(s)
def call (stage, task, label) {
    logger.debug('Retrieving config')
    def config = lazyConfig()

    logger.debug('Collect steps to be resolved')
    def steps = []
    if (task instanceof Closure) {
        logger.trace("Task is a Closure = ${task.toString()}")
        // If task is a Closure, just add it in the step list
        steps += task
    } else {
        logger.debug('Prepare shell scripts from (list of) String')
        def scripts = []
        if (task instanceof List) {
            logger.trace("Task is a List (${task.toString()})")
            scripts = lazyRes(stage, task, label)
        } else if (task instanceof String) {
            logger.trace("Task is a String (${task.toString()})")
            scripts = lazyRes(stage, [ task ], label)
        } else {
            logger.error('Give up if not a Closure, not a List and not a String!')
            def err = "${stage}/${label} No idea what to do with task = ${task.toString()}"
            logger.fatal(err)
            error err.toString()
        }

        // Collect all scripts as shell steps
        logger.trace("Task was referring to {scripts.size()} scripts (${scripts})")
        scripts.each { script ->
                logger.trace("Change mode of ${script} to allow execution")
                sh("chmod +x ${config.dir}/${stage}/${script}")
                steps += { sh("${config.dir}/${stage}/${script}") }
        }
    }
    logger.trace("Task has been converted in ${steps.size()} steps (${steps.toString()})")

    logger.debug('Return list of resolved steps')
    return steps
}
