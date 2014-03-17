package cz.vity.freerapid.plugins.services.h2porn;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.digest.DigestUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;

/**
 * @author birchie
 */
public class TubeDownloadBuilder {
    final String MagicKey = "9a09f60b56a8fc3e1fb8f96f3d0bfaf6";
    private String fileURL;
    private String AHV;

    public TubeDownloadBuilder(String fileURL) {
        this.fileURL = fileURL;
    }

    public MethodBuilder doDownloadParams(final String linkUrl, final MethodBuilder builder) throws PluginImplementationException {
        final String sTime = getTime();
        final String baseUrl = getBaseUrl(linkUrl);
        this.AHV = DigestUtils.md5Hex(MagicKey + baseUrl + MagicKey);
        builder.setParameter("time", sTime);
        builder.setParameter("ahv", getAHV(linkUrl, sTime));
        builder.setParameter("cv", getCV(sTime));
        builder.setParameter("ref", fileURL);
        return builder;
    }

    private String getTime() {
        final Date dt = new Date();
        final SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        return df.format(dt);
    }

    private String getAHV(String videoUrl, final String time) {
        if (videoUrl.contains("?"))
            videoUrl = videoUrl.substring(0, videoUrl.indexOf("?"));
        return DigestUtils.md5Hex(videoUrl + time + AHV);
    }

    private String getCV(final String time) {
        return DigestUtils.md5Hex(time + AHV);
    }

    private String getBaseUrl(final String fileURL) throws PluginImplementationException {
        final Matcher match = PlugUtils.matcher("http://(www\\.)?(.+?)/", fileURL);
        if (!match.find())
            throw new PluginImplementationException("Error extracting url");
        return match.group(2);
    }

}
