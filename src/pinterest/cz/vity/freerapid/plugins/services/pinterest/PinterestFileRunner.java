package cz.vity.freerapid.plugins.services.pinterest;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author bircie
 */
class PinterestFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PinterestFileRunner.class.getName());

    private final static int PINSperPAGE = 25;

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        if (fileURL.contains("/pin/")) {
            final Matcher match = PlugUtils.matcher("<img\\s*?src=\".+?/([^/]+?)\"\\s*?class=\"pinImage\"", content);
            if (!match.find())
                throw new PluginImplementationException("File name not found");
            httpFile.setFileName(match.group(1).trim());
        } else {
            httpFile.setFileName("Board: " + PlugUtils.getStringBetween(content, "<h1 class=\"boardName\">", "</h1>"));
            final Matcher match = PlugUtils.matcher("<div.+?PinCount.+?PinCount.+?>\\s+?(.+?)Pins\\s+?</div>", content);
            if (match.find())
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1)));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            final String content = getContentAsString();
            checkNameAndSize(content);//extract file name and size from the page
            if (fileURL.contains("/pin/")) {
                final Matcher match = PlugUtils.matcher("<img\\s*?src=\"(.+?)\"\\s*?class=\"pinImage\"", content);
                if (!match.find())
                    throw new PluginImplementationException("Download link not found");
                final String dlLink = match.group(1).trim();
                if (!tryDownloadAndSaveFile(getGetMethod(dlLink))) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            } else {
                List<URI> list = new LinkedList<URI>();
                final Matcher matchPin = PlugUtils.matcher("<a href=\"(/pin/[^\"]+?)\" class=\"pinImageWrapper", content);
                while (matchPin.find())
                    list.add(new URI("http://www.pinterest.com" + matchPin.group(1)));
                int maxSize = PINSperPAGE;
                // check for more links
                if (list.size() == maxSize) {
                    final String source = PlugUtils.getStringBetween(content, "<link rel=\"canonical\" href=\"", "\">");
                    final Matcher matchD1 = PlugUtils.matcher("name\": \"BoardFeedResource\", (\"options\": \\{\"board_id\":.+?)\\},", content);
                    if (!matchD1.find()) throw new PluginImplementationException("Error D1");
                    final Matcher matchD2 = PlugUtils.matcher("context:(.+?\\}),", content);    // {"ios_deep_link": "pinterest:\/\/board\/XXXX\/XXXX", "app_version": "XXXX", "https_exp": XXXX}
                    if (!matchD2.find()) throw new PluginImplementationException("Error D2");
                    final Matcher matchD3 = PlugUtils.matcher("Footer-\\d+?\"\\}\\], \"errorStrategy\": 1, \"data\": \\{\\}, (\"options\": \\{\"scrollable\": true.+?), \"uid\": \"Grid-\\d", content);
                    if (!matchD3.find()) throw new PluginImplementationException("Error D3");
                    String data = "{" + matchD1.group(1).replaceAll("\\s", "") + ",\"context\":" +
                            matchD2.group(1).replaceAll("\\s", "") + ",\"module\":{\"name\":\"GridItems\"," +
                            matchD3.group(1).replaceAll("\\s", "") + "},\"append\":true,\"error_strategy\":1}";
                    long time = System.currentTimeMillis();

                    while (list.size() == maxSize) {
                        maxSize += PINSperPAGE;
                        time += 1;
                        final HttpMethod httpMethod = getMethodBuilder()
                                .setReferer(fileURL)
                                .setAction("http://www.pinterest.com/resource/BoardFeedResource/get/")
                                .setParameter("source_url", URLEncoder.encode(source, "UTF-8"))
                                .setParameter("data", URLEncoder.encode(data, "UTF-8"))
                                .setParameter("_", "" + time)
                                .toGetMethod();
                        httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
                        if (!makeRedirectedRequest(httpMethod)) {
                            checkProblems();
                            throw new ServiceConnectionProblemException();
                        }
                        final Matcher matchP2 = PlugUtils.matcher("<a href=\\\\\"(/pin/[^\"]+?)\\\\\" class=\\\\\"pinImageWrapper", getContentAsString());
                        while (matchP2.find())
                            list.add(new URI("http://www.pinterest.com" + matchP2.group(1)));
                        final Matcher matchB = PlugUtils.matcher("bookmarks\": \\[\"(.+?)\"\\]", getContentAsString());
                        if (!matchB.find()) throw new PluginImplementationException("Error B");

                        final String d1 = data.substring(0, data.indexOf("bookmarks\":[\"") + ("bookmarks\":[\"").length());
                        final String d2 = data.substring(data.indexOf("\"]"));
                        data = d1 + matchB.group(1) + d2;
                    }
                }

                if (list.isEmpty()) throw new PluginImplementationException("No links found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                logger.info("Added " + list.size() + " links");
                httpFile.setFileName("Link(s) Extracted !");
                httpFile.setState(DownloadState.COMPLETED);
                httpFile.getProperties().put("removeCompleted", true);
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("We couldn't find that page")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Something went wrong!") ||
                contentAsString.contains("Our server is experiencing a mild case of the hiccups")) {
            throw new ErrorDuringDownloadingException("Internal Pinterest error");
        }
    }

}