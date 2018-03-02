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

import org.jenkins.ci.lazy.plConfig

// Default method
def call(String name, String sdir = 'Jenkinsdir') {
	def distsDef = []
	def stagesDef = []
	def flagsDef = []

	// Init stage to configure the pipeline
	stage ("Init") {
		node {
			// Checkout the code in the workspace
			checkout scm

			// Parse defined stages
			stagesDef = sh(
				returnStdout: true,
				script: 'find ' + sdir + ' -mindepth 1 -maxdepth 1 -type d -exec basename "{}" \\; | grep -Po "^[^.]+" | sort | uniq'
			).split('\n')
			echo "Stages defined in workspace:\n" + stagesDef.join("\n")

			// Parse defined distributions
			distsDef = sh(
				returnStdout: true,
				script: 'find ' + sdir + ' -mindepth 1 -maxdepth 2 -type f -name "*.Dockerfile" -exec basename "{}" \\; | grep -Po "^[^.]+" | sort | uniq'
			).split('\n')

			echo "Distributions defined in workspace:\n" + distsDef.join("\n")
		}
	}

	// Define parameters and their default values
	properties([
		parameters([
			booleanParam(name: 'extended', defaultValue: false, description: 'Enable extended stages (requires extended lib)'),
			textParam(name: 'listStages', defaultValue: stagesDef.join("\n"), description: 'List of stages to go through (when relevant)'),
			textParam(name: 'listFlags', defaultValue: flagsDef.join("\n"), description: 'List of custom flags to be set'),
			textParam(name: 'listDists', defaultValue: distsDef.join("\n"), description: 'List of distribution to use for this build'),
			string(name: 'labelDocker', defaultValue: 'docker', description: 'Label of node(s) to run docker steps'),
			//			string(name: 'lbMacOS10', defaultValue: 'mac', description: 'Node label for Mac OS X 10'),
			//			string(name: 'lbWindows10', defaultValue: 'windows', description: 'Node label for Windows 10'),
			// Parameters to load the extended library
			string(name: 'libExtRemote', defaultValue: 'https://github.com/digital-me/jenkins-lib-lazy-ext.git', description: 'Git URL of the extended shared library'),
			string(name: 'libExtBranch', defaultValue: 'master', description: 'Git branch for the Extended shared library'),
			string(name: 'libExtCredId', defaultValue: 'none', description: 'Credentials to access the Extended shared library'),
			choice(name: 'verbose', choices: ['1', '2', '0'].join("\n"), defaultValue: '1', description: 'Control verbosity (where implemented)'),
		])
	])

	// Instanciate a configuration object based on the parameters
	def config = new plConfig(name, sdir, params, env.BRANCH_NAME)

	// Load Extended library if available and update configuration accordingly
	echo 'Trying to load Extended library...'
	try {
		library(
				identifier: "libExt@${params.libExtBranch}",
				retriever: modernSCM([
					$class: 'GitSCMSource',
					remote: params.libExtRemote,
					credentialsId: params.libExtCredId
				])
				)
		echo 'Extended shared library loaded: extended features are supported'
	} catch (error) {
		echo 'Extended shared library could NOT be loaded: extended features are disabled'
		if(config.verbose) {
			echo "Warning message:\n${error.message}"
		}
		config.extended = false
	}

	return config
}
