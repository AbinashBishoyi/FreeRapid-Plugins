package cz.vity.freerapid.plugins.services.rapidshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginContext;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek
 */
class RapidShareRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(RapidShareRunner.class.getName());
    ConfigurationStorageSupport storage;
    PluginContext context;

    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            enterCheck();
        } else
            throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
    }

    public void run() throws Exception {
        super.run();
        //       storage = getPluginService().getPluginContext().getConfigurationStorageSupport();
        context = getPluginService().getPluginContext();

        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            enterCheck();
            Matcher matcher = getMatcherAgainstContent("form id=\"ff\" action=\"([^\"]*)\"");
            if (!matcher.find()) {
                throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
            }
            String s = matcher.group(1);
            logger.info("Found File URL - " + s);
            client.setReferer(fileURL);
            final PostMethod postMethod = getPostMethod(s);
            postMethod.addParameter("dl.start", "Free");
            if (makeRequest(postMethod)) {
                matcher = getMatcherAgainstContent("var c=([0-9]+);");
                if (!matcher.find()) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new PluginImplementationException();
                }
                s = matcher.group(1);

                int seconds = new Integer(s);
                matcher = getMatcherAgainstContent("form name=\"dlf\" action=\"([^\"]*)\"");
                if (matcher.find()) {
                    s = matcher.group(1);
                    logger.info("Download URL: " + s);
                    String myUrl = getPrefferedMirror();
                    if (!"".equals(myUrl)) s = myUrl;
                    //implemented http://wordrider.net/forum/read.php?11,3017,3028#msg-3028
                    int i1 = s.toLowerCase().indexOf("http://");
                    if (i1 == 0) {
                        i1 += "http://".length();
                        final int i2 = s.indexOf('/', i1);
                        if (i2 > 0) {
                            final String subs = s.substring(i1, i2);
                            String ip = translateToIP(subs);
                            logger.info("Changing " + subs + " to " + ip);
                            s = new StringBuilder(s).replace(i1, i2, ip).toString();
                        }
                    }
                    downloadTask.sleep(seconds + 1);
                    final PostMethod method = getPostMethod(s);
                    method.addParameter("mirror", "on");

                    if (!tryDownloadAndSaveFile(method)) {
                        checkProblems();
                        logger.warning(getContentAsString());
                        throw new IOException("File input stream is empty.");
                    }
                } else {
                    checkProblems();
                    logger.info(getContentAsString());
                    throw new PluginImplementationException();
                }
            } else
                throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
        } else
            throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
    }

    private String getPrefferedMirror() throws Exception {
        RapidShareServiceImpl service = (RapidShareServiceImpl) getPluginService();
        RapidShareMirrorConfig config = service.getConfig();
        MirrorChooser chooser = new MirrorChooser(context, config);
        chooser.setContent(getContentAsString());
        return chooser.getPreferredURL(getContentAsString());
    }

    private void enterCheck() throws NotRecoverableDownloadException, InvalidURLOrServiceProblemException, ServiceConnectionProblemException, YouHaveToWaitException {
        Matcher matcher;
        if (!getContentAsString().contains("form id=\"ff\" action=")) {

//            matcher = getMatcherAgainstContent("class=\"klappbox\">((\\s|.)*?)</div>");
//            if (matcher.find()) {
//                final String error = matcher.group(1);
//                if (error.contains("illegal content") || error.contains("file has been removed") || error.contains("has removed") || error.contains("file is neither allocated to") || error.contains("limit is reached"))
//                    throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>" + error);
//                if (error.contains("file could not be found"))
//                    throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>" + error);
//                logger.warning(getContentAsString());
//                throw new InvalidURLOrServiceProblemException("<b>RapidShare error:</b><br>" + error);
//            }
            if (getContentAsString().contains("has removed file"))
                throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>The uploader has removed this file from the server.");
            if (getContentAsString().contains("file could not be found"))
                throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>The file could not be found. Please check the download link.");
            if (getContentAsString().contains("illegal content"))
                throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>Illegal content. File was removed.");
            if (getContentAsString().contains("file has been removed"))
                throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>Due to a violation of our terms of use, the file has been removed from the server.");
            if (getContentAsString().contains("limit is reached"))
                throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br>To download this file, the uploader either needs to transfer this file into his/her Collector's Account, or upload the file again. The file can later be moved to a Collector's Account. The uploader just needs to click the delete link of the file to get further information.");
            if (getContentAsString().contains("This file is larger than")) {
                throw new NotRecoverableDownloadException("This file is larger than 200 Megabyte. To download this file, you either need a Premium Account, or the owner of this file may carry the downloading cost by making use of \"TrafficShare\".");
            }
            if (getContentAsString().contains("Currently a lot of users") || getContentAsString().contains("We regret")) {
                matcher = getMatcherAgainstContent("Please try again in ([0-9]+) minute");
                if (matcher.find()) {
                    throw new YouHaveToWaitException("Currently a lot of users are downloading files.", Integer.parseInt(matcher.group(1)) * 60 + 20);
                }
                throw new ServiceConnectionProblemException("<b>RapidShare error:</b><br>Currently a lot of users are downloading files.");
            }
            if (getContentAsString().contains("no more download slots")) {
                throw new ServiceConnectionProblemException("There are no more download slots available for free users right now");
            }
            if (getContentAsString().contains("Unfortunately right now our servers are overloaded")) {
                throw new ServiceConnectionProblemException("Unfortunately right now our servers are overloaded and we have no more download slots left for non-members. Of course you can also try again later.");
            }
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        matcher = getMatcherAgainstContent("\"downloadlink\">(.*?)<font");
        if (matcher.find()) {
            final String trimmedURL = matcher.group(1).trim();
            final int i = trimmedURL.lastIndexOf('/');
            if (i > 0)
                httpFile.setFileName(trimmedURL.substring(i + 1));
        }

        //| 5277 KB</font>
        matcher = getMatcherAgainstContent("\\| (.*? .B)</font>");
        if (matcher.find()) {
            Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
            logger.info("File size " + a);
            httpFile.setFileSize(a);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }

    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, NotRecoverableDownloadException, InvalidURLOrServiceProblemException {
        Matcher matcher;//Your IP address XXXXXX is already downloading a file.  Please wait until the download is completed.
        if (getContentAsString().contains("You have reached the")) {
            matcher = getMatcherAgainstContent("try again in about ([0-9]+) minute");
            if (matcher.find()) {
                throw new YouHaveToWaitException("You have reached the download-limit for free-users.", Integer.parseInt(matcher.group(1)) * 60 + 10);
            }
            throw new ServiceConnectionProblemException("<b>RapidShare error:</b><br>You have reached the download-limit for free-users.");
        }
        matcher = getMatcherAgainstContent("IP address (.*?) is already");
        if (matcher.find()) {
            final String ip = matcher.group(1);
            throw new ServiceConnectionProblemException(String.format("<b>RapidShare error:</b><br>Your IP address %s is already downloading a file. <br>Please wait until the download is completed.", ip));
        }
        if (getContentAsString().contains("Currently a lot of users") || getContentAsString().contains("We regret")) {
            matcher = getMatcherAgainstContent("Please try again in ([0-9]+) minute");
            if (matcher.find()) {
                throw new YouHaveToWaitException("Currently a lot of users are downloading files.", Integer.parseInt(matcher.group(1)) * 60 + 20);
            }
            throw new ServiceConnectionProblemException("<b>RapidShare error:</b><br>Currently a lot of users are downloading files.");
        }
        if (getContentAsString().contains("you either need a Premium Account")) {
            throw new URLNotAvailableAnymoreException("This file is larger than 200 Megabyte. To download this file, you either need a Premium Account, or the owner of this file may carry the downloading cost by making use of \"TrafficShare\".");
        }
        if (getContentAsString().contains("momentarily not available")) {
            throw new ServiceConnectionProblemException("The server is momentarily not available.");
        }
        if (getContentAsString().contains("This file is larger than")) {
            throw new NotRecoverableDownloadException("This file is larger than 200 Megabyte. To download this file, you either need a Premium Account, or the owner of this file may carry the downloading cost by making use of \"TrafficShare\".");
        }
        if (getContentAsString().contains("Unfortunately right now our servers are overloaded")) {
            throw new ServiceConnectionProblemException("Unfortunately right now our servers are overloaded and we have no more download slots left for non-members. Of course you can also try again later.");
        }
        if (getContentAsString().contains("no more download slots")) {
            throw new ServiceConnectionProblemException("There are no more download slots available for free users right now");
        }
    }

    private static String translateToIP(final String value) {
        try {
            InetAddress addr = InetAddress.getByName(value);
            byte[] ipAddr = addr.getAddress();

            // Convert to dot representation
            StringBuilder ipAddrStr = new StringBuilder(20);
            final int length = ipAddr.length;
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    ipAddrStr.append('.');
                }
                ipAddrStr.append(ipAddr[i] & 0xFF);
            }
            return ipAddrStr.toString();
        } catch (UnknownHostException e) {
            return value;
        }
    }

//
//    public static void main(String[] args) {
//        String s = "http://rs617l32.rapidshare.com/files/xxxxxxxxx/xxxxxxx/myfiles.part11.rar";
//        int i1 = s.toLowerCase().indexOf("http://");
//        if (i1 == 0) {
//            i1 += "http://".length();
//            final int i2 = s.indexOf('/', i1);
//            if (i2 > 0) {
//                final String subs = s.substring(i1, i2);
//                String ip = translateToIP(subs);
//                logger.info("Changing " + subs + " to " + ip);
//                s = new StringBuilder(s).replace(i1, i2, ip).toString();
//            }
//        }
//
//        System.out.println("s = " + s);
//    }

}

