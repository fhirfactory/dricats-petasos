/*
 * Copyright (c) 2021 Mark A. Hunter
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
package net.fhirfactory.dricats.petasos.tasking.factories;

import net.fhirfactory.pegacorn.core.interfaces.topology.ProcessingPlantInterface;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosFulfillmentTask;
import net.fhirfactory.pegacorn.core.model.petasos.wup.PetasosTaskJobCard;
import net.fhirfactory.pegacorn.core.model.petasos.wup.valuesets.PetasosTaskExecutionStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;

@ApplicationScoped
public class PetasosTaskJobCardFactory {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosTaskJobCardFactory.class);

    @Inject
    private ProcessingPlantInterface processingPlant;

    public PetasosTaskJobCard newPetasosTaskJobCard(PetasosFulfillmentTask fulfillmentTask){
        getLogger().debug(".newPetasosTaskJobCard(): Entry, fulfillmentTask->{}", fulfillmentTask);
        PetasosTaskJobCard jobCard = new PetasosTaskJobCard();
        jobCard.setActionableTaskId(fulfillmentTask.getActionableTaskId());
        jobCard.setClusterMode(processingPlant.getMeAsASoftwareComponent().getConcurrencyMode());
        jobCard.setLastActivityCheckInstant(Instant.EPOCH);
        jobCard.setCurrentStatus(PetasosTaskExecutionStatusEnum.PETASOS_TASK_ACTIVITY_STATUS_WAITING);
        jobCard.setExecutingFulfillmentTaskId(null);
        jobCard.setExecutingFulfillmentTaskIdAssignmentInstant(Instant.EPOCH);
        jobCard.setSystemMode(processingPlant.getMeAsASoftwareComponent().getResilienceMode());
        jobCard.setLastRequestedStatus(PetasosTaskExecutionStatusEnum.PETASOS_TASK_ACTIVITY_STATUS_WAITING);
        jobCard.setExecutingProcessingPlant(processingPlant.getMeAsASoftwareComponent().getComponentID());
        jobCard.setExecutingWorkUnitProcessor(fulfillmentTask.getTaskFulfillment().getFulfillerWorkUnitProcessor().getComponentID());
        getLogger().debug(".newPetasosTaskJobCard(): Exit, jobCard->{}", jobCard);
        return(jobCard);
    }


    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }
}
