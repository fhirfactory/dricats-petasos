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
package net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.management.topology;

public class IPCConnectionInstanceDetail {
    private boolean httpBased;
    private boolean jgroupsBased;

    private String serverEndpointDetail;
    private String clientEndpointDetail;

    public IPCConnectionInstanceDetail(){
        this.httpBased = false;
        this.jgroupsBased = false;
    }

    public boolean isHttpBased() {
        return httpBased;
    }

    public void setHttpBased(boolean httpBased) {
        this.httpBased = httpBased;
    }

    public boolean isJgroupsBased() {
        return jgroupsBased;
    }

    public void setJgroupsBased(boolean jgroupsBased) {
        this.jgroupsBased = jgroupsBased;
    }

    public String getServerEndpointDetail() {
        return serverEndpointDetail;
    }

    public void setServerEndpointDetail(String serverEndpointDetail) {
        this.serverEndpointDetail = serverEndpointDetail;
    }

    public String getClientEndpointDetail() {
        return clientEndpointDetail;
    }

    public void setClientEndpointDetail(String clientEndpointDetail) {
        this.clientEndpointDetail = clientEndpointDetail;
    }
}
