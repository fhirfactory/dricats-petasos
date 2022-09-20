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
package net.fhirfactory.dricats.model.transforms.tofhir.device.factories;

import net.fhirfactory.dricats.internals.fhir.r4.codesystems.PegacornIdentifierCodeEnum;
import net.fhirfactory.dricats.internals.fhir.r4.resources.identifier.PegacornIdentifierFactory;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import net.fhirfactory.dricats.configuration.defaults.dricats.systemwide.ReferenceProperties;
import net.fhirfactory.dricats.internals.fhir.r4.resources.device.factories.DeviceMetaTagFactory;
import net.fhirfactory.dricats.internals.fhir.r4.resources.device.factories.DeviceSpecialisationFactory;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.component.datatypes.ComponentTypeDefinition;
import net.fhirfactory.dricats.model.component.valuesets.ComponentConnectivityRoleEnum;
import net.fhirfactory.dricats.model.petasos.participant.capabilities.valuesets.ConcurrencyModeEnum;
import net.fhirfactory.dricats.model.petasos.participant.capabilities.valuesets.ResilienceModeEnum;
import net.fhirfactory.dricats.model.petasos.participant.datatypes.PetasosParticipantId;
import net.fhirfactory.dricats.model.petasos.participant.valuesets.NetworkSecurityZoneEnum;

