package com.criticollab.osgi.mavenindex.persist;/**
 * Created by ses on 2/18/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class Bundle {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(Bundle.class);

    private int id;
    private String symbolicName;
    private String name;

    private Collection<BundleVersion> bundleVersions;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public void setSymbolicName(String symbolicName) {
        this.symbolicName = symbolicName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<BundleVersion> getBundleVersions() {
        return bundleVersions;
    }

    public void setBundleVersions(Collection<BundleVersion> bundleVersions) {
        this.bundleVersions = bundleVersions;
    }
}
