package com.criticollab.osgi.mavenindex.persist;/**
 * Created by ses on 2/22/17.
 */

import aQute.bnd.version.MavenVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Basic;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

@Embeddable
public class Version implements Comparable<Version> {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(Version.class);
    private int major = 0;
    private int minor = 0;
    private int micro = 0;
    private String qualifier = "";
    private transient aQute.bnd.version.Version bndVersion;

    public Version() {
    }

    public Version(int major, int minor, int micro, String qualifier) {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.qualifier = qualifier;
    }

    public Version(aQute.bnd.version.Version bndVersion) {
        setVersion(bndVersion);
    }


    public Version(String versionString) {
        aQute.bnd.version.Version v = null;
        try {
            v = aQute.bnd.version.Version.parseVersion(versionString);
        } catch (IllegalArgumentException e) {
            v = MavenVersion.parseString(versionString).getOSGiVersion();
        }

    }

    @Basic(optional = false)
    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    @Basic(optional = false)
    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    @Basic(optional = false)
    public int getMicro() {
        return micro;
    }

    public void setMicro(int micro) {
        this.micro = micro;
    }

    @Basic(optional = false)
    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    @Transient
    public aQute.bnd.version.Version getVersion() {
        if (bndVersion == null) {
            bndVersion = new aQute.bnd.version.Version(major, minor, micro, qualifier);
        }
        return bndVersion;
    }

    public void setVersion(aQute.bnd.version.Version bndVersion) {
        this.major = bndVersion.getMajor();
        this.minor = bndVersion.getMinor();
        this.micro = bndVersion.getMicro();
        this.qualifier = bndVersion.getQualifier();
        this.bndVersion = bndVersion;
    }

    @Override
    public String toString() {
        return getVersion().toString();
    }

    @Override
    public int hashCode() {
        return getVersion().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return getVersion().equals(obj);
    }

    @Override
    public int compareTo(Version o) {
        if (o == null) {
            return 1;
        }
        ;
        return getVersion().compareTo(o.getVersion());
    }
}
