/*
 * The MIT License
 *
 * Copyright 2022 mhunter.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.fhirfactory.dricats.petasos.ipc.technologies.jgroups;

import java.util.UUID;

import javax.inject.Inject;
import net.fhirfactory.dricats.model.configuration.filebased.archetypes.valuesets.ClusterFunctionNameEnum;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.dricats.util.SystemPropertyHelper;

/**
 *
 * @author mhunter
 */
public class JGroupsChannelNamingUtilities {
    private static final Logger LOG = LoggerFactory.getLogger(JGroupsChannelNamingUtilities.class);

    private String currentUUID;

    private String CHANNEL_NAME_SEPARATOR = "::";

    private static int JGROUPS_INTEGRATION_POINT_SITE_POSITION_IN_CHANNEL_NAME = 0;
    private static int JGROUPS_INTEGRATION_POINT_ZONE_POSITION_IN_CHANNEL_NAME = 1;
    private static int JGROUPS_INTEGRATION_POINT_SERVICE_NAME_POSITION_IN_CHANNEL_NAME = 2;
    private static int JGROUPS_INTEGRATION_POINT_FUNCTION_POSITION_IN_CHANNEL_NAME = 3;
    private static int JGROUPS_INTEGRATION_POINT_UNIQUE_ID_POSITION_IN_CHANNEL_NAME = 4;

    private static int SUBSYSTEM_NAME_POSITION_IN_PROCESSING_PLANT_ID = 0;
    private static int UNIQUE_KEY_POSITION_IN_PROCESSING_PLANT_ID = 1;

    @Inject
    private SystemPropertyHelper pegacornProperties;

    //
    // Constructor(s)
    //

    public JGroupsChannelNamingUtilities(){
        this.currentUUID = UUID.randomUUID().toString();
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }

    //
    // Business Methods
    //

    public String buildChannelName(String site, String zone, String subsystemName, String functionName, String uniqueId){
        String channelName = site
                + getChannelNameSeparator()
                + zone
                + getChannelNameSeparator()
                + subsystemName
                + getChannelNameSeparator()
                + functionName
                + getChannelNameSeparator()
                + uniqueId;
        return(channelName);
    }

    public String buildProcessingPlantName(String endpointServiceName, String endpointUniqueID ){
        String endpointName =  endpointServiceName
                + getChannelNameSeparator()
                + endpointUniqueID;
        return(endpointName);
    }

    public String buildUniqueComponentName(String name){
        String uuidString = getCurrentUUID().replace("-","");
        String uniqueQualifier = pegacornProperties.getProperty("MY_POD_IP", uuidString);
        String uniqueUsefulQualifier = uniqueQualifier.replace(".", "-");
        String uniqueName = name + "-" + uniqueUsefulQualifier;
        return(uniqueName);
    }

    public String buildProcessingPlantFromChannelName(String channelName){
        if(StringUtils.isEmpty(channelName)){
            return(null);
        }
        String endpointServiceName = getEndpointSubsystemNameFromChannelName(channelName);
        String endpointUniqueID = getEndpointUniqueIDFromChannelName(channelName);
        String endpointName = buildProcessingPlantName(endpointServiceName,endpointUniqueID);
        return(endpointName);
    }

    public String getRoutingGroupChannelNameFromOtherChannelName(String channelName){
        if(StringUtils.isEmpty(channelName)){
            return(null);
        }
        String routingName = remapFunctionTypeInChannelName(channelName, ClusterFunctionNameEnum.PETASOS_ROUTING_SERVICES);
        return(routingName);
    }

    public String getTopologyServicesChannelNameFromOtherChannelName(String channelName){
        if(StringUtils.isEmpty(channelName)){
            return(null);
        }
        String oamDiscoveryName = remapFunctionTypeInChannelName(channelName, ClusterFunctionNameEnum.PETASOS_TOPOLOGY_SERVICES);
        return(oamDiscoveryName);
    }

