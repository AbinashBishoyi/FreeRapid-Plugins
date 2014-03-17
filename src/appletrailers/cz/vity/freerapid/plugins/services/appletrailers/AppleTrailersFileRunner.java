package cz.vity.freerapid.plugins.services.appletrailers;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 * @author tong2shot
 */
class AppleTrailersFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AppleTrailersFileRunner.class.getName());
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
            fileURL = fileURL.replaceFirst("#.+", "");
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
            if (getContentAsString().contains("<h4>Download</h4>")) {
                final Matcher videos = getMatcherAgainstContent("(?s-m)<li class=[\"']trailer[^<>]*?>(.+?)(?:<!--/trailer-->|$)");
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
            } else {
                final Matcher grid2colMatcher = Pattern.compile("<div class='grid2col'>(.+?)</div><!--/grid2col-->", Pattern.DOTALL).matcher(getContentAsString());
                final Matcher videoQualityPageMatcher = Pattern.compile("<a href=\"([^<>\"]+)\"[^<>]*?>.*?(\\d+?)p<span>").matcher(getContentAsString());
                final Matcher trailerNameMatcher = Pattern.compile("<h3>(.+?)</h3>").matcher(getContentAsString());
                //there is only one page for each quality of all trailers, so we need to cache them, that way we don't have to rerequest them
                //http://trailers.apple.com/trailers/wb/pacificrim/includes/extralarge.html#videos-extralarge   -> 720p, multiple trailers
                //http://trailers.apple.com/trailers/wb/pacificrim/includes/large.html#videos-large             -> 480p, multiple trailers
                final HashMap<String, String> videoQualityPageCache = new HashMap<String, String>(); //map to cache video quality pages, key=page url, value=page content
                while (grid2colMatcher.find()) {
                    trailerNameMatcher.region(grid2colMatcher.start(1), grid2colMatcher.end(1));
                    videoQualityPageMatcher.region(grid2colMatcher.start(1), grid2colMatcher.end(1));

                    if (!trailerNameMatcher.find()) {
                        throw new PluginImplementationException("Trailer name not found");
                    }
                    String trailerName = trailerNameMatcher.group(1);

                    //select page with the highest video quality for the trailer
                    TreeMap<Integer, String> videoQualityPageMap = new TreeMap<Integer, String>(); //map to store all available qualities for the trailer, sorted ascending, k=vid quality, v=page url
                    while (videoQualityPageMatcher.find()) {
                        videoQualityPageMap.put(Integer.parseInt(videoQualityPageMatcher.group(2)), videoQualityPageMatcher.group(1));
                    }
                    if (videoQualityPageMap.isEmpty()) {
                        throw new PluginImplementationException("Video quality page not found");
                    }
                    //http://trailers.apple.com/trailers/wb/pacificrim/includes/extralarge.html#videos-extralarge
                    //multiple trailers in the highest quality page
                    String highestQualityPageUrl = videoQualityPageMap.get(videoQualityPageMap.lastKey());

                    HttpMethod httpMethod;
                    String highestQualityPageContent;
                    if (!videoQualityPageCache.containsKey(highestQualityPageUrl)) { //not in cache
                        httpMethod = getMethodBuilder()
                                .setReferer(fileURL)
                                .setBaseURL(fileURL)
                                .setAction(highestQualityPageUrl)
                                .toGetMethod();
                        if (!makeRedirectedRequest(httpMethod)) {
                            checkProblems();
                            throw new ServiceConnectionProblemException();
                        }
                        checkProblems();
                        videoQualityPageCache.put(highestQualityPageUrl, getContentAsString());
                        highestQualityPageContent = getContentAsString();
                    } else { //already in cache
                        highestQualityPageContent = videoQualityPageCache.get(highestQualityPageUrl);
                    }

                    //search trailer video page based on trailer name
                    Matcher matcher = Pattern.compile("<a href=\"(.+?)\" class=\"block nested-trigger\">.*?<h4>(.+?)</h4>.*?</a>", Pattern.DOTALL).matcher(highestQualityPageContent);
                    int start = 0;
                    String action = null;
                    while (matcher.find(start)) {
                        if (matcher.group(2).equals(trailerName)) {
                            action = matcher.group(1);
                            break;
                        }
                        start = matcher.end();
                    }
                    if (action == null) {
                        throw new PluginImplementationException("Trailer video page not found");
                    }
                    //http://trailers.apple.com/trailers/wb/pacificrim/includes/featurette2/extralarge.html#overlay-teaser1-extralarge
                    httpMethod = getMethodBuilder(highestQualityPageContent)
                            .setReferer(fileURL)
                            .setBaseURL(fileURL)
                            .setAction(action)
                            .toGetMethod();
                    if (!makeRedirectedRequest(httpMethod)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    checkProblems();
                    try {
                        uriList.add(new URI(PlugUtils.replaceEntities(PlugUtils.getStringBetween(getContentAsString(), "\"movieLink\" href=\"", "\"")).replaceFirst("[\\?#].+", "")));
                    } catch (URISyntaxException e) {
                        LogUtils.processException(logger, e);
                    }
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