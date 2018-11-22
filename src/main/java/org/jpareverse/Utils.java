package org.jpareverse;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Utils {

    public static Properties readPropertiesFile(String confDir, String propertiesFile) throws IOException {
        InputStream stream = null;
        Properties prop = new Properties();
        try {
            if (confDir == null) {
                confDir = ""; // coloca um nome de diretório para não encontrar
            }
            String fileName = confDir + propertiesFile;
            stream = new FileInputStream(fileName);
            prop.setProperty("propertiesFile", fileName);
        } catch (FileNotFoundException e) {
                throw new FileNotFoundException("property file '" + propertiesFile + "' not found in the classpath");
        }

        // load properties file into it
        prop.load(stream);
        stream.close();


        return prop;
    }
}
