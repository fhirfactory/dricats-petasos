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
package net.fhirfactory.dricats.petasos.observations.configuration.topology.factories.common;

import net.fhirfactory.dricats.model.component.valuesets.ComponentTypeEnum;
import net.fhirfactory.dricats.model.simplified.resources.summaries.isc.base.ISCSummary;
import net.fhirfactory.dricats.model.petasos.participant.topology.ProcessingPlantPetasosParticipantNameHolder;
import net.fhirfactory.dricats.model.topology.endpoints.base.EndpointTopologyNode;
import net.fhirfactory.dricats.model.topology.nodes.softwarecomponents.base.SoftwareComponentTopologyNodeBase;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.inject.Inject;

public abstract class PetasosMonitoredComponentFactory {

    abstract protected Logger getLogger();

    @Inject
    private ProcessingPlantPetasosParticipantNameHolder participantNameHolder;

    /**
     * Creates the "base" MonitoredComponentSummary object
     *
     * @param monitoredNode The SoftwareComponentSummary shell object to be populated
     * @param topologyNode The SoftwareComponent on which the SoftwareCOmponentSummary is to be Based
     * @return
     */
    protected ISCSummary newPetasosMonitoredComponent(ISCSummary monitoredNode, SoftwareComponentTopologyNodeBase topologyNode){
        getLogger().debug(".newITOpsMonitoredNode(): Entry, monitoredNode->{}, topologyNode->{}", monitoredNode, topologyNode);
        monitoredNode.setComponentId(topologyNode.getNodeId());
        if(topologyNode.hasSiteNode()) {
            monitoredNode.setSite(topologyNode.getSiteNode().getParticipantName());
        } else {
            monitoredNode.setSite("Unknown");
        }
        if(topologyNode.hasInstructureNode()) {
            monitoredNode.setInfrastructureNode(topologyNode.getInfrastructureNode().getParticipantName());
        } else {
            monitoredNode.setInfrastructureNode("Unknown");
        }
        if(topologyNode.hasPlatformNode()){
            monitoredNode.setPlatformNode(topologyNode.getPlatformNode().getParticipantName());
        } else {
            monitoredNode.setPlatformNode("Unknown");
        }
        monitoredNode.setSubsystemParticipantName(participantNameHolder.getSubsystemParticipantName());
        monitoredNode.setName(topologyNode.getParticipantName());
        if(StringUtils.isEmpty(topologyNode.getParticipantDisplayName())){
            monitoredNode.setDisplayName(monitoredNode.getName());
        } else {
            monitoredNode.setDisplayName(topologyNode.getParticipantDisplayName());
        }
        ComponentTypeEnum nodeTypeEnum = topologyNode.getNodeType().getComponentArchetype();
        monitoredNode.setComponentType(nodeTypeEnum);
        monitoredNode.setVersion(topologyNode.getNodeVersion());
        if(topologyNode.getConcurrencyMode() != null) {
            monitoredNode.setConcurrencyMode(topologyNode.getConcurrencyMode().getDisplayName());
        }
        if(topologyNode.getResilienceMode() != null) {
            monitoredNode.setResilienceMode(topologyNode.getResilienceMode().getDisplayName());
        }
        getLogger().debug(".newITOpsMonitoredNode(): Exit, monitoredNode->{}", monitoredNode);
        return(monitoredNode);
    }

    protected ISCSummary newPetasosMonitoredComponent(ISCSummary monitoredNode, EndpointTopologyNode topologyNode){
        getLogger().debug(".newITOpsMonitoredNode(): Entry, monitoredNode->{}, topologyNode->{}", monitoredNode, topologyNode);
        monitoredNode.setComponentId(topologyNode.getNodeId());
        monitoredNode.setSubsystemParticipantName(participantNameHolder.getSubsystemParticipantName());
        monitoredNode.setName(topologyNode.getParticipantName());
        if(StringUtils.isEmpty(topologyNode.getParticipantDisplayName())){
            monitoredNode.setDisplayName(monitoredNode.getName());
        } else {
            monitoredNode.setDisplayName(topologyNode.getParticipantDisplayName());
        }
        ComponentTypeEnum nodeTypeEnum = topologyNode.getNodeType().getComponentArchetype();
        monitoredNode.setComponentType(nodeTypeEnum);
        monitoredNode.setVersion(topologyNode.getNodeVersion());
        getLogger().debug(".newITOpsMonitoredNode(): Exit, monitoredNode->{}", monitoredNode);
        return(monitoredNode);
    }
}
