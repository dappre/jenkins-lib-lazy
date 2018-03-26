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

    def dstDockerfile = "./${stage}/${filename}"
    logger.debug('buildImage', "Dockerfile will be saved in ${dstDockerfile}")
    
    logger.debug('buildImage', 'Enter sub-folder where Dockerfiles and scripts are located')
    dir(config.dir) {
        logger.debug('buildImage', 'Lookup fo the relevant Dockerfile in sub workspace first')
        def srcDockerfile = sh(
            returnStdout: true,
            script: "ls -1 ${stage}/${label}.Dockerfile 2> /dev/null || ls -1 ${label}.Dockerfile 2> /dev/null || echo"
        ).trim()

        def contentDockerfile = ''
        if (srcDockerfile != null && srcDockerfile != '') {
            logger.debug('buildImage', 'Read Dockerfile from workspace if existing')
            contentDockerfile = readFile(srcDockerfile)
        } else {
            logger.debug('buildImage', 'Extract Dockerfile from shared lib')
            try {
                contentDockerfile = libraryResource("${config.dir}/${stage}/${label}.Dockerfile")
            } catch (hudson.AbortException e) {
                contentDockerfile = libraryResource("${config.dir}/${label}.Dockerfile")
            }
        }

        logger.debug('buildImage', 'Write the selected Dockerfile to workspace sub-folder')
        writeFile(
            file: dstDockerfile,
            text: contentDockerfile
        )
    }

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

    logger.debug('Run each shell scripts as task inside the Docker')
    imgDocker.inside(args) {
        logger.debug("Calling each of the ${steps.size()} steps")
        steps.each { step ->
            logger.trace("Current step = ${step.toString()}")
            step()
        }
    }

    logger.debug('All steps have been called')
}
