package com.criticollab.osgi.mavenindex.persist;/**
 * Created by ses on 2/18/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class Artifact {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(Artifact.class);

    private int id;

    private Group group;
    private String name;
    private Collection<ArtifactVersion> versions;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<ArtifactVersion> getVersions() {
        return versions;
    }

    public void setVersions(Collection<ArtifactVersion> versions) {
        this.versions = versions;
    }
}
