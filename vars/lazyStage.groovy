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

def call (body) {
    def params = [
        name:   null,
        input:  null,
        tasks:  [],
        env: this.env,
    ]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    logger.debug(params.name, 'Retrieving config')
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
        logger.warn(params.name, "Skipped because (config.stages.${params.name} is not set)")
        return 0
    }

    stage(Character.toUpperCase(params.name.charAt(0)).toString() + params.name.substring(1)) {
        logger.info(params.name, "Started")

        logger.debug(params.name, 'Process input first')
        def input = params.input ? input(params.input) : null
        logger.trace(params.name, "Input = ${input}")

        logger.debug(params.name, 'Convert a single task in a List before walk in')
        def tasks = (params.tasks instanceof List) ? params.tasks : [params.tasks]

        logger.debug(params.name, 'Collect all tasks in a Map of pipeline branches (as Closure) to be run in parallel')
        def branches = [:]
        def index = 1

        tasks.each { task ->
            logger.debug(params.name, 'Add possible missing keys to the task Map')
            logger.trace(params.name, "Task content before = ${task.dump()}")
            task = [
                run:    { error 'Nothing to run' },
                on:     'default',
                pre:    null,
                post:   null,
                in:     [null,],
                args:   '',
            ] + task
            logger.trace(params.name, "Task content after = ${task.dump()}")

            logger.debug(params.name, 'Prepare the list of dists to be used for this task block')
            if (task.in == '*') {
                if (config.dists) {
                    logger.debug(params.name, "Expanding '*' with configured dists (${config.dists})")
                    task.in = config.dists
                } else {
                    error "Using '*' as value for inside key requires dists to be configured"
                }
            }

            logger.debug(params.name, 'Walking in task.in to populate branches')
            task.in.each { dist ->
                logger.trace("${params.name}/${index}/${task.on}", "Processing dist = ${dist.toString()}")
                branches += lazyNode(params.name, index++, task, dist)
            }
        }

        logger.debug(params.name, 'Execute block(s) in parallel with input injected in environment')
        withEnv(["LAZY_INPUT=${input}"]) {
            parallel(branches)
        }
        logger.info(params.name, 'Finished')
    }
}
