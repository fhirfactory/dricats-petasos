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
package net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.manager;

import net.fhirfactory.dricats.model.petasos.participant.components.common.PetasosParticipant;
import net.fhirfactory.dricats.model.petasos.tasking.routing.subscriptions.datatypes.DataParcelManifestSubscriptionMask;
import net.fhirfactory.dricats.petasos.observations.metrics.agents.WorkUnitProcessorMetricsAgent;
import net.fhirfactory.dricats.petasos.tasking.audit.brokers.PetasosFulfillmentTaskAuditServicesBroker;
import net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.worker.archetypes.ExternalIngresWUPContainerRoute;
import net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.worker.archetypes.StandardWUPContainerRoute;
import net.fhirfactory.dricats.petasos.participant.manager.LocalPetasosParticipantSubscriptionMapIM;
import net.fhirfactory.dricats.petasos.tasking.observations.metrics.agents.WorkUnitProcessorMetricsAgent;
import org.apache.camel.CamelContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.List;
import java.util.Locale;
import net.fhirfactory.dricats.configuration.defaults.petasos.PetasosPropertyConstants;
import net.fhirfactory.dricats.interfaces.petasos.participant.topology.ProcessingPlantConfigurationServiceInterface;
import net.fhirfactory.dricats.model.petasos.participant.archetypes.wup.valuesets.WUPArchetypeEnum;

/**
 * @author Mark A. Hunter
 */

@ApplicationScoped
public class WorkUnitProcessorFrameworkManager {
    private static final Logger LOG = LoggerFactory.getLogger(WorkUnitProcessorFrameworkManager.class);

    @Inject
    private CamelContext camelctx;

    @Inject
    private LocalPetasosParticipantSubscriptionMapIM topicServer;

    @Inject
    private ProcessingPlantConfigurationServiceInterface processingPlant;

    @Inject
    private PetasosFulfillmentTaskAuditServicesBroker auditBroker;

