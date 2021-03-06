/*  
 * Copyright 2008 CoreMedia AG, Hamburg
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

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.util.DateHelper;

import java.nio.ByteBuffer;
import java.util.Date;

/**
 * <h1>4cc = "{@value #TYPE}"</h1>
 * This box defines overall information which is media-independent, and relevant to the entire presentation
 * considered as a whole.
 */
public class MediaHeaderBox extends AbstractFullBox {
    public static final String TYPE = "mdhd";


    private Date creationTime = new Date();
    private Date modificationTime = new Date();
    private long timescale;
    private long duration;
    private String language = "eng";

    public MediaHeaderBox() {
        super(TYPE);
    }

    public Date getCreationTime() {
        if (!isParsed()) {
            parseDetails();
        }
        return creationTime;
    }

    public Date getModificationTime() {
        if (!isParsed()) {
            parseDetails();
        }
        return modificationTime;
    }

    public long getTimescale() {
        if (!isParsed()) {
            parseDetails();
        }
        return timescale;
    }

    public long getDuration() {
        if (!isParsed()) {
            parseDetails();
        }
        return duration;
    }

    public String getLanguage() {
        if (!isParsed()) {
            parseDetails();
        }
        return language;
    }

    protected long getContentSize() {
        long contentSize = 4;
        if (getVersion() == 1) {
            contentSize += 8 + 8 + 4 + 8;
        } else {
            contentSize += 4 + 4 + 4 + 4;
        }
        contentSize += 2;
        contentSize += 2;
        return contentSize;

    }

    public void setCreationTime(Date creationTime) {
        if (!isParsed()) {
            parseDetails();
        }
        this.creationTime = creationTime;
    }

    public void setModificationTime(Date modificationTime) {
        if (!isParsed()) {
            parseDetails();
        }
        this.modificationTime = modificationTime;
    }

    public void setTimescale(long timescale) {
        if (!isParsed()) {
            parseDetails();
        }
        this.timescale = timescale;
    }

    public void setDuration(long duration) {
        if (!isParsed()) {
            parseDetails();
        }
        this.duration = duration;
    }

    public void setLanguage(String language) {
        if (!isParsed()) {
            parseDetails();
        }
        this.language = language;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        if (getVersion() == 1) {
            creationTime = DateHelper.convert(IsoTypeReader.readUInt64(content));
            modificationTime = DateHelper.convert(IsoTypeReader.readUInt64(content));
            timescale = IsoTypeReader.readUInt32(content);
            duration = IsoTypeReader.readUInt64(content);
        } else {
            creationTime = DateHelper.convert(IsoTypeReader.readUInt32(content));
            modificationTime = DateHelper.convert(IsoTypeReader.readUInt32(content));
            timescale = IsoTypeReader.readUInt32(content);
            duration = IsoTypeReader.readUInt32(content);
        }
        language = IsoTypeReader.readIso639(content);
        IsoTypeReader.readUInt16(content);
    }


    public String toString() {
        if (!isParsed()) {
            parseDetails();
        }
        StringBuilder result = new StringBuilder();
        result.append("MediaHeaderBox[");
        result.append("creationTime=").append(getCreationTime());
        result.append(";");
        result.append("modificationTime=").append(getModificationTime());
        result.append(";");
        result.append("timescale=").append(getTimescale());
        result.append(";");
        result.append("duration=").append(getDuration());
        result.append(";");
        result.append("language=").append(getLanguage());
        result.append("]");
        return result.toString();
    }

    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        if (getVersion() == 1) {
            IsoTypeWriter.writeUInt64(byteBuffer, DateHelper.convert(creationTime));
            IsoTypeWriter.writeUInt64(byteBuffer, DateHelper.convert(modificationTime));
            IsoTypeWriter.writeUInt32(byteBuffer, timescale);
            IsoTypeWriter.writeUInt64(byteBuffer, duration);
        } else {
            IsoTypeWriter.writeUInt32(byteBuffer, DateHelper.convert(creationTime));
            IsoTypeWriter.writeUInt32(byteBuffer, DateHelper.convert(modificationTime));
            IsoTypeWriter.writeUInt32(byteBuffer, timescale);
            IsoTypeWriter.writeUInt32(byteBuffer, duration);
        }
        IsoTypeWriter.writeIso639(byteBuffer, language);
        IsoTypeWriter.writeUInt16(byteBuffer, 0);
    }
}
