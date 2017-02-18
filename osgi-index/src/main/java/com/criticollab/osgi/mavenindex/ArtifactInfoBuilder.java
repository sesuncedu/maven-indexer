package com.criticollab.osgi.mavenindex;
/**
 * Created by ses on 2/17/17.
 */

import org.apache.maven.index.ArtifactAvailability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("Duplicates")
public class ArtifactInfoBuilder {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(ArtifactInfoBuilder.class);

    static ArtifactInfo getArtifactInfoFromDocument(FauxDocument document) {
        ArtifactInfo ai;
        if (document.get("del") != null) {
            logger.debug("deleted: {}", document);
            ai = null;
        } else {
            ai = new ArtifactInfo();
        }
        if (ai == null) return ai;
        return buildArtifactInfoFromDocument(document, ai);
    }

    static ArtifactInfo buildArtifactInfoFromDocument(FauxDocument document, ArtifactInfo ai) {
        boolean b = doMinimal(document, ai);
        b = doJar(document, ai);
        // nothing to update, minimal will maintain it.

        b = false;
        b = doPlugin(document, ai);
        b = doOSGI(document, ai);
        if (b) {

        }
        return ai;
    }

    private static boolean doMinimal(FauxDocument document, ArtifactInfo ai) {
        boolean res = false;

        String uinfo1 = "u";
        String uinfo = document.get(uinfo1);

        if (uinfo != null) {
            String[] r = ArtifactInfo.FS_PATTERN.split(uinfo);

            ai.setGroupId(r[0]);

            ai.setArtifactId(r[1]);

            ai.setVersion(r[2]);

            ai.setClassifier(ArtifactInfo.renvl(r[3]));

            if (r.length > 4) {
                ai.setFileExtension(r[4]);
            }

            res = true;
        }

        String info = document.get("i");

        if (info != null) {
            String[] r = ArtifactInfo.FS_PATTERN.split(info);

            ai.setPackaging(ArtifactInfo.renvl(r[0]));

            ai.setLastModified(Long.parseLong(r[1]));

            ai.setSize(Long.parseLong(r[2]));

            ai.setSourcesExists(ArtifactAvailability.fromString(r[3]));

            ai.setJavadocExists(ArtifactAvailability.fromString(r[4]));

            ai.setSignatureExists(ArtifactAvailability.fromString(r[5]));

            if (r.length > 6) {
                ai.setFileExtension(r[6]);
            } else {
                if (ai.getClassifier() != null //
                        || "pom".equals(ai.getPackaging()) //
                        || "war".equals(ai.getPackaging()) //
                        || "ear".equals(ai.getPackaging())) {
                    ai.setFileExtension(ai.getPackaging());
                } else {
                    ai.setFileExtension("jar"); // best guess
                }
            }

            res = true;
        }

        String name = document.get("n");

        if (name != null) {
            ai.setName(name);

            res = true;
        }

        String description = document.get("d");

        if (description != null) {
            ai.setDescription(description);

            res = true;
        }

        // sometimes there's a pom without packaging(default to jar), but no artifact, then the value will be a "null"
        // String
        if ("null".equals(ai.getPackaging())) {
            ai.setPackaging(null);
        }

        String sha1 = document.get(ArtifactInfo.SHA1);

        if (sha1 != null) {
            ai.setSha1(sha1);
        }

        return res;

        // artifactInfo.fname = ???
    }

    private static boolean doOSGI(FauxDocument document, ArtifactInfo ai) {
        boolean b;
        boolean updated = false;

        String bundleSymbolicName = document.get("Bundle-SymbolicName");

        if (bundleSymbolicName != null) {
            ai.setBundleSymbolicName(bundleSymbolicName);

            updated = true;
        }

        String bundleVersion = document.get("Bundle-Version");

        if (bundleVersion != null) {
            ai.setBundleVersion(bundleVersion);

            updated = true;
        }

        String bundleExportPackage = document.get("Export-Package");

        if (bundleExportPackage != null) {
            ai.setBundleExportPackage(bundleExportPackage);

            updated = true;

        }

        String bundleExportService = document.get("Export-Service");

        if (bundleExportService != null) {
            ai.setBundleExportService(bundleExportService);

            updated = true;

        }

        String bundleDescription = document.get("Bundle-Description");

        if (bundleDescription != null) {
            ai.setBundleDescription(bundleDescription);

            updated = true;

        }


        String bundleName = document.get("Bundle-Name");

        if (bundleName != null) {
            ai.setBundleName(bundleName);

            updated = true;

        }


        String bundleLicense = document.get("Bundle-License");

        if (bundleLicense != null) {
            ai.setBundleLicense(bundleLicense);

            updated = true;

        }

        String bundleDocUrl = document.get("Bundle-DocURL");

        if (bundleDocUrl != null) {
            ai.setBundleDocUrl(bundleDocUrl);

            updated = true;

        }

        String bundleImportPackage = document.get("Import-Package");

        if (bundleImportPackage != null) {
            ai.setBundleImportPackage(bundleImportPackage);

            updated = true;

        }

        String bundleRequireBundle = document.get("Require-Bundle");

        if (bundleRequireBundle != null) {
            ai.setBundleRequireBundle(bundleRequireBundle);

            updated = true;

        }

        b = updated;
        return b;
    }

    private static boolean doPlugin(FauxDocument document, ArtifactInfo ai) {
        boolean b;
        boolean res = false;

        if ("maven-plugin".equals(ai.getPackaging())) {
            ai.setPrefix(document.get("px"));

            String goals = document.get("gx");

            if (goals != null) {
                ai.setGoals(ArtifactInfo.str2lst(goals));
            }

            res = true;
        }

        b = res;
        return b;
    }

    private static boolean doJar(FauxDocument document, ArtifactInfo ai) {
        boolean b;
        boolean result;
        String names = document.get("c");

        if (names != null) {
            if (names.length() == 0 || names.charAt(0) == '/') {
                ai.setClassNames(names);
            } else {
                // conversion from the old format
                String[] lines = names.split("\\n");
                StringBuilder sb = new StringBuilder();
                for (String line : lines) {
                    sb.append('/').append(line).append('\n');
                }
                ai.setClassNames(sb.toString());
            }

            result = true;
        } else {

            result = false;
        }
        b = result;
        return b;
    }
}
