package net.fhirfactory.dricats.model.transforms.tofhir.task.factories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.fhirfactory.dricats.configuration.defaults.dricats.systemwide.ReferenceProperties;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.dataparcel.DataParcelManifest;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.work.UoWPayload;

@ApplicationScoped
public class TaskWorkItemFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TaskWorkItemFactory.class);

    private ObjectMapper jsonMapper;

    @Inject
    private ReferenceProperties systemWideProperties;

    private static final String PEGACORN_TASK_PARAMETER_COMPONENT_DATA_PARCEL_DESCRIPTOR = "/task-parameter-component-data-parcel-descriptor";

    //
    // Constructor(s)
    //

    public TaskWorkItemFactory(){
        jsonMapper = new ObjectMapper();
        jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
        JavaTimeModule module = new JavaTimeModule();
        this.jsonMapper.registerModule(module);
        this.jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    //
    // Business Methods
    //

    public String getPegacornTaskParameterComponentDataParcelDescriptor(){
        String codeSystem = systemWideProperties.getPegacornCodeSystemSite() + PEGACORN_TASK_PARAMETER_COMPONENT_DATA_PARCEL_DESCRIPTOR;
        return (codeSystem);
    }

    public Task.ParameterComponent newWorkItemPayload(UoWPayload payload){
        String parcelManifestAsString = mapDataParcelManifestToString(payload.getPayloadManifest());
        if(StringUtils.isEmpty(parcelManifestAsString)){
            return(null);
        }
        CodeableConcept  workItemType = new CodeableConcept();
        Coding workItemTypeCoding = new Coding();
        workItemTypeCoding.setSystem(getPegacornTaskParameterComponentDataParcelDescriptor());
        workItemTypeCoding.setCode(parcelManifestAsString);
        workItemType.addCoding(workItemTypeCoding);
        Task.ParameterComponent taskWorkItem = new Task.ParameterComponent();
        taskWorkItem.setType(workItemType);
        taskWorkItem.setValue(new StringType(payload.getPayload()));
        return(taskWorkItem);
    }

    protected String mapDataParcelManifestToString(DataParcelManifest parcelManifest){
        try {
            String s = getJSONMapper().writeValueAsString(parcelManifest);
            return(s);
        } catch (JsonProcessingException e) {
            getLogger().error(".mapDataParcelManifestToString(): Error -->{}", ExceptionUtils.getStackTrace(e));
            return(null);
        }
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }

    protected ObjectMapper getJSONMapper(){
        return(this.jsonMapper);
    }
}
