package cz.vity.freerapid.plugins.services.bbc;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.rtmp.SwfVerificationHelper;
import cz.vity.freerapid.plugins.services.tunlr.Tunlr;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class BbcFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(BbcFileRunner.class.getName());

    private final static String SWF_URL = "http://www.bbc.co.uk/emp/releases/iplayer/revisions/617463_618125_4/617463_618125_4_emp.swf";
    private final static SwfVerificationHelper helper = new SwfVerificationHelper(SWF_URL);

    private void checkUrl() {
        fileURL = fileURL.replace("/programmes/", "/iplayer/episode/");
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkUrl();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        String name;
        final Matcher matcher = getMatcherAgainstContent("<div class=\"module\" id=\"programme-info\">\\s*?<h2>(.+?)<span class=\"blq-hide\"> - </span><span>(.*?)</span></h2>");
        if (matcher.find()) {
            final String series = matcher.group(1).replace(": ", " - ");
            final String episode = matcher.group(2).replace(": ", " - ");
            name = series + (episode.isEmpty() ? "" : " - " + episode);
        } else {
            try {
                name = PlugUtils.getStringBetween(getContentAsString(), "emp.setEpisodeTitle(\"", "\"").replace("\\/", ".").replace(": ", " - ");
            } catch (PluginImplementationException e1) {
                try {
                    name = PlugUtils.getStringBetween(getContentAsString(), "<meta name=\"title\" content=\"", "\" />");
                } catch (PluginImplementationException e2) {
                    throw new PluginImplementationException("File name not found");
                }
            }
        }
        httpFile.setFileName(name + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkUrl();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            //sometimes they redirect, set fileURL to the new page
            fileURL = method.getURI().toString();
            final String pid;
            Matcher matcher = PlugUtils.matcher("/programmes/([a-z\\d]+)", fileURL);
            if (matcher.find()) {
                method = getGetMethod("http://www.bbc.co.uk/iplayer/playlist/" + matcher.group(1));
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException();
                }
                matcher = getMatcherAgainstContent("<item[^<>]*?identifier=\"([^<>]+?)\"");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Identifier not found");
                }
                pid = matcher.group(1);
            } else {
                matcher = getMatcherAgainstContent("emp\\.setPid\\(\".+?\", \"(.+?)\"\\);");
                if (!matcher.find()) {
                    throw new PluginImplementationException("PID not found");
                }
                pid = matcher.group(1);
            }
            method = getGetMethod("http://www.bbc.co.uk/mediaselector/4/mtis/stream/" + pid + "?cb=" + new Random().nextInt(100000));
            if (!client.getSettings().isProxySet()) {
                Tunlr.setupMethod(method);
            }
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            matcher = getMatcherAgainstContent("<error id=\"(.+?)\"");
            if (matcher.find()) {
                final String id = matcher.group(1);
                if (id.equals("notavailable")) {
                    throw new URLNotAvailableAnymoreException("Playlist not found");
                } else if (id.equals("notukerror")) {
                    throw new NotRecoverableDownloadException("This video is not available in your area");
                } else {
                    throw new NotRecoverableDownloadException("Error fetching playlist: '" + id + "'");
                }
            }
            final List<Stream> list = new ArrayList<Stream>();
            try {
                final NodeList nodeList = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                        new ByteArrayInputStream(getContentAsString().getBytes("UTF-8"))
                ).getElementsByTagName("media");
                for (int i = 0, n = nodeList.getLength(); i < n; i++) {
                    try {
                        final Element element = (Element) nodeList.item(i);
                        final Stream stream = Stream.get(element);
                        if (stream != null) {
                            list.add(stream);
                        }
                    } catch (Exception e) {
                        LogUtils.processException(logger, e);
                    }
                }
            } catch (Exception e) {
                throw new PluginImplementationException("Error parsing playlist XML", e);
            }
            if (list.isEmpty()) throw new PluginImplementationException("No suitable streams found");
            final RtmpSession rtmpSession = Collections.min(list).getRtmpSession();
            rtmpSession.getConnectParams().put("pageUrl", fileURL);
            rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
            helper.setSwfVerification(rtmpSession, client);
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("this programme is not available")) {
            throw new URLNotAvailableAnymoreException("This programme is not available anymore");
        }
        if (getContentAsString().contains("Page not found") || getContentAsString().contains("page was not found")) {
            throw new URLNotAvailableAnymoreException("Page not found");
        }
    }

    private static class Stream implements Comparable<Stream> {
        private final String server;
        private final String app;
        private final String play;
        private final boolean encrypted;
        private final int bitrate;

        public static Stream get(final Element media) {
            final Element connection = (Element) media.getElementsByTagName("connection").item(0);
            String protocol = connection.getAttribute("protocol");
            if (protocol == null || protocol.isEmpty()) {
                protocol = connection.getAttribute("href");
            }
            if (protocol == null || protocol.isEmpty() || !protocol.startsWith("rtmp")) {
                logger.info("Not supported: " + media.getAttribute("service"));
                return null;//of what they serve, only RTMP streams are supported at the moment
            }
            final String server = connection.getAttribute("server");
            String app = connection.getAttribute("application");
            app = (app == null || app.isEmpty() ? "ondemand" : app) + "?" + PlugUtils.replaceEntities(connection.getAttribute("authString"));
            final String play = connection.getAttribute("identifier");
            final boolean encrypted = protocol.startsWith("rtmpe") || protocol.startsWith("rtmpte");
            final int bitrate = Integer.parseInt(media.getAttribute("bitrate"));
            return new Stream(server, app, play, encrypted, bitrate);
        }

        private Stream(String server, String app, String play, boolean encrypted, int bitrate) {
            this.server = server;
            this.app = app;
            this.play = play;
            this.encrypted = encrypted;
            this.bitrate = bitrate;
            logger.info("server = " + server);
            logger.info("app = " + app);
            logger.info("play = " + play);
            logger.info("encrypted = " + encrypted);
            logger.info("bitrate = " + bitrate);
        }

        public RtmpSession getRtmpSession() {
            return new RtmpSession(server, 1935, app, play, encrypted);
        }

        @Override
        public int compareTo(final Stream that) {
            return Integer.valueOf(that.bitrate).compareTo(this.bitrate);
        }
    }

}