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
}
