package com.criticollab.osgi.mavenindex.persist;/**
 * Created by ses on 2/18/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@NamedQuery(name = "artifactVersionByArtifactAndVersion", query = "from ArtifactVersion where artifact=:artifact and version=:version", hints = {
        @QueryHint(name = "javax.persistence.cache.retrieveMode", value = "USE"),
        @QueryHint(name = "javax.persistence.cache.storeMode", value = "REFRESH"),
        @QueryHint(name = "org.hibernate.cacheable", value = "true")})
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"artifact_id", "version"})})
@Entity
public class ArtifactVersion {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(ArtifactVersion.class);

    private int id;
    private Artifact artifact;
    private String version;
    private Set<MavenResource> resources = new HashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(targetEntity = Artifact.class)
    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    @Column(nullable = false)
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @OneToMany(mappedBy = "artifactVersion")
    public Set<MavenResource> getResources() {
        return resources;
    }

    public void setResources(Set<MavenResource> resources) {
        this.resources = resources;
    }
}
