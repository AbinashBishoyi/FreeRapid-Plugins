package cz.vity.freerapid.plugins.services.channel5;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class Channel5FileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(Channel5FileRunner.class.getName());

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
        final String name;
        final Matcher matcher = getMatcherAgainstContent("<h3 class=\"episode_header\"><span class=\"sifr_grey_light\">(?:(?:Season|Series) (\\d+) \\- Episode (\\d+))?(?:: )?(.+?)?</span></h3>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found (1)");
        }
        final Matcher matcher2 = getMatcherAgainstContent("<h[12]><span class=\"sifr_white\">(.+?)</span></h[12]>");
        if (!matcher2.find()) {
            throw new PluginImplementationException("File name not found (2)");
        }
        final String program = matcher2.group(1);
        final String seasonNum = matcher.group(1);
        final String episodeNum = matcher.group(2);
        final String episode = matcher.group(3);
        final boolean episodeSet = episode != null;
        final boolean seasonAndEpisodeNumSet = seasonNum != null && episodeNum != null;
        if (episodeSet && seasonAndEpisodeNumSet) {
            final int seasonNumI = Integer.parseInt(seasonNum);
            final int episodeNumI = Integer.parseInt(episodeNum);
            name = String.format("%s - S%02dE%02d - %s", program, seasonNumI, episodeNumI, episode);
        } else if (episodeSet && !seasonAndEpisodeNumSet) {
            name = String.format("%s - %s", program, episode);
        } else if (seasonAndEpisodeNumSet) {
            final int seasonNumI = Integer.parseInt(seasonNum);
            final int episodeNumI = Integer.parseInt(episodeNum);
            name = String.format("%s - S%02dE%02d", program, seasonNumI, episodeNumI);
        } else {
            name = program;
        }
        httpFile.setFileName(name + ".flv");
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
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("this page does not exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}