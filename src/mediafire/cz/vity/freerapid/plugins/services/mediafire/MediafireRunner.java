package cz.vity.freerapid.plugins.services.mediafire;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, ntoskrnl
 */
class MediafireRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MediafireRunner.class.getName());

    public MediafireRunner() {
        super();
    }

    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else
            throw new ServiceConnectionProblemException();
    }

    public void run() throws Exception {
        super.run();

        if (isList()) {
            runList();
            return;
        }

        final HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();

            if (getContentAsString().contains("dh('');")) { //if passworded
                while (getContentAsString().contains("dh('');")) {
                    HttpMethod postPwd = getMethodBuilder()
                            .setReferer(fileURL)
                            .setBaseURL("http://www.mediafire.com/")
                            .setActionFromFormByName("form_password", true)
                            .setAndEncodeParameter("downloadp", getPassword())
                            .toPostMethod();
                    if (!makeRedirectedRequest(postPwd)) {
                        throw new PluginImplementationException("Some issue while posting password");
                    }
                }
            }

            if (getContentAsString().contains("cu('")) {
                Matcher matcher = getMatcherAgainstContent("cu\\('([^']+)','([^']+)','([^']+)'\\)");

                if (!matcher.find()) {
                    throw new PluginImplementationException();
                }
                String qk = matcher.group(1);
                String pk = matcher.group(2);
                String r = matcher.group(3);
                String url = "http://www.mediafire.com/dynamic/download.php?qk=" + qk + "&pk=" + pk + "&r=" + r;
                logger.info("Script target URL " + url);
                GetMethod method = getGetMethod(url);

                if (makeRequest(method)) {

                    String u2 = PlugUtils.getStringBetween(getContentAsString(), "key to support (", ")\"");
                    String m1 = PlugUtils.getStringBetween(getContentAsString(), "='download", "com';var");
                    String mh = PlugUtils.getStringBetween(getContentAsString(), "var mH='", "';");
                    String my = PlugUtils.getStringBetween(getContentAsString(), "var mY='", "';");

                    String finalLink = "http://download" + m1 + "com/" + u2 + "g/" + mh + "/" + my;
                    logger.info("Final URL " + finalLink);

                    GetMethod method2 = getGetMethod(finalLink);
                    client.getHTTPClient().getParams().setParameter("considerAsStream", "text/plain");
                    if (!tryDownloadAndSaveFile(method2)) {
                        checkProblems();
                        logger.info(getContentAsString());
                        throw new PluginImplementationException();
                    }

                } else {
                    checkProblems();
                    throw new PluginImplementationException();
                }
            } else {
                checkProblems();
                throw new PluginImplementationException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws Exception {
        if (isList()) return;

        PlugUtils.checkFileSize(httpFile, content, "sharedtabsfileinfo1-fs\" value=\"", "\">");
        PlugUtils.checkName(httpFile, content, "sharedtabsfileinfo1-fn\" value=\"", "\">");

    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("The key you provided for file download was invalid") || contentAsString.contains("How can MediaFire help you?")) {
            throw new URLNotAvailableAnymoreException(String.format("File not found"));
        }
    }

    private void runList() throws Exception {
        final HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(getMethod)) {
            final Matcher matcher = getMatcherAgainstContent("src=\"(/js/myfiles.php[^\"]+?)\"");
            if (!matcher.find()) throw new PluginImplementationException("URL to list not found");
            HttpMethod listMethod = getMethodBuilder().setBaseURL("http://www.mediafire.com").setAction(matcher.group(1)).toHttpMethod();

            if (makeRedirectedRequest(listMethod)) parseList();
            else throw new ServiceConnectionProblemException();

        } else throw new ServiceConnectionProblemException();
    }

    /* this seems to be unused

    String parseLink(String rawlink) throws Exception {

        String link = "";

        Matcher matcher = PlugUtils.matcher("([^']*)'([^']*)'", rawlink);
        while (matcher.find()) {

            Matcher matcher1 = PlugUtils.matcher("\\+\\s*(\\w+)", matcher.group(1));
            while (matcher1.find()) {

                link = link + (getVar(matcher1.group(1)));
            }
            link = link + matcher.group(2);

        }
        matcher = PlugUtils.matcher("([^']*)'$", rawlink);
        if (matcher.find()) {

            Matcher matcher1 = PlugUtils.matcher("\\+\\s*(\\w+)", matcher.group(1));
            if (matcher1.find()) {

                link = link + (getVar(matcher1.group(1)));
            }


        }

        return link;
    }

    private String getVar(String s) throws PluginImplementationException {

        Matcher matcher = PlugUtils.matcher("var " + s + "\\s*=\\s*'([^']*)'", getContentAsString());
        if (matcher.find()) {
            return matcher.group(1);
        } else
            throw new PluginImplementationException("Parameter " + s + " was not found");
    }
    */

    private void parseList() {
        final Matcher matcher = getMatcherAgainstContent("oe\\[[0-9]+\\]=Array\\('([^']+?)'");
        int start = 0;
        final List<URI> uriList = new LinkedList<URI>();
        while (matcher.find(start)) {
            final String link = "http://www.mediafire.com/download.php?" + matcher.group(1);
            try {
                uriList.add(new URI(link));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
            start = matcher.end();
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }


    private boolean isList() {
        return (fileURL.contains("?sharekey="));
    }

    private String getPassword() throws Exception {
        MediafirePasswordUI ps = new MediafirePasswordUI();
        if (getDialogSupport().showOKCancelDialog(ps, "Secured file on Mediafire")) {
            return (ps.getPassword());
        } else throw new NotRecoverableDownloadException("This file is secured with a password");

    }

}
