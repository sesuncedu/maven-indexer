package com.criticollab.osgi.mavenindex;


import com.google.common.base.Strings;
import org.apache.maven.index.ArtifactAvailability;
import org.apache.maven.index.ArtifactInfoRecord;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * ArtifactInfo holds the values known about an repository artifact. This is a simple Value Object kind of stuff.
 * Phasing out.
 *
 * @author Jason van Zyl
 * @author Eugene Kuleshov
 */


class ArtifactInfo extends ArtifactInfoRecord {
    static final String UINFO = "u";

    static final String DELETED = "del";

    static final String INFO = "i";

    static final String NAME = "n";

    static final String DESCRIPTION = "d";

    static final String LAST_MODIFIED = "m";
    static final String SHA1 = "1";

    private final transient VersionScheme versionScheme;
    private String fileName;
    private String fileExtension;
    private String groupId;
    private String artifactId;
    private String version;
    private transient Version artifactVersion;
    private String classifier;
    /**
     * Artifact packaging for the main artifact and extension for secondary artifact (no classifier)
     */
    private String packaging;
    private String name;
    private String description;
    private long lastModified = -1;

    private long mavenLastModified = -1;
    private long size = -1;
    private String md5;
    private String sha1;
    private ArtifactAvailability sourcesExists = ArtifactAvailability.NOT_PRESENT;
    private ArtifactAvailability javadocExists = ArtifactAvailability.NOT_PRESENT;
    private ArtifactAvailability signatureExists = ArtifactAvailability.NOT_PRESENT;
    private String bundleVersion;
    private String bundleSymbolicName;
    private String bundleExportPackage;

    private String bundleExportService;

    private String bundleDescription;

    private String bundleName;

    private String bundleLicense;

    private String bundleDocUrl;

    private String bundleImportPackage;

    private String bundleRequireBundle;
    private boolean deleted = false;


    ArtifactInfo() {
        versionScheme = new GenericVersionScheme();
    }

    static String nvl(String v) {
        return v == null ? NA : v;
    }

    static String renvl(String v) {
        return NA.equals(v) ? null : v;
    }

    static String lst2str(Collection<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s).append(ArtifactInfo.FS);
        }
        return sb.length() == 0 ? sb.toString() : sb.substring(0, sb.length() - 1);
    }

    static List<String> str2lst(String str) {
        return Arrays.asList(ArtifactInfo.FS_PATTERN.split(str));
    }

    private Version getArtifactVersion() {
        if (artifactVersion == null) {
            try {
                artifactVersion = versionScheme.parseVersion(version);
            } catch (InvalidVersionSpecificationException e) {
                // will not happen, only with version ranges but we should not have those
                // we handle POM versions here, not dependency versions
            }
        }

        return artifactVersion;
    }

    void setArtifactVersion(Version artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    private String getUinfo() {
        return //
                groupId + FS + //
                artifactId + FS + //
                version + FS + //
                nvl(classifier) + FS + //
                fileExtension
                // .append( StringUtils.isEmpty( classifier ) || StringUtils.isEmpty( packaging ) ? "" : FS + packaging ) //
                ; // extension is stored in the packaging field when classifier is not used
    }

    // ----------------------------------------------------------------------------
    // Utils
    // ----------------------------------------------------------------------------

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        if (isDeleted()) {
            result.append("DELETED: ");
        }
        result.append(getUinfo());
        if (!Strings.isNullOrEmpty(getPackaging())) {
            result.append("[").append(getPackaging()).append("]");
        }

        return result.toString();
    }

    String getFileName() {
        return fileName;
    }

    void setFileName(String fileName) {
        this.fileName = fileName;
    }

    String getFileExtension() {
        return fileExtension;
    }

    void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    String getGroupId() {
        return groupId;
    }

    void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    String getArtifactId() {
        return artifactId;
    }

    void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    String getVersion() {
        return version;
    }

    void setVersion(String version) {
        this.version = version;
    }

    String getClassifier() {
        return classifier;
    }

    void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    String getPackaging() {
        return packaging;
    }

    void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    long getLastModified() {
        return lastModified;
    }

    void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    long getSize() {
        return size;
    }

    void setSize(long size) {
        this.size = size;
    }

    String getMd5() {
        return md5;
    }

    void setMd5(String md5) {
        this.md5 = md5;
    }

    String getSha1() {
        return sha1;
    }

    void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    ArtifactAvailability getSourcesExists() {
        return sourcesExists;
    }

    void setSourcesExists(ArtifactAvailability sourcesExists) {
        this.sourcesExists = sourcesExists;
    }

    ArtifactAvailability getJavadocExists() {
        return javadocExists;
    }

    void setJavadocExists(ArtifactAvailability javadocExists) {
        this.javadocExists = javadocExists;
    }

    ArtifactAvailability getSignatureExists() {
        return signatureExists;
    }

    void setSignatureExists(ArtifactAvailability signatureExists) {
        this.signatureExists = signatureExists;
    }

    String getBundleVersion() {
        return bundleVersion;
    }

    void setBundleVersion(String bundleVersion) {
        this.bundleVersion = bundleVersion;
    }

    String getBundleSymbolicName() {
        return bundleSymbolicName;
    }

    void setBundleSymbolicName(String bundleSymbolicName) {
        this.bundleSymbolicName = bundleSymbolicName;
    }

    String getBundleExportPackage() {
        return bundleExportPackage;
    }

    void setBundleExportPackage(String bundleExportPackage) {
        this.bundleExportPackage = bundleExportPackage;
    }

    String getBundleExportService() {
        return bundleExportService;
    }

    void setBundleExportService(String bundleExportService) {
        this.bundleExportService = bundleExportService;
    }

    String getBundleDescription() {
        return bundleDescription;
    }

    void setBundleDescription(String bundleDescription) {
        this.bundleDescription = bundleDescription;
    }

    String getBundleName() {
        return bundleName;
    }

    void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    String getBundleLicense() {
        return bundleLicense;
    }

    void setBundleLicense(String bundleLicense) {
        this.bundleLicense = bundleLicense;
    }

    String getBundleDocUrl() {
        return bundleDocUrl;
    }

    void setBundleDocUrl(String bundleDocUrl) {
        this.bundleDocUrl = bundleDocUrl;
    }

    String getBundleImportPackage() {
        return bundleImportPackage;
    }

    void setBundleImportPackage(String bundleImportPackage) {
        this.bundleImportPackage = bundleImportPackage;
    }

    String getBundleRequireBundle() {
        return bundleRequireBundle;
    }

    void setBundleRequireBundle(String bundleRequireBundle) {
        this.bundleRequireBundle = bundleRequireBundle;
    }

    long getMavenLastModified() {
        return mavenLastModified;
    }

    void setMavenLastModified(long mavenLastModified) {
        this.mavenLastModified = mavenLastModified;
    }

    VersionScheme getVersionScheme() {
        return versionScheme;
    }

    boolean isDeleted() {
        return deleted;
    }

    void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

}
