
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
package net.fhirfactory.dricats.petasos.observations.configuration.topology.factories;

import net.fhirfactory.dricats.interfaces.petasos.participant.topology.ProcessingPlantConfigurationServiceInterface;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.simplified.resources.summaries.isc.ISCEndpointSummary;
import net.fhirfactory.dricats.model.simplified.resources.summaries.isc.ISCPortSummary;
import net.fhirfactory.dricats.model.simplified.resources.summaries.isc.base.ISCSummary;
import net.fhirfactory.dricats.model.topology.endpoints.base.EndpointTopologyNode;
import net.fhirfactory.dricats.model.topology.endpoints.base.adapters.IPCAdapter;
import net.fhirfactory.dricats.model.topology.endpoints.base.valuesets.EndpointTopologyNodeTypeEnum;
import net.fhirfactory.dricats.model.topology.endpoints.file.FileShareSinkETN;
import net.fhirfactory.dricats.model.topology.endpoints.file.FileShareSourceETN;
import net.fhirfactory.dricats.model.topology.endpoints.file.adapter.FileShareSinkAdapter;
import net.fhirfactory.dricats.model.topology.endpoints.file.adapter.FileShareSourceAdapter;
import net.fhirfactory.dricats.model.topology.endpoints.http.HTTPClientETN;
import net.fhirfactory.dricats.model.topology.endpoints.http.HTTPServerETN;
import net.fhirfactory.dricats.model.topology.endpoints.http.adapters.HTTPClientAdapter;
import net.fhirfactory.dricats.model.topology.endpoints.jgroups.JGroupsClusterConnectorETN;
import net.fhirfactory.dricats.model.topology.endpoints.jgroups.adapters.JGroupsClientAdapter;
import net.fhirfactory.dricats.model.topology.endpoints.mllp.MLLPClientETN;
import net.fhirfactory.dricats.model.topology.endpoints.mllp.MLLPServerETN;
import net.fhirfactory.dricats.model.topology.endpoints.mllp.adapters.MLLPClientAdapter;
import net.fhirfactory.dricats.petasos.observations.configuration.topology.factories.common.PetasosMonitoredComponentFactory;
import net.fhirfactory.dricats.petasos.participant.solution.LocalSolution;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class PetasosEndpointSummaryFactory extends PetasosMonitoredComponentFactory {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosEndpointSummaryFactory.class);

    @Inject
    private LocalSolution topologyIM;

    @Inject
    private ProcessingPlantConfigurationServiceInterface processingPlant;

    @Override
    protected Logger getLogger() {
        return (LOG);
    }

    public ISCSummary newEndpoint(String wupParticipantName, EndpointTopologyNode endpointTopologyNode){
        getLogger().debug(".newEndpoint(): Entry, endpointTopologyNode->{}", endpointTopologyNode);
        if(endpointTopologyNode == null){
            return(null);
        }
        ISCEndpointSummary endpoint = new ISCEndpointSummary();
        endpoint = (ISCEndpointSummary) newPetasosMonitoredComponent(endpoint, endpointTopologyNode);
        endpoint.setEndpointType(endpointTopologyNode.getEndpointType());
        endpoint.setWupParticipantName(wupParticipantName);
        if(StringUtils.isNotEmpty(endpointTopologyNode.getParticipantDisplayName())) {
            endpoint.setDisplayName(endpointTopologyNode.getParticipantDisplayName());
        } else {
            endpoint.setDisplayName(endpoint.getWupParticipantName());
        }
        boolean isEncrypted = false;
        for(IPCAdapter currentAdapter: endpointTopologyNode.getAdapterList()){
            if(currentAdapter.isEncrypted()){
                isEncrypted = true;
                break;
            }
        }
        switch(endpointTopologyNode.getEndpointType()){
            case JGROUPS_INTEGRATION_POINT: {
                JGroupsClusterConnectorETN jgroupsEndpoint = (JGroupsClusterConnectorETN)endpointTopologyNode;
                if(jgroupsEndpoint.getAdapterList() != null) {
                    for(IPCAdapter currentAdapter: jgroupsEndpoint.getAdapterList()) {
                        JGroupsClientAdapter jgroupsAdapter = (JGroupsClientAdapter) currentAdapter;
                        ISCPortSummary portSummary = new ISCPortSummary();
                        portSummary.setEncrypted(isEncrypted);
                        portSummary.setPortType(EndpointTopologyNodeTypeEnum.JGROUPS_INTEGRATION_POINT.getDisplayName());
                        if (jgroupsAdapter.getPortNumber() != null) {
                            portSummary.setHostPort(Integer.toString(jgroupsAdapter.getPortNumber()));
                        } else {
                            portSummary.setHostPort("Unknown");
                        }
                        portSummary.setHostDNSName(jgroupsAdapter.getHostName());
                        ComponentId componentId = new ComponentId();
                        componentId.setId(portSummary.getHostDNSName()+"-"+portSummary.getHostPort());
                        componentId.setDisplayName(portSummary.getHostDNSName()+"-"+portSummary.getHostPort());
                        portSummary.setComponentId(componentId);
                        endpoint.getClientPorts().add(portSummary);
                    }
                }
                break;
            }
            case HTTP_API_SERVER:
                HTTPServerETN httpServerETN = (HTTPServerETN)endpointTopologyNode;
                if(httpServerETN.getHTTPServerAdapter() != null) {
                    ISCPortSummary portSummary = new ISCPortSummary();
                    portSummary.setHostPort(Integer.toString(httpServerETN.getHTTPServerAdapter().getPortNumber()));
                    portSummary.setHostDNSName(httpServerETN.getHTTPServerAdapter().getHostName());
                    if(StringUtils.isNotEmpty(httpServerETN.getConnectedSystemName())) {
                        endpoint.setConnectedSystemName(httpServerETN.getConnectedSystemName());
                    } else {
                        endpoint.setConnectedSystemName("Not Specified In Configuration");
                    }
                    if(httpServerETN.getHTTPServerAdapter().getServicePortValue() != null) {
                        portSummary.setServicePort(Integer.toString(httpServerETN.getHTTPServerAdapter().getServicePortValue()));
                    } else {
                        portSummary.setServicePort("Unknown");
                    }
                    if(StringUtils.isNotEmpty(httpServerETN.getHTTPServerAdapter().getServiceDNSName())) {
                        portSummary.setServiceDNSName(httpServerETN.getHTTPServerAdapter().getServiceDNSName());
                    } else {
                        portSummary.setServiceDNSName("Unknown");
                    }
                    portSummary.setPortType(EndpointTopologyNodeTypeEnum.HTTP_API_SERVER.getDisplayName());
                    ComponentId componentId = new ComponentId();
                    componentId.setId(portSummary.getHostDNSName()+"-"+portSummary.getHostPort());
                    componentId.setDisplayName(portSummary.getHostDNSName()+"-"+portSummary.getHostPort());
                    portSummary.setComponentId(componentId);
                    endpoint.getServerPorts().add(portSummary);
                    endpoint.setServer(true);
                }
                break;
            case HTTP_API_CLIENT:
                HTTPClientETN httpClient = (HTTPClientETN)endpointTopologyNode;
                if(httpClient.getHTTPClientAdapters() != null) {
                    for(HTTPClientAdapter currentAdapter: httpClient.getHTTPClientAdapters()) {
                        ISCPortSummary portSummary = new ISCPortSummary();
                        portSummary.setComponentId(httpClient.getNodeId());
                        portSummary.setHostPort(Integer.toString(currentAdapter.getPortNumber()));
                        portSummary.setHostDNSName(currentAdapter.getHostName());
                        if(httpClient.hasConnectedSystemName()) {
                            endpoint.setConnectedSystemName(httpClient.getConnectedSystemName());
                        } else {
                            endpoint.setConnectedSystemName("Not Specified In Configuration");
                        }
                        if(currentAdapter.getPortNumber() != null) {
                            portSummary.setServicePort(Integer.toString(currentAdapter.getPortNumber()));
                        } else {
                            portSummary.setServicePort("Unknown");
                        }
                        if(StringUtils.isNotEmpty(currentAdapter.getHostName())) {
                            portSummary.setServiceDNSName(currentAdapter.getHostName());
                        } else {
                            portSummary.setServiceDNSName("Unknown");
                        }
                        portSummary.setPortType(EndpointTopologyNodeTypeEnum.HTTP_API_CLIENT.getDisplayName());
                        ComponentId componentId = new ComponentId();
                        componentId.setId(portSummary.getHostDNSName()+"-"+portSummary.getHostPort());
                        componentId.setDisplayName(portSummary.getHostDNSName()+"-"+portSummary.getHostPort());
                        portSummary.setComponentId(componentId);
                        endpoint.getServerPorts().add(portSummary);
                    }
                }
                break;
            case MLLP_SERVER: {
                MLLPServerETN mllpServerEndpoint = (MLLPServerETN)endpointTopologyNode;
                if(mllpServerEndpoint.getMLLPServerAdapter() != null) {
                    ISCPortSummary portSummary = new ISCPortSummary();
                    portSummary.setName(mllpServerEndpoint.getParticipantName());
                    portSummary.setDisplayName(mllpServerEndpoint.getParticipantDisplayName());
                    portSummary.setComponentId(mllpServerEndpoint.getNodeId());
                    portSummary.setHostPort(Integer.toString(mllpServerEndpoint.getMLLPServerAdapter().getPortNumber()));
                    portSummary.setHostDNSName(mllpServerEndpoint.getMLLPServerAdapter().getHostName());
                    if(mllpServerEndpoint.hasConnectedSystemName()) {
                        endpoint.setConnectedSystemName(mllpServerEndpoint.getConnectedSystemName());
                    } else {
                        endpoint.setConnectedSystemName("Not Specified In Configuration");
                    }
                    if(mllpServerEndpoint.getMLLPServerAdapter().getServicePortValue() != null) {
                        portSummary.setServicePort(Integer.toString(mllpServerEndpoint.getMLLPServerAdapter().getServicePortValue()));
                    } else {
                        portSummary.setServicePort("Unknown");
                    }
                    if(StringUtils.isNotEmpty(mllpServerEndpoint.getMLLPServerAdapter().getServiceDNSName())) {
                        portSummary.setServiceDNSName(mllpServerEndpoint.getMLLPServerAdapter().getServiceDNSName());
                    } else {
                        portSummary.setServiceDNSName("Unknown");
                    }
                    portSummary.setPortType(EndpointTopologyNodeTypeEnum.MLLP_SERVER.getDisplayName());
                    ComponentId componentId = new ComponentId();
                    componentId.setId(portSummary.getHostDNSName()+"-"+portSummary.getHostPort());
                    componentId.setDisplayName(portSummary.getHostDNSName()+"-"+portSummary.getHostPort());
                    portSummary.setComponentId(componentId);
                    endpoint.getServerPorts().add(portSummary);
                    endpoint.setServer(true);
                }
                break;
            }
            case MLLP_CLIENT: {
                MLLPClientETN mllpClientEndpoint = (MLLPClientETN)endpointTopologyNode;
                if(mllpClientEndpoint.getMLLPClientAdapters() != null) {
                    if (!mllpClientEndpoint.getMLLPClientAdapters().isEmpty()) {
                        for(MLLPClientAdapter currentAdapter: mllpClientEndpoint.getMLLPClientAdapters()){
                            ISCPortSummary portSummary = new ISCPortSummary();
                            portSummary.setName(mllpClientEndpoint.getParticipantName());
                            portSummary.setDisplayName(mllpClientEndpoint.getParticipantDisplayName());
                            portSummary.setEncrypted(currentAdapter.isEncrypted());
                            portSummary.setPortType(EndpointTopologyNodeTypeEnum.MLLP_CLIENT.getDisplayName());
                            if(mllpClientEndpoint.hasConnectedSystemName()) {
                                endpoint.setConnectedSystemName(mllpClientEndpoint.getConnectedSystemName());
                            } else {
                                endpoint.setConnectedSystemName("Not Specified In Configuration");
                            }
                            portSummary.setHostDNSName(currentAdapter.getHostName());
                            if (currentAdapter.getPortNumber() != null) {
                                portSummary.setHostPort(Integer.toString(currentAdapter.getPortNumber()));
                            } else {
                                portSummary.setHostPort("Unknown");
                            }
                            ComponentId componentId = new ComponentId();
                            componentId.setId(portSummary.getHostDNSName()+"-"+portSummary.getHostPort());
                            componentId.setDisplayName(portSummary.getHostDNSName()+"-"+portSummary.getHostPort());
                            portSummary.setComponentId(componentId);
                            endpoint.getServerPorts().add(portSummary);
                        }
                    }
                }
                break;
            }

            case SQL_SERVER:
                break;
            case SQL_CLIENT:
                break;
            case LDAP_SERVER:
                break;
            case LDAP_CLIENT:
                break;
            case FILE_SHARE_SOURCE: {
                FileShareSourceETN fileSourceEndpoint = (FileShareSourceETN) endpointTopologyNode;
                if (fileSourceEndpoint.getAdapterList() != null) {
                    if (!fileSourceEndpoint.getAdapterList().isEmpty()) {
                        for (IPCAdapter currentIPCAdapter : fileSourceEndpoint.getAdapterList()) {
                            FileShareSourceAdapter currentAdapter = (FileShareSourceAdapter) currentIPCAdapter;
                            ISCPortSummary portSummary = new ISCPortSummary();
                            portSummary.setName(fileSourceEndpoint.getParticipantName());
                            portSummary.setDisplayName(fileSourceEndpoint.getParticipantDisplayName());
                            portSummary.setEncrypted(currentAdapter.isEncrypted());
                            portSummary.setPortType(EndpointTopologyNodeTypeEnum.FILE_SHARE_SOURCE.getDisplayName());
                            if (fileSourceEndpoint.hasConnectedSystemName()) {
                                endpoint.setConnectedSystemName(fileSourceEndpoint.getConnectedSystemName());
                            } else {
                                endpoint.setConnectedSystemName("Not Specified In Configuration");
                            }
                            portSummary.setDisplayName(currentAdapter.getFilePathAlias());
                            portSummary.setBasePath(currentAdapter.getFilePath());
                            portSummary.setHostDNSName(currentAdapter.getHostName());
                            ComponentId componentId = new ComponentId();
                            componentId.setId( processingPlant.getSubsystemParticipantName() + "-" + portSummary.getHostDNSName());
                            componentId.setDisplayName(processingPlant.getSubsystemParticipantName() + "-" + portSummary.getHostDNSName());
                            portSummary.setComponentId(componentId);
                            endpoint.getServerPorts().add(portSummary);
                        }
                    }
                }
                break;
            }
            case FILE_SHARE_SINK: {
                FileShareSinkETN fileSinkEndpoint = (FileShareSinkETN) endpointTopologyNode;
                if (fileSinkEndpoint.getAdapterList() != null) {
                    if (!fileSinkEndpoint.getAdapterList().isEmpty()) {
                        for (IPCAdapter currentIPCAdapter : fileSinkEndpoint.getAdapterList()) {
                            FileShareSinkAdapter currentAdapter = (FileShareSinkAdapter) currentIPCAdapter;
                            ISCPortSummary portSummary = new ISCPortSummary();
                            portSummary.setName(fileSinkEndpoint.getParticipantName());
                            portSummary.setDisplayName(fileSinkEndpoint.getParticipantDisplayName());
                            portSummary.setEncrypted(currentAdapter.isEncrypted());
                            portSummary.setPortType(EndpointTopologyNodeTypeEnum.FILE_SHARE_SINK.getDisplayName());
                            if (fileSinkEndpoint.hasConnectedSystemName()) {
                                endpoint.setConnectedSystemName(fileSinkEndpoint.getConnectedSystemName());
                            } else {
                                endpoint.setConnectedSystemName("Not Specified In Configuration");
                            }
                            portSummary.setHostDNSName(currentAdapter.getHostName());
                            portSummary.setBasePath(currentAdapter.getFilePath());
                            portSummary.setDisplayName(currentAdapter.getFilePathAlias());
                            ComponentId componentId = new ComponentId();
                            componentId.setId(processingPlant.getSubsystemParticipantName() + "-" + portSummary.getHostDNSName());
                            componentId.setDisplayName(processingPlant.getSubsystemParticipantName() + "-" + portSummary.getHostDNSName());
                            portSummary.setComponentId(componentId);
                            endpoint.getServerPorts().add(portSummary);
                        }
                    }
                }
                break;
            }
            case OTHER_API_SERVER:
                break;
            case OTHER_API_CLIENT:
                break;
            case OTHER_SERVER:
                break;
            case OTHER_CLIENT:
                break;
        }
        getLogger().debug(".newEndpoint(): Exit, endpoint->{}", endpoint);
        return(endpoint);
    }
}
