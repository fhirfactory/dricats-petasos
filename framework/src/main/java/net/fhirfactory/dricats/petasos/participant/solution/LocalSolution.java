/*
 * Copyright (c) 2020 Mark A. Hunter (ACT Health)
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
package net.fhirfactory.dricats.petasos.participant.solution;

import net.fhirfactory.dricats.configuration.defaults.dricats.systemwide.DeploymentSystemIdentificationInterface;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.component.valuesets.ComponentTypeEnum;
import net.fhirfactory.dricats.model.topology.nodes.SolutionTopologyNode;
import net.fhirfactory.dricats.model.topology.nodes.base.TopologyNode;
import net.fhirfactory.dricats.model.topology.nodes.datatypes.TopologyNodeFDN;
import net.fhirfactory.dricats.model.petasos.participant.valuesets.NetworkSecurityZoneEnum;
import org.hl7.fhir.r4.model.Organization;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mark A. Hunter
 * @since 2020-07-01
 */

public abstract class LocalSolution extends SolutionTopologyNode implements DeploymentSystemIdentificationInterface {

    private ConcurrentHashMap<ComponentId, TopologyNode> nodeIdMap;
    private ConcurrentHashMap<String, TopologyNode> nodeNameMap;
    private Object nodeIdMapLock;
    private Object nodeNameMapLock;

    public LocalSolution() {
        getLogger().debug(".TopologyDM(): Constructor initialisation");
        this.nodeIdMap = new ConcurrentHashMap<>();
        this.nodeNameMap = new ConcurrentHashMap<>();
        this.nodeIdMapLock = new Object();
        this.nodeNameMapLock = new Object();
    }

    private void insertOrOverwriteTopologyNode(SolutionTopologyNode node){
        getLogger().debug(".insertOrOverwriteTopologyNode(): Entry");
        synchronized (getNodeIdMap()) {
            if (nodeIdMap.containsKey(node.getNodeId())) {
                nodeIdMap.remove(node.getNodeId());
            }
            nodeIdMap.put(node.getNodeId(), node);
        }
        getLogger().debug(".insertOrOverwriteTopologyNode(): Exit");
    }

    @Override
    public String getSystemName() {
        return (getParticipantName());
    }

    @Override
    public String getSystemVersion() {
        return (getNodeType().getVersion());
    }

    public ComponentId getSystemId() {
        return (getNodeId());
    }

    @Override
    public Organization getSystemOwnerName() {
        return (getSystemOwner());
    }

    /**
     * This function adds an entry to the Element Set.
     * <p>
     * Note that the default behaviour is to UPDATE the values with the set if
     * there already exists an instance for the specified FDNToken (identifier).
     *
     * Note, we have to do a deep inspection of the ConcurrentHashMap key (FDNToken) content,
     * as the default only only looks for equivalence with respect to the action Object instance.
     *
     * @param newElement The NodeElement to be added to the Set
     */
    public void addTopologyNode(TopologyNode newElement) {
        getLogger().debug(".addTopologyNode(): Entry, newElement --> {}", newElement);
        if (newElement == null) {
            throw (new IllegalArgumentException(".addTopologyNode(): newElement is null"));
        }
        if (newElement.getNodeId() == null) {
            throw (new IllegalArgumentException(".addTopologyNode(): bad Id within newElement"));
        }
        synchronized (getNodeMapLock()) {
            ComponentId componentId = null;
            if(getNodeIdMap().containsKey(newElement.getNodeId())){
                TopologyNode node = getNodeIdMap().get(newElement.getNodeId());
                if(node != null){
                    componentId = node.getNodeId();
                }
                getNodeIdMap().remove(newElement.getNodeId());
            }
            if(componentId != null){
                if(getNodeIdMap().containsKey(componentId)) {
                    getNodeIdMap().remove(componentId);
                }
            }
            getNodeIdMap().put(newElement.getNodeId(), newElement);
        }
        getLogger().debug(".addTopologyNode(): Exit");
    }

    /**
     *
     * @param elementID
     */
    public void deleteTopologyNode(TopologyNodeFDN elementID) {
        getLogger().debug(".deleteTopologyNode(): Entry, elementID --> {}", elementID);
        if (elementID == null) {
            throw (new IllegalArgumentException(".removeNode(): elementID is null"));
        }
        synchronized (getNodeMapLock()){
            if(getNodeIdMap().containsKey(elementID)){
                getNodeIdMap().remove(elementID);
            }
            TopologyNode node = getNodeIdMap().get(elementID);
            ComponentId componentId = null;
            if(node != null){
                componentId = node.getNodeId();
            }
            if(componentId != null) {
                if (nodeIdMap.containsKey(componentId)){
                    nodeIdMap.remove(componentId);
                }
            }
        }
        getLogger().debug(".deleteTopologyNode(): Exit");
    }

    /**
     *
     * @param componentId
     */
    public void deleteTopologyNode(ComponentId componentId) {
        getLogger().debug(".deleteTopologyNode(): Entry, componentId --> {}", componentId);
        if (componentId == null) {
            throw (new IllegalArgumentException(".removeNode(): elementID is null"));
        }
        synchronized (getNodeMapLock()){
            ComponentId nodeId = null;
            if(getNodeIdMap().containsKey(componentId)){
                TopologyNode node = nodeIdMap.get(componentId);
                if(node != null) {
                    nodeId = node.getNodeId();
                }
                getNodeIdMap().remove(componentId);
            }
            if(nodeId != null){
                if(getNodeIdMap().containsKey(nodeId)){
                    getNodeIdMap().remove(nodeId);
                }
            }
        }
        getLogger().debug(".deleteTopologyNode(): Exit");
    }

