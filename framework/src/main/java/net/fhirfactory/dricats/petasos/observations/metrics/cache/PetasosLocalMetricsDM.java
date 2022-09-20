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
package net.fhirfactory.dricats.petasos.observations.metrics.cache;

import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.petasos.observations.metrics.component.WorkUnitProcessorMetricsData;
import net.fhirfactory.dricats.model.petasos.observations.metrics.component.common.CommonComponentMetricsData;
import net.fhirfactory.dricats.petasos.tasking.observations.common.LocalOAMCacheBase;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class PetasosLocalMetricsDM extends LocalOAMCacheBase {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosLocalMetricsDM.class);

    private Map<ComponentId, CommonComponentMetricsData> nodeMetricsMap;
    private Map<ComponentId, Object> nodeMetricsLockMap;
    private Object metricsSetMapLock;

    //
    // Constructor(s)
    //

    public PetasosLocalMetricsDM(){
        super();
        this.nodeMetricsMap = new ConcurrentHashMap<>();
        this.nodeMetricsLockMap = new ConcurrentHashMap<>();
        this.metricsSetMapLock = new Object();
    }

    //
    // Getters (and Setters)
    //

    public Map<ComponentId, CommonComponentMetricsData> getNodeMetricsMap() {
        return nodeMetricsMap;
    }

    public Map<ComponentId, Object> getNodeMetricsLockMap() {
        return nodeMetricsLockMap;
    }

    public Object getMetricsSetMapLock() {
        return metricsSetMapLock;
    }

    protected Logger getLogger(){
        return(LOG);
    }

    //
    // Helper / Business Methods
    //

    public void addNodeMetrics(CommonComponentMetricsData metrics, Object lockObject){
        getLogger().debug(".addNodeMetrics(): Entry, metrics->{}", metrics);
        synchronized (getMetricsSetMapLock()) {
            if (getNodeMetricsLockMap().containsKey(metrics.getComponentID())) {
                getNodeMetricsLockMap().remove(metrics.getComponentID());
            }
            if(getNodeMetricsMap().containsKey(metrics.getComponentID())){
                getNodeMetricsMap().remove(metrics.getComponentID());
            }
            getNodeMetricsMap().put(metrics.getComponentID(), metrics);
            getNodeMetricsLockMap().put(metrics.getComponentID(), lockObject);
            refreshCurrentStateUpdateInstant();
        }
        getLogger().debug(".addNodeMetrics(): Exit");
    }

    public List<CommonComponentMetricsData> getAllMetricsSets(){
        List<CommonComponentMetricsData> metricsSetList = new ArrayList<>();
        synchronized (getMetricsSetMapLock()){
            metricsSetList.addAll(getNodeMetricsMap().values());
        }
        return(metricsSetList);
    }

    public Object getNodeMetricsLock(ComponentId componentID){
        Object lock = getNodeMetricsLockMap().get(componentID);
        return(lock);
    }

    public void setNodeMetricsLock(ComponentId componentIdType, Object lockObject){
        if(getNodeMetricsLockMap().containsKey(componentIdType)){
            getNodeMetricsLockMap().remove(componentIdType);
        }
        getNodeMetricsLockMap().put(componentIdType, lockObject);
    }

    public CommonComponentMetricsData getNodeMetrics(ComponentId componentID){
        CommonComponentMetricsData metrics = getNodeMetricsMap().get(componentID);
        return(metrics);
    }

    public WorkUnitProcessorMetricsData getWorkUnitProcessingMetricsData(ComponentId componetId){
        WorkUnitProcessorMetricsData nodeMetrics = null;
        Object lockObject = getNodeMetricsLock(componetId);
        if(lockObject == null){
            return(null);
        }
        synchronized (lockObject) {
            nodeMetrics = SerializationUtils.clone((WorkUnitProcessorMetricsData) getNodeMetrics(componetId));
        }
        if(nodeMetrics == null){
            nodeMetrics = new WorkUnitProcessorMetricsData(componetId);
            getNodeMetricsMap().put(componetId, nodeMetrics);
            getNodeMetricsLockMap().put(componetId, new Object());
        }
        return(nodeMetrics);
    }

}
