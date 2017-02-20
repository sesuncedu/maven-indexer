package com.criticollab.osgi.mavenindex.persist;/**
 * Created by ses on 2/18/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class ArtifactVersion {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(ArtifactVersion.class);

    private int id;
    private Artifact artifact;
    private String version;
    private Set<MavenResource> resources;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Set<MavenResource> getResources() {
        return resources;
    }

    public void setResources(Set<MavenResource> resources) {
        this.resources = resources;
    }
}
