package com.dnainvest.misc;

import com.dnainvest.main.Main;

import java.io.*;
import java.util.Properties;

public class PropertiesHandler {
    private Properties properties = new Properties();

    public PropertiesHandler() {
        try (InputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getProperty(String property) {
        String retVal = null;
        try {
            retVal = properties.getProperty(property);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retVal;
    }


    public int getProperty(String property, int... x) {
        int retVal = 0;
        try {
            retVal = Integer.valueOf(properties.getProperty(property));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retVal;
    }

    public void setProperty(String property, String value) {
        try (OutputStream output = new FileOutputStream("src/config.properties")) {
            properties = new Properties();
            properties.setProperty(property, value);
            // save properties to project root folder
            properties.store(output, null);
            System.out.println(properties);
        } catch (IOException io) {
            io.printStackTrace();
        }
    }


}
