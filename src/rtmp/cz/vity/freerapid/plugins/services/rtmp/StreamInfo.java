package cz.vity.freerapid.plugins.services.rtmp;

/**
 * @author ntoskrnl
 */
public class StreamInfo {

    private int time = 0;
    private int duration = -1;

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

}
