package cz.vity.freerapid.plugins.services.rtmp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static cz.vity.freerapid.plugins.services.rtmp.Packet.Type.VIDEO_DATA;

/**
 * @author Peter Thomas
 * @author ntoskrnl
 */
class WriterStatus {

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
        logger.info("final video duration: " + (time - seekTime) / 1000 + " seconds, start (seek) time: " + seekTime);
    }

    public int getChannelAbsoluteTime(Header header) {
        final int channelId = header.getChannelId();
        Integer channelTime = channelTimeMap.get(channelId);
        if (channelTime == null) { // first packet
            logger.fine("first packet!");
            channelTime = seekTime;
        }
        if (videoChannel == -1 && header.getPacketType() == VIDEO_DATA) {
            videoChannel = channelId;
            logger.info("video channel id is: " + videoChannel);
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
            //throw new RuntimeException("video channel id not initialized!");  //commented, so audio-only RTMP stream can get through
        }
        channelTimeMap.put(videoChannel, time); // absolute
        logVideoProgress(time);
    }

    public int getVideoChannelTime() {
        Integer time = channelTimeMap.get(videoChannel);
        return time == null ? 0 : time;
    }

    private void logVideoProgress(int time) {
        logger.fine("time: " + time + ", seek: " + seekTime);
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