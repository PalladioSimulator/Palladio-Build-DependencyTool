package org.palladiosimulator.builddependencytool.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.palladiosimulator.dependencytool.ManifestMF;

import aQute.lib.unmodifiable.Sets;

public class ManifestMFTest {

    @Test
    public void simpleTest() throws IOException {
        URL manifestURL = getClass().getResource("/pcm_manifest.mf");
        ManifestMF manifest = new ManifestMF(manifestURL.openStream());
        Set<String> testBundles = manifest.getRequiredBundles(true);
        assertTrue(testBundles.size() == 13);
        assertTrue(testBundles.contains("org.eclipse.core.runtime"));
        assertTrue(testBundles.contains("org.eclipse.emf.ecore"));
        assertTrue(testBundles.contains("org.eclipse.ocl"));
        assertTrue(testBundles.contains("org.eclipse.ocl.ecore"));
        assertTrue(testBundles.contains("org.eclipse.emf.cdo"));
        assertTrue(testBundles.contains("org.eclipse.emf.ecore.xmi"));
        assertTrue(testBundles.contains("de.uka.ipd.sdq.identifier"));
        assertTrue(testBundles.contains("de.uka.ipd.sdq.probfunction"));
        assertTrue(testBundles.contains("de.uka.ipd.sdq.stoex"));
        assertTrue(testBundles.contains("de.uka.ipd.sdq.units"));
        assertTrue(testBundles.contains("de.uka.ipd.sdq.errorhandling"));
        assertTrue(testBundles.contains("org.palladiosimulator.pcm.workflow"));
        assertTrue(testBundles.contains("org.palladiosimulator.commons.stoex.api"));
    }
    
    @Test
    public void versionRangeTest() throws IOException {
        URL manifestURL = getClass().getResource("/commons.stoex_manifest.mf");
        ManifestMF manifest = new ManifestMF(manifestURL.openStream());
        Set<String> testBundles = manifest.getRequiredBundles(true);
        assertTrue(testBundles.size() == 19);
        assertTrue(testBundles.contains("org.eclipse.xtext"));
        assertTrue(testBundles.contains("org.eclipse.xtext.xbase"));
        assertTrue(testBundles.contains("org.eclipse.xtext.generator"));
        assertTrue(testBundles.contains("org.apache.commons.logging"));
        assertTrue(testBundles.contains("org.eclipse.emf.codegen.ecore"));
        assertTrue(testBundles.contains("org.eclipse.emf.mwe.utils"));
        assertTrue(testBundles.contains("org.eclipse.emf.mwe2.launch"));
        assertTrue(testBundles.contains("org.eclipse.uml2"));
        assertTrue(testBundles.contains("org.eclipse.uml2.codegen.ecore"));
        assertTrue(testBundles.contains("org.eclipse.uml2.codegen.ecore.ui"));
        assertTrue(testBundles.contains("org.eclipse.uml2.common"));
        assertTrue(testBundles.contains("org.eclipse.uml2.common.edit"));
        assertTrue(testBundles.contains("org.objectweb.asm"));
        assertTrue(testBundles.contains("de.uka.ipd.sdq.stoex"));
        assertTrue(testBundles.contains("org.eclipse.emf"));
        assertTrue(testBundles.contains("org.eclipse.xtext.util"));
        assertTrue(testBundles.contains("org.antlr.runtime"));
        assertTrue(testBundles.contains("org.eclipse.xtext.common.types"));
        assertTrue(testBundles.contains("org.eclipse.xtext.xbase.lib"));
    }
    
    @Test
    public void testHandleOptionalDependencies() throws IOException {
        URL manifestURL = getClass().getResource("/commons.stoex_manifest.mf");
        ManifestMF manifest = new ManifestMF(manifestURL.openStream());
        Set<String> actualBundles = manifest.getRequiredBundles(false);
        Set<String> expectedBundles = Sets.of("org.eclipse.xtext",
                "org.eclipse.uml2",
                "org.eclipse.uml2.codegen.ecore",
                "org.eclipse.uml2.codegen.ecore.ui",
                "org.eclipse.uml2.common",
                "org.eclipse.uml2.common.edit",
                "de.uka.ipd.sdq.stoex",
                "org.eclipse.emf",
                "org.eclipse.xtext.util",
                "org.antlr.runtime",
                "org.eclipse.xtext.common.types",
                "org.eclipse.xtext.xbase.lib");
        assertEquals(expectedBundles.size(), actualBundles.size());
        assertEquals(expectedBundles, actualBundles);
    }
    
    @Test
    public void testNoDependencies() throws IOException {
        URL manifestURL = getClass().getResource("/example_models_manifest.mf");
        ManifestMF manifest = new ManifestMF(manifestURL.openStream());
        Set<String> actualBundles = manifest.getRequiredBundles(false);
        assertEquals(0, actualBundles.size());
    }
}
