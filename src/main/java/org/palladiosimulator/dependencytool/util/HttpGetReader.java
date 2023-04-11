package org.palladiosimulator.dependencytool.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public interface HttpGetReader {

    InputStream read(URL url) throws IOException;
    
}
