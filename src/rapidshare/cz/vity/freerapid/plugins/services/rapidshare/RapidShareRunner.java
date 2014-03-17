package cz.vity.freerapid.plugins.services.rapidshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.InetAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, ntoskrnl
 */
class RapidShareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RapidShareRunner.class.getName());
    private final static String SERVICE_WEB = "http://rapidshare.com/";
    private final static int INTERVAL = 10000;

    private String fileID;
    private String fileName;
    private long lastRunCheck;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkURL();
        final HttpMethod method = getMethodBuilder()
                .setReferer(SERVICE_WEB)
                .setAction("https://api.rapidshare.com/cgi-bin/rsapi.cgi")
                .setParameter("sub", "download")
                .setParameter("fileid", fileID)
                .setParameter("filename", fileName)
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
        lastRunCheck = System.currentTimeMillis();
    }

    @Override
    public void run() throws Exception {
        super.run();
        if (System.currentTimeMillis() >= lastRunCheck + INTERVAL) {
            logger.info("Doing runCheck...");
            runCheck();
        }
        checkProblems();
        final Matcher matcher = getMatcherAgainstContent("DL:(.+?),(.+?),(\\d+)");
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing API response");
        }
        final int wait = Integer.parseInt(matcher.group(3));
        final String host = translateToIP(matcher.group(1));
        final HttpMethod method = getMethodBuilder()
                .setReferer(SERVICE_WEB)
                .setAction("http://" + host + "/cgi-bin/rsapi.cgi")
                .setParameter("sub", "download")
                .setParameter("editparentlocation", "1")
                .setParameter("bin", "1")
                .setParameter("fileid", fileID)
                .setParameter("filename", fileName)
                .setParameter("dlauth", matcher.group(2))
                .toGetMethod();
        if (wait > 0) {
            downloadTask.sleep(wait + 1);
        }
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void checkURL() throws Exception {
        Matcher matcher = PlugUtils.matcher("!download(?:%7C|\\|)(?:[^%\\|]+)(?:%7C|\\|)(\\d+)(?:%7C|\\|)([^%\\|]+)", fileURL);
        if (matcher.find()) {
            fileURL = "http://rapidshare.com/files/" + matcher.group(1) + "/" + matcher.group(2);
            httpFile.setNewURL(new URL(fileURL));
        }
        matcher = PlugUtils.matcher("/files/(\\d+)/(.+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        fileID = matcher.group(1);
        fileName = URLDecoder.decode(matcher.group(2), "UTF-8");
        httpFile.setFileName(fileName);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        checkFileProblems();
        final String content = getContentAsString();
        if (content.contains("You need RapidPro to download more files from your IP address")
                || content.contains("All free download slots are full")) {
            throw new ServiceConnectionProblemException("All free download slots are full");
        }
        if (content.contains("File owner's public traffic exhausted")) {
            throw new ServiceConnectionProblemException("File owner's public traffic exhausted");
        }
        Matcher matcher = getMatcherAgainstContent("You need to wait (\\d+) seconds[^\"']*");
        if (matcher.find()) {
            throw new YouHaveToWaitException(matcher.group(), Integer.parseInt(matcher.group(1)) + 10);
        }
        if (content.contains("Please stop flooding our download servers")) {
            throw new YouHaveToWaitException("RapidShare server says: Please stop flooding our download servers", 360);
        }
        if (content.contains("IP address modified")
                || content.contains("Download auth invalid")
                || content.contains("Download session expired")
                || content.contains("Download session invalid")
                || content.contains("Download session modified")
                || content.contains("Download ticket not ready")
                || content.contains("download: session invalid")) {
            throw new ServiceConnectionProblemException("Temporary server problem");
        }
        if (content.contains("Secure download link modified")
                || content.contains("Secured link expired")
                || content.contains("Secured link modified")) {
            throw new ServiceConnectionProblemException("The file was requested using an invalid secure link");
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        Matcher matcher = getMatcherAgainstContent("File deleted R(\\d+)");
        if (matcher.find()) {
            final int r = Integer.parseInt(matcher.group(1));
            if (r == 1 || r == 2) {
                throw new URLNotAvailableAnymoreException("The file was deleted by the owner");
            }
            if (r == 3 || r == 5) {
                throw new URLNotAvailableAnymoreException("The file was deleted due to no downloads in a longer period");
            }
            if (r == 4 || r == 8) {
                throw new URLNotAvailableAnymoreException("The file is suspected to be contrary to our terms and conditions and has been locked up for clarification");
            }
            if (r >= 10 && r <= 15) {
                throw new URLNotAvailableAnymoreException("This file is marked as illegal");
            }
        }
        if (content.contains("File deleted")
                || content.contains("File not found")
                || content.contains("Folder not found")
                || content.contains("File physically not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("This file is too big to download it for free")) {
            throw new NotRecoverableDownloadException("This file is too big to download it for free");
        }
        if (content.contains("This file is marked as illegal")) {
            throw new URLNotAvailableAnymoreException("This file is marked as illegal");
        }
        if (content.contains("File incomplete")
                || content.contains("raid error on server")) {
            throw new URLNotAvailableAnymoreException("File corrupted or incomplete");
        }
        if (content.contains("SSL downloads are only available for RapidPro customers")) {
            throw new NotRecoverableDownloadException("SSL downloads are only available for RapidPro customers");
        }
        matcher = getMatcherAgainstContent("ERROR:([^\"']+)");
        if (matcher.find()) {
            throw new NotRecoverableDownloadException("RapidShare error: " + matcher.group(1));
        }
    }

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

