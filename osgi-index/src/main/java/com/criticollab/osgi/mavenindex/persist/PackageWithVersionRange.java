package com.criticollab.osgi.mavenindex.persist;/**
 * Created by ses on 2/22/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Embedded;
import javax.persistence.Entity;

@Entity
public class PackageWithVersionRange extends Package {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(PackageWithVersionRange.class);

    private VersionRange versionRange;


    @Embedded
    public VersionRange getVersionRange() {
        return versionRange;
    }

    public void setVersionRange(VersionRange versionRange) {
        this.versionRange = versionRange;
    }

}
