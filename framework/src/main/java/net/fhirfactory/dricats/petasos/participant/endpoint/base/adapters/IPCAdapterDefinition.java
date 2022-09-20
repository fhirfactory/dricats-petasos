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
package net.fhirfactory.dricats.petasos.participant.endpoint.base.adapters;

import java.io.Serializable;
import java.util.Objects;

public class IPCAdapterDefinition implements Serializable {
    private String interfaceFormalName;
    private String interfaceFormalVersion;

    public IPCAdapterDefinition(){
        interfaceFormalName = null;
        interfaceFormalVersion = null;
    }

    public String getInterfaceFormalName() {
        return interfaceFormalName;
    }

    public void setInterfaceFormalName(String interfaceFormalName) {
        this.interfaceFormalName = interfaceFormalName;
    }

    public String getInterfaceFormalVersion() {
        return interfaceFormalVersion;
    }

    public void setInterfaceFormalVersion(String interfaceFormalVersion) {
        this.interfaceFormalVersion = interfaceFormalVersion;
    }

    public String getInterfaceDefinitionTag(){
        String newTag = "Interface:" + getInterfaceFormalName() + "-" + getInterfaceFormalVersion();
        return(newTag);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IPCAdapterDefinition that = (IPCAdapterDefinition) o;
        return Objects.equals(getInterfaceFormalName(), that.getInterfaceFormalName()) && Objects.equals(getInterfaceFormalVersion(), that.getInterfaceFormalVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInterfaceFormalName(), getInterfaceFormalVersion());
    }

    @Override
    public String toString() {
        return "IPCInterfaceDefinition{" +
                "interfaceFormalName=" + interfaceFormalName +
                ", interfaceFormalVersion=" + interfaceFormalVersion +
                '}';
    }
}
