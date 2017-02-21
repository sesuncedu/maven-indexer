package com.criticollab.osgi.mavenindex.persist;/**
 * Created by ses on 2/18/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.QueryHint;
import java.util.HashSet;
import java.util.Set;

@NamedQuery(name = "artifactByName", query = "from Artifact where name=:name", hints = {
        @QueryHint(name = "javax.persistence.cache.retrieveMode", value = "USE"),
        @QueryHint(name = "javax.persistence.cache.storeMode", value = "REFRESH"),
        @QueryHint(name = "org.hibernate.cacheable", value = "true")})
@Entity
public class Artifact {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(Artifact.class);

    private int id;

    private MavenGroup mavenGroup;
    private String name;
    private Set<ArtifactVersion> versions = new HashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(optional = false)
    public MavenGroup getMavenGroup() {
        return mavenGroup;
    }

    public void setMavenGroup(MavenGroup mavenGroup) {
        this.mavenGroup = mavenGroup;
    }

    @Column(nullable = false, unique = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @OneToMany(mappedBy = "artifact")
    public Set<ArtifactVersion> getVersions() {
        return versions;
    }

    public void setVersions(Set<ArtifactVersion> versions) {
        this.versions = versions;
    }
}
