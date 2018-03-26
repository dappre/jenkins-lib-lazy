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
import groovy.json.JsonSlurper
import groovy.json.JsonParserType
import groovy.json.JsonOutput

@Field private logger = new Logger(this)
@Field public static Map config = [:]

// Convert List to json String, without any extra sandbox permissions
def toJson (List list) {
    def json = "[\n"
    list.eachWithIndex { it, i ->
      logger.trace('toJson/list', "${it} / index = ${i}")
        json += "  \"${it}\""
        if (i < (list.size() - 1) ) {
            json += ",\n"
        } else {
            json += "\n]"
        }
    }
    return json
}

// Convert Map to json String, without any extra sandbox permissions
def toJson(Map map) {
    def json = "{\n"
    map.eachWithIndex { k, v, i ->
      logger.trace('toJson/map', "${k}=${v} / index = ${i}")
        json += "  ${k}: ${v}"
        if (i < (map.size() - 1) ) {
            json += ",\n"
        } else {
            json += "\n}"
        }
    }
    return json
}

// Convert Map to json String, requires extra sandbox permissions
def toJsonPretty(obj) {
    def jsonOut = new groovy.json.JsonOutput()
    return jsonOut.prettyPrint(jsonOut.toJson(obj))
}

// Parse Json to Groovy object, requires extra sandbox permissions
def parseText(text) {
    logger.debug('parseText', 'Convert Json text to Map object')
    logger.trace('parseText', "Parse from String = " + text.replaceAll("\\\n", "\\\\\\n"))
    def jsonIn = new groovy.json.JsonSlurper().setType(JsonParserType.LAX)
    def readVal = jsonIn.parseText(text)
    def result = [:]
    result.putAll(readVal)
    return result
}

// Parse Json to Groovy object, without any extra sandbox permissions
def parseTextToMap(text) {
    logger.debug('parseTextToMap', 'Convert list of key/pair from String to Map')
    logger.trace('parseTextToMap', "Parse from String = " + text.replaceAll("\\\n", "\\\\\\n"))
    Map map = [:]
    text.findAll(/"?([^=\n\s,"]+)"?\s*:\s*"?([^\n\s,"]+)"?/) { group ->
        // REM: In Groovy console, the Closure parameters are 'full, key, value ->'
        def key = group[1]
        def value = group[2]
        logger.trace('parseTextToMap', "Add key = ${key} and value = ${value}")
        map[key] = value
    }

    logger.trace('mapFromText', "Resulting map = ${map}")
    return map
}

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
            env:            env.LAZY_ENV ? parseText(env.LAZY_ENV) : [ DRYRUN: false, ],
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
            textParam(name: 'LAZY_ENV', defaultValue: toJsonPretty(args.env), description: 'List of custom environment variables to be set (format: json, default: { "DRYRUN": false })'),
//            textParam(name: 'LAZY_ENV', defaultValue: toJson(args.env), description: 'List of custom environment variables to be set (format: json, default: { "DRYRUN": false })'),
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
            env:            params.LAZY_ENV && params.LAZY_ENV.trim() != '' ? parseText(params.LAZY_ENV) : args.env,
            onLabels:       params.LAZY_ONLABELS && params.LAZY_ONLABELS.trim() != '' ? mapFromText(params.LAZY_ONLABELS.trim()) : args.onLabels,
            inLabels:       params.LAZY_INLABELS && params.LAZY_INLABELS.trim() != '' ? params.LAZY_INLABELS.trim().split("\n") : args.inLabels,
            stageFilter:    params.LAZY_STAGEFILTER && params.LAZY_STAGEFILTER.trim() != '' ? params.LAZY_STAGEFILTER.trim().split("\n") : args.stageFilter,
            logLevel:       params.LAZY_LOGLEVEL && params.LAZY_LOGLEVEL.trim() != '' ? params.LAZY_LOGLEVEL.trim() : args.logLevel,
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
