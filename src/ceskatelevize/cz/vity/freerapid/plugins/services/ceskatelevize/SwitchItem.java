package cz.vity.freerapid.plugins.services.ceskatelevize;

import java.util.ArrayList;
import java.util.List;

/**
 * @author JPEXS
 * @author tong2shot
 */
public class SwitchItem implements Comparable<SwitchItem> {
    private final String base;
    private final double duration;
    private final List<Video> videos = new ArrayList<Video>();

    public SwitchItem(String base, double duration) {
        this.base = base;
        this.duration = duration;
    }

    public String getBase() {
        return base;
    }

    public double getDuration() {
        return duration;
    }

    public List<Video> getVideos() {
        return videos;
    }

    public void addVideo(Video video) {
        videos.add(video);
    }

    @Override
    public String toString() {
        return "SwitchItem{" +
                "base='" + base + '\'' +
                ", duration=" + duration +
                ", videos=" + videos +
                '}';
    }

    @Override
    public int compareTo(SwitchItem that) {
        return Double.compare(this.duration, that.duration);
    }

}
