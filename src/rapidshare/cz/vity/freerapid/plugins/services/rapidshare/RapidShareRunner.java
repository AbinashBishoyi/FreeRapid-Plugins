package cz.vity.freerapid.plugins.services.rapidshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginContext;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, ntoskrnl
 */
class RapidShareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RapidShareRunner.class.getName());
    private final static String SERVICE_WEB = "http://rapidshare.com/";
    private PluginContext context;
    private String fileID;
    private String fileName;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkURL();
        final HttpMethod method = getMethodBuilder()
                .setReferer(SERVICE_WEB)
                .setAction("http://api.rapidshare.com/cgi-bin/rsapi.cgi")
                .setParameter("sub", "download_v1")
                .setParameter("fileid", fileID)
                .setAndEncodeParameter("filename", fileName)
                .setParameter("try", "1")
                .setParameter("cbf", "RSAPIDispatcher")
                .setParameter("cbid", "1")
                .toGetMethod();
        if (makeRedirectedRequest(method)) {
            checkFileProblems();
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        context = getPluginService().getPluginContext();
        runCheck();
        checkProblems();
        final Matcher matcher = getMatcherAgainstContent("DL:(.+?),(.+?),(\\d+)");
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing API response");
        }
        final int wait = Integer.parseInt(matcher.group(3)) + 1;
        String host = matcher.group(1);
        final String prefer = getPreferredMirror();
        if (prefer != null && !prefer.isEmpty()) host = prefer;
        host = translateToIP(host);
        final HttpMethod method = getMethodBuilder()
                .setReferer(SERVICE_WEB)
                .setAction("http://" + host + "/cgi-bin/rsapi.cgi")
                .setParameter("sub", "download_v1")
                .setParameter("editparentlocation", "1")
                .setParameter("bin", "1")
                .setParameter("fileid", fileID)
                .setAndEncodeParameter("filename", fileName)
                .setParameter("dlauth", matcher.group(2))
                .toGetMethod();
        downloadTask.sleep(wait);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void checkURL() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("/files/(\\d+)/(.+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        fileID = matcher.group(1);
        fileName = matcher.group(2);
        httpFile.setFileName(fileName);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        checkFileProblems();
        if (getContentAsString().contains("You need RapidPro to download more files from your IP address")) {
            throw new ServiceConnectionProblemException("You need RapidPro to download more files from your IP address");
        }
        Matcher matcher = getMatcherAgainstContent("You need to wait (\\d+) seconds[^\"']*");
        if (matcher.find()) {
            throw new YouHaveToWaitException(matcher.group(), Integer.parseInt(matcher.group(1)) + 10);
        }
        if (getContentAsString().contains("All free download slots are full")) {
            throw new ServiceConnectionProblemException("All free download slots are full");
        }
        if (getContentAsString().contains("Please stop flooding our download servers")) {
            throw new YouHaveToWaitException("RapidShare server says: Please stop flooding our download servers", 360);
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("File deleted")) {
            throw new URLNotAvailableAnymoreException("File has been deleted");
        }
        if (getContentAsString().contains("This file is too big to download it for free")) {
            throw new NotRecoverableDownloadException("This file is too big to download it for free");
        }
        Matcher matcher = getMatcherAgainstContent("ERROR:([^\"']+)");
        if (matcher.find()) {
            throw new NotRecoverableDownloadException("RapidShare error: " + matcher.group(1));
        }
    }

    private String getPreferredMirror() throws Exception {
        RapidShareServiceImpl service = (RapidShareServiceImpl) getPluginService();
        RapidShareMirrorConfig config = service.getConfig();
        MirrorChooser chooser = new MirrorChooser(context, config);
        chooser.setContent(getContentAsString());
        return chooser.getPreferredURL(getContentAsString());
    }

    /*
    private void enterCheck() throws ErrorDuringDownloadingException {
        Matcher matcher;
        if (!getContentAsString().contains("form id=\"ff\" action=")) {
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

    private void checkProblems() throws ErrorDuringDownloadingException {
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
    */

    private static String translateToIP(String s) {
        //implemented http://wordrider.net/forum/read.php?11,3017,3028#msg-3028
        int i1 = s.toLowerCase().indexOf("http://");
        if (i1 == 0) {
            i1 += "http://".length();
            final int i2 = s.indexOf('/', i1);
            if (i2 > 0) {
                final String subs = s.substring(i1, i2);
                String ip = hostToIP(subs);
                logger.info("Changing " + subs + " to " + ip);
                s = new StringBuilder(s).replace(i1, i2, ip).toString();
            }
        }
        return s;
    }

    private static String hostToIP(final String value) {
        try {
            InetAddress addr = InetAddress.getByName(value);
            byte[] ipAddr = addr.getAddress();

            // Convert to dot representation
            StringBuilder ipAddrStr = new StringBuilder(15);
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

}

