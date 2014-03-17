package cz.vity.freerapid.plugins.services.itv;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.rtmp.SwfVerificationHelper;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import java.util.Locale;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class ItvFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(ItvFileRunner.class.getName());

    private final static String SWF_URL = "http://www.itv.com/mercury/Mercury_VideoPlayer.swf";
    private final static SwfVerificationHelper helper = new SwfVerificationHelper(SWF_URL);

    private String id;
    private String content;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getMethodBuilder()
                .setAction("http://mercury.itv.com/PlaylistService.svc")
                .setReferer(SWF_URL)
                .toPostMethod();
        method.setRequestHeader("SOAPAction", "\"http://tempuri.org/PlaylistService/GetPlaylist\"");//double quotes on purpose
        ((PostMethod) method).setRequestEntity(new StringRequestEntity(getPlaylistRequestContent(), "text/xml", "utf-8"));
        makeRedirectedRequest(method);
        checkProblems();
        //getContentAsString() is set to something else in checkName(),
        //but this content is required in run()
        content = getContentAsString();
        checkName();
    }

    private String getPlaylistRequestContent() throws ErrorDuringDownloadingException {
        return String.format(PLAYLIST_REQUEST_BASE, getRandomGuid(), getId());
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<faultcode>(.+?)</faultcode>\\s*?<faultstring[^<>]*?>(.+?)</faultstring>");
        if (matcher.find()) {
            final String id = matcher.group(1).trim();
            if (id.equals("s:InvalidVodcrid") || id.equals("s:ContentUnavailable")) {
                throw new URLNotAvailableAnymoreException("File not found");
            } else if (id.equals("s:InvalidGeoRegion")) {
                throw new NotRecoverableDownloadException("This video is not available in your area");
            } else {
                throw new NotRecoverableDownloadException("Error fetching playlist: '" + id + "', '" + matcher.group(2).trim() + "'");
            }
        }
        if (getContentAsString().contains("Page not found")) {
            throw new URLNotAvailableAnymoreException("Page not found");
        }
    }

    private void checkName() throws Exception {
        setFileStreamContentTypes(new String[]{}, new String[]{"application/javascript"});
        final HttpMethod method = getMethodBuilder()
                .setReferer(fileURL)
                .setAction("http://mercury.itv.com/api/html/dotcom/Episode/Index/" + getId() + "/?callback=jsCallBackEpisode")
                .toGetMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        PlugUtils.checkName(httpFile, getContentAsString(), "<h1>", "<\\/h1>");
        httpFile.setFileName(httpFile.getFileName() + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private String getId() throws ErrorDuringDownloadingException {
        if (id == null) {
            final Matcher matcher = PlugUtils.matcher("Filter=(\\d+)", fileURL);
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing file URL");
            }
            id = matcher.group(1);
        }
        return id;
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();
        Matcher matcher = PlugUtils.matcher("<Video timecode=[^<>]*?>(.+?)</Video>", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("'Video' tag not found in playlist");
        }
        final String video = matcher.group(1);
        matcher = PlugUtils.matcher("(?s)<MediaFiles base=\"(rtmp.+?)\"", video);
        if (!matcher.find()) {
            throw new PluginImplementationException("URL not found in playlist");
        }
        final String url = PlugUtils.replaceEntities(matcher.group(1));
        matcher = PlugUtils.matcher("(mp4:.+?\\.mp4)", video);
        if (!matcher.find()) {
            throw new PluginImplementationException("Play name not found in playlist");
        }
        String play;
        do {
            //get the last item, which is the highest quality
            play = matcher.group(1);
        } while (matcher.find());
        final RtmpSession rtmpSession = new RtmpSession(url, play);
        rtmpSession.getConnectParams().put("pageUrl", fileURL);
        rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
        helper.setSwfVerification(rtmpSession, client);
        tryDownloadAndSaveFile(rtmpSession);
    }

    private static String getRandomGuid() {
        //returns a string like this: 6D3D963A-B6C7-0A3E-D1E0-A0A1611A2B86
        final byte[] b = new byte[18];
        new Random().nextBytes(b);
        final char[] c = Hex.encodeHex(b);
        c[8] = '-';
        c[13] = '-';
        c[18] = '-';
        c[23] = '-';
        return new String(c).toUpperCase(Locale.ENGLISH);
    }

    private final static String PLAYLIST_REQUEST_BASE =
            "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n" +
                    "  <SOAP-ENV:Body>\r\n" +
                    "    <tem:GetPlaylist xmlns:tem=\"http://tempuri.org/\" xmlns:itv=\"http://schemas.datacontract.org/2004/07/Itv.BB.Mercury.Common.Types\" xmlns:com=\"http://schemas.itv.com/2009/05/Common\">\r\n" +
                    "      <tem:request>\r\n" +
                    "        <itv:RequestGuid>%s</itv:RequestGuid>\r\n" +
                    "        <itv:Vodcrid>\r\n" +
                    "          <com:Id>%s</com:Id>\r\n" +
                    "          <com:Partition>itv.com</com:Partition>\r\n" +
                    "        </itv:Vodcrid>\r\n" +
                    "      </tem:request>\r\n" +
                    "      <tem:userInfo>\r\n" +
                    "        <itv:GeoLocationToken>\r\n" +
                    "          <itv:Token/>\r\n" +
                    "        </itv:GeoLocationToken>\r\n" +
                    "        <itv:RevenueScienceValue>.</itv:RevenueScienceValue>\r\n" +
                    "      </tem:userInfo>\r\n" +
                    "      <tem:siteInfo>\r\n" +
                    "        <itv:AdvertisingRestriction>None</itv:AdvertisingRestriction>\r\n" +
                    "        <itv:AdvertisingSite>ITV</itv:AdvertisingSite>\r\n" +
                    "        <itv:Area>ITVPLAYER.VIDEO</itv:Area>\r\n" +
                    "        <itv:Platform>DotCom</itv:Platform>\r\n" +
                    "        <itv:Site>ItvCom</itv:Site>\r\n" +
                    "      </tem:siteInfo>\r\n" +
                    "    </tem:GetPlaylist>\r\n" +
                    "  </SOAP-ENV:Body>\r\n" +
                    "</SOAP-ENV:Envelope>";

}