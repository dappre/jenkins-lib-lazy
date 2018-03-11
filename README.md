# Jenkins Lib Lazy
Re-usable shared library to setup lazy Jenkins pipelines

## TODO
1. Implement lazyPipeline using Closure argument and build stageList from it
2. Allow definition of distList from lazyPipeline to lazyConfig since it is likely project driven
3. Allow definition of all node label from env and paramters (i.e.: default, mac, android, windows, ...) 
4. Implement logging and replace verbose level
5. Avoid required permissions if possible
6. Allows flexible properties to be passed as env


## Required permissions:
- staticMethod java.lang.Character toUpperCase char
- method java.lang.Class isInstance java.lang.Object
- staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods invokeMethod java.lang.Object
- staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods contains java.lang.Object[] java.lang.Object
