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
    private String exportService;
    private String description;
    private String name;
    private String license;
    private String docUrl;
    private Parameters exportPackage = new Parameters();
    private Parameters importPackage = new Parameters();
    private Parameters requireBundle = new Parameters();

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

    @Column(length = Short.MAX_VALUE)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ManyToOne(cascade = CascadeType.ALL)
    public Parameters getRequireBundle() {
        return requireBundle;
    }

    public void setRequireBundle(Parameters requireBundle) {
        this.requireBundle = requireBundle;
    }

    public void setRequireBundle(aQute.bnd.header.Parameters parameters) {
        setRequireBundle(new Parameters(parameters));
    }

    @ManyToOne(cascade = CascadeType.ALL)
    public Parameters getExportPackage() {
        return exportPackage;
    }


    public void setExportPackage(aQute.bnd.header.Parameters exportPackage) {
        this.exportPackage = new Parameters(exportPackage);
    }

    public void setExportPackage(Parameters exportPackage) {
        this.exportPackage = exportPackage;
    }

    @ManyToOne(cascade = CascadeType.ALL)
    public Parameters getImportPackage() {
        return importPackage;
    }

    public void setImportPackage(aQute.bnd.header.Parameters importPackage) {
        this.importPackage = new Parameters(importPackage);
    }

    public void setImportPackage(Parameters importPackage) {
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