    /**
     *
     * @return
     */
    public Set<TopologyNode> getTopologyNodeSet() {
        getLogger().debug(".getTopologyNodeSet(): Entry");
        Set<TopologyNode> elementSet = new LinkedHashSet<TopologyNode>();
        if (getNodeIdMap().isEmpty()) {
            getLogger().debug(".getTopologyNodeSet(): Exit, The module map is empty, returning null");
            return (null);
        }
        synchronized (getNodeMapLock()) {
            elementSet.addAll(getNodeIdMap().values());
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug(".getTopologyNodeSet(): Exit, returning an element set, size --> {}", elementSet.size());
        }
        return (elementSet);
    }

    public TopologyNode getTopologyNode(ComponentId nodeId) {
        getLogger().debug(".getTopologyNode(): Entry, nodeId --> {}", nodeId);
        if (nodeId == null) {
            getLogger().debug(".getTopologyNode(): Exit, provided a null nodeId , so returning null");
            return (null);
        }
        TopologyNode retrievedNode = null;
        synchronized (getNodeMapLock()) {
            retrievedNode = getNodeIdMap().get(nodeId);
        }
        getLogger().debug(".getTopologyNode(): Exit, retrievedNode->{}", retrievedNode);
        return (retrievedNode);
    }


    private boolean stringValuesMatch(String stringA, String stringB){
        if(stringA == null && stringB == null){
            return(true);
        }
        if(stringA == null || stringB == null){
            return(false);
        }
        boolean stringsAreEqual = stringA.contentEquals(stringB);
        return(stringsAreEqual);
    }

    public List<TopologyNode> getTopologyNodes(ComponentTypeEnum nodeType, String nodeName, String nodeVersion){
        getLogger().debug(".getTopologyNodes(): Entry, nodeType->{}, nodeName->{}, nodeVersion->{}", nodeType, nodeName, nodeVersion);
        List<TopologyNode> nodeList = new ArrayList<>();
        Collection<TopologyNode> topologyNodes= null;
        getLogger().trace(".getTopologyNodes(): Getting the set of existing node FDNs - start");
        synchronized (getNodeMapLock()) {
            topologyNodes = getNodeIdMap().values();
        }
        getLogger().trace(".getTopologyNodes(): Getting the set of existing node FDNs - End");
        getLogger().trace(".getTopologyNodes(): Now interating through to see if we can found the required node");
        for (TopologyNode currentNode: topologyNodes) {
            if (getLogger().isTraceEnabled()) {
                getLogger().trace(".getSoftwareComponent(): Search Cache Entry : node.Id->{}, node.participantName, nodeComponentType->{}", currentNode.getNodeId(), currentNode.getParticipantName(), currentNode.getNodeType());
            }
            getLogger().trace(".getTopologyNodes(): Comparing nodeType: Start");
            boolean nodeTypeMatches = nodeType.equals(currentNode.getNodeType().getComponentArchetype());
            getLogger().trace(".getTopologyNodes(): Comparing nodeType: Finish, result->{}", nodeTypeMatches);
            getLogger().trace(".getTopologyNodes(): Comparing nodeName: Start");
            boolean nodeNameMatches = nodeName.contentEquals(getParticipantName());
            getLogger().trace(".getTopologyNodes(): Comparing nodeName: Finish, result->{}", nodeNameMatches);
            getLogger().trace(".getTopologyNodes(): Comparing nodeName: Version");
            boolean nodeVersionMatches = nodeVersion.contentEquals(currentNode.getNodeType().getVersion());
            getLogger().trace(".getTopologyNodes(): Comparing nodeVersion: Finish, result->{}", nodeVersionMatches);
            if (nodeTypeMatches && nodeNameMatches && nodeVersionMatches) {
                getLogger().trace(".getTopologyNodes(): Node found!!! Adding to search result!");
                nodeList.add(currentNode);
            }
        }
        getLogger().trace(".getSoftwareComponent(): Exit");
        return(nodeList);
    }

    public NetworkSecurityZoneEnum getDeploymentNetworkSecurityZone(ComponentId id){
        Collection<TopologyNode> nodeCollection = null;
        synchronized (getNodeMapLock()) {
            nodeCollection = getNodeIdMap().values();
        }
        for(TopologyNode currentNode: nodeCollection){
            boolean nameSame = currentNode.getNodeId().equals(id);
            boolean isProcessingPlant = currentNode.getNodeType().getComponentArchetype().equals(ComponentTypeEnum.PROCESSING_PLANT);
            if(nameSame && isProcessingPlant){
                return(currentNode.getNetworkZone());
            }
        }
        return(NetworkSecurityZoneEnum.INTERNET);
    }
    
    
    //
    // Getters (and Setters)
    //

    protected ConcurrentHashMap<ComponentId, TopologyNode> getNodeIdMap(){
        return(getNodeIdMap());
    }
    
    protected Object getNodeMapLock(){
        return(this.nodeIdMapLock);
    }
}
