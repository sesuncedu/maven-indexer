package com.criticollab.osgi.mavenindex.persist;/**
 * Created by ses on 2/18/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"mavenArtifact_id", "classifier"})}, indexes = {
        @Index(columnList = "classifier"), @Index(columnList = "packaging"), @Index(columnList = "md5"),
        @Index(columnList = "sha1")})
public class MavenResource {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(MavenResource.class);
    String classifier;
    String packaging;
    String fileExtension;
    String md5;
    String sha1;
    Date lastModified;

    long size;
    private int id;
    private MavenArtifact mavenArtifact;
    private Bundle bundle;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(optional = false)
    public MavenArtifact getMavenArtifact() {
        return mavenArtifact;
    }

    public void setMavenArtifact(MavenArtifact mavenArtifact) {
        this.mavenArtifact = mavenArtifact;
    }

    @Column(nullable = true)
    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    @Column(nullable = true)
    public String getPackaging() {
        return packaging;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    @Column(nullable = true)
    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    @Column(nullable = true)
    public String getMD5() {
        return md5;
    }

    public void setMD5(String md5) {
        this.md5 = md5;
    }

    @Column(nullable = true)
    public String getSHA1() {
        return sha1;
    }

    public void setSHA1(String sha1) {
        this.sha1 = sha1;
    }

    @Column(nullable = true)
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Column(nullable = true)
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @ManyToOne
    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }
}
