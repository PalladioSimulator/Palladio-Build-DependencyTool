package org.palladiosimulator.dependencytool;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Holds content of a manifest.mf file.
 */

public class ManifestMF {

    private Manifest manifest;

    public ManifestMF(InputStream stream) throws IOException {
        manifest = new Manifest(stream);
    }

    /**
     * Parses the manifest.mf file for names of bundles specified in require-bundle.
     * 
     * @param includeOptions
     *            If set to false, optional dependencies will be ignored.
     * @return Names of bundles required in this manifest.mf file.
     */
    public Set<String> getRequiredBundles(boolean includeOptionals) {
        Predicate<RequiredBundle> filterPredicate = rb -> true;
        if (!includeOptionals) {
            filterPredicate = rb -> !rb.isOptional;
        }
        return getRequiredBundles(filterPredicate);
    }

    protected Set<String> getRequiredBundles(Predicate<RequiredBundle> filter) {
        Attributes attr = manifest.getMainAttributes();
        if (attr.containsKey(new Attributes.Name("Require-Bundle"))) {
            String requiredBundlesString = attr.getValue("Require-Bundle");
            Pattern entryPattern = Pattern.compile("([^\",]|((\"[^\"]+\")+))+");
            Matcher matcher = entryPattern.matcher(requiredBundlesString);
            Stream<String> entries = matcher.results()
                .map(mr -> mr.group(0));
            return entries.map(RequiredBundle::new)
                .filter(filter)
                .map(RequiredBundle::getBundleId)
                .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    protected static class RequiredBundle {
        private final String bundleId;
        private final boolean isOptional;
        private final boolean reexport;
        private final String versionRange;

        public RequiredBundle(String manifestLine) {
            super();

            String[] lineParts = manifestLine.split(";");
            this.bundleId = lineParts[0];

            boolean isOptional = false;
            boolean reexport = false;
            String versionRange = null;

            for (String linePart : lineParts) {
                if ("visibility:=reexport".equals(linePart)) {
                    reexport = true;
                } else if ("resolution:=optional".equals(linePart)) {
                    isOptional = true;
                } else if (linePart.startsWith("bundle-version=")) {
                    versionRange = linePart.replaceFirst("bundle-version=\"([^\"]+)\"", "$1");
                }
            }

            this.isOptional = isOptional;
            this.reexport = reexport;
            this.versionRange = versionRange;
        }

        public String getBundleId() {
            return bundleId;
        }

        public boolean isOptional() {
            return isOptional;
        }

        public boolean isReexport() {
            return reexport;
        }

        public String getVersionRange() {
            return versionRange;
        }

    }
}
