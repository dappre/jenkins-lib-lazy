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

// Function to extract a resource from different possible paths in the shared libs
def libraryResource(ArrayList paths) {
	logger.debug('libraryResource', 'Extract resource from shared lib')
	def resContent = ''
	for ( path in paths ) {
		try {
			logger.debug('libraryResource', "Looking for resource with path = ${path}")
        	resContent = libraryResource(path)
		    break
		} catch (hudson.AbortException e) {
			logger.debug('libraryResource', "Resource not found with path =  ${path}")
		}
	}
	return  resContent
}

// Function to prepare and list resources (copy from lib to workspace if needed)
def call(stage, resources, label) {
    logger.debug('Retrieving config')
    def config = lazyConfig()

    def resList = []

    logger.debug('Enter sub-folder where resources are located')
    dir("${config.dir}") {
        resources.each { res ->
            def resDst = "${stage}/${res}"    // Default resource location
            logger.debug('Lookup fo the relevant resource in sub workspace first')
            // TODO: Rework to use fileExists?
            def resSrc = sh(
                returnStdout: true,
                script: "ls -1 ${stage}/${label}.${res} 2> /dev/null || ls -1 ${stage}/${res} 2> /dev/null || ls -1 ${label}.${res} 2> /dev/null || ls -1 ${res} 2> /dev/null || echo"
            ).trim()

            if (resSrc != null && resSrc != '') {
                logger.debug('Use resource from workspace since existing')
                resDst = resSrc
            } else {
                logger.debug('Extract resource from shared lib')
                def resContent = libraryResource([ "${config.dir}/${stage}/${label}.${res}", "${config.dir}/${stage}/${res}", "${config.dir}/${label}.${res}", "${config.dir}/${res}", ])

                logger.debug('Write the selected resource to workspace sub-folder')
				logger.trace("Copy res = ${res} from library to resDst = ${resDst}")
                writeFile(
                    file: resDst,
                    text: resContent
                )
            }

            logger.debug('Add the path to the resource in the final list to be returned')
            logger.trace("Resource list content is now = ${resList.toString()}")
            resList += resDst
        }
    }

    logger.trace("Found ${resList.size()} resources = ${resList}")
    
    return resList
}
