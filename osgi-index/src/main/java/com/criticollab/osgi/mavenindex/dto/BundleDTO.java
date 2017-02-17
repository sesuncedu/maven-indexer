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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Collection;
import java.util.Iterator;

@Entity
@Table(name = "BUNDLE", schema = "APP", catalog = "")
public class BundleDTO {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(BundleDTO.class);
    private int id;
    private String bsn;
    private String bundleversion;
    private MavenresourceDTO mavenresourceByMavenresourceid;
    private Collection<BundleexportpackageDTO> bundleexportpackagesById;

    @Id
    @Column(name = "ID", nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "BSN", nullable = false, length = 8192)
    public String getBsn() {
        return bsn;
    }

    public void setBsn(String bsn) {
        this.bsn = bsn;
    }

    @Basic
    @Column(name = "BUNDLEVERSION", nullable = false, length = 256)
    public String getBundleversion() {
        return bundleversion;
    }

    public void setBundleversion(String bundleversion) {
        this.bundleversion = bundleversion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BundleDTO bundleDTO = (BundleDTO) o;

        if (id != bundleDTO.id) return false;
        if (bsn != null ? !bsn.equals(bundleDTO.bsn) : bundleDTO.bsn != null) return false;
        if (bundleversion != null ? !bundleversion.equals(bundleDTO.bundleversion) : bundleDTO.bundleversion != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (bsn != null ? bsn.hashCode() : 0);
        result = 31 * result + (bundleversion != null ? bundleversion.hashCode() : 0);
        return result;
    }

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "MAVENRESOURCEID", referencedColumnName = "ID", nullable = false)
    public MavenresourceDTO getMavenresourceByMavenresourceid() {
        return mavenresourceByMavenresourceid;
    }

    public void setMavenresourceByMavenresourceid(MavenresourceDTO mavenresourceByMavenresourceid) {
        this.mavenresourceByMavenresourceid = mavenresourceByMavenresourceid;
    }

    @OneToMany()
    @JoinColumn(name = "bundleId")
    public Collection<BundleexportpackageDTO> getBundleexportpackagesById() {
        return bundleexportpackagesById;
    }

    public void setBundleexportpackagesById(Collection<BundleexportpackageDTO> bundleexportpackagesById) {
        this.bundleexportpackagesById = bundleexportpackagesById;
    }


    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder().append("\nBundle-Symbolic-Name: ").
                append(getBsn()).append("\n").
                append("Bundle-Version: ").
                append(getBundleversion()).append('\n');
                if(getBundleexportpackagesById().size() >0) {
                    buf.append("Export-Package: ");
                    for (Iterator<BundleexportpackageDTO> iterator = getBundleexportpackagesById().iterator(); iterator.hasNext(); ) {
                        BundleexportpackageDTO export = iterator.next();
                        buf.append(export.getPackageDTO().getValue());
                        buf.append(";version=\"");
                        buf.append(export.getVersion());
                        buf.append("\"");
                        if(export.getAttrs() != null ) {
                            if(export.getAttrs().length() >0) {
                                buf.append("; ");
                                buf.append(export.getAttrs());
                            }

                        }
                        if(iterator.hasNext()) {
                            buf.append(", ");
                        }
                    }
                }
                buf.append("\n");
        return buf.toString();
    }
}
