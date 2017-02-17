package com.criticollab.osgi.mavenindex.dto;/**
 * Created by ses on 2/16/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "MAVENRESOURCE", schema = "APP", catalog = "")
public class MavenresourceDTO {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(MavenresourceDTO.class);
    private int id;
    private Timestamp lastmodified;
    private String packaging;
    private String ext;
    private String sha1;
    private String resourcename;
    private String sha256;
    private MavenartifactversionDTO mavenartifactversionByMavenartifactversion;
    private MavenClassifierDTO mavenClassifierByClassifier;

    @Id
    @Column(name = "ID", nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "LASTMODIFIED", nullable = true)
    public Timestamp getLastmodified() {
        return lastmodified;
    }

    public void setLastmodified(Timestamp lastmodified) {
        this.lastmodified = lastmodified;
    }

    @Basic
    @Column(name = "PACKAGING", nullable = true, length = 256)
    public String getPackaging() {
        return packaging;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    @Basic
    @Column(name = "EXT", nullable = true, length = 256)
    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    @Basic
    @Column(name = "SHA1", nullable = true, length = 256)
    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    @Basic
    @Column(name = "RESOURCENAME", nullable = true, length = 1024)
    public String getResourcename() {
        return resourcename;
    }

    public void setResourcename(String resourcename) {
        this.resourcename = resourcename;
    }

    @Basic
    @Column(name = "SHA256", nullable = true, length = 64)
    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MavenresourceDTO that = (MavenresourceDTO) o;

        if (id != that.id) return false;
        if (lastmodified != null ? !lastmodified.equals(that.lastmodified) : that.lastmodified != null) return false;
        if (packaging != null ? !packaging.equals(that.packaging) : that.packaging != null) return false;
        if (ext != null ? !ext.equals(that.ext) : that.ext != null) return false;
        if (sha1 != null ? !sha1.equals(that.sha1) : that.sha1 != null) return false;
        if (resourcename != null ? !resourcename.equals(that.resourcename) : that.resourcename != null) return false;
        if (sha256 != null ? !sha256.equals(that.sha256) : that.sha256 != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (lastmodified != null ? lastmodified.hashCode() : 0);
        result = 31 * result + (packaging != null ? packaging.hashCode() : 0);
        result = 31 * result + (ext != null ? ext.hashCode() : 0);
        result = 31 * result + (sha1 != null ? sha1.hashCode() : 0);
        result = 31 * result + (resourcename != null ? resourcename.hashCode() : 0);
        result = 31 * result + (sha256 != null ? sha256.hashCode() : 0);
        return result;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MAVENARTIFACTVERSION", referencedColumnName = "ID", nullable = false)
    public MavenartifactversionDTO getMavenartifactversionByMavenartifactversion() {
        return mavenartifactversionByMavenartifactversion;
    }

    public void setMavenartifactversionByMavenartifactversion(MavenartifactversionDTO mavenartifactversionByMavenartifactversion) {
        this.mavenartifactversionByMavenartifactversion = mavenartifactversionByMavenartifactversion;
    }
    @ManyToOne
    @JoinColumn(name = "CLASSIFIER", referencedColumnName = "ID")
    public MavenClassifierDTO getMavenClassifierByClassifier() {
        return mavenClassifierByClassifier;
    }

    public void setMavenClassifierByClassifier(MavenClassifierDTO mavenClassifierByClassifier) {
        this.mavenClassifierByClassifier = mavenClassifierByClassifier;
    }
}
