package com.criticollab.osgi.mavenindex.persist;/**
 * Created by ses on 2/18/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


@Entity
public class Bundle {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(Bundle.class);

    private int id;
    private String symbolicName;
    private Map<String, String> symbolicNameAttributes = new HashMap<>();
    private Version version;
    private Set<PackageWithVersion> exportPackage = new HashSet<>();
    private String exportService;
    private String description;
    private String name;
    private String license;
    private String docUrl;
    private Set<PackageWithVersionRange> importPackage = new HashSet<>();
    private Parameters requireBundle;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(nullable = false)
    public String getSymbolicName() {
        return symbolicName;
    }

    public void setSymbolicName(String symbolicName) {
        this.symbolicName = symbolicName;
    }

    @CollectionTable(name = "bsnAttributes")
    @ElementCollection

    public Map<String, String> getSymbolicNameAttributes() {
        return symbolicNameAttributes;
    }

    public void setSymbolicNameAttributes(Map<String, String> symbolicNameAttributes) {
        this.symbolicNameAttributes = symbolicNameAttributes;
    }

    @Basic
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Embedded()
    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    @Column(length = Short.MAX_VALUE)
    public String getExportService() {
        return exportService;
    }

    public void setExportService(String exportService) {
        this.exportService = exportService;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @OneToOne
    public Parameters getRequireBundle() {
        return requireBundle;
    }

    public void setRequireBundle(aQute.bnd.header.Parameters parameters) {
        setRequireBundle(new Parameters(parameters));
    }

    public void setRequireBundle(Parameters requireBundle) {
        this.requireBundle = requireBundle;
    }

    @CollectionTable(name = "bundleExportPackage")
    @OneToMany(cascade = CascadeType.ALL)
    public Set<PackageWithVersion> getExportPackage() {
        return exportPackage;
    }

    public void setExportPackage(Set<PackageWithVersion> exportPackage) {
        this.exportPackage = exportPackage;
    }

    @CollectionTable(name = "bundleImportPackage")
    @OneToMany(cascade = CascadeType.ALL)
    public Set<PackageWithVersionRange> getImportPackage() {
        return importPackage;
    }

    public void setImportPackage(Set<PackageWithVersionRange> importPackage) {
        this.importPackage = importPackage;
    }

    @Column(length = 8192)
    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getDocUrl() {
        return docUrl;
    }

    public void setDocUrl(String docUrl) {
        this.docUrl = docUrl;
    }
}
