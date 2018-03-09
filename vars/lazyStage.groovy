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

	params.tasks.each { t ->
		if (t.body instanceof Closure) {
			t.body.call()
		} else if (t.body instanceof List) {
			t.body.each { st ->
				echo "Need to do ${st}"
			}
		} else if (t.body instanceof String) {
			echo "Need to do ${t.body}"
		} else {
			error "No idea what to do with ${t.body}"
		}
	}
}
