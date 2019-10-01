package org.palladiosimulator.builddependencytool.tests;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.palladiosimulator.dependencytool.ManifestMF;

public class ManifestMFTest {

    @Test
    public void simpleTest() throws IOException {
        URL manifestURL = getClass().getResource("/pcm_manifest.mf");
        ManifestMF manifest = new ManifestMF(manifestURL.openStream());
        Set<String> testBundles = manifest.getRequiredBundles();
        assert(testBundles.size() == 11);
        assert(testBundles.contains("org.eclipse.core.runtime"));
        assert(testBundles.contains("org.eclipse.emf.ecore"));
        assert(testBundles.contains("org.eclipse.ocl"));
        assert(testBundles.contains("org.eclipse.ocl.ecore"));
        assert(testBundles.contains("org.eclipse.emf.cdo"));
        assert(testBundles.contains("org.eclipse.emf.ecore.xmi"));
        assert(testBundles.contains("de.uka.ipd.sdq.identifier"));
        assert(testBundles.contains("de.uka.ipd.sdq.probfunction"));
        assert(testBundles.contains("de.uka.ipd.sdq.stoex"));
        assert(testBundles.contains("de.uka.ipd.sdq.units"));
        assert(testBundles.contains("de.uka.ipd.sdq.errorhandling"));
    }
    
    @Test
    public void versionRangeTest() throws IOException {
        URL manifestURL = getClass().getResource("/commons.stoex_manifest.mf");
        ManifestMF manifest = new ManifestMF(manifestURL.openStream());
        Set<String> testBundles = manifest.getRequiredBundles();
        assert(testBundles.size() == 19);
        assert(testBundles.contains("org.eclipse.xtext"));
        assert(testBundles.contains("org.eclipse.xtext.xbase"));
        assert(testBundles.contains("org.eclipse.xtext.generator"));
        assert(testBundles.contains("org.apache.commons.logging"));
        assert(testBundles.contains("org.eclipse.emf.codegen.ecore"));
        assert(testBundles.contains("org.eclipse.emf.mwe.utils"));
        assert(testBundles.contains("org.eclipse.emf.mwe2.launch"));
        assert(testBundles.contains("org.eclipse.uml2"));
        assert(testBundles.contains("org.eclipse.uml2.codegen.ecore"));
        assert(testBundles.contains("org.eclipse.uml2.codegen.ecore.ui"));
        assert(testBundles.contains("org.eclipse.uml2.common"));
        assert(testBundles.contains("org.eclipse.uml2.common.edit"));
        assert(testBundles.contains("org.objectweb.asm"));
        assert(testBundles.contains("de.uka.ipd.sdq.stoex"));
        assert(testBundles.contains("org.eclipse.emf"));
        assert(testBundles.contains("org.eclipse.xtext.util"));
        assert(testBundles.contains("org.antlr.runtime"));
        assert(testBundles.contains("org.eclipse.xtext.common.types"));
        assert(testBundles.contains("org.eclipse.xtext.xbase.lib"));
    }
}
