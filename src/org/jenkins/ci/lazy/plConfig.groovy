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

package org.jenkins.ci.lazy

class plConfig implements Serializable {
	// Project name
	protected String name
	// Script dir
	protected String sdir
	// Flag for the extended lib support
	protected Boolean extended = false
	// Stage list to execute
	protected ArrayList stages
	// Flag list to be set
	protected ArrayList flags
	// Distro list to use
	protected ArrayList dists
	// Lable list to use
	protected Map labels = [
		docker:		'docker',
	];
	// Other options
	protected Integer verbose	= 1
	protected String branch		= 'master'

	
	public plConfig(String n = null, String d = null, Map p, String b = null) {
		name		= n
		sdir		= d
		extended 	= extended
		dists		= p.listDists.split("\n")
		stages		= p.listStages.split("\n")
		labels = [
			docker:		p.labelDocker,
		];
		verbose		= p.verbose as Integer
		flags		= p.listFlags.split("\n")
		if (b) {
			branch		= b
		}
	}

	public update(String n, String d, Map p, String b = null) {
		name		= n
		sdir		= d
		if (b) {
			branch		= b
		}
	}
}