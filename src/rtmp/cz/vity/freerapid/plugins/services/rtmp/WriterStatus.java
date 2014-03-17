package cz.vity.freerapid.plugins.services.rtmp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static cz.vity.freerapid.plugins.services.rtmp.Packet.Type.AUDIO_DATA;
import static cz.vity.freerapid.plugins.services.rtmp.Packet.Type.VIDEO_DATA;

/**
 * @author Peter Thomas
 * @author ntoskrnl
 */
class WriterStatus {

    private static final Logger logger = Logger.getLogger(WriterStatus.class.getName());

    private Map<Integer, Integer> channelTimeMap = new ConcurrentHashMap<Integer, Integer>();
    private int mediaChannel;
    private int seekTime;
    private long lastUpdateTime;
    private RtmpSession session;

    public WriterStatus(int seekTime, RtmpSession session) {
        this.seekTime = seekTime;
        this.session = session;
    }

    public void logFinalVideoDuration() {
        Integer time = channelTimeMap.get(mediaChannel);
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
        if (mediaChannel == 0 && (header.getPacketType() == VIDEO_DATA || header.getPacketType() == AUDIO_DATA)) {
            mediaChannel = channelId;
            logger.info("media channel id is: " + mediaChannel);
        }
        if (header.isRelative()) {
            channelTime = channelTime + header.getTime();
        } else {
            channelTime = seekTime + header.getTime();
        }
        channelTimeMap.put(channelId, channelTime);
        if (header.getPacketType() == VIDEO_DATA) {
            logProgress(channelTime);
        }
        return channelTime;
    }

    public void updateChannelTime(int time) {
        if (mediaChannel == 0) {
            throw new RuntimeException("media channel id not initialized!");
        }
        channelTimeMap.put(mediaChannel, time); // absolute
        logProgress(time);
    }

    public int getChannelTime() {
        Integer time = channelTimeMap.get(mediaChannel);
        return time == null ? 0 : time;
    }

    private void logProgress(int time) {
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