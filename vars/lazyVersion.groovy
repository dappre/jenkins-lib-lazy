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

// Get version from text
@NonCPS
def match(text, regexp, part = 0) {
	logger.debug('match', 'Getting version value from text')
    def matcher = text =~ regexp
	def group = matcher ? matcher[0] : null
	if (group) {
        logger.trace('match', "Matcher for '${regexp}' in text returned ${matcher}")
	} else {
        logger.warn('match', "Could not match '${regexp}' in text")
	}
    if (part) {
        return group[part]
    } else {
        return group
    }
}

// Get version from file
def get(filePath, regexp) {
	logger.debug('get', "Getting version out of ${filePath} in workspace")
    def fileContent = readFile(encoding: 'UTF-8', file: filePath)
    def version = match(fileContent, regexp, 2)
    if (!version) {
        logger.warn('get', "Could not get version matching '${regexp}' in ${filePath}")
    }
	return version
}

// Set version
def set(filePath, regexp, version) {
	logger.debug('set', "Setting version inside ${filePath} in workspace")
    logger.trace('set', "Prepare to change version = ${version}")
    def fileContent = readFile(encoding: 'UTF-8', file: filePath)
    def gVersion = match(fileContent, regexp, 0)
    logger.debug('set', "Replacing version in ${configFilePath}")
    fileContent = fileContent.replace(gVersion[0], gVersion[1] + version + gVersion[3])
    logger.debug('set', "Writing new ${filePath} in workspace")
    writeFile(encoding: 'UTF-8', file: filePath, text: fileContent)
}

// Lookup default filePath and regexp for known types
def getDefault(type) {
	// TODO
}
// Call setter if type and version is passed as argument
def call(type, version) {
    logger.debug('Called with 3 arguments')
    set(getDefault(type), version)
}

// Call getter if type is passed as argument
def call(type) {
    logger.debug('Called with 2 argument')
    return get(getDefault(type))
}
