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
package net.fhirfactory.dricats.petasos.ipc.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.dricats.interfaces.petasos.participant.endpoint.WorkUnitProcessorEndpointInterface;
import net.fhirfactory.dricats.interfaces.petasos.participant.topology.WorkshopInterface;
import net.fhirfactory.dricats.model.configuration.filebased.archetypes.valuesets.ClusterFunctionNameEnum;
import net.fhirfactory.dricats.model.petasos.participant.components.endpoint.InternalCamelRoute;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.dataparcel.DataParcelManifest;
import net.fhirfactory.dricats.model.petasos.tasking.routing.subscriptions.datatypes.DataParcelManifestSubscriptionMask;
import net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.connections.PetasosRMIClusterConnection;
import net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.connections.base.PetasosClusterConnection;
import net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.management.interfaces.PetasosClusterConnectionRegistrationService;
import net.fhirfactory.dricats.petasos.ipc.technologies.jgroups.beans.PetasosEdgeDoNothingBean;
import net.fhirfactory.dricats.petasos.participant.manager.LocalPetasosParticipantCacheIM;
import net.fhirfactory.dricats.petasos.participant.manager.LocalPetasosParticipantSubscriptionMapIM;
import net.fhirfactory.dricats.petasos.participant.workshops.EdgeWorkshop;
import net.fhirfactory.dricats.petasos.participant.wup.messagebased.MOAStandardWUP;

@ApplicationScoped
public class PetasosClusterServicesManagerWUP extends MOAStandardWUP implements PetasosClusterConnectionRegistrationService{
    private static final Logger LOG = LoggerFactory.getLogger(PetasosClusterServicesManagerWUP.class);

    private static String WUP_VERSION = "1.0.0";
    private ConcurrentHashMap<ClusterFunctionNameEnum, PetasosClusterConnection> petasosServiceConnections;

    @Inject
    private PetasosRMIClusterConnection petasosMessagingEndpoint;

    @Inject
    private EdgeWorkshop edgeWorkshop;

    @Inject
    private PetasosEdgeDoNothingBean doNothingBean;

    @Inject
    private LocalPetasosParticipantSubscriptionMapIM topicServer;

    @Inject
    private LocalPetasosParticipantCacheIM localPetasosParticipantCacheIM;
    
    //
    // Constructor(s)
    //
    
    public PetasosClusterServicesManagerWUP() {
    	super();
    	this.petasosServiceConnections = new ConcurrentHashMap<>();
    }
    
    //
    // Overrides & Getters (& Setters)
    //

    @Override
    protected WorkshopInterface specifyWorkshop() {
        return (edgeWorkshop);
    }


    protected PetasosRMIClusterConnection getPetasosMessagingEndpoint() {
        return (petasosMessagingEndpoint);
    }

    public LocalPetasosParticipantSubscriptionMapIM getTopicServer(){
        return(this.topicServer);
    }

    @Override
    protected Logger specifyLogger() {
        return (LOG);
    }
    
    

    //
    // WUP Specification
    //

    @Override
    protected Set<DataParcelManifestSubscriptionMask> specifySubscriptionTopics() {
        Set<DataParcelManifestSubscriptionMask> subscriptionList = new HashSet<>();
        return (subscriptionList);
    }

    @Override
    protected List<DataParcelManifest> declarePublishedTopics() {
        return (new ArrayList<>());
    }
    
    @Override
	protected String specifyParticipantName() {
		String participantName = edgeWorkshop.getPetasosParticipantInformation().getParticipantId().getParticipantName() + "." + getClass().getSimpleName();
		return(participantName);
	}

	@Override
	protected String specifyParticipantDisplayName() {
		return(getClass().getSimpleName());
	}

	@Override
	protected String specifyParticipantVersion() {
		return(WUP_VERSION);
	}

	@Override
	protected String specifyComponentName() {
		return(getClass().getSimpleName());
	}

	@Override
	protected String specifyComponentVersion() {
		return(WUP_VERSION);
	}

	@Override
	protected WorkUnitProcessorEndpointInterface specifyIngresEndpoint() {
		InternalCamelRoute camelEndpoint = new InternalCamelRoute(getPetasosParticipant().getParticipantId(), WUP_VERSION, false);
		return(camelEndpoint);
	}
    
    @Override
	protected WorkUnitProcessorEndpointInterface specifyEgressEndpoint() {
		InternalCamelRoute camelEndpoint = new InternalCamelRoute(getPetasosParticipant().getParticipantId(), WUP_VERSION, true);
		return(camelEndpoint);
	}

    //
    // Route
    //

    @Override
    public void configure() throws Exception {

        getLogger().info("PetasosEdgeGeneralIPCWUP :: WUPIngresPoint/ingresFeed --> {}", ingresFeed());
        getLogger().info("PetasosEdgeGeneralIPCWUP :: WUPEgressPoint/egressFeed --> {}", egressFeed());

        fromIncludingPetasosServices(ingresFeed())
                .routeId(getClass().getSimpleName())
                .bean(doNothingBean, "doNothing")
                .to(egressFeed());
    }

    //
    // Endpoint Services
    //

    @Override
    public void registerConnector(ClusterFunctionNameEnum functionType, PetasosClusterConnection endpoint) {
        if(petasosServiceConnections.containsKey(functionType)){
        	petasosServiceConnections.remove(functionType);
        }
        petasosServiceConnections.put(functionType, endpoint);
    }

	
}
