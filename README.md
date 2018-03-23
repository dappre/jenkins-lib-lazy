# Jenkins Lib Lazy
Re-usable shared library to setup lazy Jenkins pipelines

### Scope
LazyLib is a collection of global methods, classes and resources to ease the maintenance of Jenkins pipelines.
The primary goal was to make pipelines from different projects looks pretty much the same,
so they could be easier to understand, design and mostly maintain.
For instance, if a bunch of your projects use a common shell script of Dockerfile, chances are you will either
- have to update it in every project separately
- implement some step to provision it from a shared lib
- or both depending if you need to test a specific version of it for one project   

This lazyLib components are:
- lazyConfig: wrapper for properties and parameters steps
- lazyStage: wrapper for stage step using lazyNode in parallel
- lazyNode: wrapper for node step using lazyDocker and lazyStep
- lazyDocker: wrapper for docker steps using lazyStep with dynamic resolution of Dockerfile
- lazyStep: wrapper for shells or any other steps with dynammic resolution of scripts


### TODO
1. Support single branch pipeline jobs, maybe disabling dynamic parameters
1. Document properly config, env and dynamic parameters
1. Allow failFast to be defined per config and/or per stage
1. Avoid required permissions when possible


### Required permissions:
- staticMethod java.lang.Character toUpperCase char
- method java.lang.Class isInstance java.lang.Object
- staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods invokeMethod java.lang.Object
- staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods contains java.lang.Object[] java.lang.Object
