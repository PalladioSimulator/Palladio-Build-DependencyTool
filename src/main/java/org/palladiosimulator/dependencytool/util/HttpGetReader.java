package org.palladiosimulator.dependencytool;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public interface HttpGetReader {

    InputStream read(URL url) throws IOException;
    
}
