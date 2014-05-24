/*
 * Copyright 2011 castLabs, Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.mp4parser.boxes.mp4;

import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.ESDescriptor;

/**
 * <h1>4cc = "{@value #TYPE}"</h1>
 * ES Descriptor Box.
 */
public class ESDescriptorBox extends AbstractDescriptorBox {
    public static final String TYPE = "esds";


    public ESDescriptorBox() {
        super(TYPE);
    }

    public ESDescriptor getEsDescriptor() {
        return (ESDescriptor) super.getDescriptor();
    }

    public void setEsDescriptor(ESDescriptor esDescriptor) {
        super.setDescriptor(esDescriptor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ESDescriptorBox that = (ESDescriptorBox) o;

        if (data != null ? !data.equals(that.data) : that.data != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return data != null ? data.hashCode() : 0;
    }
}
