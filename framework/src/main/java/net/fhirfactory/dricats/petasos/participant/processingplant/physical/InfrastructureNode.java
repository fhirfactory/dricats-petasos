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
import net.fhirfactory.dricats.model.simplified.resources.summaries.isc.ISCInfrastructureNodeSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class InfrastructureNode {
    private static final Logger LOG = LoggerFactory.getLogger(InfrastructureNode.class);

    private String nodeName;
    private String nodeDNSEntry;
    private String nodeAddress;

    @Inject
    private DeploymentSite deploymentSite;

    @Inject
    private EdgeProcessingPlantConfigurationService processingPlantConfigurationService;

    //
    // Constructor(s)
    //

    public InfrastructureNode(){
        this.nodeName = null;
        this.nodeAddress = null;
        this.nodeDNSEntry = null;
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

    public String getNodeDNSEntry() {
        return nodeDNSEntry;
    }

    public void setNodeDNSEntry(String nodeDNSEntry) {
        this.nodeDNSEntry = nodeDNSEntry;
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
    protected EdgeProcessingPlantConfigurationService getProcessingPlantConfigurationService(){
        return(this.processingPlantConfigurationService);
    }

    //
    // toString()
    //

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("InfrastructureNode{");
        sb.append("nodeName=").append(nodeName);
        sb.append(", nodeDNSEntry=").append(nodeDNSEntry);
        sb.append(", nodeAddress=").append(nodeAddress);
        sb.append('}');
        return sb.toString();
    }

    //
    // toSummary()
    //

    public ISCInfrastructureNodeSummary toSummary(){
        getLogger().debug(".toSummary(): Entry");
        ISCInfrastructureNodeSummary summary = new ISCInfrastructureNodeSummary();

        getLogger().debug(".toSummary(): Exit, summary->{}", summary);
        return(summary);
    }
}
