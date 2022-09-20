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
package net.fhirfactory.dricats.petasos.tasking.fulfilment.outcomes;

import net.fhirfactory.pegacorn.camel.BaseRouteBuilder;
import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TaskOutcomeCollectionAndProcessingWorker extends BaseRouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(TaskOutcomeCollectionAndProcessingWorker.class);
    protected Logger getLogger(){
        return(LOG);
    }

    @Inject
    private TaskOutcome2NewTasksBean outcome2NewTasksBean;

    @Inject
    private TaskOutcomeCaptureBean taskOutcomeCapture;

    @Override
    public void configure() {

        getLogger().debug(".configure(): Entry");

        fromWithStandardExceptionHandling(PetasosPropertyConstants.TASK_OUTCOME_COLLECTION_QUEUE)
                .routeId(PetasosPropertyConstants.TASK_OUTCOME_COLLECTION_QUEUE)
                .bean(taskOutcomeCapture, "captureAndRegisterOutcome(*, Exchange)")
                .split().method(outcome2NewTasksBean, "collectOutcomesAndCreateNewTasks(*, Exchange)")
                .to(PetasosPropertyConstants.TASK_DISTRIBUTION_QUEUE);

    }
}
