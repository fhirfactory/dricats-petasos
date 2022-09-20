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
package net.fhirfactory.dricats.petasos.observations.notifications;

import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.ITOpsNotificationContent;
import net.fhirfactory.pegacorn.internals.hl7v2.helpers.UltraDefensivePipeParser;
import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.model.petasos.dataparcel.DataParcelTypeDescriptor;
import net.fhirfactory.pegacorn.core.model.petasos.dataparcel.valuesets.DataParcelDescriptorKeyEnum;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.fulfillment.valuesets.FulfillmentExecutionStatusEnum;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWPayload;
import net.fhirfactory.dricats.internals.fhir.r4.internal.topics.HL7V2XTopicFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.util.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class PetasosITOpsNotificationContentFactory {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosITOpsNotificationContentFactory.class);

    private DateTimeFormatter timeFormatter;

    @Inject
    private HL7V2XTopicFactory hl7V2XTopicFactory;

    @Inject
    private UltraDefensivePipeParser pipeParser;

    //
    // Constructor(s)
    //

    public PetasosITOpsNotificationContentFactory(){
        timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss.SSS").withZone(ZoneId.of(PetasosPropertyConstants.DEFAULT_TIMEZONE));
    }

    //
    // Getters and Setters
    //

    protected DateTimeFormatter getTimeFormatter(){
        return (timeFormatter);
    }

    protected Logger getLogger(){
        return(LOG);
    }

    //
    // Business Methods
    //

    public String newNotificationContentFromUoWPayload(UoWPayload payload) {
        if (payload == null) {
            return ("");
        }
        if (isHL7V2Payload(payload)) {
            StringBuilder notificationContentBuilder = new StringBuilder();
            if(payload.getPayloadManifest().hasContainerDescriptor()){
                String containerMessageDefiner = payload.getPayloadManifest().getContainerDescriptor().getDataParcelDefiner();
                String containerMessageCategory= payload.getPayloadManifest().getContainerDescriptor().getDataParcelCategory();
                String containerMessageSubcategory = payload.getPayloadManifest().getContainerDescriptor().getDataParcelSubCategory();
                String containerMessageResource= payload.getPayloadManifest().getContainerDescriptor().getDataParcelResource();
                String containerMessageVersion = payload.getPayloadManifest().getContainerDescriptor().getVersion();
                notificationContentBuilder.append("Container Payload: "+containerMessageDefiner+"::"+containerMessageCategory+"::"+containerMessageSubcategory+"::"+ containerMessageResource+"("+containerMessageVersion+") \n");
            }
            String messageType = payload.getPayloadManifest().getContentDescriptor().getDataParcelSubCategory();
            String messageTrigger = payload.getPayloadManifest().getContentDescriptor().getDataParcelResource();
            String messageVersion = payload.getPayloadManifest().getContentDescriptor().getVersion();
            if(payload.getPayloadManifest().hasContainerDescriptor()){
                notificationContentBuilder.append("Content Payload: HL7 v2.x: Trigger:"+messageType+"^"+messageTrigger+"("+messageVersion+")");
            } else {
                notificationContentBuilder.append("Content Payload: HL7 v2.x: Trigger:" + messageType + "^" + messageTrigger + "(" + messageVersion + ") \n");
                List<String> metadataBody = pipeParser.extractMetadataFromHL7v2xMessage(payload.getPayload());
                for(String metadataSegment: metadataBody) {
                    notificationContentBuilder.append(metadataSegment);
                }
            }
            return (notificationContentBuilder.toString());
        }
        //
        // Default Message
        StringBuilder messageBuilder = new StringBuilder();
        if(payload.getPayloadManifest().hasContainerDescriptor()){
            if(payload.getPayloadManifest().hasContainerDescriptor()){
                String containerMessageDefiner = payload.getPayloadManifest().getContainerDescriptor().getDataParcelDefiner();
                String containerMessageCategory= payload.getPayloadManifest().getContainerDescriptor().getDataParcelCategory();
                String containerMessageSubcategory = payload.getPayloadManifest().getContainerDescriptor().getDataParcelSubCategory();
                String containerMessageResource= payload.getPayloadManifest().getContainerDescriptor().getDataParcelResource();
                String containerMessageVersion = payload.getPayloadManifest().getContainerDescriptor().getVersion();
                messageBuilder.append("Container Payload: "+containerMessageDefiner+"::"+containerMessageCategory+"::"+containerMessageSubcategory+"::"+ containerMessageResource+"("+containerMessageVersion+") \n");
            }
        }
        String definer = getContentDescriptorElement(payload, DataParcelDescriptorKeyEnum.DATASET_DEFINER);
        String category = getContentDescriptorElement(payload, DataParcelDescriptorKeyEnum.DATASET_CATEGORY);
        String subcategory = getContentDescriptorElement(payload, DataParcelDescriptorKeyEnum.DATASET_SUBCATEGORY);
        String resource = getContentDescriptorElement(payload, DataParcelDescriptorKeyEnum.DATASET_RESOURCE);
        String version = getContentDescriptorElement(payload, DataParcelDescriptorKeyEnum.DATASET_VERSION);
        messageBuilder.append("Content Payload: "+definer+"::"+category+"::"+subcategory+"::"+ resource+"("+version+") \n");
        return(messageBuilder.toString());
    }

    public String payloadTypeFromUoW(UoWPayload payload){
        if(payload == null){
            return("---");
        }
        if(StringUtils.isEmpty(payload.getPayload())){
            return("---");
        }
        StringBuilder messageBuilder = new StringBuilder();
        if(payload.getPayloadManifest().hasContainerDescriptor()){
            if(payload.getPayloadManifest().hasContainerDescriptor()){
                String containerMessageDefiner = payload.getPayloadManifest().getContainerDescriptor().getDataParcelDefiner();
                String containerMessageCategory= payload.getPayloadManifest().getContainerDescriptor().getDataParcelCategory();
                String containerMessageSubcategory = payload.getPayloadManifest().getContainerDescriptor().getDataParcelSubCategory();
                String containerMessageResource= payload.getPayloadManifest().getContainerDescriptor().getDataParcelResource();
                String containerMessageVersion = payload.getPayloadManifest().getContainerDescriptor().getVersion();
                messageBuilder.append(containerMessageDefiner+"::"+containerMessageCategory+"::"+containerMessageSubcategory+"::"+ containerMessageResource+"("+containerMessageVersion+")/");
            }
        }
        String definer = getContentDescriptorElement(payload, DataParcelDescriptorKeyEnum.DATASET_DEFINER);
        String category = getContentDescriptorElement(payload, DataParcelDescriptorKeyEnum.DATASET_CATEGORY);
        String subcategory = getContentDescriptorElement(payload, DataParcelDescriptorKeyEnum.DATASET_SUBCATEGORY);
        String resource = getContentDescriptorElement(payload, DataParcelDescriptorKeyEnum.DATASET_RESOURCE);
        String version = getContentDescriptorElement(payload, DataParcelDescriptorKeyEnum.DATASET_VERSION);
        messageBuilder.append(definer+"::"+category+"::"+subcategory+"::"+ resource+"("+version+")");
        return(messageBuilder.toString());
    }

    public String newNotificationContentFromUoWPayload(FulfillmentExecutionStatusEnum status, UoWPayload payload){
        if(payload == null){
            return("");
        }
        StringBuilder notificationContentBuilder = new StringBuilder();
        notificationContentBuilder.append("Task: Status("+status.getDisplayName() + "): Internal Timestamp:" + getTimeFormatter().format(Instant.now()) + "\n");
        if(isHL7V2Payload(payload)){
            if(payload.getPayloadManifest().hasContainerDescriptor()){
                String containerMessageDefiner = payload.getPayloadManifest().getContainerDescriptor().getDataParcelDefiner();
                String containerMessageCategory= payload.getPayloadManifest().getContainerDescriptor().getDataParcelCategory();
                String containerMessageSubcategory = payload.getPayloadManifest().getContainerDescriptor().getDataParcelSubCategory();
                String containerMessageResource= payload.getPayloadManifest().getContainerDescriptor().getDataParcelResource();
                String containerMessageVersion = payload.getPayloadManifest().getContainerDescriptor().getVersion();
                notificationContentBuilder.append("Container Payload: "+containerMessageDefiner+"::"+containerMessageCategory+"::"+containerMessageSubcategory+"::"+ containerMessageResource+"("+containerMessageVersion+") \n");
            }
            String messageType = payload.getPayloadManifest().getContentDescriptor().getDataParcelSubCategory();
            String messageTrigger = payload.getPayloadManifest().getContentDescriptor().getDataParcelResource();
            String messageVersion = payload.getPayloadManifest().getContentDescriptor().getVersion();
            if(payload.getPayloadManifest().hasContainerDescriptor()){
                notificationContentBuilder.append("Content Payload: HL7 v2.x: Trigger: "+messageType+"^"+messageTrigger+"("+messageVersion+")");
            } else {
                notificationContentBuilder.append("Content Payload: HL7 v2.x: Trigger: " + messageType + "^" + messageTrigger + "(" + messageVersion + ") \n");
                List<String> metadataBody = pipeParser.extractMetadataFromHL7v2xMessage(payload.getPayload());
                for(String metadataSegment: metadataBody) {
                    notificationContentBuilder.append(metadataSegment);
                }
            }
            return(notificationContentBuilder.toString());
        }
        //
        // Default Message
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("Task: Status("+status.getDisplayName() + "): Internal Timestamp:" + getTimeFormatter().format(Instant.now()) + "\n");
        if(payload.getPayloadManifest().hasContainerDescriptor()){
            if(payload.getPayloadManifest().hasContainerDescriptor()){
                String containerMessageDefiner = payload.getPayloadManifest().getContainerDescriptor().getDataParcelDefiner();
                String containerMessageCategory= payload.getPayloadManifest().getContainerDescriptor().getDataParcelCategory();
                String containerMessageSubcategory = payload.getPayloadManifest().getContainerDescriptor().getDataParcelSubCategory();
                String containerMessageResource= payload.getPayloadManifest().getContainerDescriptor().getDataParcelResource();
                String containerMessageVersion = payload.getPayloadManifest().getContainerDescriptor().getVersion();
                notificationContentBuilder.append("Container Payload: "+containerMessageDefiner+"::"+containerMessageCategory+"::"+containerMessageSubcategory+"::"+ containerMessageResource+"("+containerMessageVersion+") \n");
            }
        }
        String definer = getContentDescriptorElement(payload, DataParcelDescriptorKeyEnum.DATASET_DEFINER);
        String category = getContentDescriptorElement(payload, DataParcelDescriptorKeyEnum.DATASET_CATEGORY);
        String subcategory = getContentDescriptorElement(payload, DataParcelDescriptorKeyEnum.DATASET_SUBCATEGORY);
        String resource = getContentDescriptorElement(payload, DataParcelDescriptorKeyEnum.DATASET_RESOURCE);
        String version = getContentDescriptorElement(payload, DataParcelDescriptorKeyEnum.DATASET_VERSION);
        messageBuilder.append("Content Payload: "+definer+"::"+category+"::"+subcategory+"::"+ resource+"("+version+") \n");
        return(messageBuilder.toString());
    }

    public ITOpsNotificationContent newNotificationForSoftFailure( String errorMessage){
        StringBuilder unformattedContent = new StringBuilder();
        StringBuilder formattedContent = new StringBuilder();

        if(StringUtils.isEmpty(errorMessage)){
            return(null);
        }

        unformattedContent.append("--- Soft Failure ---\n");
        unformattedContent.append(errorMessage + "\n");
        unformattedContent.append("----------------------");

        formattedContent.append("<table>");
        formattedContent.append("<tr>");
        formattedContent.append("<th><font color=red>Soft Failure</font></th><th><font color=red>"+getTimeFormatter().format(Instant.now())+"</font></th>");
        formattedContent.append("</tr>");
        formattedContent.append("</table>");

        ITOpsNotificationContent notificationContent = new ITOpsNotificationContent();
        notificationContent.setContent(unformattedContent.toString());
        notificationContent.setFormattedContent(formattedContent.toString());
        return(notificationContent);
    }

    //
    // HL7 v2 Message Content Helpers
    //
    public List<String> getHL7v2MetadataHeaderInfo(UoWPayload payload){

        List<String> headerInfo = new ArrayList<>();
        if(payload.getPayloadManifest().hasContainerDescriptor()){
            StringBuilder containerBuilder = new StringBuilder();
            String containerMessageDefiner = payload.getPayloadManifest().getContainerDescriptor().getDataParcelDefiner();
            String containerMessageCategory= payload.getPayloadManifest().getContainerDescriptor().getDataParcelCategory();
            String containerMessageSubcategory = payload.getPayloadManifest().getContainerDescriptor().getDataParcelSubCategory();
            String containerMessageResource= payload.getPayloadManifest().getContainerDescriptor().getDataParcelResource();
            String containerMessageVersion = payload.getPayloadManifest().getContainerDescriptor().getVersion();
            containerBuilder.append("Container Payload: "+containerMessageDefiner+"::"+containerMessageCategory+"::"+containerMessageSubcategory+"::"+ containerMessageResource+"("+containerMessageVersion+")");
            headerInfo.add(containerBuilder.toString());
        }
        StringBuilder contentBuilder = new StringBuilder();
        String messageType = payload.getPayloadManifest().getContentDescriptor().getDataParcelSubCategory();
        String messageTrigger = payload.getPayloadManifest().getContentDescriptor().getDataParcelResource();
        String messageVersion = payload.getPayloadManifest().getContentDescriptor().getVersion();
        contentBuilder.append("Content Payload: HL7 v2.x: Trigger: " + messageType + "^" + messageTrigger + "(" + messageVersion + ")");
        headerInfo.add(contentBuilder.toString());
        return(headerInfo);
    }

    protected String getContentDescriptorElement(UoWPayload payload, DataParcelDescriptorKeyEnum descriptorKey) {
        if(payload == null){
            return(null);
        }
        if(payload.getPayloadManifest() == null){
            return(null);
        }
        if(payload.getPayloadManifest().getContentDescriptor() == null){
            return(null);
        }
        if(descriptorKey == null){
            return(null);
        }
        String descriptorElement = getDescriptorElement(payload.getPayloadManifest().getContentDescriptor(), descriptorKey);
        return(descriptorElement);
    }

    protected String getDescriptorElement(DataParcelTypeDescriptor descriptor, DataParcelDescriptorKeyEnum descriptorKey){

        switch(descriptorKey){
            case DATASET_DEFINER:
                return(descriptor.getDataParcelDefiner());
            case DATASET_CATEGORY:
                return(descriptor.getDataParcelCategory());
            case DATASET_SUBCATEGORY:
                return(descriptor.getDataParcelSubCategory());
            case DATASET_RESOURCE:
                return(descriptor.getDataParcelResource());
            case DATASET_SEGMENT:
                return(descriptor.getDataParcelSegment());
            case DATASET_ATTRIBUTE:
                return(descriptor.getDataParcelAttribute());
            case DATASET_DISCRIMINATOR_TYPE:
                return(descriptor.getDataParcelDiscriminatorType());
            case DATASET_DISCRIMINATOR_VALUE:
                return(descriptor.getDataParcelDiscriminatorValue());
            case DATASET_NORMALISATION_STATUS:
                break;
            case DATASET_VALIDATION_STATUS:
                break;
            case DATASET_TYPE:
                break;
            case DATASET_SOURCE:
                break;
            case DATASET_INTENDED_TARGET:
                break;
            case DATASET_VERSION:
                return(descriptor.getVersion());
        }
        return(null);
    }

    public boolean isHL7V2Payload(UoWPayload payload){
        if(payload == null){
            return(false);
        }
        if(payload.getPayloadManifest() == null){
            return(false);
        }
        if(payload.getPayloadManifest().getContentDescriptor() == null){
            return(false);
        }
        DataParcelTypeDescriptor payloadDescriptor = payload.getPayloadManifest().getContentDescriptor();
        if(payloadDescriptor.getDataParcelDefiner().contentEquals(hl7V2XTopicFactory.getHl7MessageDefiner())){
            if(payloadDescriptor.getDataParcelCategory().contentEquals(hl7V2XTopicFactory.getHl7MessageCategory())){
                return(true);
            }
        }
        return(false);
    }
}
