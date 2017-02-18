package com.criticollab.osgi.mavenindex.dto;/**
 * Created by ses on 2/16/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "MAVENARTIFACT", schema = "mavendb")
public class MavenartifactDTO {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(MavenartifactDTO.class);
    private int id;
    private MavenGroupDTO mavenGroupByGroupid;
    private MavenArtifactidDTO mavenArtifactidByArtifactid;

    @Id
    @Column(name = "ID", nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MavenartifactDTO that = (MavenartifactDTO) o;

        if (id != that.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @ManyToOne
    @JoinColumn(name = "GROUPID", referencedColumnName = "ID", nullable = false)
    public MavenGroupDTO getMavenGroupByGroupid() {
        return mavenGroupByGroupid;
    }

    public void setMavenGroupByGroupid(MavenGroupDTO mavenGroupByGroupid) {
        this.mavenGroupByGroupid = mavenGroupByGroupid;
    }

    @ManyToOne
    @JoinColumn(name = "ARTIFACTID", referencedColumnName = "ID", nullable = false)
    public MavenArtifactidDTO getMavenArtifactidByArtifactid() {
        return mavenArtifactidByArtifactid;
    }

    public void setMavenArtifactidByArtifactid(MavenArtifactidDTO mavenArtifactidByArtifactid) {
        this.mavenArtifactidByArtifactid = mavenArtifactidByArtifactid;
    }
}
