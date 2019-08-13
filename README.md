# Jenkins lazyLib
Re-usable shared library to setup lazy Jenkins pipelines

## Scope
The lazyLib is a collection of global methods, classes and resources to ease the maintenance of Jenkins pipelines.

The primary goal is to make pipelines from different projects look pretty much the same,
so they could be easier to understand, design and mostly maintain.

The secondary goal is to reduce duplication of the code used by pipelines from similar projects.

For instance, if a bunch of projects use a common shell script and/or Dockerfile,
chances are it will be require to:
- update those in every project separately
- implement some step to provision those from a shared lib or a single repo
- do both if you have to add and test a specific feature for one project

So the idea of lazyLib is to define some steps/scripts to be run on some nodes,
possibly inside containers, without having to re-invent the wheel each time.

## Components
- [lazyConfig](vars/lazyConfig.groovy): wrapper for properties and parameters steps
- [lazyStage](vars/lazyStage.groovy): wrapper for stage step using lazyNode in parallel
- [lazyNode](vars/lazyNode.groovy): wrapper for node step using lazyDocker and lazyStep
- [lazyDocker](vars/lazyDocker.groovy): wrapper for docker steps using lazyStep, with [dynamic resolution of Dockerfiles](#docker-images)
- [lazyStep](vars/lazyStep.groovy): wrapper for any steps or shell scripts, with [dynamic resolution of script files](#shell-scripts)
- [lazyGitPass](vars/lazyGitPass.groovy): wrapper for git commands using user and password credential
- [lazyLogger](src/org/jenkins/ci/lazy/Logger.groovy): support levels of logging for the above components

Only the two first have to be used in the Jenkinsfile.
The others are either dependencies or optional.

## Requirements

- Jenkins v2.89+ on Linux (Mac should work, but not 'pure' Windows because depending on shell)
- Docker 1.12.6+ to use lazyDocker
- Jenkins security [permissions](#required-permissions)

## Usage

1. Load the lazyLib from Jenkinsfile:
```groovy
def libLazy = [
    remote:           'https://github.com/digital-me/jenkins-lib-lazy.git',
    branch:           'master',
    credentialsId:    null,
]

library(
    identifier: "libLazy@${libLazy.branch}",
    retriever: modernSCM([
        $class: 'GitSCMSource',
        remote: libLazy.remote,
        credentialsId: libLazy.credentialsId
    ])
)
```
Load optional custom and or extern libraries if required (same way as above).

2. Configure the pipeline with lazyConfig
```groovy
lazyConfig(
    name:     '<pipeline_name>',
    dir:      '<script_dir>',
    inLabels: [ '<docker_label1>', ...],
    onLabels: [ <node_label1>:<agent_label1>, ...],
)
```
See [lazyConfig](vars/lazyConfig.groovy) for all the available options.
Most of wich will be available as parameters if the Job is manually triggered 

3. Define stages with lazyStage
```groovy
lazyStage {
    name = '<stage_name>'
    tasks = [ run: { step1(<args>); ... }, on: [ '<node_label>',] , ]
}
```

## Advanced features
#### Multi tasks
It is possible to pass a List of task rather than a single Map:
```groovy
lazyStage {
    name = '<stage_name>'
    tasks = [
        [ run: { step1(<args>); ... }, on: [ '<node_label1>', ], ]
        [ run: { step2(<args>); ... }, on: [ '<node_label2>', ], ]
        ...
    ]
}
```

#### Shell scripts
It is possible to pass a List of script (or a single ono as a String) rather than a Closure to run:
```groovy
lazyStage {
    name = '<stage_name>'
    tasks = [ run: [ '<script>', ... ], on: [ '<node_label>', ], ]
    ]
}
```
In this case, lazyLib will lookup for each script in the following locations,
first in the local repo, then in the resource path of any loaded library:

1. `<repo_root>/<lazyDir>/<stage_name>/<node_label>.<script_name>`
2. `<repo_root>/<lazyDir>/<stage_name>/<script_name>`
3. `<repo_root>/<lazyDir>/<node_label>.<script_name>`
4. `<repo_root>/<lazyDir>/<script_name>`
5. `<lib_resources>/<lazyDir>/<stage_name>/<node_label>.<script_name>`
6. `<lib_resources>/<lazyDir>/<stage_name>/<script_name>`
7. `<lib_resources>/<lazyDir>/<node_label>.<script_name>`
8. `<lib_resources>/<lazyDir>/<script_name>`

*REM*: In case from 3 to 8, the script will be copied under `lazyDir/<stage_name>/` in the workspace.

Each resolved script will then be executed in sequence, using their absotute path from the workspace,
and no argument (yet) but with the following environment variables:
- `lazyLabel="<node_label>"`
- `lazyDir='lazyDir'` or whatever directory has been configured from [lazyConfig](vars/lazyConfig.groovy)
- `lazyStage="<stage_name>"`

Those environment variables come in addition of all the one from the configuration/parameters (i.e.: lazyEnv).

#### Docker images
Steps and scripts can be run inside Docker too.
```groovy
lazyStage {
    name = '<stage_name>'
    tasks = [
        pre: { pre_step(<args> },
        run: '<script_name>',
        post: post_step(<args>),
        in: [ '<docker_label>', ... ],
        on: '<node_label>',
    ]
}
```
Each Dockerfile will be looked up and copied same way as for the shell scripts:

1. `<repo_root>/<lazyDir>/<stage_name>/<docker_label>.Dockerfile`
2. `<repo_root><lazyDir>/<docker_label>.Dockerfile`
3. `<lib_resources>/<lazyDir>/<stage_name>/<docker_label>.Dockerfile`
4. `<lib_resources>/<lazyDir>/<docker_label>.Dockerfile`

*Important*: the resolution of the script path will also use <docker_label> in place of <node_label>. So will the lazyLabel variable.

Additionaly, the pre and post Closure steps will be executed outside the container, respectively before and after.

#### Input steps
You can ask for user input per stage (before entering the node step):
```groovy
lazyStage {
    name = '<stage_name>'
    input = [
            message: '<input_message>',
            parameters: [string(defaultValue: '', description: '<input_description>', name: '<input_name>')]
        ]
    tasks = [
        [ run: { step1(env.lazyInput, <args>); ... }, on: '<node_label>', ]
    ]
}
```
For each tasks, the environment variable lazyInput will hold the object created by the input step call with the input object defined avove.
In case of a single parameter, it will just hold its value. If more than one, it will hold a map. See Jenkins documentation for more info.


### TODO
- Improve documentation of config, env and parameters, as it can be confusing
- Add support for Fastlane with bundler
- Add support for Maven and Java
- Support single branch pipeline jobs, maybe disabling dynamic parameters
- Allow failFast to be defined per config and/or per stage
- Add examples/templates, possibly in separated repos
- Avoid required permissions if/when possible
- Add support for Windows nodes

### Required permissions:
- staticMethod java.lang.Character toUpperCase char - to make the stage nam prettier in lazyStage
- method java.lang.Class isInstance java.lang.Object - to be flexible with tasks in lazyStage, with run in lazyStep and json parsing in lazyConfig 
- staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods invokeMethod java.lang.Object
- staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods contains java.lang.Object[] java.lang.Object
- method groovy.json.JsonOutput prettyPrint java.lang.String - to print pretty Json for default value of parameters in lazyConfig
- method groovy.json.JsonOutput toJson java.lang.Object - to generate Json for default value of parameters in lazyConfig
- method groovy.json.JsonOutput toJson java.util.Map - to generate Json for default value of parameters in lazyConfig
- method groovy.json.JsonOutput toJson java.util.UUID - to generate Json for default value of parameters in lazyConfig
- method groovy.json.JsonSlurper setType groovy.json.JsonParserType - to parse Json from parameters in lazyConfig
