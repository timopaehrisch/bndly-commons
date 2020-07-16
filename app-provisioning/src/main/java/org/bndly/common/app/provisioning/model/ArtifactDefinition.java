package org.bndly.common.app.provisioning.model;

/*-
 * #%L
 * App Provisioning
 * %%
 * Copyright (C) 2013 - 2020 Cybercon GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.Objects;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ArtifactDefinition {

	private final String groupId;
	private final String artifactId;
	private final String packaging;
	private final String classifier;
	private final String version;

	public static ArtifactDefinition fromDependency(Dependency dependency) {
		String depClassifier = dependency.getClassifier();
		if (depClassifier != null) {
			return new ArtifactDefinition(dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getType() + ":" + depClassifier + ":" + dependency.getVersion());
		} else {
			return new ArtifactDefinition(dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getType() + ":" + dependency.getVersion());
		}
	}

	public static ArtifactDefinition fromArtifact(Artifact artifact) {
		String depClassifier = artifact.getClassifier();
		if (depClassifier != null) {
			return new ArtifactDefinition(artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType() + ":" + depClassifier + ":" + artifact.getVersion());
		} else {
			return new ArtifactDefinition(artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType() + ":" + artifact.getVersion());
		}
	}
	
	public ArtifactDefinition(String artifactString) {
		String[] result = artifactString.split("\\:");
		if (result.length == 5) {
			//groupId:artifactId:packaging:classifier:version
			groupId = result[0];
			artifactId = result[1];
			packaging = result[2];
			classifier = result[3];
			version = result[4];
		} else if (result.length == 4) {
			//groupId:artifactId:packaging:version
			groupId = result[0];
			artifactId = result[1];
			packaging = result[2];
			classifier = null;
			version = result[3];
		} else {
			//groupId:artifactId:version
			groupId = result[0];
			artifactId = result[1];
			packaging = null;
			classifier = null;
			version = result[2];
		}
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getPackaging() {
		return packaging;
	}

	public String getClassifier() {
		return classifier;
	}

	public String getVersion() {
		return version;
	}

	public final ArtifactDefinition reduceToGroupdIdArtifactIdVersion() {
		return new ArtifactDefinition(groupId + ":" + artifactId + ":" + version);
	}
	
	@Override
	public int hashCode() {
		int hash = 5;
		hash = 29 * hash + Objects.hashCode(this.groupId);
		hash = 29 * hash + Objects.hashCode(this.artifactId);
		hash = 29 * hash + Objects.hashCode(this.packaging);
		hash = 29 * hash + Objects.hashCode(this.classifier);
		hash = 29 * hash + Objects.hashCode(this.version);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ArtifactDefinition other = (ArtifactDefinition) obj;
		if (!Objects.equals(this.groupId, other.groupId)) {
			return false;
		}
		if (!Objects.equals(this.artifactId, other.artifactId)) {
			return false;
		}
		if (!Objects.equals(this.packaging, other.packaging)) {
			return false;
		}
		if (!Objects.equals(this.classifier, other.classifier)) {
			return false;
		}
		if (!Objects.equals(this.version, other.version)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getGroupId()).append(":").append(getArtifactId());
		if (getPackaging() != null) {
			sb.append(":").append(getPackaging());
			if (getClassifier() != null) {
				sb.append(":").append(getClassifier());
			}
		}
		sb.append(":").append(getVersion());
		return sb.toString();
	}

}