    public String getMessagingServicesChannelNameFromOtherChannelName(String channelName){
        if(StringUtils.isEmpty(channelName)){
            return(null);
        }
        String ipcName = remapFunctionTypeInChannelName(channelName, ClusterFunctionNameEnum.PETASOS_RMI_SERVICES);
        return(ipcName);
    }

    private String remapFunctionTypeInChannelName(String channelName, ClusterFunctionNameEnum functionType){
        if(StringUtils.isEmpty(channelName)){
            return(null);
        }
        String[] nameSplit = channelName.split(getChannelNameSeparator());
        if(nameSplit.length < JGROUPS_INTEGRATION_POINT_SERVICE_NAME_POSITION_IN_CHANNEL_NAME){
            return(null);
        }
        String endpointServiceName = nameSplit[JGROUPS_INTEGRATION_POINT_SERVICE_NAME_POSITION_IN_CHANNEL_NAME];
        if(nameSplit.length < JGROUPS_INTEGRATION_POINT_SITE_POSITION_IN_CHANNEL_NAME){
            return(null);
        }
        String endpointSiteName = nameSplit[JGROUPS_INTEGRATION_POINT_SITE_POSITION_IN_CHANNEL_NAME];
        if(nameSplit.length < JGROUPS_INTEGRATION_POINT_ZONE_POSITION_IN_CHANNEL_NAME){
            return(null);
        }
        String endpointZoneName = nameSplit[JGROUPS_INTEGRATION_POINT_ZONE_POSITION_IN_CHANNEL_NAME];
        if(nameSplit.length < JGROUPS_INTEGRATION_POINT_UNIQUE_ID_POSITION_IN_CHANNEL_NAME){
            return(null);
        }
        String endpointUUID = nameSplit[JGROUPS_INTEGRATION_POINT_UNIQUE_ID_POSITION_IN_CHANNEL_NAME];
        String endpointFunctionName = functionType.getGroupName();
        String oamPubSubName = buildChannelName(endpointSiteName,endpointZoneName,endpointServiceName,endpointFunctionName,endpointUUID);
        return(oamPubSubName);
    }

    public String getEndpointNameFromChannelName(String channelName){
        if(StringUtils.isEmpty(channelName)){
            return(null);
        }
        String[] nameSplit = channelName.split(getChannelNameSeparator());
        if(nameSplit.length < JGROUPS_INTEGRATION_POINT_SERVICE_NAME_POSITION_IN_CHANNEL_NAME){
            return(null);
        }
        String endpointServiceName = nameSplit[JGROUPS_INTEGRATION_POINT_SERVICE_NAME_POSITION_IN_CHANNEL_NAME];
        if(nameSplit.length < JGROUPS_INTEGRATION_POINT_UNIQUE_ID_POSITION_IN_CHANNEL_NAME){
            return(null);
        }
        String endpointUniqueID = nameSplit[JGROUPS_INTEGRATION_POINT_UNIQUE_ID_POSITION_IN_CHANNEL_NAME];
        String endpointName = buildProcessingPlantName(endpointServiceName, endpointUniqueID);
        return(endpointName);
    }

    public String getEndpointSiteFromChannelName(String channelName){
        if(StringUtils.isEmpty(channelName)){
            return(null);
        }
        String[] nameSplit = channelName.split(getChannelNameSeparator());
        if(nameSplit.length < JGROUPS_INTEGRATION_POINT_SITE_POSITION_IN_CHANNEL_NAME){
            return(null);
        }
        String endpointSite = nameSplit[JGROUPS_INTEGRATION_POINT_SITE_POSITION_IN_CHANNEL_NAME];
        return(endpointSite);
    }

    public boolean isEndpointsInSameSiteBasedOnChannelNames(String endpointChannelName1, String endpointChannelName2){
        if(StringUtils.isEmpty(endpointChannelName1) || StringUtils.isEmpty(endpointChannelName2)){
            return(false);
        }
        String endpointSite1 = getEndpointSiteFromChannelName(endpointChannelName1);
        String endpointSite2 = getEndpointSiteFromChannelName(endpointChannelName2);
        if(StringUtils.isEmpty(endpointSite1) || StringUtils.isEmpty(endpointSite2)){
            return(false);
        }
        boolean areSameSite = endpointSite1.contentEquals(endpointSite2);
        return(areSameSite);
    }

