package com.criticollab.osgi.mavenindex.persist;/**
 * Created by ses on 2/22/17.
 */

import aQute.bnd.version.MavenVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

@Embeddable
public class Version implements Comparable<Version> {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(Version.class);
    private Integer major;
    private Integer minor;
    private Integer micro;
    private String qualifier;
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
            try {
                v = MavenVersion.parseString(versionString).getOSGiVersion();
            } catch (Exception e1) {
                logger.error("Bad version format: " + versionString,
                             e1); //To change body of catch statement use File | Settings | File Templates.
            }
        }
        setVersion(v);

    }

    @Column()
    public Integer getMajor() {
        return major;
    }

    public void setMajor(Integer major) {
        bndVersion = null;
        this.major = major;
    }

    @Column()
    public Integer getMinor() {
        return minor;
    }

    public void setMinor(Integer minor) {
        bndVersion = null;
        this.minor = minor;
    }

    @Column()
    public Integer getMicro() {
        return micro;
    }

    public void setMicro(Integer micro) {
        bndVersion = null;
        this.micro = micro;
    }

    @Column(length = 8192)
    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        bndVersion = null;
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
        if (bndVersion != null) {
            this.major = bndVersion.getMajor();
            this.minor = bndVersion.getMinor();
            this.micro = bndVersion.getMicro();
            this.qualifier = bndVersion.getQualifier();
            this.bndVersion = bndVersion;
        }
    }

    @Override
    public String toString() {
        return getVersion().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Version version = (Version) o;

        if (major != null ? !major.equals(version.major) : version.major != null) return false;
        if (minor != null ? !minor.equals(version.minor) : version.minor != null) return false;
        if (micro != null ? !micro.equals(version.micro) : version.micro != null) return false;
        if (qualifier != null ? !qualifier.equals(version.qualifier) : version.qualifier != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = major != null ? major.hashCode() : 0;
        result = 31 * result + (minor != null ? minor.hashCode() : 0);
        result = 31 * result + (micro != null ? micro.hashCode() : 0);
        result = 31 * result + (qualifier != null ? qualifier.hashCode() : 0);
        return result;
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
