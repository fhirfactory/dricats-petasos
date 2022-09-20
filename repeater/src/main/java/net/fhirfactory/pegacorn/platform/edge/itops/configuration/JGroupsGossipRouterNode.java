/*
 * Copyright (c) 2021 Mark A. Hunter (ACT Health)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.fhirfactory.pegacorn.platform.edge.itops.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class JGroupsGossipRouterNode {
    private static final Logger LOG = LoggerFactory.getLogger(JGroupsGossipRouterNode.class);

    JGroupsGossipRouterNodeConfig propertyFile;

    //
    // Constructor(s)
    //

    public JGroupsGossipRouterNode(){
        this.propertyFile = readPropertyFile();
    }

    //
    // Getters and Setters
    //

    public void setPropertyFile(JGroupsGossipRouterNodeConfig propertyFile) {
        this.propertyFile = propertyFile;
    }

    public JGroupsGossipRouterNodeConfig getPropertyFile() {
        return propertyFile;
    }

    protected Logger getLogger(){
        return(LOG);
    }

    //
    // Business Logic
    //

    protected JGroupsGossipRouterNodeConfig readPropertyFile(){
        String propertyFileName = specifyPropertyFileName();
        getLogger().debug(".readPropertyFile(): Entry, propertyFileName->{}", propertyFileName);
        try {
            getLogger().trace(".readPropertyFile(): Establish YAML ObjectMapper");
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            yamlMapper.findAndRegisterModules();
            yamlMapper.configure(JsonParser.Feature.ALLOW_MISSING_VALUES, true);
            getLogger().trace(".readPropertyFile(): Reading YAML configuration file");
            this.propertyFile = (JGroupsGossipRouterNodeConfig) yamlMapper.readValue(new File(propertyFileName), JGroupsGossipRouterNodeConfig.class);
            getLogger().trace(".readPropertyFile(): Exit, file loaded, propertyFile->{}", this.propertyFile);
            return(this.propertyFile);
        } catch(FileNotFoundException noFile){
            getLogger().error(".readPropertyFile(): Configuration File->{} is not found, error->{}", propertyFileName, noFile);
        } catch(IOException ioError){
            getLogger().error(".readPropertyFile(): Configuration File->{} could not be loaded, error->{}", propertyFileName, ioError);
        }
        getLogger().info(".readPropertyFile(): failed to load file");
        return(null);
    }

    protected String specifyPropertyFileName() {
        getLogger().info(".specifyPropertyFileName(): Entry");
        String configurationFileName = getProperty("DEPLOYMENT_CONFIG_FILE");
//        String configurationFileName = System.getenv("SUBSYSTEM_CONFIG_FILE");
        if(configurationFileName == null){
            throw(new RuntimeException("Cannot load configuration file!!!! (SUBSYSTEM-CONFIG_FILE="+configurationFileName+")"));
        }
        getLogger().info(".specifyPropertyFileName(): Exit, filename->{}", configurationFileName);
        return configurationFileName;
    }

    public String getProperty(String propertyName) {
        String env = propertyName.toUpperCase(Locale.US);
        env = env.replace(".", "_");
        env = env.replace("-", "_");

        String value = System.getenv(env);
        if (StringUtils.isNotBlank(value)) {
            getLogger().info(".getProperty(): Found value in environment variable");
            if (! propertyName.equals(env)) {
                getLogger().info(".getProperty(): env -> {}", env);
            }
        } else {
            getLogger().error(".getProperty(): Cannot find environment variable");
            return(null);
        }

        value = value == null ? null : value.trim();

        //Log at warning level so the logs are always shown
        getLogger().debug(".getProperty(): propertyName->{}, value->{}", propertyName, value);

        return value;
    }

    @Override
    public String toString() {
        return "JGroupsGossipRouterNode{" +
                "propertyFile=" + propertyFile +
                '}';
    }
}
