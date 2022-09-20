/*
 * Copyright (c) 2022 Tony Aleksovski (ACT Health)
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
package net.fhirfactory.dricats.petasos.participant.endpoint.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.fhirfactory.dricats.model.topology.endpoints.base.valuesets.EndpointTopologyNodeTypeEnum;
import net.fhirfactory.dricats.model.topology.endpoints.interact.StandardInteractServerETN;
import net.fhirfactory.dricats.petasos.participant.endpoint.file.adapter.FileShareSourceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileShareSourceETN extends StandardInteractServerETN {
    private static final Logger LOG = LoggerFactory.getLogger(FileShareSourceETN.class);
    
    private String fileShareName;
    private String fileShareProtocol;
    private String fileSharePath;
    private String fileShareServer;
    
    //
    // Constructor(s)
    //

    public FileShareSourceETN(){
        super();
        this.fileShareName = null;
        this.fileShareProtocol = null;
        this.fileSharePath = null;
        this.fileShareServer = null;
        setEndpointType(EndpointTopologyNodeTypeEnum.FILE_SHARE_SOURCE);
//        setComponentSystemRole(SoftwareComponentConnectivityContextEnum.COMPONENT_ROLE_INTERACT_INGRES);
    }
    
    //
    // Getters and Setters
    //
    
    public String getFileShareName() {
        return fileShareName;
    }

    public void setFileShareName(String fileShareName) {
        this.fileShareName = fileShareName;
    }

    public String getFileShareProtocol() {
        return fileShareProtocol;
    }

    public void setFileShareProtocol(String fileShareProtocol) {
        this.fileShareProtocol = fileShareProtocol;
    }

    public String getFileSharePath() {
        return fileSharePath;
    }

    public void setFileSharePath(String fileSharePath) {
        this.fileSharePath = fileSharePath;
    }

    public String getFileShareServer() {
        return fileShareServer;
    }

    public void setFileShareServer(String fileShareServer) {
        this.fileShareServer = fileShareServer;
    }

    @JsonIgnore
    public FileShareSourceAdapter getFileShareSourceAdapter(){
        getLogger().debug(".getFileShareSourceAdapter(): Entry");
        if(getAdapterList().isEmpty()){
            getLogger().debug(".getFileShareSourceAdapter(): Exit, Adapter list is empty, returning null");
            return(null);
        }
        FileShareSourceAdapter fileShareSource = (FileShareSourceAdapter) getAdapterList().get(0);
        getLogger().debug(".getHTTPServerAdapter(): Exit, httpServer->{}", fileShareSource);
        return(fileShareSource);
    }

    @JsonIgnore
    public void setFileShareSourceAdapter(FileShareSourceAdapter fileShareSource){
        if(fileShareSource != null){
            getAdapterList().add(fileShareSource);
        }
    }

    //
    // Getters
    //

    @Override
    protected Logger getLogger(){
        return(LOG);
    }
    
    //
    // toString
    //

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FileShareSourceTopologyEndpoint{");
        sb.append("fileShareName=").append(fileShareName);
        sb.append(", fileShareProtocol=").append(fileShareProtocol);
        sb.append(", fileSharePath=").append(fileSharePath);
        sb.append(", fileShareServer=").append(fileShareServer);
        sb.append(", ").append(super.toString()).append('}');
        return sb.toString();
    }
    
    
}