@ApplicationScoped
public class DeviceFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceFactory.class);

    @Inject
    private DeviceMetaTagFactory metaTagFactory;

    @Inject
    private DevicePropertyFactory propertyFactory;

    @Inject
    private PegacornIdentifierFactory identifierFactory;

    @Inject
    private ReferenceProperties systemWideProperties;

    @Inject
    private DeviceSpecialisationFactory specialisationFactory;

    /*
    @Inject
    private EndpointFactory endpointFactory;
    */

    private static final String PEGACORN_SOFTWARE_COMPONENT_DEVICE_TYPE_SYSTEM = "/device-type";

    //
    // Business Methods
    //

    public Device newDevice(
            ComponentId deviceComponentId,
            Device.FHIRDeviceStatus deviceStatus,
            String deviceName,
            Device.DeviceNameType deviceNameType,
            Set<Device.DevicePropertyComponent> properties,
            ResilienceModeEnum resilienceMode,
            ConcurrencyModeEnum concurrencyMode,
            NetworkSecurityZoneEnum securityZone
            ){

        //
        // Create the empty Device resource
        Device device = new Device();

        //
        // Set the Security Zone
        Coding deviceSecurityZoneTag = getMetaTagFactory().newSecurityTag(securityZone.getToken(), securityZone.getDisplayName());
        device.getMeta().addTag(deviceSecurityZoneTag);

        //
        // Set the Device Name
        Device.DeviceDeviceNameComponent devName = new Device.DeviceDeviceNameComponent();
        devName.setType(deviceNameType);
        devName.setName(deviceName);
        device.addDeviceName(devName);

        //
        // Set the Device Status
        device.setStatus(deviceStatus);

        //
        // Add the Resilience and Concurrency Properties
        Device.DevicePropertyComponent resiliencePropertyComponent = getPropertyFactory().newDeviceProperty(resilienceMode);
        device.addProperty(resiliencePropertyComponent);
        Device.DevicePropertyComponent concurrencyPropertyComponent = getPropertyFactory().newDeviceProperty(concurrencyMode);
        device.addProperty(concurrencyPropertyComponent);


        //
        // Touch the resource Update marker
        device.getMeta().setLastUpdated(Date.from(Instant.now()));



        return(device);
    }

    public Device newDeviceFromSoftwareComponent(ComponentId componentId, ComponentTypeDefinition componentType, PetasosParticipantId participantId, NetworkSecurityZoneEnum securityZone){
        //
        // Create the empty Device resource
        Device device = new Device();

        //
        // Create the Identifier
        Period period = new Period();
        if(componentId.hasIdValidityStartInstant()){
            Date startDate = Date.from(componentId.getIdValidityStartInstant());
            period.setStart(startDate);
        }
        if(componentId.hasIdValidityEndInstant()){
            Date endDate = Date.from(componentId.getIdValidityEndInstant());
            period.setEnd(endDate);
        }
        Identifier identifier = getIdentifierFactory().newIdentifier(PegacornIdentifierCodeEnum.IDENTIFIER_CODE_SOFTWARE_COMPONENT, componentId.getId(), period);

        //
        // Set the Name
        Device.DeviceDeviceNameComponent nameComponent = new Device.DeviceDeviceNameComponent();
        nameComponent.setName(componentId.getDisplayName());
        nameComponent.setType(Device.DeviceNameType.USERFRIENDLYNAME);
        device.addDeviceName(nameComponent);

        //
        // Set the Distinct Identifier (the FDN)
        device.setDistinctIdentifier(componentId.getId());

        //
        // Set the Device Type
        /*
        CodeableConcept deviceType = newSoftwareComponentDeviceType(componentType);
        device.setType(deviceType);
        */

        //
        // Set the Device Model
        device.setModelNumber(participantId.getParticipantName());

        //
        // Set the Security Zone
        Coding deviceSecurityZoneTag = getMetaTagFactory().newSecurityTag(securityZone.getToken(), securityZone.getDisplayName());
        device.getMeta().addTag(deviceSecurityZoneTag);

        //
        // Add the Pod/Host IP Addresses (if set)
        /*
        if(node.getActualPodIP() != null){
            Coding podIPAddressTag = getMetaTagFactory().newPodIPAddressTag(node.getActualPodIP());
            device.getMeta().addTag(podIPAddressTag);
        }
        if(node.getActualHostIP() != null){
            Coding hostIPAddressTag = getMetaTagFactory().newHostIPAddressTag(node.getActualHostIP());
            device.getMeta().addTag(hostIPAddressTag);
        }

         */

        //
        // Add the Component Role (if present)
        if(componentType.hasConnectivityRole()){
            ComponentConnectivityRoleEnum connectivityRole = componentType.getConnectivityRole();
            Device.DeviceSpecializationComponent deviceSpecializationComponent = getSpecialisationFactory().newDeviceSpecialisation(connectivityRole.getToken(), connectivityRole.getDisplayName(), connectivityRole.getDisplayText());
            device.addSpecialization(deviceSpecializationComponent);
        }

        //
        // All done!
        return(device);
    }

    /*
    public Device newDevice(EndpointTopologyNode ipcEndpoint){

        //
        // Build using other method!
        Device device = newDevice(ipcEndpoint, null);

        //
        // All done!
        return(device);
    }
    */

    /*
    public Device newDevice(EndpointTopologyNode ipcEndpoint, Reference parentWUP){

        //
        // Build default Device using other method!
        Device device = newDeviceFromSoftwareComponent(ipcEndpoint);

        switch(ipcEndpoint.getEndpointType()){
            case JGROUPS_INTEGRATION_POINT:
                JGroupsRepeaterClientETN jgroupsEndpoint = (JGroupsRepeaterClientETN)ipcEndpoint;
                JGroupsClientAdapter jgroupsAdapter = jgroupsEndpoint.getJGroupsAdapter();
                Endpoint endpoint = getEndpointFactory().newJGroupsEndpoint(jgroupsEndpoint, jgroupsAdapter);
                device.addContained(endpoint);
                return(device);
            case HTTP_API_SERVER:
                HTTPServerETN httpServer = (HTTPServerETN) ipcEndpoint;
                HTTPServerAdapter httpServerAdapter = httpServer.getHTTPServerAdapter();
                Endpoint httpServerEndpoint = getEndpointFactory().newEndpoint(httpServer, httpServerAdapter);
                device.addContained(httpServerEndpoint);
                return(device);
            case HTTP_API_CLIENT:
                HTTPClientETN httpClientTopologyEndpoint = (HTTPClientETN) ipcEndpoint;
                for(HTTPClientAdapter currentHTTPClientAdapter: httpClientTopologyEndpoint.getHTTPClientAdapters()) {
                    Endpoint httpClientEndpoint = getEndpointFactory().newEndpoint(httpClientTopologyEndpoint, currentHTTPClientAdapter);
                    device.addContained(httpClientEndpoint);
                }
                return(device);
            case MLLP_SERVER:
                MLLPServerETN mllpServerEndpoint = (MLLPServerETN)ipcEndpoint;
                MLLPServerAdapter mllpServerAdapter = mllpServerEndpoint.getMLLPServerAdapter();
                Endpoint mllpServerFHIREndpoint = getEndpointFactory().newEndpoint(mllpServerEndpoint, mllpServerAdapter);
                device.addContained(mllpServerFHIREndpoint);
                return(device);
            case MLLP_CLIENT:
                MLLPClientETN mllpClientEndpoint = (MLLPClientETN) ipcEndpoint;
                for(MLLPClientAdapter currentMLLPClientAdapter: mllpClientEndpoint.getMLLPClientAdapters() ){
                    Endpoint mllpClientFHIREndpoint = getEndpointFactory().newEndpoint(mllpClientEndpoint, currentMLLPClientAdapter);
                    device.addContained(mllpClientFHIREndpoint);
                }
                return(device);
            case SQL_SERVER:
                break;
            case SQL_CLIENT:
                break;
            case LDAP_SERVER:
                break;
            case LDAP_CLIENT:
                break;
            case OTHER_API_SERVER:
                break;
            case OTHER_API_CLIENT:
                break;
            case OTHER_SERVER:
                break;
            case OTHER_CLIENT:
                break;
        }
        //
        // All done!

        return(null);
    }
    */
    
    /*
    public Device newDevice(WorkUnitProcessorTopologyNode wupComponent){

        //
        // Build using other method!
        Device device = newDeviceFromSoftwareComponent(wupComponent);

        //
        // All done!
        return(device);
    }
    */

    /*
    public Device newDevice(FrameworkWorkUnitProcessorTopologyNode wupComponent, Reference parentWorkshop){
        //
        // Build base SoftwareComponentt device
        Device device = newDeviceFromSoftwareComponent(wupComponent);

        //
        // Add the Parent Workshop (if present)
        if(parentWorkshop != null) {
            device.setParent(parentWorkshop);
        }



        //
        // All done!
        return(device);
    }
    */

    /*
    public Device newDevice(WorkshopTopologyNodeBase workshop){

        //
        // Build using other method!
        Device device = newDeviceFromSoftwareComponent(workshop);

        //
        // All done!
        return(device);
    }
    */
    
    /*
    public Device newDevice(WorkshopTopologyNodeBase workshop, Reference parentProcessingPlant){

        //
        // Build base SoftwareComponentt device
        Device device = newDeviceFromSoftwareComponent(workshop);

        //
        // Add the Parent ProcessingPlant (if present)
        if(parentProcessingPlant != null) {
            device.setParent(parentProcessingPlant);
        }

        //
        // All done!
        return(device);
    }
    */

    /*
    public Device newDevice(ProcessingPlantTopologyNode processingPlant){

        //
        // Build base SoftwareComponentt device
        Device device = newDeviceFromSoftwareComponent(processingPlant);

        //
        // Set the Model Name (Subsystem Name)
        Device.DeviceDeviceNameComponent nameComponent = new Device.DeviceDeviceNameComponent();
        nameComponent.setName(processingPlant.getSubsystemParticipantName());
        nameComponent.setType(Device.DeviceNameType.MODELNAME);
        device.addDeviceName(nameComponent);

        //
        // Set the Other Name (Cluster Service Name)
        Device.DeviceDeviceNameComponent clusterServiceNameComponent = new Device.DeviceDeviceNameComponent();
        clusterServiceNameComponent.setName(processingPlant.getSubsystemParticipantName());
        clusterServiceNameComponent.setType(Device.DeviceNameType.OTHER);
        device.addDeviceName(clusterServiceNameComponent);

        //
        // Add the configuration file(s) 
        Device.DevicePropertyComponent interZoneAuditConfigFileProperty = getPropertyFactory().newConfigurationFileDeviceProperty(DeviceConfigurationFilePropertyTypeEnum.CONFIG_FILE_JGROUPS_PETASOS_AUDIT, processingPlant.getPetasosAuditStackConfigFile());
        device.addProperty(interZoneAuditConfigFileProperty);
        Device.DevicePropertyComponent interZoneIPCConfigFileProperty = getPropertyFactory().newConfigurationFileDeviceProperty(DeviceConfigurationFilePropertyTypeEnum.CONFIG_FILE_JGROUPS_PETASOS_IPC_MESSAGING, processingPlant.getPetasosIPCStackConfigFile());
        device.addProperty(interZoneIPCConfigFileProperty);
        Device.DevicePropertyComponent interZoneInterceptionConfigFileProperty = getPropertyFactory().newConfigurationFileDeviceProperty(DeviceConfigurationFilePropertyTypeEnum.CONFIG_FILE_JGROUPS_PETASOS_INTERCEPTION, processingPlant.getPetasosInterceptionStackConfigFile());
        device.addProperty(interZoneInterceptionConfigFileProperty);
        Device.DevicePropertyComponent interZoneMetricsConfigFileProperty = getPropertyFactory().newConfigurationFileDeviceProperty(DeviceConfigurationFilePropertyTypeEnum.CONFIG_FILE_JGROUPS_PETASOS_METRICS, processingPlant.getPetasosMetricsStackConfigFile());
        device.addProperty(interZoneMetricsConfigFileProperty);
        Device.DevicePropertyComponent interZoneTasksConfigFileProperty = getPropertyFactory().newConfigurationFileDeviceProperty(DeviceConfigurationFilePropertyTypeEnum.CONFIG_FILE_JGROUPS_PETASOS_TASKS, processingPlant.getPetasosTaskingStackConfigFile());
        device.addProperty(interZoneTasksConfigFileProperty);
        Device.DevicePropertyComponent petasosSubscriptionConfigFileProperty = getPropertyFactory().newConfigurationFileDeviceProperty(DeviceConfigurationFilePropertyTypeEnum.CONFIG_FILE_JGROUPS_PETASOS_SUBSCRIPTION, processingPlant.getPetasosSubscriptionsStackConfigFile());
        device.addProperty(petasosSubscriptionConfigFileProperty);
        Device.DevicePropertyComponent petasosTopologyConfigFileProperty = getPropertyFactory().newConfigurationFileDeviceProperty(DeviceConfigurationFilePropertyTypeEnum.CONFIG_FILE_JGROUPS_PETASOS_TOPOLOGY, processingPlant.getPetasosSubscriptionsStackConfigFile());
        device.addProperty(petasosTopologyConfigFileProperty);

        //
        // Add Other Configuration Parameters
        for(String parameterName: processingPlant.getOtherConfigurationParameters().keySet()) {
            String otherConfigurationParameter = processingPlant.getOtherConfigurationParameter(parameterName);
            Device.DevicePropertyComponent otherConfigurationParameterComponent = new Device.DevicePropertyComponent();
            CodeableConcept parameterNameCC = new CodeableConcept();
            parameterNameCC.setText(parameterName);
            CodeableConcept parameterValueCC = new CodeableConcept();
            parameterValueCC.setText(otherConfigurationParameter);
            otherConfigurationParameterComponent.setType(parameterNameCC);
            otherConfigurationParameterComponent.addValueCode(parameterValueCC);
            device.addProperty(otherConfigurationParameterComponent);
        }

        //
        // All Done
        return(device);
    }
    */

    //
    // Some Parameter Factories
    //

    public String getSoftwareComponentDeviceTypeSystem() {
        String codeSystem = systemWideProperties.getPegacornCodeSystemSite() + PEGACORN_SOFTWARE_COMPONENT_DEVICE_TYPE_SYSTEM;
        return (codeSystem);
    }

    /*
    public CodeableConcept newSoftwareComponentDeviceType(){
        CodeableConcept softwareComponentCC = new CodeableConcept();
        Coding softwareComponentCoding = new Coding();
        softwareComponentCoding.setCode("SoftwareComponent");
        softwareComponentCoding.setCode("pegacorn.fhir.device.device-type.software-component");
        softwareComponentCoding.setDisplay("Software Component");
        softwareComponentCC.addCoding(softwareComponentCoding);
        softwareComponentCC.setText("Software Component Device Type");
        return(softwareComponentCC);
    }
    */

    /*
    public CodeableConcept newSoftwareComponentDeviceType(ComponentTypeDefinition componentType){
        CodeableConcept softwareComponentCC = new CodeableConcept();
        Coding softwareComponentCoding = new Coding();
        softwareComponentCoding.setCode(componentType.getTypeName());
        softwareComponentCoding.setCode("pegacorn.fhir.device.device-type.software-component");
        softwareComponentCoding.setDisplay(componentType.getDisplayTypeName());
        softwareComponentCC.addCoding(softwareComponentCoding);
        softwareComponentCC.setText("Software Component("+componentType.getDisplayTypeName()+")");
        return(softwareComponentCC);
    }
    */

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }

    protected DeviceMetaTagFactory getMetaTagFactory(){
        return(metaTagFactory);
    }

    protected DevicePropertyFactory getPropertyFactory(){
        return(propertyFactory);
    }

    protected PegacornIdentifierFactory getIdentifierFactory(){
        return(identifierFactory);
    }

    protected DeviceSpecialisationFactory getSpecialisationFactory(){
        return(specialisationFactory);
    }

    /*
    protected EndpointFactory getEndpointFactory(){
        return(endpointFactory);
    }
    */
}
