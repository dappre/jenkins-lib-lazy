#!/usr/bin/env groovy

def call (body) {
	def params = [
		name:		null,
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

	params.tasks.each { task ->
		// Prepare dists to be used for this task bloc
		def dists = []
		if (task.on == '*') {
			// TODO: populate from config 
			dists = [ 'all', 'dist', 'available', ]
		} else {
			dists += task.on
		}

		// Collect steps to be executed
		def steps = []
		if (task.exec instanceof Closure) {
			steps += task.exec
		} else if (task.exec instanceof List) {
			task.exec.each { subtask ->
				steps += { sh name + '/' + subtask.exec }
			}
		} else if (task.exec instanceof String) {
			steps += { sh params.name + '/' + task.exec }
		} else {
			error "No idea what to do with ${task.exec}"
		}

		// Execute steps inside or outside dists
		dists.each { dist ->
			echo "Need to exec the following steps inside (${dist})"
			steps.each { step ->
				step()
			}
			echo "Done"
		}
	}
}
