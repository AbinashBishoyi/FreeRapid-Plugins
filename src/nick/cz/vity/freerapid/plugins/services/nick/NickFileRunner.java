package cz.vity.freerapid.plugins.services.nick;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class NickFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(NickFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<span content=\"([^<>]+?)\" property=\"media:title\"");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(matcher.group(1).replace(": ", " - ") + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            final String mgid = getMgid();
            logger.info("mgid = " + mgid);
            method = getGetMethod("http://udat.mtvnservices.com/service1/dispatch.htm?feed=nick_arc_player_prime&mgid=" + mgid);
            //there seems to be no geocheck
            //method.setRequestHeader("X-Forwarded-For", "129.228.25.181");
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final List<URI> videoItems = getVideoItems();
            if (videoItems.size() > 1) {
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, videoItems);
                httpFile.getProperties().put("removeCompleted", true);
            } else {
                method = getGetMethod("http://media-utils.mtvnservices.com/services/MediaGenerator/" + mgid + "?arcStage=live&acceptMethods=fms,hdn1,hds");
                //method.setRequestHeader("X-Forwarded-For", "129.228.25.181");
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                final String url = getStreamUrl();
                final RtmpSession rtmpSession = new RtmpSession(url);
                final String playName = rtmpSession.getPlayName();
                if (playName.endsWith(".mp4") && !playName.startsWith("mp4:")) {
                    rtmpSession.setPlayName("mp4:" + playName);
                }
                rtmpSession.getConnectParams().put("pageUrl", fileURL);
                tryDownloadAndSaveFile(rtmpSession);
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getMgid() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("mgid:arc:[^:]+:([^:]+):([a-z\\d\\-]+)");
        if (!matcher.find()) {
            throw new PluginImplementationException("Video ID not found");
        }
        return "mgid:arc:video:" + matcher.group(1) + ":" + matcher.group(2);
    }

    private List<URI> getVideoItems() throws ErrorDuringDownloadingException {
        try {
            final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new ByteArrayInputStream(getContentAsString().getBytes("UTF-8"))
            );
            final XPath xpath = XPathFactory.newInstance().newXPath();
            final NodeList nodeList = (NodeList) xpath.evaluate("/rss/channel/item/link", document, XPathConstants.NODESET);
            final List<URI> list = new LinkedList<URI>();
            for (int i = 0; i < nodeList.getLength(); i++) {
                final String url = nodeList.item(i).getTextContent();
                if (!url.contains("embed-bumper-generic") && !url.contains("endplate-embedded-player")) {
                    try {
                        list.add(new URI(url));
                    } catch (final URISyntaxException e) {
                        LogUtils.processException(logger, e);
                    }
                }
            }
            if (list.isEmpty()) {
                throw new PluginImplementationException("No video items found");
            }
            return list;
        } catch (final ErrorDuringDownloadingException e) {
            throw e;
        } catch (final Exception e) {
            throw new PluginImplementationException(e);
        }
    }

    private String getStreamUrl() throws ErrorDuringDownloadingException {
        try {
            final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new ByteArrayInputStream(getContentAsString().getBytes("UTF-8"))
            );
            final XPath xpath = XPathFactory.newInstance().newXPath();
            final NodeList nodeList = (NodeList) xpath.evaluate("/package/video/item/rendition[@bitrate!='']", document, XPathConstants.NODESET);
            final List<Stream> list = new LinkedList<Stream>();
            for (int i = 0; i < nodeList.getLength(); i++) {
                final Node node = nodeList.item(i);
                final String src = xpath.evaluate("./src", node);
                final int bitrate = Integer.parseInt(node.getAttributes().getNamedItem("bitrate").getTextContent());
                list.add(new Stream(src, bitrate));
            }
            if (list.isEmpty()) {
                throw new PluginImplementationException("No streams found");
            }
            return Collections.max(list).getUrl();
        } catch (final ErrorDuringDownloadingException e) {
            throw e;
        } catch (final Exception e) {
            throw new PluginImplementationException(e);
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().isEmpty() || getContentAsString().contains("The page you're looking for")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private static class Stream implements Comparable<Stream> {
        private final String url;
        private final int bitrate;

        public Stream(final String url, final int bitrate) {
            this.url = url;
            this.bitrate = bitrate;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public int compareTo(final Stream that) {
            return Integer.valueOf(this.bitrate).compareTo(that.bitrate);
        }
    }

}