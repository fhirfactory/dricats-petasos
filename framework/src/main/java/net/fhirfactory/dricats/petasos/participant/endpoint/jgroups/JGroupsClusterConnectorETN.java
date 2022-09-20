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
package net.fhirfactory.dricats.petasos.participant.endpoint.jgroups;

import java.util.ArrayList;
import java.util.List;

import net.fhirfactory.dricats.model.topology.endpoints.jgroups.InitialHostSpecification;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import net.fhirfactory.dricats.model.configuration.filebased.archetypes.valuesets.ClusterFunctionNameEnum;
import net.fhirfactory.dricats.model.topology.endpoints.base.SocketBasedClientETN;
import net.fhirfactory.dricats.model.topology.endpoints.jgroups.adapters.JGroupsClientAdapter;

public class JGroupsClusterConnectorETN extends SocketBasedClientETN {
    private static final Logger LOG = LoggerFactory.getLogger(JGroupsClusterConnectorETN.class);

    private List<net.fhirfactory.dricats.model.topology.endpoints.jgroups.InitialHostSpecification> initialHosts;
    private String groupName;
    private String channelName;
    private String configurationFileName;
    private ClusterFunctionNameEnum channelServiceType;

    //
    // Constructor(s)
    //

    public JGroupsClusterConnectorETN(){
        super();
        this.initialHosts = new ArrayList<>();
        this.groupName = null;
        this.channelName = null;
        this.configurationFileName = null;
        this.channelServiceType = null;
    }

    public JGroupsClusterConnectorETN(JGroupsClusterConnectorETN ori){	
        super(ori);
        this.initialHosts = new ArrayList<>();
        this.groupName = null;
        this.channelName = null;
        this.configurationFileName = null;
        this.channelServiceType = null;
        
        if(!ori.getInitialHosts().isEmpty()){
            getInitialHosts().addAll(ori.getInitialHosts());
        }
        if(ori.hasGroupName()){
            setGroupName(ori.getGroupName());
        }
        if(ori.hasChannelName()){
            setChannelName(ori.getChannelName());
        }
        if(StringUtils.isNotEmpty(ori.getConfigurationFileName())){
            setConfigurationFileName(ori.getConfigurationFileName());
        }
        if(ori.hasChannelServiceType()) {
        	setChannelServiceType(ori.getChannelServiceType());
        }
    }

    //
    // Getters and Setters
    //

    @JsonIgnore
    public boolean hasChannelServiceType(){
        boolean hasValue = this.channelServiceType != null;
        return(hasValue);
    }
    
    public ClusterFunctionNameEnum getChannelServiceType() {
        return channelServiceType;
    }

    public void setChannelServiceType(ClusterFunctionNameEnum channelServiceType) {
        this.channelServiceType = channelServiceType;
    }

    @JsonIgnore
    public boolean hasConfigurationFileName(){
        boolean hasValue = this.configurationFileName != null;
        return(hasValue);
    }
    
    public String getConfigurationFileName() {
        return configurationFileName;
    }

    public void setConfigurationFileName(String configurationFileName) {
        this.configurationFileName = configurationFileName;
    }

    @JsonIgnore
    public boolean hasInitialHosts(){
        boolean hasValue = this.initialHosts != null;
        return(hasValue);
    }
    
    public List<net.fhirfactory.dricats.model.topology.endpoints.jgroups.InitialHostSpecification> getInitialHosts() {
        return initialHosts;
    }

    public void setInitialHosts(List<InitialHostSpecification> initialHosts) {
        this.initialHosts = initialHosts;
    }

    @JsonIgnore
    public boolean hasGroupName(){
        boolean hasValue = this.groupName != null;
        return(hasValue);
    }
    
    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String nameSpace) {
        this.groupName = nameSpace;
    }

    @JsonIgnore
    public boolean hasChannelName(){
        boolean hasValue = this.channelName != null;
        return(hasValue);
    }
    
    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    @JsonIgnore
    @Override
    protected Logger getLogger() {
        return (LOG);
    }

    @JsonIgnore
    public JGroupsClientAdapter getJGroupsAdapter(){
        if(getAdapterList().isEmpty()){
            return(null);
        }
        JGroupsClientAdapter jgroupsAdapter = (JGroupsClientAdapter) getAdapterList().get(0);
        return(jgroupsAdapter);
    }

    @JsonIgnore
    public void setJGroupsAdapter(JGroupsClientAdapter jgroupsAdapter){
        if(jgroupsAdapter != null){
            getAdapterList().add(jgroupsAdapter);
        }
    }

    //
    // To String
    //

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StandardEdgeIPCEndpoint{");
        sb.append("initialHosts=").append(initialHosts);
        sb.append(", groupName=").append(groupName);
        sb.append(", channelName=").append(channelName);
        sb.append(", configurationFileName=").append(configurationFileName);
        sb.append(", channelServiceType=").append(channelServiceType);
        sb.append(", ").append(super.toString()).append("}");
        return sb.toString();
    }


}
