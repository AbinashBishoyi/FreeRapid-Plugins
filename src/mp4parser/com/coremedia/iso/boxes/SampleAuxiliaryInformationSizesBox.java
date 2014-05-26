/*
 * Copyright 2009 castLabs GmbH, Berlin
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

package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import static com.googlecode.mp4parser.util.CastUtils.l2i;

/**
 * <h1>4cc = "{@value #TYPE}"</h1>
 */
public class SampleAuxiliaryInformationSizesBox extends AbstractFullBox {
    public static final String TYPE = "saiz";

    private int defaultSampleInfoSize;
    private List<Short> sampleInfoSizes = new LinkedList<Short>();
    private int sampleCount;
    private String auxInfoType;
    private String auxInfoTypeParameter;

    public SampleAuxiliaryInformationSizesBox() {
        super(TYPE);
    }

    @Override
    protected long getContentSize() {
        int size = 4;
        if ((getFlags() & 1) == 1) {
            size += 8;
        }

        size += 5;
        size += defaultSampleInfoSize == 0 ? sampleInfoSizes.size() : 0;
        return size;
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        if ((getFlags() & 1) == 1) {
            byteBuffer.put(IsoFile.fourCCtoBytes(auxInfoType));
            byteBuffer.put(IsoFile.fourCCtoBytes(auxInfoTypeParameter));
        }

        IsoTypeWriter.writeUInt8(byteBuffer, defaultSampleInfoSize);

        if (defaultSampleInfoSize == 0) {
            IsoTypeWriter.writeUInt32(byteBuffer, sampleInfoSizes.size());
            for (short sampleInfoSize : sampleInfoSizes) {
                IsoTypeWriter.writeUInt8(byteBuffer, sampleInfoSize);
            }
        } else {
            IsoTypeWriter.writeUInt32(byteBuffer, sampleCount);
        }
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        if ((getFlags() & 1) == 1) {
            auxInfoType = IsoTypeReader.read4cc(content);
            auxInfoTypeParameter = IsoTypeReader.read4cc(content);
        }

        defaultSampleInfoSize = (short) IsoTypeReader.readUInt8(content);
        sampleCount = l2i(IsoTypeReader.readUInt32(content));

        sampleInfoSizes.clear();

        if (defaultSampleInfoSize == 0) {
            for (int i = 0; i < sampleCount; i++) {
                sampleInfoSizes.add((short) IsoTypeReader.readUInt8(content));
            }
        }
    }

    public String getAuxInfoType() {
        if (!isParsed()) {
            parseDetails();
        }
        return auxInfoType;
    }

    public void setAuxInfoType(String auxInfoType) {
        if (!isParsed()) {
            parseDetails();
        }
        this.auxInfoType = auxInfoType;
    }

    public String getAuxInfoTypeParameter() {
        if (!isParsed()) {
            parseDetails();
        }
        return auxInfoTypeParameter;
    }

    public void setAuxInfoTypeParameter(String auxInfoTypeParameter) {
        if (!isParsed()) {
            parseDetails();
        }
        this.auxInfoTypeParameter = auxInfoTypeParameter;
    }

    public int getDefaultSampleInfoSize() {
        if (!isParsed()) {
            parseDetails();
        }
        return defaultSampleInfoSize;
    }

    public void setDefaultSampleInfoSize(int defaultSampleInfoSize) {
        if (!isParsed()) {
            parseDetails();
        }
        assert defaultSampleInfoSize <= 255;
        this.defaultSampleInfoSize = defaultSampleInfoSize;
    }

    public List<Short> getSampleInfoSizes() {
        if (!isParsed()) {
            parseDetails();
        }
        return sampleInfoSizes;
    }

    public void setSampleInfoSizes(List<Short> sampleInfoSizes) {
        if (!isParsed()) {
            parseDetails();
        }
        this.sampleInfoSizes = sampleInfoSizes;
    }

    public int getSampleCount() {
        if (!isParsed()) {
            parseDetails();
        }
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        if (!isParsed()) {
            parseDetails();
        }
        this.sampleCount = sampleCount;
    }

    @Override
    public String toString() {
        if (!isParsed()) {
            parseDetails();
        }
        return "SampleAuxiliaryInformationSizesBox{" +
                "defaultSampleInfoSize=" + defaultSampleInfoSize +
                ", sampleCount=" + sampleCount +
                ", auxInfoType='" + auxInfoType + '\'' +
                ", auxInfoTypeParameter='" + auxInfoTypeParameter + '\'' +
                '}';
    }
}