    public String getEndpointZoneFromChannelName(String channelName){
        if(StringUtils.isEmpty(channelName)){
            return(null);
        }
        String[] nameSplit = channelName.split(getChannelNameSeparator());
        if(nameSplit.length < JGROUPS_INTEGRATION_POINT_ZONE_POSITION_IN_CHANNEL_NAME){
            return(null);
        }
        String endpointZone = nameSplit[JGROUPS_INTEGRATION_POINT_ZONE_POSITION_IN_CHANNEL_NAME];
        return(endpointZone);
    }

    public boolean isEndpointsInSameZoneBasedOnChannelNames(String endpointChannelName1, String endpointChannelName2){
        if(StringUtils.isEmpty(endpointChannelName1) || StringUtils.isEmpty(endpointChannelName2)){
            return(false);
        }
        String endpointZone1 = getEndpointZoneFromChannelName(endpointChannelName1);
        String endpointZone2 = getEndpointZoneFromChannelName(endpointChannelName2);
        if(StringUtils.isEmpty(endpointZone1) || StringUtils.isEmpty(endpointZone2)){
            return(false);
        }
        boolean areSameZone = endpointZone1.contentEquals(endpointZone2);
        return(areSameZone);
    }

    public String getEndpointSubsystemNameFromChannelName(String channelName){
        if(StringUtils.isEmpty(channelName)){
            return(null);
        }
        String[] nameSplit = channelName.split(getChannelNameSeparator());
        if(nameSplit.length < JGROUPS_INTEGRATION_POINT_SERVICE_NAME_POSITION_IN_CHANNEL_NAME){
            return(null);
        }
        String endpointServiceName = nameSplit[JGROUPS_INTEGRATION_POINT_SERVICE_NAME_POSITION_IN_CHANNEL_NAME];
        return(endpointServiceName);
    }

    public boolean isEndpointsDeliveringSameServiceBasedOnChannelNames(String endpointChannelName1, String endpointChannelName2){
        if(StringUtils.isEmpty(endpointChannelName1) || StringUtils.isEmpty(endpointChannelName2)){
            return(false);
        }
        String endpointServiceName1 = getEndpointSubsystemNameFromChannelName(endpointChannelName1);
        String endpointServiceName2 = getEndpointSubsystemNameFromChannelName(endpointChannelName2);
        if(StringUtils.isEmpty(endpointServiceName1) || StringUtils.isEmpty(endpointServiceName2)){
            return(false);
        }
        boolean areSameService = endpointServiceName1.contentEquals(endpointServiceName2);
        return(areSameService);
    }

    public String getEndpointFunctionFromChannelName(String channelName){
        if(StringUtils.isEmpty(channelName)){
            return(null);
        }
        String[] nameSplit = channelName.split(getChannelNameSeparator());
        if(nameSplit.length < JGROUPS_INTEGRATION_POINT_FUNCTION_POSITION_IN_CHANNEL_NAME){
            getLogger().warn(".getEndpointFunctionFromChannelName(): channelName is not properly formed, value->{}", channelName);
            return(null);
        }
        String endpointFunctionName = nameSplit[JGROUPS_INTEGRATION_POINT_FUNCTION_POSITION_IN_CHANNEL_NAME];
        return(endpointFunctionName);
    }

    public boolean isEndpointsSupportingSameFunctionBaseOnChannelNames(String endpointChannelName1, String endpointChannelName2){
        if(StringUtils.isEmpty(endpointChannelName1) || StringUtils.isEmpty(endpointChannelName2)){
            return(false);
        }
        String endpointFunctionName1 = getEndpointFunctionFromChannelName(endpointChannelName1);
        String endpointFunctionName2 = getEndpointFunctionFromChannelName(endpointChannelName2);
        if(StringUtils.isEmpty(endpointFunctionName1) || StringUtils.isEmpty(endpointFunctionName2)){
            return(false);
        }
        boolean areSameFunction = endpointFunctionName1.contentEquals(endpointFunctionName2);
        return(areSameFunction);
    }

