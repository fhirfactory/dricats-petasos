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

package net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.worker.archetypes;

import net.fhirfactory.dricats.configuration.defaults.petasos.PetasosPropertyConstants;
import net.fhirfactory.dricats.model.petasos.participant.components.common.PetasosParticipant;
import net.fhirfactory.dricats.model.petasos.participant.archetypes.wup.base.WorkUnitProcessorPetasosParticipant;
import net.fhirfactory.dricats.petasos.observations.metrics.agents.WorkUnitProcessorMetricsAgent;
import net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.worker.buildingblocks.*;
import net.fhirfactory.pegacorn.core.model.topology.nodes.WorkUnitProcessorSoftwareComponent;
import net.fhirfactory.dricats.petasos.tasking.audit.brokers.PetasosFulfillmentTaskAuditServicesBroker;
import net.fhirfactory.pegacorn.petasos.tasking.fulfilment.wupcontainer.worker.buildingblocks.*;
import net.fhirfactory.dricats.petasos.tasking.fulfilment.common.BasePetasosContainerRoute;
import net.fhirfactory.dricats.model.petasos.tasking.routing.identification.WUPComponentNames;
import net.fhirfactory.pegacorn.petasos.tasking.moa.pathway.wupcontainer.worker.buildingblocks.*;
import net.fhirfactory.dricats.petasos.tasking.observations.metrics.agents.WorkUnitProcessorMetricsAgent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mark A. Hunter
 * @since 2020-07-1
 */

public class StandardWUPContainerRoute extends BasePetasosContainerRoute {
	private static final Logger LOG = LoggerFactory.getLogger(StandardWUPContainerRoute.class);
    protected Logger getLogger(){
        return(LOG);
    }

	private PetasosParticipant petasosParticipant;
	private WUPComponentNames nameSet;
	private WorkUnitProcessorMetricsAgent metricsAgent;

	//
	// Constructor(s)
	//

	public StandardWUPContainerRoute(CamelContext camelCTX, WorkUnitProcessorPetasosParticipant wupTopologyNode, PetasosFulfillmentTaskAuditServicesBroker auditTrailBroker, WorkUnitProcessorMetricsAgent metricsAgent) {
		super(camelCTX, auditTrailBroker);
		getLogger().debug(".StandardWUPContainerRoute(): Entry, context --> ###, wupNode --> {}", wupTopologyNode);
		this.petasosParticipant = wupTopologyNode;
		this.nameSet = new WUPComponentNames(getPetasosParticipant().getParticipantName());
		this.metricsAgent = metricsAgent;
	}

	public StandardWUPContainerRoute(CamelContext camelCTX, PetasosParticipant wupTopologyNode, PetasosFulfillmentTaskAuditServicesBroker auditTrailBroker, boolean requiresDirect, String sedaParameters, WorkUnitProcessorMetricsAgent metricsAgent) {
		super(camelCTX, auditTrailBroker);
		getLogger().debug(".StandardWUPContainerRoute(): Entry, context --> ###, wupNode --> {}", wupTopologyNode);
		this.petasosParticipant = wupTopologyNode;
		this.nameSet = new WUPComponentNames(getPetasosParticipant().getParticipantName(), requiresDirect, sedaParameters);
		this.metricsAgent = metricsAgent;
	}

	//
	// Business Methods (Routes)
	//

