/*
 * Copyright (c) 2022 Mark A. Hunter (ACT Health)
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
package net.fhirfactory.dricats.petasos.participant.processingplant.physical;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.fhirfactory.dricats.configuration.api.services.processingplant.EdgeProcessingPlantConfigurationService;
import net.fhirfactory.dricats.model.simplified.resources.summaries.isc.ISCPlatformNodeSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class PlatformNode {
    private static final Logger LOG = LoggerFactory.getLogger(PlatformNode.class);

    private String nodeName;
    private String nodeAddress;

    @Inject
    private DeploymentSite deploymentSite;

    @Inject
    private InfrastructureNode infrastructureNode;

    @Inject
    private EdgeProcessingPlantConfigurationService processingPlantConfigurationService;

    //
    // Constructor(s)
    //

    public PlatformNode(){
        this.nodeName = null;
        this.nodeAddress = null;
    }

    //
    // Post Construct
    //

    //
    // Getters and Setters
    //

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeAddress() {
        return nodeAddress;
    }

    public void setNodeAddress(String nodeAddress) {
        this.nodeAddress = nodeAddress;
    }

    @JsonIgnore
    protected Logger getLogger(){
        return(LOG);
    }

    @JsonIgnore
    protected InfrastructureNode getInfrastructureNode(){
        return(this.infrastructureNode);
    }

    @JsonIgnore
    protected DeploymentSite getDeploymentSite(){
        return(this.deploymentSite);
    }

    @JsonIgnore
    protected EdgeProcessingPlantConfigurationService getProcessingPlantConfigurationService(){
        return(this.processingPlantConfigurationService);
    }

    //
    // toString()
    //

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PlatformNode{");
        sb.append("nodeName='").append(nodeName).append('\'');
        sb.append(", nodeAddress='").append(nodeAddress).append('\'');
        sb.append('}');
        return sb.toString();
    }

    //
    // toSummary()
    //

    public ISCPlatformNodeSummary toSummary(){
        getLogger().debug(".toSummary(): Entry");
        ISCPlatformNodeSummary summary = new ISCPlatformNodeSummary();

        getLogger().debug(".toSummary(): Exit, summary->{}", summary);
        return(summary);
    }
}
