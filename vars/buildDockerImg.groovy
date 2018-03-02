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

// Function to copy Dockerfile from lib to workspace if needed and build the image
def call(name, config, dist, filename = 'Dockerfile') {
	def dstDockerfile = "./${name}/${filename}"

	// Enter sub-folder where Dockerfiles and scripts are located
	dir(config.sdir) {
		// Lookup fo the relevant Dockerfile in sub workspace first
		def srcDockerfile = sh(
			returnStdout: true,
			script: "ls -1 ${name}/${dist}.Dockerfile 2> /dev/null || ls -1 ${dist}.Dockerfile 2> /dev/null || echo"
		).trim()

		def contentDockerfile = ''
		if (srcDockerfile != null && srcDockerfile != '') {
			// Read Dockerfile from workspace if existing
			contentDockerfile = readFile(srcDockerfile)
		} else {
			// Extract Dockerfile from shared lib
			try {
				contentDockerfile = libraryResource("${config.sdir}/${name}/${dist}.Dockerfile")
			} catch (hudson.AbortException e) {
				contentDockerfile = libraryResource("${config.sdir}/${dist}.Dockerfile")
			}
		}

		// Write the selected Dockerfile to workspace sub-folder
		writeFile(
			file: dstDockerfile,
			text: contentDockerfile
		)
	}

	return docker.build("${config.name}-${name}-${dist}:${config.branch}", "--build-arg dir=${name} -f ${config.sdir}/${dstDockerfile} ${config.sdir}")
}
