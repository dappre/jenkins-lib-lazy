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

// Function to copy Dockerfile from lib to workspace if needed and build the image
def buildImage(stage, label, args = '', filename = 'Dockerfile') {
    logger.debug('buildImage', 'Retrieving config')
    def config = lazyConfig()

    logger.debug('buildImage', 'Collect Dockerfile from repo or lib image')
    def dstDockerfile = lazyRes(stage, [ filename, ], label)[0]

    logger.debug('buildImage', 'Get uid of current UID and GID to build docker image')
    // This will allow Jenkins to manipulate content generated within Docker
    def uid = sh(returnStdout: true, script: 'id -u').trim()
    def gid = sh(returnStdout: true, script: 'id -g').trim()

    logger.debug('buildImage', 'Build and return Docker image')
    withEnv(["UID=${uid}", "GID=${gid}"]) {
        return docker.build(
            "${config.name}-${stage}-${label}:${config.branch}",
            "--build-arg dir=${stage} --build-arg uid=${env.UID} --build-arg gid=${env.GID} -f ${config.dir}/${dstDockerfile} ${config.dir}"
        )
    }
}

def call (stage, task, label, args = '') {
    logger.debug('Retrieving global config')
    def config = lazyConfig()

    logger.debug('Collect steps from tasks for further execution')
    def steps = lazyStep(stage, task, label)
    
    logger.debug('Build the relevant Docker image')
    def imgDocker = buildImage(stage, label)

    // Because of https://issues.jenkins-ci.org/browse/JENKINS-49076
    // And related to https://issues.jenkins-ci.org/browse/JENKINS-54389
    envList = []
    envPath = ''
	logger.debug("Inspecting docker image to retrieve entrypoint")
	entryPointStr = sh(returnStdout: true, script: "docker inspect -f '{{.ContainerConfig.Entrypoint}}' ${config.name}-${stage}-${label}:${config.branch}").trim()
	entryPoint = entryPointStr.substring(1, entryPointStr.length()-1).split(',')
	logger.trace("Entrypoint = ${entryPoint}")
    logger.debug('Extract missing environment variables')
    imgDocker.inside(args) {
        envRaw = sh(returnStdout: true, script: "${entryPoint.join(' ')} env").trim()
    }
    logger.trace("Extracted environment variables =\n" + envRaw)
    envArr = envRaw.split('\n')
    logger.debug("Extracted ${envArr.size()} environment variables")
    if (envArr.size()) envArr.each { envStr ->
        logger.trace("Environment string to parse = ${envStr}")
        if (envStr ==~ /^PATH=.+/) {
            logger.debug("Extract PATH environment variable from container")
            envPath = envStr
            logger.trace("Variable ${envStr}")
        } else if (envStr ==~ /^[^=\s]+=.*/) {
            logger.debug("Adding a new environment variable")
            varName = envStr.split('=')[0]
            logger.trace("Adding environment variable ${envStr}")
            envList << envStr
        } else if (envList.size()) {
            logger.debug("Appending more data to last environment variable")
            varIndex = envList.size() - 1
            varName = envList[varIndex].split('=')[0]
            envList[varIndex] += '\n' + envStr
            logger.trace("Updating environment variable ${envList[varIndex]}")
        } else {
            logger.warn("No clue what to do with this string: ${envStr}")
        }
    }
    if (envPath) {
      logger.debug('Overwrite PATH from extract environment variables')
      args += " -e ${envPath}"
    }

    logger.debug('Run each shell scripts as task inside the Docker')
    imgDocker.inside(args) {
        withEnv(envList as List) {
        logger.debug("Calling each of the ${steps.size()} steps")
            steps.each { step ->
                logger.trace("Current step = ${step.toString()}")
                if (config.timestampsLog) {
                    logger.debug('Enable timestamps for this step')
                    timestamps {
                        step()
                    }
                } else {
                    step()
                }
            }
        }
    }

    logger.debug('All steps have been called')
}
