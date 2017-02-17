package com.criticollab.osgi.mavenindex.dto;/**
 * Created by ses on 2/16/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "BUNDLEEXPORTPACKAGE", schema = "APP" )
public class BundleexportpackageDTO {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(BundleexportpackageDTO.class);
    private int id;
    private String version;
    private String attrs;
    private int packageId;
    private PackageDTO packageDTO;
    @Id
    @Column(name = "ID", nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "VERSION", nullable = false, length = 256)
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Basic
    @Column(name = "ATTRS", nullable = true, length = 8192)
    public String getAttrs() {
        return attrs;
    }

    public void setAttrs(String attrs) {
        this.attrs = attrs;
    }

    @ManyToOne
    @JoinColumn(name="packageId",referencedColumnName = "id")
    public PackageDTO getPackageDTO() {
        return packageDTO;
    }

    public void setPackageDTO(PackageDTO packageDTO) {
        this.packageDTO = packageDTO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BundleexportpackageDTO that = (BundleexportpackageDTO) o;

        if (id != that.id) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;
        if (attrs != null ? !attrs.equals(that.attrs) : that.attrs != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (attrs != null ? attrs.hashCode() : 0);
        return result;
    }
}
