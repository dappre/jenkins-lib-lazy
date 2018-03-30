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

// Convert Map to List of String, to use config.env in withEnv
def mapToList(Map map) {
    def list = []
    map.each { k, v ->
      list += "${k}=${v}"
    }
    return list
}

def call (body) {
    logger.debug(params.name, 'Retrieving config')
    def config = lazyConfig()

    logger.debug(params.name, 'Injecting environment variables from configuration Map to List')
    def stageEnv = mapToList(config.env)
    stageEnv += [ "LAZY_BRANCH=${config.branch}", "LAZY_LOGLEVEL=${config.logLevel}", ]
    logger.trace(params.name, "Injected environment = ${stageEnv}")
    withEnv(stageEnv as List) {
        def params = [
            lazyConfig: config,
            name:   null,
            onlyif: true,
            input:  null,
            tasks:  [],
            // FIXME: Not sure why, but it seems like the global
            // vars are not available within the stage Closure
            // so we need to sort of copy them for now
            env:            this.env,
            currentBuild:   this.currentBuild,
        ]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = params
        body()
    
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
    
        if (config.stageFilter && !config.stageFilter.contains(params.name)) {
            logger.warn(params.name, "Skipped because (config.stageFilter.${params.name} is not set)")
            return 0
        }

        if (! params.onlyif) {
            logger.info(params.name, "Skipped because onlif condition returned 'false'")
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
    
                logger.debug(params.name, 'Prepare the list of docker labels to be used for this task block')
                if (task.in == '*') {
                    if (config.inLabels) {
                        logger.debug(params.name, "Expanding '*' with configured inLabels (${config.inLabels})")
                        task.in = config.inLabels
                    } else {
                        error "Using '*' as value for 'in' key requires inLabels to be configured"
                    }
                }
    
                logger.debug(params.name, 'Walking in task.in to populate branches')
                task.in.each { label ->
                    logger.trace("${params.name}/${index}/${task.on}", "Processing label = ${label.toString()}")
                    branches += lazyNode(params.name, index++, task, label)
                }
            }
    
            logger.debug(params.name, 'Execute block(s) in parallel with input injected in environment')
            withEnv(["lazyInput=${input.toString()}"]) {
                parallel(branches)
            }
            logger.info(params.name, 'Finished')
        }
    }
}