	@Override
	public void configure() {
		getLogger().debug(".configure(): Entry!, for wupNode --> {}", this.petasosParticipant);
		getLogger().debug("StandardWUPContainerRoute :: EndPointWUPContainerIngresProcessorIngres --> {}", nameSet.getEndPointWUPContainerIngresProcessorIngres());
		getLogger().debug("StandardWUPContainerRoute :: EndPointWUPContainerIngresProcessorEgress --> {}", nameSet.getEndPointWUPContainerIngresProcessorEgress());
		getLogger().debug("StandardWUPContainerRoute :: EndPointWUPContainerIngresGatekeeperIngres --> {}", nameSet.getEndPointWUPContainerIngresGatekeeperIngres());
		getLogger().debug("StandardWUPContainerRoute :: EndPointWUPIngresConduitIngres --> {}", nameSet.getEndPointWUPIngresConduitIngres());
		getLogger().debug("StandardWUPContainerRoute :: EndPointWUPIngres --> {}", nameSet.getEndPointWUPIngres());
		getLogger().debug("StandardWUPContainerRoute :: EndPointWUPEgress --> {}", nameSet.getEndPointWUPEgress());
		getLogger().debug("StandardWUPContainerRoute :: EndPointWUPEgressConduitEgress --> {}", nameSet.getEndPointWUPEgressConduitEgress());
		getLogger().debug("StandardWUPContainerRoute :: EndPointWUPContainerEgressProcessorIngres --> {}", nameSet.getEndPointWUPContainerEgressProcessorIngres());
		getLogger().debug("StandardWUPContainerRoute :: EndPointWUPContainerEgressProcessorEgress --> {}", nameSet.getEndPointWUPContainerEgressProcessorEgress());
		getLogger().debug("StandardWUPContainerRoute :: EndPointWUPContainerEgressGatekeeperIngres --> {}", nameSet.getEndPointWUPContainerEgressMetadataProcessorIngres());

		specifyCamelExecutionExceptionHandler();

		NodeDetailInjector nodeDetailInjector = new NodeDetailInjector();

		fromWithStandardExceptionHandling(nameSet.getEndPointWUPContainerIngresProcessorIngres())
				.routeId(nameSet.getEndPointWUPContainerIngresProcessorIngres())
				.log(LoggingLevel.DEBUG, "Processing Task->${body}")
				.process(nodeDetailInjector)
				.bean(WUPContainerIngresProcessor.class, "ingresContentProcessor(*, Exchange)")
				.to(nameSet.getEndPointWUPContainerIngresGatekeeperIngres());

		fromWithStandardExceptionHandling(nameSet.getEndPointWUPContainerIngresGatekeeperIngres())
				.routeId(nameSet.getEndPointWUPContainerIngresGatekeeperIngres())
				.process(nodeDetailInjector)
				.bean(WUPContainerIngresGatekeeper.class, "ingresGatekeeper(*, Exchange)");

		fromWithStandardExceptionHandling(nameSet.getEndPointWUPContainerInterceptionRedirectPointIngres())
				.routeId(nameSet.getEndPointWUPContainerInterceptionRedirectPointIngres())
				.process(nodeDetailInjector)
				.bean(WUPContainerInterceptionRedirectPoint.class, "interceptionRedirectDecisionPoint(*, Exchange)");

		fromWithStandardExceptionHandling(nameSet.getEndPointWUPIngresConduitIngres())
				.routeId(nameSet.getEndPointWUPIngresConduitIngres())
				.process(nodeDetailInjector)
				.bean(WUPIngresConduit.class, "forwardIntoWUP(*, Exchange)")
				.to(nameSet.getEndPointWUPIngres());

		fromWithStandardExceptionHandling(nameSet.getEndPointWUPEgress())
				.routeId(nameSet.getEndPointWUPEgress())
				.process(nodeDetailInjector)
				.bean(WUPEgressConduit.class, "receiveFromWUP(*, Exchange)")
				.to( nameSet.getEndPointWUPContainerInterceptionReturnPointIngres());

		fromWithStandardExceptionHandling(nameSet.getEndPointWUPContainerInterceptionReturnPointIngres())
				.routeId(nameSet.getEndPointWUPContainerInterceptionReturnPointIngres())
				.process(nodeDetailInjector)
				.bean(WUPContainerInterceptionReturnPoint.class, "returnRedirectedTask(*, Exchange)")
				.to( nameSet.getEndPointWUPContainerEgressProcessorIngres());

		fromWithStandardExceptionHandling(nameSet.getEndPointWUPContainerEgressProcessorIngres())
				.routeId(nameSet.getEndPointWUPContainerEgressProcessorIngres())
				.process(nodeDetailInjector)
				.bean(WUPContainerEgressProcessor.class, "egressContentProcessor(*, Exchange)")
				.to(nameSet.getEndPointWUPContainerEgressMetadataProcessorIngres());

		fromWithStandardExceptionHandling(nameSet.getEndPointWUPContainerEgressMetadataProcessorIngres())
				.routeId(nameSet.getEndPointWUPContainerEgressMetadataProcessorIngres())
				.process(nodeDetailInjector)
				.bean(WUPContainerEgressMetadataProcessor.class, "alterTaskWorkItemMetadata(*, Exchange)")
				.to(PetasosPropertyConstants.TASK_OUTCOME_COLLECTION_QUEUE);

	}

	//
	// Content Injectors
	//

	protected class NodeDetailInjector implements Processor {
		@Override
		public void process(Exchange exchange) throws Exception {
			getLogger().debug("NodeDetailInjector.process(): Entry");
			boolean alreadyInPlace = false;
			if(exchange.hasProperties()) {
				WorkUnitProcessorSoftwareComponent wupTN = exchange.getProperty(PetasosPropertyConstants.WUP_PETASOS_PARTICIPANT_EXCHANGE_PROPERTY_NAME, WorkUnitProcessorSoftwareComponent.class);
				if (wupTN != null) {
					alreadyInPlace = true;
				}
			}
			if(!alreadyInPlace) {
				exchange.setProperty(PetasosPropertyConstants.WUP_PETASOS_PARTICIPANT_EXCHANGE_PROPERTY_NAME, getWupParticipant());
			}
			exchange.setProperty(PetasosPropertyConstants.WUP_METRICS_AGENT_EXCHANGE_PROPERTY, getMetricsAgent());
		}
	}

	//
	// Getters (and Setters)
	//

	public PetasosParticipant getPetasosParticipant() {
		return petasosParticipant;
	}

	protected WorkUnitProcessorMetricsAgent getMetricsAgent(){
		return(this.metricsAgent);
	}

	protected WUPComponentNames getNameSet() {
		return nameSet;
	}
}
