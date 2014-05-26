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
import com.googlecode.mp4parser.util.Matrix;

import java.nio.ByteBuffer;
import java.util.Date;

/**
 * <h1>4cc = "{@value #TYPE}"</h1>
 * <code>
 * Box Type: 'mvhd'<br>
 * Container: {@link com.coremedia.iso.boxes.MovieBox} ('moov')<br>
 * Mandatory: Yes<br>
 * Quantity: Exactly one<br><br>
 * </code>
 * This box defines overall information which is media-independent, and relevant to the entire presentation
 * considered as a whole.
 */
public class MovieHeaderBox extends AbstractFullBox {
    private Date creationTime;
    private Date modificationTime;
    private long timescale;
    private long duration;
    private double rate = 1.0;
    private float volume = 1.0f;
    private Matrix matrix = Matrix.ROTATE_0;
    private long nextTrackId;

    private int previewTime;
    private int previewDuration;
    private int posterTime;
    private int selectionTime;
    private int selectionDuration;
    private int currentTime;


    public static final String TYPE = "mvhd";

    public MovieHeaderBox() {
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

    public double getRate() {
        if (!isParsed()) {
            parseDetails();
        }
        return rate;
    }

    public float getVolume() {
        if (!isParsed()) {
            parseDetails();
        }
        return volume;
    }

    public Matrix getMatrix() {
        if (!isParsed()) {
            parseDetails();
        }
        return matrix;
    }

    public long getNextTrackId() {
        if (!isParsed()) {
            parseDetails();
        }
        return nextTrackId;
    }

    protected long getContentSize() {
        long contentSize = 4;
        if (getVersion() == 1) {
            contentSize += 28;
        } else {
            contentSize += 16;
        }
        contentSize += 80;
        return contentSize;
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
        rate = IsoTypeReader.readFixedPoint1616(content);
        volume = IsoTypeReader.readFixedPoint88(content);
        IsoTypeReader.readUInt16(content);
        IsoTypeReader.readUInt32(content);
        IsoTypeReader.readUInt32(content);

        matrix = Matrix.fromByteBuffer(content);

        previewTime = content.getInt();
        previewDuration = content.getInt();
        posterTime = content.getInt();
        selectionTime = content.getInt();
        selectionDuration = content.getInt();
        currentTime = content.getInt();

        nextTrackId = IsoTypeReader.readUInt32(content);

    }

    public String toString() {
        if (!isParsed()) {
            parseDetails();
        }
        StringBuilder result = new StringBuilder();
        result.append("MovieHeaderBox[");
        result.append("creationTime=").append(getCreationTime());
        result.append(";");
        result.append("modificationTime=").append(getModificationTime());
        result.append(";");
        result.append("timescale=").append(getTimescale());
        result.append(";");
        result.append("duration=").append(getDuration());
        result.append(";");
        result.append("rate=").append(getRate());
        result.append(";");
        result.append("volume=").append(getVolume());
        result.append(";");
        result.append("matrix=").append(matrix);
        result.append(";");
        result.append("nextTrackId=").append(getNextTrackId());
        result.append("]");
        return result.toString();
    }


    @Override
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
        IsoTypeWriter.writeFixedPoint1616(byteBuffer, rate);
        IsoTypeWriter.writeFixedPoint88(byteBuffer, volume);
        IsoTypeWriter.writeUInt16(byteBuffer, 0);
        IsoTypeWriter.writeUInt32(byteBuffer, 0);
        IsoTypeWriter.writeUInt32(byteBuffer, 0);

        matrix.getContent(byteBuffer);

        byteBuffer.putInt(previewTime);
        byteBuffer.putInt(previewDuration);
        byteBuffer.putInt(posterTime);
        byteBuffer.putInt(selectionTime);
        byteBuffer.putInt(selectionDuration);
        byteBuffer.putInt(currentTime);

        IsoTypeWriter.writeUInt32(byteBuffer, nextTrackId);
    }


    public void setCreationTime(Date creationTime) {
        if (!isParsed()) {
            parseDetails();
        }
        this.creationTime = creationTime;
        if (DateHelper.convert(creationTime) >= (1l << 32)) {
            setVersion(1);
        }

    }

    public void setModificationTime(Date modificationTime) {
        if (!isParsed()) {
            parseDetails();
        }
        this.modificationTime = modificationTime;
        if (DateHelper.convert(modificationTime) >= (1l << 32)) {
            setVersion(1);
        }

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
        if (duration >= (1l << 32)) {
            setVersion(1);
        }
    }

    public void setRate(double rate) {
        if (!isParsed()) {
            parseDetails();
        }
        this.rate = rate;
    }

    public void setVolume(float volume) {
        if (!isParsed()) {
            parseDetails();
        }
        this.volume = volume;
    }

    public void setMatrix(Matrix matrix) {
        if (!isParsed()) {
            parseDetails();
        }
        this.matrix = matrix;
    }

    public void setNextTrackId(long nextTrackId) {
        if (!isParsed()) {
            parseDetails();
        }
        this.nextTrackId = nextTrackId;
    }

    public int getPreviewTime() {
        if (!isParsed()) {
            parseDetails();
        }
        return previewTime;
    }

    public void setPreviewTime(int previewTime) {
        if (!isParsed()) {
            parseDetails();
        }
        this.previewTime = previewTime;
    }

    public int getPreviewDuration() {
        if (!isParsed()) {
            parseDetails();
        }
        return previewDuration;
    }

    public void setPreviewDuration(int previewDuration) {
        if (!isParsed()) {
            parseDetails();
        }
        this.previewDuration = previewDuration;
    }

    public int getPosterTime() {
        if (!isParsed()) {
            parseDetails();
        }
        return posterTime;
    }

    public void setPosterTime(int posterTime) {
        if (!isParsed()) {
            parseDetails();
        }
        this.posterTime = posterTime;
    }

    public int getSelectionTime() {
        if (!isParsed()) {
            parseDetails();
        }
        return selectionTime;
    }

    public void setSelectionTime(int selectionTime) {
        if (!isParsed()) {
            parseDetails();
        }
        this.selectionTime = selectionTime;
    }

    public int getSelectionDuration() {
        if (!isParsed()) {
            parseDetails();
        }
        return selectionDuration;
    }

    public void setSelectionDuration(int selectionDuration) {
        if (!isParsed()) {
            parseDetails();
        }
        this.selectionDuration = selectionDuration;
    }

    public int getCurrentTime() {
        if (!isParsed()) {
            parseDetails();
        }
        return currentTime;
    }

    public void setCurrentTime(int currentTime) {
        if (!isParsed()) {
            parseDetails();
        }
        this.currentTime = currentTime;
    }
}
