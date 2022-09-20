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
package net.fhirfactory.dricats.petasos.tasking.factories.metadata;

import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.model.petasos.dataparcel.DataParcelTypeDescriptor;
import net.fhirfactory.pegacorn.core.model.petasos.dataparcel.valuesets.DataParcelDescriptorKeyEnum;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWPayload;
import net.fhirfactory.dricats.internals.fhir.r4.internal.topics.HL7V2XTopicFactory;
import net.fhirfactory.pegacorn.internals.hl7v2.helpers.UltraDefensivePipeParser;
import net.fhirfactory.pegacorn.internals.hl7v2.triggerevents.valuesets.HL7v2SegmentTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class HL7v2xTaskMetadataExtractor {
    private static final Logger LOG = LoggerFactory.getLogger(HL7v2xTaskMetadataExtractor.class);

    private DateTimeFormatter timeFormatter;

    @Inject
    private HL7V2XTopicFactory hl7V2XTopicFactory;

    @Inject
    private UltraDefensivePipeParser defensivePipeParser;

    //
    // Constructor(s)
    //

    public HL7v2xTaskMetadataExtractor(){
        timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss.SSS").withZone(ZoneId.of(PetasosPropertyConstants.DEFAULT_TIMEZONE));
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }

    protected DateTimeFormatter getTimeFormatter(){
        return(timeFormatter);
    }

    //
    // Business Methods
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

    public String getContentDescriptorElement(UoWPayload payload, DataParcelDescriptorKeyEnum descriptorKey) {
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

    public String getDescriptorElement(DataParcelTypeDescriptor descriptor, DataParcelDescriptorKeyEnum descriptorKey){

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

    public List<String> extractMetadataFromHL7v2xMessage(String payload){
        if(payload == null){
            List<String> errorPayload = new ArrayList<>();
            errorPayload.add("*empty payload*");
            return(errorPayload);
        }
        List<String> segmentList = getSegmentList(payload);
        String pid = null;
        String msh = null;
        if(segmentList != null){
            pid = getPatientIdentitySegment(segmentList);
            msh = getMessageHeaderSegment(segmentList);
        }
        List<String> metadata = new ArrayList<>();
        if(msh == null){
            metadata.add("*malformed payload (cannot resolve MSH)*");
            return(metadata);
        }
        metadata.add(msh);
        if(pid == null){
            pid = "No PID Segment";
        }
        metadata.add(pid);
        return(metadata);
    }

    public String getMSH(String payload){
        List<String> segmentList = getSegmentList( payload);
        String msh = defensivePipeParser.getSegment(segmentList, HL7v2SegmentTypeEnum.MSH.getKey(), 1);
        return(msh);
    }

    public String getPID(String payload){
        List<String> segmentList = getSegmentList( payload);
        String pid = getPatientIdentitySegment(segmentList);
        return(pid);
    }

    protected String getMessageHeaderSegment(List<String> segmentList){
        if(segmentList == null){
            return(null);
        }
        for(String currentSegment: segmentList){
            String currentSegmentStart = currentSegment.substring(0, 6);
            if(currentSegmentStart.contains("MSH")){
                return(currentSegment);
            }
        }
        return(null);
    }

    protected String getPatientIdentitySegment(List<String> segmentList){
        if(segmentList == null){
            return(null);
        }
        String segment = defensivePipeParser.getSegment(segmentList, HL7v2SegmentTypeEnum.PID.getKey(), 1);
        return(segment);
    }

    public List<String> getSegmentList(String message){
        List<String> segmentList = defensivePipeParser.getSegmentList(message);
        return(segmentList);
    }

}
