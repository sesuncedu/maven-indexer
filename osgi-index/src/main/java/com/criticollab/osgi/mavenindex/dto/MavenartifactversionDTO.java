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

@Entity
@Table(name = "MAVENARTIFACTVERSION", schema = "mavendb")
public class MavenartifactversionDTO {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(MavenartifactversionDTO.class);
    private int id;
    private String mavenversion;
    private MavenartifactDTO mavenartifactByMavenartifact;

    @Id
    @Column(name = "ID", nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "MAVENVERSION", nullable = false, length = 256)
    public String getMavenversion() {
        return mavenversion;
    }

    public void setMavenversion(String mavenversion) {
        this.mavenversion = mavenversion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MavenartifactversionDTO that = (MavenartifactversionDTO) o;

        if (id != that.id) return false;
        if (mavenversion != null ? !mavenversion.equals(that.mavenversion) : that.mavenversion != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (mavenversion != null ? mavenversion.hashCode() : 0);
        return result;
    }

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "MAVENARTIFACT", referencedColumnName = "ID", nullable = false)
    public MavenartifactDTO getMavenartifactByMavenartifact() {
        return mavenartifactByMavenartifact;
    }

    public void setMavenartifactByMavenartifact(MavenartifactDTO mavenartifactByMavenartifact) {
        this.mavenartifactByMavenartifact = mavenartifactByMavenartifact;
    }
}
