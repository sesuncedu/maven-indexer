package com.criticollab.osgi.mavenindex.persist;/**
 * Created by ses on 2/21/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.Set;

@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"groupId", "artifactId", "version"})})
@Entity
public class MavenArtifact {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(MavenArtifact.class);
    private int id;
    private String groupId;
    private String artifactId;
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

    @Column(nullable = false, length = 8192)
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Column(nullable = false)
    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    @Column(nullable = false)
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @OneToMany(mappedBy = "mavenArtifact")
    public Set<MavenResource> getResources() {
        return resources;
    }

    public void setResources(Set<MavenResource> resources) {
        this.resources = resources;
    }

    @Override
    public String toString() {
        return String.format("#<MavenArtifact(%d) %s:%s:%s", id, groupId, artifactId, version);
    }
}
