/*
 * Copyright 2002-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.vity.freerapid.plugins.services.rtmp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static cz.vity.freerapid.plugins.services.rtmp.Packet.Type.VIDEO_DATA;

public class WriterStatus {

    private static final Logger logger = Logger.getLogger(WriterStatus.class.getName());

    private Map<Integer, Integer> channelTimeMap = new ConcurrentHashMap<Integer, Integer>();
    private int videoChannel = -1;
    private int seekTime;
    private long lastUpdateTime;
    private RtmpSession session;

    public WriterStatus(int seekTime, RtmpSession session) {
        this.seekTime = seekTime;
        this.session = session;
    }

    public void logFinalVideoDuration() {
        Integer time = channelTimeMap.get(videoChannel);
        if (time == null) {
            logger.warning("video duration is null");
            return;
        }
        logger.fine("final video duration: " + (time - seekTime) / 1000 + " seconds, start (seek) time: " + seekTime);
    }

    public int getChannelAbsoluteTime(Header header) {
        final int channelId = header.getChannelId();
        Integer channelTime = channelTimeMap.get(channelId);
        if (channelTime == null) { // first packet
            logger.finest("first packet!");
            channelTime = seekTime;
        }
        if (videoChannel == -1 && header.getPacketType() == VIDEO_DATA) {
            videoChannel = channelId;
            logger.fine("video channel id is: " + videoChannel);
        }
        if (header.isRelative()) {
            channelTime = channelTime + header.getTime();
        } else {
            channelTime = seekTime + header.getTime();
        }
        channelTimeMap.put(channelId, channelTime);
        if (header.getPacketType() == VIDEO_DATA) {
            logVideoProgress(channelTime);
        }
        return channelTime;
    }

    public void updateVideoChannelTime(int time) {
        if (videoChannel == -1) {
            throw new RuntimeException("video channel id not initialized!");
        }
        channelTimeMap.put(videoChannel, time); // absolute
        logVideoProgress(time);
    }

    private void logVideoProgress(int time) {
        if (RtmpSession.DEBUG) {
            logger.finest("time: " + time + ", seek: " + seekTime);
        }
        long currentTime = System.currentTimeMillis();
        if (session.getHttpFile() != null && currentTime >= lastUpdateTime + 1000) {
            lastUpdateTime = currentTime;
            int duration = session.getStreamDuration();
            if (duration > 0 && time > 0) {
                long size = (long) ((double) session.getHttpFile().getRealDownload() / ((double) time / (double) duration));
                if (size > 0) {
                    session.getHttpFile().setFileSize(size);
                }
            }
        }
    }

}