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

@Field private logger = new Logger(this, 'DEBUG')
@Field public static Map config = [:]

def mapFromText(String text) {
	logger.push('mapFromText')
	
	// Convert labels List from parameters back to Map in config
	Map map = [:]
	logger.debug("Start parsing text to map from content = " + text.replaceAll("\\\n", "\\\\\\n"))
	text.findAll(/([^=\n]+)=([^\n]+)/) { group ->
		// REM: In Groovy console, the Closure parameters are 'full, key, value ->'
		def key = group[1]
		def value = group[2]
		logger.debug("Adding one matched tuple of key = ${key} and value = ${value}")
		map[key] = value
	}

	logger.pop() // To remove previously added extra loggin info

	return map
}

// Default method
def call(Map args = [:]) {
	if (config && config.verbose > 1) echo "Config from lazyConfig = ${config}"
	logger.info("Current config = ${config}")

	if (!config) {
		logger.push('init')
		
		// Override default arguments
		args = [
			name:	env.JOB_NAME,
			sdir:	env.LAZY_SDIR ? env.LAZY_SDIR : 'lazy-pipeline',
			stages:	env.LAZY_STAGES ? env.LAZY_STAGES.split("/n") : [],
			flags:	env.LAZY_FLAGS ? env.LAZY_FLAGS.split("/n") : [],
			labels:	env.LAZY_LABELS ? mapFromText(env.LAZY_LABELS) : [ default: 'master' ],
			dists:	env.LAZY_DISTS ? env.LAZY_DISTS.split("/n") : [],
			] + args

		// Define parameters and their default values
		properties([
			parameters([
				textParam(name: 'stages', defaultValue: args.stages.join("\n"), description: 'List of stages to go through (default: blank = all)'),
				textParam(name: 'flags', defaultValue: args.flags.join("\n"), description: 'List of custom flags to be set (default: blank = none)'),
				textParam(name: 'labels', defaultValue: args.labels.collect{ it }.join("\n"), description: 'Map of node label to use for docker and other targeted agent'),
				textParam(name: 'dists', defaultValue: args.dists.join("\n"), description: 'List of distribution to use inside docker'),
				//string(name: 'labelDocker', defaultValue: 'docker', description: 'Label of node(s) to run docker steps'),
				//			string(name: 'lbMacOS10', defaultValue: 'mac', description: 'Node label for Mac OS X 10'),
				//			string(name: 'lbWindows10', defaultValue: 'windows', description: 'Node label for Windows 10'),
				choice(name: 'verbose', choices: ['1', '2', '0'].join("\n"), defaultValue: '1', description: 'Control verbosity (where implemented)'),
				// Parameters to load/enable the extended library
				string(name: 'libExtRemote', defaultValue: 'https://github.com/digital-me/jenkins-lib-lazy-ext.git', description: 'Git URL of the extended shared library'),
				string(name: 'libExtBranch', defaultValue: 'master', description: 'Git branch for the Extended shared library'),
				string(name: 'libExtCredId', defaultValue: 'none', description: 'Credentials to access the Extended shared library'),
				booleanParam(name: 'extended', defaultValue: false, description: 'Enable extended stages (requires extended lib)'),
			])
		])

		// Convert labels List from parameters back to Map in config
		Map labels = [:]
		if (params.labels.trim() != '') {
			params.labels.trim().findAll(/([^=\n]+)=([^\n]+)/) { group ->
				// REM: In Groovy console, the Closure parameters are 'full, key, value ->'
				def key = group[1]
				def value = group[2]
				Map label = labels[key] = value
			}
		}

		// Instanciate a configuration object based on the parameters
		config.putAll([
			name		: args.name,
			sdir		: args.sdir,
			stages		: params.stages && params.stages.trim() != '' ? params.stages.trim().split("\n") : args.stages,
			flags		: params.flags && params.flags.trim() != '' ? params.flags.trim().split("\n") : args.flags,
			labels		: params.labels && params.labels.trim() != '' ? mapFromText(params.labels.trim()) : args.labels,
			dists		: params.dists && params.dists.trim() != '' ? params.dists.trim().split("\n") : args.dists,
			verbose		: params.verbose as Integer,
			extended 	: true,
			branch		: env.BRANCH_NAME,
		])
		logger.debug("New config = ${config}")
		
		// Load Extended library if available and update configuration accordingly
		logger.push('lib')
		logger.info('Trying to load Extended library...')
		try {
			library(
					identifier: "libExt@${params.libExtBranch}",
					retriever: modernSCM([
						$class: 'GitSCMSource',
						remote: params.libExtRemote,
						credentialsId: params.libExtCredId
					])
					)
			logger.info('Extended shared library loaded: extended features are supported')
		} catch (error) {
			logger.info('Extended shared library could NOT be loaded: extended features are disabled')
			logger.warning("Extended shared library loading error message: ${error.message}")
			config.extended = false
		}
		logger.reset()
	}

	return config
}
