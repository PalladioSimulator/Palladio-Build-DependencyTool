package org.palladiosimulator.builddependencytool.tests;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.palladiosimulator.dependencytool.FeatureXML;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class FeatureXMLTest {

    @Test
    public void testPlugins() throws ParserConfigurationException, SAXException, IOException {
        URL featureURL = getClass().getResource("/pcm_feature.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document document = dBuilder.parse(featureURL.openStream());
        document.getDocumentElement().normalize();
        FeatureXML testFeatureXML = new FeatureXML(document, false);
        Set<String> testFeatures = testFeatureXML.getRequiredFeatures();
        Set<String> testPlugins = testFeatureXML.getRequiredBundles();
        assert(testFeatures.size() == 0);
        assert(testPlugins.size() == 6);
        assert(testPlugins.contains("org.palladiosimulator.pcm.resources"));
        assert(testPlugins.contains("org.palladiosimulator.pcm"));
        assert(testPlugins.contains("de.uka.ipd.sdq.pcm.stochasticexpressions"));
        assert(testPlugins.contains("de.uka.ipd.sdq.stoex.analyser"));
        assert(testPlugins.contains("org.palladiosimulator.pcm.ui"));
        assert(testPlugins.contains("org.palladiosimulator.pcm.help"));
    }
    
    @Test
    public void testFeatures() throws ParserConfigurationException, SAXException, IOException {
        URL featureURL = getClass().getResource("/core-commons_feature.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document document = dBuilder.parse(featureURL.openStream());
        document.getDocumentElement().normalize();
        FeatureXML testFeatureXML = new FeatureXML(document, false);
        Set<String> testFeatures = testFeatureXML.getRequiredFeatures();
        Set<String> testPlugins = testFeatureXML.getRequiredBundles();
        assert(testFeatures.size() == 7);
        assert(testPlugins.size() == 1);
        assert(testPlugins.contains("org.palladiosimulator.commons"));
        assert(testFeatures.contains("de.uka.ipd.sdq.dialogs.feature"));
        assert(testFeatures.contains("de.uka.ipd.sdq.errorhandling.feature"));
        assert(testFeatures.contains("de.uka.ipd.sdq.identifier.feature"));
        assert(testFeatures.contains("de.uka.ipd.sdq.stoex.feature"));
        assert(testFeatures.contains("de.uka.ipd.sdq.units.feature"));
        assert(testFeatures.contains("de.uka.ipd.sdq.statistics.feature"));
        assert(testFeatures.contains("de.uka.ipd.sdq.probfunction.feature"));
    }
    
    @Test
    public void testIncludeTrue() throws ParserConfigurationException, SAXException, IOException {
        URL featureURL = getClass().getResource("/core-commons_feature.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document document = dBuilder.parse(featureURL.openStream());
        document.getDocumentElement().normalize();
        FeatureXML testFeatureXML = new FeatureXML(document, true);
        Set<String> testFeatures = testFeatureXML.getRequiredFeatures();
        Set<String> testPlugins = testFeatureXML.getRequiredBundles();
        assert(testFeatures.size() == 7);
        assert(testPlugins.size() == 7);
        
        assert(testPlugins.contains("org.eclipse.emf.ecore"));
        assert(testPlugins.contains("org.eclipse.emf.edit"));
        assert(testPlugins.contains("org.eclipse.emf.edit.ui"));
        assert(testPlugins.contains("org.eclipse.core.runtime"));
        assert(testPlugins.contains("org.eclipse.core.resources"));
        assert(testPlugins.contains("org.palladiosimulator.branding"));
        
        assert(testPlugins.contains("org.palladiosimulator.commons"));
        assert(testFeatures.contains("de.uka.ipd.sdq.dialogs.feature"));
        assert(testFeatures.contains("de.uka.ipd.sdq.errorhandling.feature"));
        assert(testFeatures.contains("de.uka.ipd.sdq.identifier.feature"));
        assert(testFeatures.contains("de.uka.ipd.sdq.stoex.feature"));
        assert(testFeatures.contains("de.uka.ipd.sdq.units.feature"));
        assert(testFeatures.contains("de.uka.ipd.sdq.statistics.feature"));
        assert(testFeatures.contains("de.uka.ipd.sdq.probfunction.feature"));
    }

}
