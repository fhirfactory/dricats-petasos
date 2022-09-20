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

import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.petasos.tasking.definition.PetasosActionableTask;
import net.fhirfactory.dricats.model.petasos.tasking.definition.PetasosFulfillmentTask;
import net.fhirfactory.dricats.petasos.tasking.factories.PetasosFulfillmentTaskFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ActionableTaskFulfillmentTaskTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(ActionableTaskFulfillmentTaskTransformer.class);

    @Inject
    private PetasosFulfillmentTaskFactory fulfillmentTaskFactory;

    //
    // Constructor(s)
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

    public PetasosFulfillmentTask createFulfillmentTask(PetasosActionableTask actionableTask, ComponentId componentId){
        getLogger().debug(".createFulfillmentTask(): Entry, actionableTask->{}", actionableTask);

        //
        // Defensive Programming
        if(actionableTask == null){
            getLogger().debug(".createFulfillmentTask(): Exit, Ingres Actionable Task is null, returning an empty list for routing.");
            return(null);
        }
        if(!actionableTask.hasTaskWorkItem()){
            getLogger().debug(".createFulfillmentTask(): Exit, Ingres Actionable Task has no work item, returning an empty list for routing.");
            return(null);
        }
        if(!actionableTask.getTaskWorkItem().hasIngresContent()){
            getLogger().debug(".createFulfillmentTask(): Exit, Ingres Actionable Task has a work item with no ingres content, returning an empty list for routing.");
            return(null);
        }
        if(!actionableTask.getTaskWorkItem().getIngresContent().hasDataParcelQualityStatement()){
            getLogger().debug(".createFulfillmentTask(): Exit, Ingres Actionable Task has a work item with no ingres content manifest, returning an empty list for routing.");
            return(null);
        }

        PetasosFulfillmentTask petasosFulfillmentTask = fulfillmentTaskFactory.newFulfillmentTask(actionableTask, componentId);

        getLogger().debug(".createFulfillmentTask(): Exit, petasosFulfillmentTask->{}", petasosFulfillmentTask);
        return(petasosFulfillmentTask);
    }
}
