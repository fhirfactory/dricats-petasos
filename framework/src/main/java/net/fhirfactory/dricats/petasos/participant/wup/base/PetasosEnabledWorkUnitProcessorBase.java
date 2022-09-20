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
package net.fhirfactory.dricats.petasos.participant.wup.base;

import net.fhirfactory.dricats.configuration.api.services.processingplant.EdgeProcessingPlantConfigurationService;
import net.fhirfactory.dricats.interfaces.petasos.participant.endpoint.WorkUnitProcessorEndpointInterface;
import net.fhirfactory.dricats.model.component.valuesets.ComponentStatusEnum;
import net.fhirfactory.dricats.model.petasos.participant.capabilities.valuesets.ConcurrencyModeEnum;
import net.fhirfactory.dricats.model.petasos.participant.capabilities.valuesets.ResilienceModeEnum;
import net.fhirfactory.dricats.model.petasos.participant.components.common.PetasosParticipant;
import net.fhirfactory.dricats.petasos.participant.processingplant.PetasosEnabledProcessingPlantInformationAccessor;
import net.fhirfactory.dricats.petasos.participant.processingplant.physical.DeploymentSite;
import net.fhirfactory.dricats.petasos.participant.processingplant.physical.InfrastructureNode;
import net.fhirfactory.dricats.petasos.participant.processingplant.physical.PlatformNode;

import javax.inject.Inject;

public abstract class PetasosEnabledWorkUnitProcessorBase extends PetasosParticipant {

    private boolean initialised;
    private ComponentStatusEnum operationalStatus;
    private ResilienceModeEnum resilienceMode;
    private ConcurrencyModeEnum concurrencyMode;
    private WorkUnitProcessorEndpointInterface ingresEndpoint;
    private WorkUnitProcessorEndpointInterface egressEndpoint;

    @Inject
    private PetasosEnabledProcessingPlantInformationAccessor petasosEnabledProcessingPlantServicesAccessor;

    @Inject
    private EdgeProcessingPlantConfigurationService processingPlantConfigurationService;

    @Inject
    private DeploymentSite deploymentSite;

    @Inject
    private InfrastructureNode infrastructureNode;

    @Inject
    private PlatformNode platformNode;
}