    public void buildWUPFramework(PetasosParticipant wup, List<DataParcelManifestSubscriptionMask> subscribedTopics, WUPArchetypeEnum wupArchetype, WorkUnitProcessorMetricsAgent metricsAgent) {
        LOG.debug(".buildWUPFramework(): Entry, wup --> {}, subscribedTopics --> {}, wupArchetype --> {}", wup, subscribedTopics, wupArchetype);
        try {
            switch (wupArchetype) {
                case WUP_NATURE_STIMULI_TRIGGERED_WORKFLOW: {
                    LOG.trace(".buildWUPFramework(): Building a WUP_NATURE_STIMULI_TRIGGERED_BEHAVIOUR route");
                    StandardWUPContainerRoute standardWUPRoute = new StandardWUPContainerRoute(camelctx, wup, auditBroker, false, getSedaParameters(), metricsAgent);
                    LOG.trace(".buildWUPFramework(): Route created, now adding it to he CamelContext!");
                    camelctx.addRoutes(standardWUPRoute);
                    break;
                }
                case WUP_NATURE_TIMER_TRIGGERED_WORKFLOW: {
                    LOG.trace(".buildWUPFramework(): Building a WUP_NATURE_LADON_TIMER_TRIGGERED_BEHAVIOUR route");
                    ExternalIngresWUPContainerRoute ingresRoute = new ExternalIngresWUPContainerRoute(camelctx, wupNode, auditBroker, metricsAgent);
                    camelctx.addRoutes(ingresRoute);
                    break;
                }
                case WUP_NATURE_LADON_BEHAVIOUR_WRAPPER:
                case WUP_NATURE_LADON_STANDARD_MOA: {
                    LOG.trace(".buildWUPFramework(): Building a WUP_NATURE_LADON_STANDARD_MOA route");
                    StandardWUPContainerRoute standardWUPRoute = new StandardWUPContainerRoute(camelctx, wupNode, auditBroker,  metricsAgent);
                    LOG.trace(".buildWUPFramework(): Route created, now adding it to he CamelContext!");
                    camelctx.addRoutes(standardWUPRoute);
                    break;
                }
                case WUP_NATURE_MESSAGE_WORKER: {
                    LOG.trace(".buildWUPFramework(): Building a WUP_NATURE_MESSAGE_WORKER route");
                    StandardWUPContainerRoute standardWUPRoute = new StandardWUPContainerRoute(camelctx, wupNode, auditBroker, metricsAgent);
                    LOG.trace(".buildWUPFramework(): Route created, now adding it to he CamelContext!");
                    camelctx.addRoutes(standardWUPRoute);
                    break;
                }
                case WUP_NATURE_API_PUSH:
                    LOG.trace(".buildWUPFramework(): Building a WUP_NATURE_API_PUSH route");
                    ExternalIngresWUPContainerRoute ingresRouteForAPIPush = new ExternalIngresWUPContainerRoute(camelctx, wupNode, auditBroker, metricsAgent);
                    camelctx.addRoutes(ingresRouteForAPIPush);
                    break;
                case WUP_NATURE_API_ANSWER:
                    LOG.trace(".buildWUPFramework(): Building a WUP_NATURE_API_ANSWER route");
                    break;
                case WUP_NATURE_API_RECEIVE:
                    LOG.trace(".buildWUPFramework(): Building a WUP_NATURE_API_RECEIVE route");
                    break;
                case WUP_NATURE_API_CLIENT:
                    LOG.trace(".buildWUPFramework(): Building a WUP_NATURE_MESSAGE_EXTERNAL_EGRESS_POINT route");
                    StandardWUPContainerRoute egressRoute = new StandardWUPContainerRoute(camelctx, wupNode, auditBroker, metricsAgent);
                    camelctx.addRoutes(egressRoute);
                    break;
                case WUP_NATURE_MESSAGE_EXTERNAL_EGRESS_POINT: {
                    LOG.trace(".buildWUPFramework(): Building a WUP_NATURE_MESSAGE_EXTERNAL_EGRESS_POINT route");
                    StandardWUPContainerRoute standardWUPRoute = new StandardWUPContainerRoute(camelctx, wupNode, auditBroker, false, getSedaParameters(), metricsAgent);
                    camelctx.addRoutes(standardWUPRoute);
                    break;
                }
                case WUP_NATURE_MESSAGE_EXTERNAL_INGRES_POINT:
                    LOG.trace(".buildWUPFramework(): Building a WUP_NATURE_MESSAGE_EXTERNAL_INGRES_POINT route");
                    ExternalIngresWUPContainerRoute ingresRoute = new ExternalIngresWUPContainerRoute(camelctx, wupNode, auditBroker, metricsAgent);
                    camelctx.addRoutes(ingresRoute);
                    break;
                case WUP_NATURE_MESSAGE_EXTERNAL_CONCURRENT_INGRES_POINT:
                    LOG.trace(".buildWUPFramework(): Building a WUP_NATURE_MESSAGE_EXTERNAL_CONCURRENT_INGRES_POINT route");

            }
        } catch (Exception Ex) {
            // TODO We really must handle this exception, either by cancelling the whole Processing Plant or, at least, raising an alarm
        }
    }

    private String getSedaParameters(){
        String sedaQueueSize = processingPlant.getMeAsASoftwareComponent().getOtherConfigurationParameter(PetasosPropertyConstants.SEDA_QUEUE_SIZE_PARAMETER_NAME);
        String sedaBlockOnFull = processingPlant.getMeAsASoftwareComponent().getOtherConfigurationParameter(PetasosPropertyConstants.SEDA_QUEUE_BLOCK_ON_FULL_PARAMETER_NAME);
        if(StringUtils.isEmpty(sedaQueueSize) && StringUtils.isEmpty(sedaBlockOnFull)){
            return(null);
        }
        if(StringUtils.isNotEmpty(sedaQueueSize) && StringUtils.isNotEmpty(sedaBlockOnFull)){
            String sedaParameters = "?size="+sedaQueueSize+"&blockWhenFull="+sedaBlockOnFull.toLowerCase(Locale.ROOT);
            return(sedaParameters);
        }
        if(StringUtils.isNotEmpty(sedaQueueSize)){
            String sedaParameters = "?size="+sedaQueueSize;
            return(sedaParameters);
        }
        String sedaParameters = "?blockWhenFull="+sedaBlockOnFull.toLowerCase(Locale.ROOT);
        return(sedaParameters);
    }
}
