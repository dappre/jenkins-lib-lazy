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
@Field public static Map config = [:]

// Convert list of key/pair from text to Map
def mapFromText(String text) {
    logger.debug('mapFromText', 'Convert list of key/pair from String to Map')
    logger.trace('mapFromText', "Parse from String = " + text.replaceAll("\\\n", "\\\\\\n"))
    Map map = [:]
    text.findAll(/([^=\n\s,]+)=([^\n\s,]+)/) { group ->
        // REM: In Groovy console, the Closure parameters are 'full, key, value ->'
        def key = group[1]
        def value = group[2]
        logger.trace('mapFromText', "Add key = ${key} and value = ${value}")
        map[key] = value
    }

    logger.trace('mapFromText', "Resulting map = ${map}")
    return map
}

// Default method
def call(Map args = [:]) {
    logger.trace("Current config = ${config}")

    if (!config) {
        logger.debug('init', 'Preparing config from env if available or hardcoded defaults if needed, squashed with args if set')
        args = [
            name:           env.JOB_NAME,
            dir:            env.LAZY_DIR ?: 'lazyDir',
            env:            env.LAZY_ENV ? env.LAZY_ENV.split(",") : [],
            onLabels:       env.LAZY_ONLABELS ? mapFromText(env.LAZY_ONLABELS) : [ default: 'master' ],
            inLabels:       env.LAZY_INLABELS ? env.LAZY_INLABELS.split(",") : [],
            stageFilter:    env.LAZY_STAGEFILTER ? env.LAZY_STAGEFILTER.split(",") : [],
            logLevel:       env.LAZY_LOGLEVEL ?: 'INFO',
            noPoll:         env.LAZY_NOPOLL ?: 'master',
            cronPoll:       env.LAZY_CRONPOLL ?: 'H/10 * * * *',
            branch:         env.BRANCH_NAME ?: env.LAZY_BRANCH ?: 'master',
            ] + args
		logger.trace('init', "Initial config = ${params.toString()}")
			
        def props = []

		logger.debug('init', 'Add parameters property')
        props += parameters([
            textParam(name: 'LAZY_ENV', defaultValue: args.env.join("\n"), description: 'List of custom environment variables to be set (default: blank = none)'),
            textParam(name: 'LAZY_ONLABELS', defaultValue: args.onLabels.collect{ it }.join("\n"), description: 'Map of node labels for \'on\' values, for docker and other agent'),
            textParam(name: 'LAZY_INLABELS', defaultValue: args.inLabels.join("\n"), description: 'List of docker labels for \'in\' values (default: blank = all)'),
            textParam(name: 'LAZY_STAGEFILTER', defaultValue: args.stageFilter.join("\n"), description: 'Filter stages to go through (default: blank = all)'),
            choice(name: 'LAZY_LOGLEVEL', choices: logger.getLevels().join("\n"), defaultValue: 'INFO', description: 'Control logLevel (where implemented)'),
        ])
        logger.trace('init', "Parameters content = ${params.toString()}")
        logger.debug('init', 'Create config map based on the user parameters and the prepared ones')
        config.putAll([
            name:           args.name,
            dir:            args.dir,
            env:            params.env && params.env.trim() != '' ? params.env.trim().split("\n") : args.env,
            onLabels:       params.onLabels && params.onLabels.trim() != '' ? mapFromText(params.onLabels.trim()) : args.onLabels,
            inLabels:       params.inLabels && params.inLabels.trim() != '' ? params.inLabels.trim().split("\n") : args.inLabels,
            stageFilter:    params.stageFilter && params.stageFilter.trim() != '' ? params.stageFilter.trim().split("\n") : args.stageFilter,
            logLevel:       params.logLevel && params.logLevel.trim() != '' ? params.logLevel.trim() : args.logLevel,
            noPoll:         args.noPoll,
            cronPoll:       args.cronPoll,
            branch:         args.branch,
        ])
        logger.debug('init', 'Set default logging level from config')
        logger.setLevel(config.logLevel)
        logger.trace('init', "New config = ${config}")

        logger.debug('init', 'Add buildDiscarder property')
        props += buildDiscarder(
            logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10')
        )

        logger.debug('init', 'Disable concurrent builds by default')
        props += disableConcurrentBuilds()

        logger.debug('init', 'Disable multibranch indexing')
        props += overrideIndexTriggers(true)

        if (!(env.BRANCH_NAME ==~ /${config.noPoll}/)) {
            logger.info('init', 'Add pollSCM trigger property')
            props += pipelineTriggers([pollSCM(config.cronPoll)])
        }

        logger.debug('init', "Processing ${props.size()} properties")
        properties(props)
    }

    logger.debug('Return config map')
    return config
}
