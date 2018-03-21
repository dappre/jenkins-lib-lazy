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
            name:        env.JOB_NAME,
            sdir:        env.LAZY_SDIR ?: 'lazyDir',
            env:         env.LAZY_ENV ? env.LAZY_ENV.split(",") : [],
            labels:      env.LAZY_LABELS ? mapFromText(env.LAZY_LABELS) : [ default: 'master' ],
            dists:       env.LAZY_DISTS ? env.LAZY_DISTS.split(",") : [],
            stages:      env.LAZY_STAGES ? env.LAZY_STAGES.split(",") : [],
            verbosity:   env.LAZY_VERBOSITY ?: 'INFO',
            nopoll:      env.LAZY_NOPOLL ?: 'master',
            cronpoll:    env.LAZY_CRONPOLL ?: 'H/10 * * * *',
            branch:      env.BRANCH_NAME ?: env.LAZY_BRANCH ?: 'master',
            ] + args
		logger.trace('init', "Initial config = ${params.toString()}")
			
        def props = []

		logger.debug('init', 'Add parameters property')
        props += parameters([
            textParam(name: 'env', defaultValue: args.env.join("\n"), description: 'List of custom environment variables to be set (default: blank = none)'),
            textParam(name: 'labels', defaultValue: args.labels.collect{ it }.join("\n"), description: 'Map of node label to use for docker and other targeted agent'),
            textParam(name: 'dists', defaultValue: args.dists.join("\n"), description: 'List of distribution to use inside docker'),
            textParam(name: 'stages', defaultValue: args.stages.join("\n"), description: 'List of stages to go through (default: blank = all)'),
            choice(name: 'verbosity', choices: logger.getLevels().join("\n"), defaultValue: 'INFO', description: 'Control verbosity (where implemented)'),
            // Parameters to load/enable the extended library
            string(name: 'libExtRemote', defaultValue: 'https://github.com/digital-me/jenkins-lib-lazy-ext.git', description: 'Git URL of the extended shared library'),
            string(name: 'libExtBranch', defaultValue: 'master', description: 'Git branch for the Extended shared library'),
            string(name: 'libExtCredId', defaultValue: 'none', description: 'Credentials to access the Extended shared library'),
            booleanParam(name: 'extended', defaultValue: false, description: 'Enable extended stages (requires extended lib)'),
        ])
        logger.trace('init', "Parameters content = ${params.toString()}")
        logger.debug('init', 'Create config map based on the user parameters and the prepared ones')
        config.putAll([
            name        : args.name,
            sdir        : args.sdir,
            env         : params.env && params.env.trim() != '' ? params.env.trim().split("\n") : args.env,
            labels      : params.labels && params.labels.trim() != '' ? mapFromText(params.labels.trim()) : args.labels,
            dists       : params.dists && params.dists.trim() != '' ? params.dists.trim().split("\n") : args.dists,
            stages      : params.stages && params.stages.trim() != '' ? params.stages.trim().split("\n") : args.stages,
            verbosity   : params.verbosity && params.verbosity.trim() != '' ? params.verbosity.trim() : args.verbosity,
            nopoll      : args.nopoll,
            cronpoll    : args.cronpoll,
            extended    : true,
            branch      : args.branch,
        ])
        logger.debug('init', 'Set default logging level from config')
        logger.setLevel(config.verbosity)
        logger.trace('init', "New config = ${config}")

        logger.debug('init', 'Add buildDiscarder property')
        props += buildDiscarder(
            logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10')
        )

        logger.debug('init', 'Disable concurrent builds by default')
        props += disableConcurrentBuilds()

        if (!(env.BRANCH_NAME ==~ /${config.nopoll}/)) {
            logger.info('init', 'Add pollSCM trigger property')
            props += pipelineTriggers([pollSCM(config.cronpoll)])
        }

        logger.debug('init', "Processing ${props.size()} properties")
        properties(props)

        logger.debug('init', 'Load Extended library if available and update configuration accordingly')
        logger.info('lib', 'Trying to load Extended library...')
        try {
            library(
                    identifier: "libExt@${params.libExtBranch}",
                    retriever: modernSCM([
                        $class: 'GitSCMSource',
                        remote: params.libExtRemote,
                        credentialsId: params.libExtCredId
                    ])
                    )
            logger.info('lib', 'Extended shared library loaded: extended features are supported')
        } catch (error) {
            logger.info('lib', 'Extended shared library could NOT be loaded: extended features are disabled')
            logger.warn('lib', "Extended shared library loading error message: ${error.message}")
            config.extended = false
        }
    }

    logger.debug('Return config map')
    return config
}
