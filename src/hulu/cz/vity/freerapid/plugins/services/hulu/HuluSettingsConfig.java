package cz.vity.freerapid.plugins.services.hulu;

/**
 * @author tong2shot
 */
public class HuluSettingsConfig {

    private String username;
    private String password;
    private int qualityHeightIndex;
    private int videoFormatIndex;
    private int cdnIndex;
    private int portIndex;
    private boolean downloadSubtitles;

    public static final int MIN_HEIGHT = -2;
    public static final int MAX_HEIGHT = -1;
    public static final int MIN_HEIGHT_INDEX = 0;
    public static final int MAX_HEIGHT_INDEX = 10;
    public static final int ANY_VIDEO_FORMAT = 0;

    private final int[] qualityHeightMap = {MIN_HEIGHT, 240, 360, 480, MAX_HEIGHT}; //map of qualityHeight
    private final static int[] qualityHeightIndexMap = {MIN_HEIGHT_INDEX, 1, 2, 3, MAX_HEIGHT_INDEX}; //map of qualityHeightIndex, to anticipate higher quality (576,720,1080,2160,4320,etc) in the future
    private final String[] videoFormatMap = {"Any", "h264", "vp6"};
    private final String[] cdnMap = {"akamai", "limelight", "level3"};
    private final int[] portMap = {1935, 80};

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getQualityHeightIndex() {
        return qualityHeightIndex;
    }

    public void setQualityHeightIndex(int qualityHeightIndex) {
        this.qualityHeightIndex = qualityHeightIndex;
    }

    public int getVideoFormatIndex() {
        return videoFormatIndex;
    }

    public void setVideoFormatIndex(int videoFormatIndex) {
        this.videoFormatIndex = videoFormatIndex;
    }

    public int getPortIndex() {
        return portIndex;
    }

    public void setPortIndex(int portIndex) {
        this.portIndex = portIndex;
    }

    public int getCdnIndex() {
        return cdnIndex;
    }

    public void setCdnIndex(int cdnIndex) {
        this.cdnIndex = cdnIndex;
    }

    public int getQualityHeight() {
        for (int i = 0; i < qualityHeightIndexMap.length; i++) {
            if (qualityHeightIndexMap[i] == qualityHeightIndex)
                return qualityHeightMap[i];
        }
        return MAX_HEIGHT; //default, the highest
    }

    public String getVideoFormat() {
        return videoFormatMap[videoFormatIndex];
    }

    public String getCdn() {
        return cdnMap[cdnIndex];
    }

    public int getPort() {
        return portMap[portIndex];
    }

    public boolean isDownloadSubtitles() {
        return downloadSubtitles;
    }

    public void setDownloadSubtitles(boolean downloadSubtitles) {
        this.downloadSubtitles = downloadSubtitles;
    }

}
