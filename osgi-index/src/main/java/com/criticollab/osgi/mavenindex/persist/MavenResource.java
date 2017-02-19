package com.criticollab.osgi.mavenindex.persist;/**
 * Created by ses on 2/18/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenResource {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(MavenResource.class);

    private int id;

    private ArtifactVersion artifactVersion;
    private BundleVersion bundleVersion;

}
