package com.criticollab.osgi.mavenindex.persist;/**
 * Created by ses on 2/18/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.Collection;


@Entity
public class Bundle {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(Bundle.class);

    private int id;
    private String symbolicName;
    private String name;

    private Collection<BundleVersion> bundleVersions;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(nullable = false, unique = true)
    public String getSymbolicName() {
        return symbolicName;
    }

    public void setSymbolicName(String symbolicName) {
        this.symbolicName = symbolicName;
    }

    @Basic
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @OneToMany(mappedBy = "bundle")
    public Collection<BundleVersion> getBundleVersions() {
        return bundleVersions;
    }

    public void setBundleVersions(Collection<BundleVersion> bundleVersions) {
        this.bundleVersions = bundleVersions;
    }
}
