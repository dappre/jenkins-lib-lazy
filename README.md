# Jenkins Lib Lazy
Re-usable shared library to setup lazy Jenkins pipelines

## Scope
LazyLib is a collection of global methods, classes and resources to ease the maintenance of Jenkins pipelines.
The primary goal was to make pipelines from different projects looks pretty much the same,
so they could be easier to understand, design and mostly maintain.
For instance, if a bunch of your projects use a common shell script of Dockerfile, chances are you will either
- have to update it in every project separately
- implement some step to provision it from a shared lib
- or both depending if you need to test a specific version of it for one project   

## Components
- lazyConfig: wrapper for properties and parameters steps
- lazyStage: wrapper for stage step using lazyNode in parallel
- lazyNode: wrapper for node step using lazyDocker and lazyStep
- lazyDocker: wrapper for docker steps using lazyStep with dynamic resolution of Dockerfile
- lazyStep: wrapper for shells or any other steps with dynammic resolution of scripts
- lazyGitPass: wrapper for git command using user/password credential
- lazyLogger: support levels of logging for the above components

## Basic usage

1. Load the lazyLib from Jenkinsfile:
```groovy
def libCmn = [
    remote:           'https://code.in.digital-me.nl/git/DEVops/JenkinsLibLazy.git',
    branch:           'master',
    credentialsId:    null,
]

library(
    identifier: "libCmn@${libCmn.branch}",
    retriever: modernSCM([
        $class: 'GitSCMSource',
        remote: libCmn.remote,
        credentialsId: libCmn.credentialsId
    ])
)
```
Load optional custom and or extern libraries if required (same way as above).

2. Configure the pipeline with lazyConfig
```groovy
lazyConfig(
    name: '<pipeline_name>',
)
```
See lazyConfig for all the available options.
Most of wich will be available as parameters if the Job is manually triggered 

3. Define stages with lazyStage
```groovy
lazyStage {
    name = '<stage_name>'
    tasks = [ run: { step1(<args>); ... }, on: '<node_label>', ]
}
```

## Advanced usage
..* Multi tasks
It is possible to pass a List of task rather than a single Map:
```groovy
lazyStage {
    name = '<stage_name>'
    tasks = [
        [ run: { step1(<args>); ... }, on: '<node_label1>', ]
        [ run: { step2(<args>); ... }, on: '<node_label2>', ]
        ...
    ]
}
```

#### Shell scripts
It is possible to pass a List of script (or a single on as a String) rather than a Closure to be run:
```groovy
lazyStage {
    name = '<stage_name>'
    tasks = [ run: [ '<script1.sh>', ... ], on: '<node_label>', ]
    ]
}

```
In this case, lazyLib will lookup for each script in the following locations,
first in the local repo, then in the resource path of any loaded library.
1. <repo_root>/lazyDir/<stage_name>/
2. <repo_root>lazyDir/
3. <lib_resources>/lazyDir/<stage_name>/
4. <lib_resources>/lazyDir/
In case from 2 to 4, the script will be copied in /lazyDir/<stage_name>/ in the workspace.

#### Docker images
Steps and scripts can be run inside Docker too.
Each Dockerfile will be lookup and copied same way as described for the shell scripts above.
```groovy
lazyStage {
    name = '<stage_name>'
    tasks = [
        pre: { pre_step(<args> },
        run: '<script1.sh>',
        post: post_step(<args>),
        in: [ '<docker_label1>', ... ],
        on: '<node_label>',
    ]
}

```

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
- Document properly config, env and dynamic parameters
- Add support for Fastlane with bundler
- Add support for Maven and Java 
- Support single branch pipeline jobs, maybe disabling dynamic parameters
- Allow failFast to be defined per config and/or per stage
- Avoid required permissions if/when possible


### Required permissions:
- staticMethod java.lang.Character toUpperCase char
- method java.lang.Class isInstance java.lang.Object
- staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods invokeMethod java.lang.Object
- staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods contains java.lang.Object[] java.lang.Object
