package com.criticollab.osgi.mavenindex;
/**
 * Created by ses on 2/17/17.
 */

import org.apache.maven.index.ArtifactAvailability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.criticollab.osgi.mavenindex.ArtifactInfo.*;
import static org.osgi.framework.Constants.*;

@SuppressWarnings("Duplicates")
class ArtifactInfoBuilder {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(ArtifactInfoBuilder.class);

    static ArtifactInfo getArtifactInfoFromDocument(FauxDocument document) {
        return buildArtifactInfoFromDocument(document);
    }

    private static ArtifactInfo buildArtifactInfoFromDocument(FauxDocument document) {
        ArtifactInfo ai = new ArtifactInfo();
        Builder builder = new Builder(document, ai);
        if (document.containsKey("del")) {
            addUinfoIfNotNull(ai, document.get("del"));
            ai.setDeleted(true);
            builder.setIfNotNull(LAST_MODIFIED, ArtifactInfo::setMavenLastModified, Long::valueOf);
            builder.setUpdated(true);
        } else {
            doMinimal(builder);
            doOSGI(builder);
        }
        return ai;
    }

    private static Builder doMinimal(Builder builder) {

        FauxDocument document1 = builder.getDocument();
        ArtifactInfo artifactInfo = builder.getArtifactInfo();

        builder.setUpdated(addUinfoIfNotNull(artifactInfo, document1.get(UINFO)));
        builder.setUpdated(addInfoIfNotNull(artifactInfo, document1.get(INFO)));
        builder.setIfNotNull(NAME, ArtifactInfo::setName).
                setIfNotNull(DESCRIPTION, ArtifactInfo::setDescription).
                setIfNotNull(SHA1, ArtifactInfo::setSha1).
                setIfNotNull(LAST_MODIFIED, ArtifactInfo::setMavenLastModified, Long::valueOf);

        if ("null".equals(artifactInfo.getPackaging())) {
            artifactInfo.setPackaging(null);
        }
        return builder;
    }

    static List<String> split(CharSequence input, char delim) {
        List<String> result = new ArrayList<>();
        int len = input.length();
        StringBuilder buf = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            if (c == delim) {
                result.add(buf.toString());
                buf.setLength(0);
            } else {
                buf.append(c);
            }
        }
        if (buf.length() > 0) {
            result.add(buf.toString());
        }
        return result;
    }

    private static boolean addInfoIfNotNull(ArtifactInfo ai, String info) {
        if (info != null) {
            List<String> splits = split(info, '|');
            ai.setPackaging(renvl(splits.get(0)));
            ai.setLastModified(Long.parseLong(splits.get(1)));
            ai.setSize(Long.parseLong(splits.get(2)));
            ai.setSourcesExists(ArtifactAvailability.fromString(splits.get(3)));
            ai.setJavadocExists(ArtifactAvailability.fromString(splits.get(4)));
            ai.setSignatureExists(ArtifactAvailability.fromString(splits.get(5)));

            if (splits.size() > 6) {
                ai.setFileExtension(splits.get(6));
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

            return true;
        }
        return false;
    }

    private static boolean addUinfoIfNotNull(ArtifactInfo ai, String uinfo) {
        boolean res = false;

        if (uinfo != null) {
            List<String> splits = split(uinfo, '|');

            ai.setGroupId(splits.get(0));
            ai.setArtifactId(splits.get(1));
            ai.setVersion(splits.get(2));
            ai.setClassifier(renvl(splits.get(3)));
            if (splits.size() > 4) {
                ai.setFileExtension(splits.get(4));
            }

            res = true;
        }
        return res;
    }

    private static Builder doOSGI(Builder builder) {
        //noinspection deprecation
        return builder.setIfNotNull(BUNDLE_SYMBOLICNAME, ArtifactInfo::setBundleSymbolicName).
                setIfNotNull(BUNDLE_VERSION, ArtifactInfo::setBundleVersion).
                setIfNotNull(EXPORT_PACKAGE, ArtifactInfo::setBundleExportPackage).
                setIfNotNull(EXPORT_SERVICE, ArtifactInfo::setBundleExportService).
                setIfNotNull(BUNDLE_DESCRIPTION, ArtifactInfo::setBundleDescription).
                setIfNotNull(BUNDLE_NAME, ArtifactInfo::setBundleName).
                setIfNotNull(BUNDLE_LICENSE, ArtifactInfo::setBundleLicense).
                setIfNotNull(BUNDLE_DOCURL, ArtifactInfo::setBundleDocUrl).
                setIfNotNull(IMPORT_PACKAGE, ArtifactInfo::setBundleImportPackage).
                setIfNotNull(REQUIRE_BUNDLE, ArtifactInfo::setBundleRequireBundle);
    }

    static class Builder {
        private final FauxDocument document;
        private final ArtifactInfo ai;
        private boolean updated;

        public Builder(FauxDocument document, ArtifactInfo ai) {
            this.document = document;
            this.ai = ai;
            this.updated = false;
        }

        <T> Builder setIfNotNull(String key, BiConsumer<ArtifactInfo, T> setter, Function<String, T> transformer) {
            String value = document.get(key);
            if (value != null) {
                T result = transformer.apply(value);
                setValue(setter, result);
            } else {
                logger.debug("Null value would have been sent to transformer");
            }
            return this;
        }

        Builder setIfNotNull(String key, BiConsumer<ArtifactInfo, String> setter) {
            String value = document.get(key);
            return setValue(setter, value);
        }

        private <T> Builder setValue(BiConsumer<ArtifactInfo, T> setter, T value) {
            if (value != null) {
                setter.accept(ai, value);
                updated = true;
            }
            return this;
        }

        public ArtifactInfo getArtifactInfo() {
            return ai;
        }

        public boolean isUpdated() {
            return updated;
        }

        public void setUpdated(boolean b) {
            this.updated = b;
        }

        public FauxDocument getDocument() {
            return document;
        }
    }
}