    public String getEndpointUniqueIDFromChannelName(String channelName){
        if(StringUtils.isEmpty(channelName)){
            return(null);
        }
        String[] nameSplit = channelName.split(getChannelNameSeparator());
        if(nameSplit.length < JGROUPS_INTEGRATION_POINT_UNIQUE_ID_POSITION_IN_CHANNEL_NAME){
            return(null);
        }
        String endpointUniqueID = nameSplit[JGROUPS_INTEGRATION_POINT_UNIQUE_ID_POSITION_IN_CHANNEL_NAME];
        return(endpointUniqueID);
    }

    public String getSubsystemNameFromEndpointName(String channelName){
        if(StringUtils.isEmpty(channelName)){
            return(null);
        }
        String[] nameSplit = channelName.split(getChannelNameSeparator());
        if(nameSplit.length < SUBSYSTEM_NAME_POSITION_IN_PROCESSING_PLANT_ID){
            return(null);
        }
        String endpointFunctionName = nameSplit[SUBSYSTEM_NAME_POSITION_IN_PROCESSING_PLANT_ID];
        return(endpointFunctionName);
    }

    public boolean isEndpointsSupportingSameServiceBasedOnEndpointNames(String endpointName1, String endpointName2) {
        if (StringUtils.isEmpty(endpointName1) || StringUtils.isEmpty(endpointName2)) {
            return (false);
        }
        String endpointServiceName1 = getSubsystemNameFromEndpointName(endpointName1);
        String endpointServiceName2 = getSubsystemNameFromEndpointName(endpointName2);
        if (StringUtils.isEmpty(endpointServiceName1) || StringUtils.isEmpty(endpointServiceName2)) {
            return (false);
        }
        boolean areSameService = endpointServiceName1.contentEquals(endpointServiceName2);
        return (areSameService);
    }

    public String getEndpointUniqueIDFromEndpointName(String endpointName){
        if(StringUtils.isEmpty(endpointName)){
            return(null);
        }
        String[] nameSplit = endpointName.split(getChannelNameSeparator());
        if(nameSplit.length < UNIQUE_KEY_POSITION_IN_PROCESSING_PLANT_ID){
            return(null);
        }
        String endpointScopeName = nameSplit[UNIQUE_KEY_POSITION_IN_PROCESSING_PLANT_ID];
        return(endpointScopeName);
    }

    //
    // Getters (and Setters)
    //

    public String getChannelNameSeparator() {
        return CHANNEL_NAME_SEPARATOR;
    }

    public String getCurrentUUID(){
        return(this.currentUUID);
    }

    public  String getPetasosRoutingGroupName(){
        return(ClusterFunctionNameEnum.PETASOS_ROUTING_SERVICES.getGroupName());
    }

    public String getPetasosTopologyGroupName(){
        return(ClusterFunctionNameEnum.PETASOS_TOPOLOGY_SERVICES.getGroupName());
    }

    public String getPetasosMessagingGroupName(){
        return(ClusterFunctionNameEnum.PETASOS_RMI_SERVICES.getGroupName());
    }

    public String getPetasosTaskingGroupName(){
        return(ClusterFunctionNameEnum.PETASOS_TASKING_SERVICES.getGroupName());
    }

    public String getPetasosInterceptionGroupName(){
        return(ClusterFunctionNameEnum.PETASOS_INTERCEPTION_SERVICES.getGroupName());
    }

    public String getPetasosObservationsGroupName(){
        return(ClusterFunctionNameEnum.PETASOS_OBSERVATION_SERVICES.getGroupName());
    }
    
    public String getPetasosMediaGroupName(){
        return(ClusterFunctionNameEnum.PETASOS_MEDIA_SERVICES.getGroupName());
    }
    
    public String getPetasosAuditGroupName(){
        return(ClusterFunctionNameEnum.PETASOS_AUDIT_SERVICES.getGroupName());
    }
}
