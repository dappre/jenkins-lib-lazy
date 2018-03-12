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
@Field public static Map config = [:]

// Default method
def call(Map args = [:]) {
	if (config && config.verbose > 1) echo "Config from lazyConfig = ${config}"

	if (!config) {
		// Override default arguments
		args = [
			name:	env.JOB_NAME,
			sdir:	'lazy-ci',
			dists:	[],
			stages:	[],
			flags:	[],
			] + args

		// Define parameters and their default values
		properties([
			parameters([
				textParam(name: 'stages', defaultValue: args.stages.join("\n"), description: 'List of stages to go through (default: blank = all)'),
				textParam(name: 'flags', defaultValue: args.flags.join("\n"), description: 'List of custom flags to be set (default: blank = none)'),
				textParam(name: 'dists', defaultValue: args.dists.join("\n"), description: 'List of distribution to use for this build'),
				string(name: 'labelDocker', defaultValue: 'docker', description: 'Label of node(s) to run docker steps'),
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

		// Instanciate a configuration object based on the parameters
		config.putAll([
			name		: args.name,
			sdir		: args.sdir,
			stages		: params.stages.trim() != '' ? params.stages.trim().split("\n") : [],
			flags		: params.flags.trim() != '' ? params.flags.trim().split("\n") : [],
			dists		: params.dists.trim() != '' ? params.dists.trim().split("\n") : [],
			labels		: [
				docker:		params.labelDocker,
				default:	'master',
			],
			verbose		: params.verbose as Integer,
			extended 	: true,
			branch		: env.BRANCH_NAME,
		])

		// Load Extended library if available and update configuration accordingly
		if (config.verbose) echo 'Trying to load Extended library...'
		try {
			library(
					identifier: "libExt@${params.libExtBranch}",
					retriever: modernSCM([
						$class: 'GitSCMSource',
						remote: params.libExtRemote,
						credentialsId: params.libExtCredId
					])
					)
			if (config.verbose) echo 'Extended shared library loaded: extended features are supported'
		} catch (error) {
			if (config.verbose) echo 'Extended shared library could NOT be loaded: extended features are disabled'
			if(config.verbose > 1) {
				echo "Warning message:\n${error.message}"
			}
			config.extended = false
		}
	}

	return config
}
