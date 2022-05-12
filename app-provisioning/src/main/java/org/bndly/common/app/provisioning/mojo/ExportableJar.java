package org.bndly.common.app.provisioning.mojo;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.*;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.CollectingDependencyNodeVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ExportableJar {

    private final Dependency dependency;
    private final MavenSession mavenSession;
    private final MavenProject project;
    private final ArtifactResolver resolver;
    private final ProjectBuilder projectBuilder;
    private final DependencyGraphBuilder dependencyGraphBuilder;
    private final ArtifactFilter projectDependencyArtifactFilter;
    private final Log log;
    private final List<ExportableJar> transitiveExportedDependencies;
    private Artifact artifact;

    public ExportableJar(
            Dependency dependency,
            MavenSession mavenSession,
            MavenProject project,
            ArtifactResolver resolver,
            ProjectBuilder projectBuilder,
            DependencyGraphBuilder dependencyGraphBuilder,
            ArtifactFilter projectDependencyArtifactFilter,
            Log log
    ) {
        this.dependency = dependency;
        this.mavenSession = mavenSession;
        this.project = project;
        this.resolver = resolver;
        this.projectBuilder = projectBuilder;
        this.dependencyGraphBuilder = dependencyGraphBuilder;
        this.projectDependencyArtifactFilter = projectDependencyArtifactFilter;
        this.log = log;
        transitiveExportedDependencies = new ArrayList<>();
    }

    public ExportableJar init(Map<String, ExportableJar> exportableJars) throws MojoExecutionException {
        // resolve the artifact
        artifact = resolveArtifact(dependency);
        if (artifact == null || artifact.getFile() == null) {
            throw new MojoExecutionException("could not retrieve file location for " + dependency);
        }

        Collection<MavenProject> reactor = mavenSession.getProjects();
        ProjectBuildingRequest request = mavenSession.getProjectBuildingRequest();
        DefaultProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(request);
        ProjectBuildingResult result;
        try {
            result = projectBuilder.build(artifact, request);
        } catch (ProjectBuildingException e) {
            throw new MojoExecutionException("could not build maven project in order to resolve dependencies of dependency from " + artifact, e);
        }
        buildingRequest.setProject(result.getProject());

        DependencyNode graph;
        try {
            graph = dependencyGraphBuilder.buildDependencyGraph(project, projectDependencyArtifactFilter, reactor);
        } catch (DependencyGraphBuilderException e) {
            throw new MojoExecutionException("could not build dependency graph for artifact " + artifact, e);
        }
        CollectingDependencyNodeVisitor collector = new CollectingDependencyNodeVisitor();
        graph.accept(collector);
        for (DependencyNode node : collector.getNodes()) {
            String scope = node.getArtifact().getScope();
            if (!"compile".equals(scope) && !"runtime".equals(scope)) {
                log.debug("skip dependency " + node.getArtifact() + " because scope " + scope + " differs [compile,runtime]");
                continue;
            }
            String type = node.getArtifact().getType();
            if(!(type == null || "jar".equals(type))) {
                log.debug("skip dependency " + node.getArtifact() + " because type " + type + " differs [jar]");
                continue;
            }
            if (ArtifactUtils.key(artifact).equals(ArtifactUtils.key(node.getArtifact()))) {
                log.debug("skip dependency " + node.getArtifact());
            } else {
                log.debug("found transitive exported dependency " + node.getArtifact() + " for dependency " + dependency);
                ExportableJar dep = exportableJars.get(ArtifactUtils.versionlessKey(node.getArtifact()));
                if (dep == null) {
                    throw new MojoExecutionException("could not find exportable jar for " + node.getArtifact());
                }
                transitiveExportedDependencies.add(dep);
            }
        }
        return this;
    }

    public ExportableJar putInto(Map<String, ExportableJar> exportableJars) {
        exportableJars.put(ArtifactUtils.versionlessKey(getDependency().getGroupId(), getDependency().getArtifactId()), this);
        return this;
    }

    public Dependency getDependency() {
        return dependency;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    private final Artifact resolveArtifact(final Dependency dependency) throws MojoExecutionException {
        // find artifact of dependency
        DefaultArtifactHandler artifactHandler = new DefaultArtifactHandler("jar");
        artifactHandler.setLanguage("java");
        Artifact artifactTmp = new DefaultArtifact(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion(),
                dependency.getScope(),
                dependency.getType(),
                dependency.getClassifier(),
                artifactHandler
        );

        return resolveArtifact(artifactTmp);
    }

    private final Artifact resolveArtifact(final Artifact artifact) throws MojoExecutionException {
        // artifacts from the maven project object may have been resolved, but the file of the artifact is not yet attached.
        if (artifact.getFile() == null && artifact.isResolved()) {
            Artifact found = mavenSession.getLocalRepository().find(artifact);
            if (found != null && found.getFile() != null) {
                return found;
            }
        }
        ArtifactResolutionResult result = resolver.resolve(new ArtifactResolutionRequest()
                .setArtifact(artifact)
                .setRemoteRepositories(project.getRemoteArtifactRepositories())
                .setLocalRepository(mavenSession.getLocalRepository())
        );
        List<Exception> exceptions = result.getExceptions();
        if (exceptions != null && !exceptions.isEmpty()) {
            throw new MojoExecutionException("could not resolve artifact", exceptions.get(0));
        }
        Set<Artifact> resultArtifacts = result.getArtifacts();
        if (resultArtifacts.isEmpty()) {
            throw new MojoExecutionException("could not find artifact: " + artifact);
        }
        return resultArtifacts.iterator().next();
    }
}
