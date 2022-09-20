/*
 * Copyright (c) 2022 Mark A. Hunter
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

package net.fhirfactory.dricats.petasos.tasking.orchestration.helpers;

import net.fhirfactory.dricats.model.petasos.participant.datatypes.PetasosParticipantId;
import net.fhirfactory.dricats.model.petasos.tasking.definition.PetasosFulfillmentTask;
import net.fhirfactory.dricats.model.petasos.tasking.routing.identification.WUPComponentNames;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LocalTaskDistributor {
    private static final Logger LOG = LoggerFactory.getLogger(LocalTaskDistributor.class);

    @Produce
    private ProducerTemplate camelProducerService;

    //
    // Constructor(s)
    //

    public LocalTaskDistributor(){

    }

    //
    // Post Construct
    //

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }

    //
    // Business Methods
    //

    public void distributeMessage(PetasosParticipantId participantId, PetasosFulfillmentTask petasosFulfillmentTask){
        getLogger().debug(".distributeMessage(): Entry, participantId->{}, petasosFulfillmentTask->{}", participantId, petasosFulfillmentTask);
        //
        // Derive where the Task is actually going to be forwarded to
        WUPComponentNames routeName = new WUPComponentNames(participantId.getParticipantName(), participantId.getParticipantVersion());
        String targetCamelEndpoint = routeName.getEndPointWUPContainerIngresProcessorIngres();
        //
        // Forward the Task
        if(getLogger().isDebugEnabled())
        {
            getLogger().debug(".forwardTask(): Forwarding To->{}, Task->{}",participantId, petasosFulfillmentTask.getActionableTaskId().getId() );
        }
        camelProducerService.sendBody(targetCamelEndpoint, ExchangePattern.InOnly, petasosFulfillmentTask);
        getLogger().trace(".distributeMessage(): Exit");
    }
}
