/*
 * The MIT License
 *
 * Copyright 2022 Mark A. Hunter.
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
package net.fhirfactory.dricats.petasos.ipc.technologies.jgroups.maps;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.dricats.model.configuration.filebased.archetypes.valuesets.ClusterFunctionNameEnum;
import net.fhirfactory.dricats.model.petasos.ipc.endpoints.valuesets.JGroupsChannelStatusEnum;

/**
 *
 * @author MAHun
 */
@ApplicationScoped
public class JGroupsClusterConnectionStatusMap {
    private static final Logger LOG = LoggerFactory.getLogger(JGroupsClusterConnectionStatusMap.class);
    
    private ConcurrentHashMap<ClusterFunctionNameEnum, JGroupsChannelStatusEnum> statusMap;
    private Object statusMapLock;
    
    //
    // Constructor(s)
    //
    
    public JGroupsClusterConnectionStatusMap(){
        this.statusMap = new ConcurrentHashMap<>();
        this.statusMapLock = new Object();
        this.statusMap.put(ClusterFunctionNameEnum.PETASOS_AUDIT_SERVICES, JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_UNKNOWN);
        this.statusMap.put(ClusterFunctionNameEnum.PETASOS_TOPOLOGY_SERVICES, JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_UNKNOWN);
        this.statusMap.put(ClusterFunctionNameEnum.PETASOS_INTERCEPTION_SERVICES, JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_UNKNOWN);
        this.statusMap.put(ClusterFunctionNameEnum.PETASOS_RMI_SERVICES, JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_UNKNOWN);
        this.statusMap.put(ClusterFunctionNameEnum.PETASOS_MEDIA_SERVICES, JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_UNKNOWN);
        this.statusMap.put(ClusterFunctionNameEnum.PETASOS_ROUTING_SERVICES, JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_UNKNOWN);
        this.statusMap.put(ClusterFunctionNameEnum.PETASOS_OBSERVATION_SERVICES, JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_UNKNOWN);
        this.statusMap.put(ClusterFunctionNameEnum.PETASOS_TASKING_SERVICES, JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_UNKNOWN);
    }
    
    //
    // Getters (and Setters)
    //
    
    protected Logger getLogger(){
        return(LOG);
    }
    
    protected Map<ClusterFunctionNameEnum, JGroupsChannelStatusEnum> getStatusMap(){
        return(statusMap);
    }
    
    protected Object getStatusMapLock(){
        return(statusMapLock);
    }
    
    //
    // Business Methods
    //
    
    public void updateStatus(ClusterFunctionNameEnum functionNameEnum, JGroupsChannelStatusEnum statusEnum){
        synchronized(getStatusMapLock()){
            getStatusMap().replace(functionNameEnum, statusEnum);
        }
    }
    
    public JGroupsChannelStatusEnum getStatus(ClusterFunctionNameEnum functionNameEnum ){
    	JGroupsChannelStatusEnum status;
        synchronized(getStatusMapLock()){
            status = getStatusMap().get(functionNameEnum);
        }
        return(status);
    }
    
    public JGroupsChannelStatusEnum getAggregateStatus(){
    	JGroupsChannelStatusEnum aggregateStatus = JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_OPERATIONAL;
        
        //
        // all OPERATIONAL test
        boolean allOperational = false;
        for(JGroupsChannelStatusEnum currentStatus: getStatusMap().values()){
            if(!(currentStatus.equals(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_OPERATIONAL))){
                allOperational = false;
                break;
            }
        }
        if(allOperational){
            return(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_OPERATIONAL);
        }
        
        //
        // all STARTING test
        boolean allStarting = true;
        for(JGroupsChannelStatusEnum currentStatus: getStatusMap().values()){
            if(!(currentStatus.equals(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_STARTED))){
                allStarting = false;
                break;
            }
        }
        if(allStarting){
            return(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_STARTED);
        }

        //
        // all UNKNOWN test
        boolean allUnknown = true;
        for(JGroupsChannelStatusEnum currentStatus: getStatusMap().values()){
            if(!(currentStatus.equals(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_UNKNOWN))){
                allUnknown = false;
                break;
            }
        }
        if(allUnknown){
            return(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_UNKNOWN);
        }
        
        //
        // starting or operational test
        boolean allEitherStartingOrOperational = true;
        for(JGroupsChannelStatusEnum currentStatus: getStatusMap().values()){
            if(!(currentStatus.equals(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_STARTED) || currentStatus.equals(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_OPERATIONAL))){
                allEitherStartingOrOperational = false;
                break;
            }
        }
        if(allEitherStartingOrOperational){
            return(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_STARTED);
        }
    
        
        return(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_FAILED);
    }
}
