#!/usr/bin/env groovy

def call (body) {
	def params = [
		name:		null,
		config:		null,
		tasks:		[],
		dockerArgs:	'',
	]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = params
	body()

	def err = null
	if (!params.name) {
		err = 'Stage always needs a name'
	} else if (!params.tasks) {
		err = 'Stage always needs some tasks'
	}

	if (err) {
		error err
	}

	stage(Character.toUpperCase(params.name.charAt(0)).toString() + params.name.substring(1)) {
		params.tasks.each { task ->
			// Prepare dists to be used for this task bloc
			def dists = []
			if (task.on == '*') {
				// TODO: populate from config 
				dists = [ 'all', 'dist', 'available', ]
			} else {
				dists += task.on
			}
	
			// Execute steps inside or outside dists
			dists.each { dist ->
				node('master') {
					// Collect steps to be executed
					def steps = []
					if (task.exec instanceof Closure) {
						// If task is a Closuer, just add it in the step list
						steps += task.exec
					} else {
						// Prepare shell scripts from list or string
						def shTasks = null
						if (task.exec instanceof List) {
							shTasks = prepareShTasks(params.name, params.config, dist, task.exec)
						} else if (task.exec instanceof String) {
							shTasks = prepareShTasks(params.name, params.config, dist, [ task.exec ])
						} else {
							// Give up if not a Closure, not a List and not a String!
							error "No idea what to do with ${task.exec}"
						}
						// Collect all shel steps
						shTasks.each { shTask ->
							steps += { sh shTask }
						}
					}

					echo "Need to exec the following steps inside (${dist})"
					steps.each { step ->
						step()
					}
					echo "Done"
				}
			}
		}
	}
}
