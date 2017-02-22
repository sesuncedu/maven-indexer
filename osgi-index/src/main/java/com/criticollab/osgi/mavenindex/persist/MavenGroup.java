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
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.QueryHint;
import java.util.HashSet;
import java.util.Set;

@NamedQuery(name = "groupByName", query = "from MavenGroup where name=:name", hints = {
        @QueryHint(name = "javax.persistence.cache.retrieveMode", value = "USE"),
        @QueryHint(name = "javax.persistence.cache.storeMode", value = "REFRESH"),
        @QueryHint(name = "org.hibernate.cacheable", value = "true")})
//@Entity
public class MavenGroup {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(MavenGroup.class);

    private int id;
    private String name;

    private Set<Artifact> artifacts = new HashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(nullable = false, unique = true, length = 8192)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @OneToMany(mappedBy = "mavenGroup")
    public Set<Artifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(Set<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    @Override
    public String toString() {
        return "#<MavenGroup " + getName() + ">";
    }
}
