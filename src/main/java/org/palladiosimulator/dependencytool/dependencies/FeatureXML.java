package org.palladiosimulator.dependencytool.dependencies;

import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Handling of feature.xml files.
 */
public class FeatureXML {
    
    private Set<String> featureSet = new HashSet<>();
    private Set<String> bundleSet = new HashSet<>();
    private String feature = "feature";
    private String plugin = "plugin";
    private String id = "id";
    
    /**
     * Create a new feature.xml object from a feature.xml file.
     * 
     * @param doc The content of the feature.xml file.
     * @param includeImports Additionally parse imports if true.
     */
    public FeatureXML(Document doc, Boolean includeImports) {
        NodeList nList = doc.getElementsByTagName(feature).item(0).getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node nNode = nList.item(i);
            Boolean isNode = nNode.getNodeType() == Node.ELEMENT_NODE;
            // if includeImports is true: check for plugin and feature imports
            if (includeImports && isNode && nNode.getNodeName().equals("requires")) {
                parseImports(nNode);
            }
            // check for sub feature or plugin definitions
            if (isNode && nNode.getNodeName().equals(feature)) {
                featureSet.add(nNode.getAttributes().getNamedItem(id).getTextContent());
            } else if (isNode && nNode.getNodeName().equals(plugin)) {
                bundleSet.add(nNode.getAttributes().getNamedItem(id).getTextContent());
            }
            // check for additionally includes features
            if (isNode && nNode.getNodeName().equals("includes")) {
                parseIncludes(nNode);
            }
        }
    }
    
    public Set<String> getRequiredFeatures() {
        return featureSet;
    }
    
    public Set<String> getRequiredBundles() {
        return bundleSet;
    }
    
    // Parse feature and plugin imports and add them to respective set.
    private void parseImports(Node nNode) {
        for (int j = 0; j < nNode.getChildNodes().getLength(); j++) {
            Node childNode = nNode.getChildNodes().item(j);
            if (childNode.getNodeName().equals("import")) {
                for (int k = 0; k < childNode.getAttributes().getLength(); k++) {
                    Node attr = childNode.getAttributes().item(k);
                    if (attr.getNodeName().equals(feature)) {
                        featureSet.add(attr.getTextContent());
                    } else if (attr.getNodeName().equals(plugin)) {
                        bundleSet.add(attr.getTextContent());
                    }
                }
            }
        }
    }
    
    // Parse includes and add them to featureSet.
    private void parseIncludes(Node nNode) {
        for (int i = 0; i < nNode.getAttributes().getLength(); i++) {
            Node attr = nNode.getAttributes().item(i);
            if (attr.getNodeName().equals(id)) {
                featureSet.add(attr.getTextContent());
            }
        }
    }
}
