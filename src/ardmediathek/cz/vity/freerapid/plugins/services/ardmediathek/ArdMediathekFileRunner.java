package cz.vity.freerapid.plugins.services.ardmediathek;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class ArdMediathekFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(ArdMediathekFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
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
        //<title>ARD Mediathek: Der letzte Patriarch - 'Der letzte Patriarch - Teil 2' in voller Länge - Freitag, 10.09.2010 | Das Erste</title>
        final Matcher matcher = getMatcherAgainstContent("<title>ARD Mediathek: (.+?) \\- [^\\|\\-]+? \\| [^\\|\\-]+?</title>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(matcher.group(1) + ".flv");
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
            // first number: type; 0 = flash, 1 = silverlight, 2 = windows media (only flash is supported atm)
            // second number: quality; higher is better
            // first string: server
            // second string: stream
            final Matcher matcher = getMatcherAgainstContent("mediaCollection.addMediaStream\\(0, (\\d+), \"(.*?)\", \"(.*?)\"");
            final List<Stream> list = new ArrayList<Stream>();
            while (matcher.find()) {
                list.add(new Stream(matcher.group(2), matcher.group(3), matcher.group(1)));
            }
            if (list.isEmpty()) throw new PluginImplementationException("No streams found");
            Collections.sort(list);
            final String url = list.get(0).getUrl();
            if (url.startsWith("rtmp")) {
                final RtmpSession rtmpSession = new RtmpSession(url.replaceFirst("rtmp[ts]", "rtmp"));
                tryDownloadAndSaveFile(rtmpSession);
            } else if (url.startsWith("http")) {
                method = getGetMethod(url);
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            } else {
                throw new PluginImplementationException("Unknown URL: " + url);
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Leider konnte die gew&uuml;nschte Seite")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("Diese Sendung ist f\u00FCr Jugendliche")) {
            throw new NotRecoverableDownloadException("This video is unsuitable for minors and has limited viewing times");
        }
    }

    private static class Stream implements Comparable<Stream> {
        private final String url;
        private final int quality;

        public Stream(String server, String stream, String quality) throws Exception {
            this.url = server + PlugUtils.replaceEntities(stream);
            this.quality = Integer.parseInt(quality);
            logger.info("url = " + this.url);
            logger.info("quality = " + this.quality);
        }

        public String getUrl() {
            return url;
        }

        @Override
        public int compareTo(Stream that) {
            return Integer.valueOf(that.quality).compareTo(this.quality);
        }
    }

}