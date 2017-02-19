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

}
