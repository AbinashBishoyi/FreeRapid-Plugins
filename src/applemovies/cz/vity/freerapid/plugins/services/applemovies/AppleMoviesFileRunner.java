package cz.vity.freerapid.plugins.services.applemovies;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class AppleMoviesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AppleMoviesFileRunner.class.getName());
    private final static String USER_AGENT = "QuickTime/7.6.6 (qtver=7.6.6;os=Windows NT 5.1Service Pack 3)";

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        if (fileURL.endsWith(".mov")) {
            final int lastIndex = fileURL.lastIndexOf('/') + 1;
            final String firstPartOfURL = fileURL.substring(0, lastIndex);
            final String lastPartOfURL = fileURL.substring(lastIndex);

            httpFile.setFileName(lastPartOfURL.replace("_h", "_"));

            String downloadURL = fileURL;
            if (!lastPartOfURL.contains("_h")) {
                downloadURL = firstPartOfURL + lastPartOfURL.replace("_", "_h");
            }

            //cannot use getGetMethod(), custom user agent is necessary
            final GetMethod method = new GetMethod(downloadURL);
            method.setRequestHeader("User-Agent", USER_AGENT);

            logger.info("Downloading from " + downloadURL);
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            if (!makeRedirectedRequest(getGetMethod(fileURL))) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            try {
                PlugUtils.checkName(httpFile, getContentAsString(), "<title>", "- Movie Trailers");
            } catch (PluginImplementationException e) {
                LogUtils.processException(logger, e);
            }
            if (!makeRedirectedRequest(getMethodBuilder().setReferer(fileURL).setAction(fileURL + "includes/playlists/web.inc").toGetMethod())) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final List<URI> uriList = new ArrayList<URI>();
            final Matcher videos = getMatcherAgainstContent("(?s-m)<li class=\"trailer[^<>]*?>(.+?)(?:<!--/trailer-->|$)");
            while (videos.find()) {
                final List<String> list = new ArrayList<String>();
                final Matcher qualities = PlugUtils.matcher("href=\"(http://trailers\\.apple\\.com/.+?\\d+?p\\.mov)\"", videos.group(1));
                while (qualities.find()) {
                    list.add(qualities.group(1));
                }
                if (list.isEmpty()) throw new PluginImplementationException("Video qualities not found");
                //sort the list to determine best quality available
                Collections.sort(list, new Comparator<String>() {
                    @Override
                    public int compare(String one, String two) {
                        Matcher m1 = PlugUtils.matcher("(\\d+?)p\\.mov", one);
                        m1.find();
                        Matcher m2 = PlugUtils.matcher("(\\d+?)p\\.mov", two);
                        m2.find();
                        return Integer.valueOf(m2.group(1)).compareTo(Integer.valueOf(m1.group(1)));
                    }
                });
                try {
                    uriList.add(new URI(list.get(0)));
                } catch (URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
            if (uriList.isEmpty()) throw new PluginImplementationException("Videos not found");
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
            httpFile.getProperties().put("removeCompleted", true);
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("the page you’re looking for can’t be found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}