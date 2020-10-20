package org.palladiosimulator.builddependencytool.tests;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.palladiosimulator.dependencytool.ManifestHandler;

public class ManifestHandlerTest {
    
    @Test
    public void testOneBundle() throws IOException {
        Set<String> bundles = new HashSet<>();
        bundles.add("org.palladiosimulator.pcm");
        ManifestHandler handler = new ManifestHandler("Palladio-Core-PCM", bundles);
        Set<String> dependencies = handler.getDependencies();
        assert(dependencies.size() == 12);
        assert(dependencies.contains("org.eclipse.core.runtime"));
        assert(dependencies.contains("org.eclipse.emf.ecore"));
        assert(dependencies.contains("org.eclipse.ocl"));
        assert(dependencies.contains("org.eclipse.ocl.ecore"));
        assert(dependencies.contains("org.eclipse.emf.cdo"));
        assert(dependencies.contains("org.eclipse.emf.ecore.xmi"));
        assert(dependencies.contains("de.uka.ipd.sdq.identifier"));
        assert(dependencies.contains("de.uka.ipd.sdq.probfunction"));
        assert(dependencies.contains("de.uka.ipd.sdq.stoex"));
        assert(dependencies.contains("de.uka.ipd.sdq.units"));
        assert(dependencies.contains("de.uka.ipd.sdq.errorhandling"));
        assert(dependencies.contains("org.palladiosimulator.pcm.workflow"));
    }
    
    @Test
    public void testTwoBundles() throws IOException {
        Set<String> bundles = new HashSet<>();
        bundles.add("org.palladiosimulator.pcm");
        bundles.add("org.palladiosimulator.pcm.ui");
        ManifestHandler handler = new ManifestHandler("Palladio-Core-PCM", bundles);
        Set<String> dependencies = handler.getDependencies();
        assert(dependencies.size() == 23);
        assert(dependencies.contains("org.eclipse.core.runtime"));
        assert(dependencies.contains("org.eclipse.emf.ecore"));
        assert(dependencies.contains("org.eclipse.ocl"));
        assert(dependencies.contains("org.eclipse.ocl.ecore"));
        assert(dependencies.contains("org.eclipse.emf.cdo"));
        assert(dependencies.contains("org.eclipse.emf.ecore.xmi"));
        assert(dependencies.contains("de.uka.ipd.sdq.identifier"));
        assert(dependencies.contains("de.uka.ipd.sdq.probfunction"));
        assert(dependencies.contains("de.uka.ipd.sdq.stoex"));
        assert(dependencies.contains("de.uka.ipd.sdq.units"));
        assert(dependencies.contains("de.uka.ipd.sdq.errorhandling"));
        assert(dependencies.contains("org.eclipse.ui"));
        assert(dependencies.contains("org.eclipse.ui.console"));
        assert(dependencies.contains("org.eclipse.debug.ui"));
        assert(dependencies.contains("org.eclipse.emf.edit"));
        assert(dependencies.contains("org.eclipse.emf"));
        assert(dependencies.contains("org.palladiosimulator.pcm"));
        assert(dependencies.contains("org.eclipse.emf.edit.ui"));
        assert(dependencies.contains("org.eclipse.ui.forms"));
        assert(dependencies.contains("org.eclipse.ui.ide"));
        assert(dependencies.contains("org.eclipse.sirius.ui"));
        assert(dependencies.contains("org.eclipse.ui.workbench"));
        assert(dependencies.contains("org.palladiosimulator.pcm.workflow"));
    }

}
