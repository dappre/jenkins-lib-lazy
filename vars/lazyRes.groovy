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

// Function to prepare and list resources (copy from lib to workspace if needed)
def call(stage, resources, label) {
    logger.debug('Retrieving config')
    def config = lazyConfig()

    def resList = []

    logger.debug('Enter sub-folder where resources are located')
    dir("${config.dir}/${stage}") {
        resources.each { res ->
            def resDst = "${res}"    // Default resource location
            logger.debug('Lookup fo the relevant resource in sub workspace first')
            // TODO: Rework to use fileExists?
            def resSrc = sh(
                returnStdout: true,
                script: "ls -1 ${label}.${res} 2> /dev/null || ls -1 ${res} 2> /dev/null || echo"
            ).trim()

            if (resSrc != null && resSrc != '') {
                logger.debug('Use resource from workspace since existing')
                resDst = resSrc
            } else {
                logger.debug('Extract resource from shared lib')
                def resContent = ''
                try {
                    resContent = libraryResource("${config.dir}/${stage}/${label}.${res}")
                } catch (hudson.AbortException e) {
                    resContent = libraryResource("${config.dir}/${stage}/${res}")
                }

                logger.debug('Write the selected resource to workspace sub-folder')
                writeFile(
                    file: resSrc,
                    text: resContent
                )
            }

            logger.debug('Add the path to the resource in the final list to be returned')
            logger.debug("Resource list content is now = ${resList.toString()}")
            resList += resDst
        }
    }

    logger.trace("Found ${resList.size()} resources = ${resList}")
    
    return resList
}
