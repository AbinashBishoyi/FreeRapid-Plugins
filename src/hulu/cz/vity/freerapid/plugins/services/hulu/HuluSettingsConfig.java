package cz.vity.freerapid.plugins.services.hulu;

/**
 * @author tong2shot
 */
public class HuluSettingsConfig {
    private String username;
    private String password;
    private int qualityHeightIndex;
    private int videoFormatIndex;

    public static final int MIN_HEIGHT = -2;
    public static final int MAX_HEIGHT = -1;
    public static final int ANY_VIDEO_FORMAT = 0;

    private final int[] qualityHeightMap = {MIN_HEIGHT, 240, 360, 480, MAX_HEIGHT}; //map of qualityHeight
    private final static int[] qualityHeightIndexMap = {0, 1, 2, 3, 10}; //map of qualityHeightIndex, to anticipate higher quality (576,720,1080,2160,4320,etc) in the future
    private final String[] videoFormatMap = {"Any", "h264", "vp6"};

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

    public int getQualityHeight() {
        for (int i = 0; i < qualityHeightIndexMap.length; i++) {
            if (qualityHeightIndexMap[i] == qualityHeightIndex)
                return qualityHeightMap[i];
        }
        return qualityHeightIndexMap[4]; //default, the highest
    }

    public String getVideoFormat() {
        return videoFormatMap[videoFormatIndex];
    }
}
