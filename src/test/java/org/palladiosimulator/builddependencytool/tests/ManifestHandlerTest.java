package org.palladiosimulator.builddependencytool.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.palladiosimulator.dependencytool.HttpGetReader;
import org.palladiosimulator.dependencytool.ManifestHandler;

public class ManifestHandlerTest {
    
    protected static HttpGetReader HTTP_GET_READER;
    
    @BeforeAll
    public static void init() throws MalformedURLException {
        Map<URL, String> urlMap = Map.of(new URL(
                "https://raw.githubusercontent.com/PalladioSimulator/Palladio-Core-PCM/master/bundles/org.palladiosimulator.pcm/META-INF/MANIFEST.MF"),
                "pcm_manifest.mf",
                new URL("https://raw.githubusercontent.com/PalladioSimulator/Palladio-Core-PCM/master/bundles/org.palladiosimulator.pcm.ui/META-INF/MANIFEST.MF"),
                "pcm_ui_manifest.mf");
        HTTP_GET_READER = createReader(urlMap);
    }
    
    @Test
    public void testOneBundle() throws IOException {
        Set<String> bundles = new HashSet<>();
        bundles.add("org.palladiosimulator.pcm");
        ManifestHandler handler = new ManifestHandler("Palladio-Core-PCM", bundles, HTTP_GET_READER);
        Set<String> dependencies = handler.getDependencies(true);
        assertTrue(dependencies.size() == 13);
        assertTrue(dependencies.contains("org.eclipse.core.runtime"));
        assertTrue(dependencies.contains("org.eclipse.emf.ecore"));
        assertTrue(dependencies.contains("org.eclipse.ocl"));
        assertTrue(dependencies.contains("org.eclipse.ocl.ecore"));
        assertTrue(dependencies.contains("org.eclipse.emf.cdo"));
        assertTrue(dependencies.contains("org.eclipse.emf.ecore.xmi"));
        assertTrue(dependencies.contains("de.uka.ipd.sdq.identifier"));
        assertTrue(dependencies.contains("de.uka.ipd.sdq.probfunction"));
        assertTrue(dependencies.contains("de.uka.ipd.sdq.stoex"));
        assertTrue(dependencies.contains("de.uka.ipd.sdq.units"));
        assertTrue(dependencies.contains("de.uka.ipd.sdq.errorhandling"));
        assertTrue(dependencies.contains("org.palladiosimulator.pcm.workflow"));
        assertTrue(dependencies.contains("org.palladiosimulator.commons.stoex.api"));
    }
    
    @Test
    public void testTwoBundles() throws IOException {
        Set<String> bundles = new HashSet<>();
        bundles.add("org.palladiosimulator.pcm");
        bundles.add("org.palladiosimulator.pcm.ui");
        ManifestHandler handler = new ManifestHandler("Palladio-Core-PCM", bundles, HTTP_GET_READER);
        Set<String> dependencies = handler.getDependencies(true);
        assertTrue(dependencies.size() == 24);
        assertTrue(dependencies.contains("org.eclipse.core.runtime"));
        assertTrue(dependencies.contains("org.eclipse.emf.ecore"));
        assertTrue(dependencies.contains("org.eclipse.ocl"));
        assertTrue(dependencies.contains("org.eclipse.ocl.ecore"));
        assertTrue(dependencies.contains("org.eclipse.emf.cdo"));
        assertTrue(dependencies.contains("org.eclipse.emf.ecore.xmi"));
        assertTrue(dependencies.contains("de.uka.ipd.sdq.identifier"));
        assertTrue(dependencies.contains("de.uka.ipd.sdq.probfunction"));
        assertTrue(dependencies.contains("de.uka.ipd.sdq.stoex"));
        assertTrue(dependencies.contains("de.uka.ipd.sdq.units"));
        assertTrue(dependencies.contains("de.uka.ipd.sdq.errorhandling"));
        assertTrue(dependencies.contains("org.eclipse.ui"));
        assertTrue(dependencies.contains("org.eclipse.ui.console"));
        assertTrue(dependencies.contains("org.eclipse.debug.ui"));
        assertTrue(dependencies.contains("org.eclipse.emf.edit"));
        assertTrue(dependencies.contains("org.eclipse.emf"));
        assertTrue(dependencies.contains("org.palladiosimulator.pcm"));
        assertTrue(dependencies.contains("org.eclipse.emf.edit.ui"));
        assertTrue(dependencies.contains("org.eclipse.ui.forms"));
        assertTrue(dependencies.contains("org.eclipse.ui.ide"));
        assertTrue(dependencies.contains("org.eclipse.sirius.ui"));
        assertTrue(dependencies.contains("org.eclipse.ui.workbench"));
        assertTrue(dependencies.contains("org.palladiosimulator.pcm.workflow"));
        assertTrue(dependencies.contains("org.palladiosimulator.commons.stoex.api"));
    }
    
    protected static HttpGetReader createReader(Map<URL, String> urlMap) {
        return new HttpGetReader() {
            @Override
            public InputStream read(URL innerUrl) throws IOException {
                assertTrue(urlMap.containsKey(innerUrl));
                return ManifestHandlerTest.class.getClassLoader().getResourceAsStream(urlMap.get(innerUrl));
            }
        };
    }

}
