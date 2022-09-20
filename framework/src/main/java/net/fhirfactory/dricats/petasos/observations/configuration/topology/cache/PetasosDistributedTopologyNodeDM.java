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
package net.fhirfactory.dricats.petasos.observations.configuration.topology.cache;

import net.fhirfactory.dricats.interfaces.petasos.participant.topology.ProcessingPlantConfigurationServiceInterface;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.petasos.ipc.technologies.jgroups.JGroupsChannelConnectorSummary;
import net.fhirfactory.dricats.model.simplified.resources.summaries.isc.ISCProcessingPlantSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class PetasosDistributedTopologyNodeDM {

    private static final Logger LOG = LoggerFactory.getLogger(PetasosDistributedTopologyNodeDM.class);

    private ConcurrentHashMap<ComponentId, JGroupsChannelConnectorSummary> nodeOriginatingPoint;
    private ConcurrentHashMap<ComponentId, ISCProcessingPlantSummary> nodeSet;
    private ConcurrentHashMap<ComponentId, String> participantMap;

    private Object activityLock;

    @Inject
    private ProcessingPlantConfigurationServiceInterface processingPlant;

    //
    // Constructor(s)
    //

    public PetasosDistributedTopologyNodeDM() {
        LOG.debug(".ITOpsCollatedNodesDM(): Constructor initialisation");
        this.nodeSet = new ConcurrentHashMap<>();
        this.nodeOriginatingPoint = new ConcurrentHashMap<>();
        this.participantMap = new ConcurrentHashMap<>();
        this.activityLock = new Object();
    }

    //
    // Business Functions
    //

    public void addProcessingPlant(JGroupsChannelConnectorSummary endpointID, ISCProcessingPlantSummary newProcessingPlant) {
        LOG.debug(".addTopologyNode(): Entry, newProcessingPlant --> {}", newProcessingPlant);
        if (newProcessingPlant == null) {
            throw (new IllegalArgumentException(".addTopologyNode(): newElement is null"));
        }
        if (newProcessingPlant.getComponentId() == null) {
            throw (new IllegalArgumentException(".addTopologyNode(): bad elementID within newElement"));
        }

        synchronized (activityLock) {
            String participantName = newProcessingPlant.getName();
            if(nodeOriginatingPoint.containsKey(newProcessingPlant.getComponentId())){
                nodeOriginatingPoint.remove(newProcessingPlant.getComponentId());
            }
            nodeOriginatingPoint.put(newProcessingPlant.getComponentId(), endpointID);
            if(nodeSet.containsKey(newProcessingPlant.getComponentId())){
                nodeSet.remove(newProcessingPlant.getComponentId());
            }
            nodeSet.put(newProcessingPlant.getComponentId(), newProcessingPlant);
            if(participantMap.containsKey(newProcessingPlant.getComponentId())){
                participantMap.remove(newProcessingPlant.getComponentId());
            }
            participantMap.put(newProcessingPlant.getComponentId(), newProcessingPlant.getName());
        }
        LOG.debug(".addTopologyNode(): Exit");
    }

    public void deleteProcessingPlant(ComponentId elementID) {
        LOG.debug(".deleteTopologyNode(): Entry, elementID --> {}", elementID);
        if (elementID == null) {
            throw (new IllegalArgumentException(".removeNode(): elementID is null"));
        }
        synchronized (activityLock) {
            if(nodeOriginatingPoint.containsKey(elementID)) {
                this.nodeOriginatingPoint.remove(elementID);
            }
            if(nodeSet.containsKey(elementID)) {
                this.nodeSet.remove(elementID);
            }
            if(participantMap.containsKey(elementID)) {
                this.participantMap.remove(elementID);
            }
        }
        LOG.debug(".deleteTopologyNode(): Exit");
    }

    public Set<ISCProcessingPlantSummary> getTopologyNodeSet() {
        LOG.debug(".getTopologyNodeSet(): Entry");
        Set<ISCProcessingPlantSummary> elementSet = new HashSet<ISCProcessingPlantSummary>();
        if (this.nodeSet.isEmpty()) {
            LOG.debug(".getTopologyNodeSet(): Exit, The module map is empty, returning empty set");
            return (elementSet);
        }
        synchronized (activityLock) {
            elementSet.addAll(this.nodeSet.values());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(".getTopologyNodeSet(): Exit, returning an element set, size --> {}", elementSet.size());
        }
        return (elementSet);
    }

    public ISCProcessingPlantSummary getProcessingPlant(ComponentId nodeID) {
        LOG.debug(".getTopologyNode(): Entry, nodeID --> {}", nodeID);
        if (nodeID == null) {
            LOG.debug(".getTopologyNode(): Exit, provided a null nodeID , so returning null");
            return (null);
        }
        ISCProcessingPlantSummary summary = null;
        synchronized (activityLock){
            nodeSet.get(nodeID);
        }
        LOG.debug(".getTopologyNode(): Exit, returning null as an element with the specified ID was not in the map");
        return (null);
    }

    //
    // Processing Plant Origination Actions
    //

    public void removeDiscoveredProcessingPlant(JGroupsChannelConnectorSummary discoveredProcessingPlant){
        LOG.debug(".removeDiscoveredProcessingPlant(): Entry, discoveredProcessingPlant->{}", discoveredProcessingPlant);
        List<ComponentId> associatedNodes = new ArrayList<>();
        synchronized(activityLock) {
            Enumeration<ComponentId> nodeFDNs = this.nodeOriginatingPoint.keys();
            while (nodeFDNs.hasMoreElements()) {
                ComponentId currentId = nodeFDNs.nextElement();
                JGroupsChannelConnectorSummary currentIP = this.nodeOriginatingPoint.get(currentId);
                if (currentIP.getChannelName().equals(discoveredProcessingPlant.getChannelName())) {
                    associatedNodes.add(currentId);
                }
            }
            for (ComponentId currentId : associatedNodes) {
                this.nodeOriginatingPoint.remove(currentId);
                this.nodeSet.remove(currentId);
                this.participantMap.remove(currentId);
            }
        }
    }

}
