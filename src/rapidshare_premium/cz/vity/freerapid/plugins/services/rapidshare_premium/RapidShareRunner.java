/*
 * $Id: RapidShareRunner.java 4736 2013-11-04 11:16:52Z tong2shot $
 *
 * Copyright (C) 2007  Tomáš Procházka & Ladislav Vitásek
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package cz.vity.freerapid.plugins.services.rapidshare_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFileDownloadTask;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitásek &amp; Tomáš Procházka &lt;<a href="mailto:tomas.prochazka@atomsoft.cz">tomas.prochazka@atomsoft.cz</a>&gt;
 */
class RapidShareRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(RapidShareRunner.class.getName());
    private String finalUrl = null;

    @Override
    public void run() throws Exception {
        super.run();

        int i = 0;
        do {
            try {
                i++;
                tryDownloadAndSaveFile(downloadTask);
                break;
            } catch (BadLoginException ex) {
                setBadConfig();
                logger.log(Level.WARNING, "Login failed: " + ex.getMessage());
                if (i > 4) {
                    throw new BadLoginException("No RS Premium account login information!");
                }
            }
        } while (true);
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        try {
            checkLogin();
        } catch (BadLoginException ex) {
            // not important at checking phase
        }
        checkFile();
    }

    private void tryDownloadAndSaveFile(HttpFileDownloadTask downloadTask) throws Exception {
        checkLogin();
        if (finalUrl == null) {
            checkFile();
        }
        finalDownload(finalUrl, downloadTask);
    }

    private void checkFile() throws Exception {
        Matcher matcher = PlugUtils.matcher("!download(?:%7C|\\|)(?:[^%\\|]+)(?:%7C|\\|)(\\d+)(?:%7C|\\|)([^%\\|]+)", fileURL);
        if (matcher.find()) {
            fileURL = "http://rapidshare.com/files/" + matcher.group(1) + "/" + matcher.group(2);
            httpFile.setNewURL(new URL(fileURL));
        } else {
            matcher = PlugUtils.matcher("/share/([A-Z0-9]+)", fileURL);
            if (matcher.find()) {
                HttpMethod method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction("https://api.rapidshare.com/cgi-bin/rsapi.cgi")
                        .setParameter("rsource", "web")
                        .setParameter("sub", "sharelinkcontent")
                        .setParameter("share", matcher.group(1))
                        .setParameter("cbid", "2")
                        .setParameter("cbf", "rsapi.system.jsonp.callback")
                        .setParameter("callt", String.valueOf(System.currentTimeMillis()))
                        .toGetMethod();
                if (!makeRedirectedRequest(method)) {
                    checkFileProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkFileProblems();
                matcher = getMatcherAgainstContent("\"file:(\\d+),([^,]+),");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Error getting file ID and file name");
                }
                fileURL = "http://rapidshare.com/files/" + matcher.group(1) + "/" + matcher.group(2);
                httpFile.setNewURL(new URL(fileURL));
            }
        }
        matcher = PlugUtils.matcher("/files/(\\d+)/(.+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        final String fileId = matcher.group(1);
        final String fileName = URLDecoder.decode(matcher.group(2), "UTF-8");

        HttpMethod method = getMethodBuilder()
                .setAction("http://api.rapidshare.com/cgi-bin/rsapi.cgi")
                .setParameter("sub", "checkfiles")
                .setParameter("files", fileId)
                .setParameter("filenames", fileName)
                .toGetMethod();

        int status = 0;
        String responseString = "";
        try {
            status = client.makeRequest(method, true);
            responseString = client.getContentAsString();
            logger.log(Level.INFO, "Response check:{0}", responseString);
        } finally {
            method.abort();
            method.releaseConnection();
        }
        if (status == HttpStatus.SC_OK && responseString != null && !responseString.isEmpty()) {
            String[] response = responseString.split(",");
            int fileStatus = Integer.parseInt(response[4]);

            if (fileStatus == 1 || fileStatus == 2 || fileStatus == 6 || fileStatus >= 50) {
                //http://rs$serverid$shorthost.rapidshare.com/files/$fileid/$filename)
                finalUrl = String.format("http://rs%s%s.rapidshare.com/files/%s/%s?directstart=1", response[3], response[5], response[0], response[1]);
                logger.info(finalUrl);
                httpFile.setFileName(response[1]);
                httpFile.setFileSize(Long.parseLong(response[2]));
                httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
            }
            if (fileStatus == 0) {
                throw new URLNotAvailableAnymoreException("File not found");
            }
            if (fileStatus == 4) {
                throw new URLNotAvailableAnymoreException("File marked as illegal");
            }
            if (fileStatus == 5) {
                throw new URLNotAvailableAnymoreException("Anonymous file locked, because it has more than 10 downloads already");
            }
            if (fileStatus == 3) {
                throw new InvalidURLOrServiceProblemException("Server down");
            }
        } else {
            throw new ServiceConnectionProblemException("Server return status " + status);
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File deleted")
                || content.contains("File not found")
                || content.contains("Folder not found")
                || content.contains("File physically not found")
                || content.contains("Share not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException {
        Matcher matcher;
        final String contentAsString = client.getContentAsString();
        matcher = Pattern.compile("IP address (.*?) is already", Pattern.MULTILINE).matcher(contentAsString);
        if (matcher.find()) {
            final String ip = matcher.group(1);
            throw new ServiceConnectionProblemException(String.format("<b>RapidShare error:</b><br>Your IP address %s is already downloading a file. <br>Please wait until the download is completed.", ip));
        }
        if (contentAsString.contains("Currently a lot of users")) {
            matcher = Pattern.compile("Please try again in ([0-9]+) minute", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(contentAsString);
            if (matcher.find()) {
                throw new YouHaveToWaitException("Currently a lot of users are downloading files.", Integer.parseInt(matcher.group(1)) * 60 + 20);
            }
            throw new ServiceConnectionProblemException(String.format("<b>RapidShare error:</b><br>Currently a lot of users are downloading files."));
        }
        if (getContentAsString().contains("momentarily not available")
                || getContentAsString().contains("Server under repair")) {
            throw new ServiceConnectionProblemException("The server is momentarily not available.");
        }
    }

    private void finalDownload(String url, HttpFileDownloadTask downloadTask) throws Exception {
        logger.info("Download URL: " + url);
        downloadTask.sleep(0);
        if (downloadTask.isTerminated()) {
            throw new InterruptedException();
        }
        httpFile.setState(DownloadState.GETTING);
        final GetMethod method = client.getGetMethod(url);
        try {
            final InputStream inputStream = client.makeFinalRequestForFile(method, httpFile, true);
            if (inputStream != null) {
                downloadTask.saveToFile(inputStream);
            } else {
                checkProblems();
                throw new IOException("File input stream is empty.");
            }
        } finally {
            method.abort(); //really important lines!!!!!
            method.releaseConnection();
        }
    }

    private void checkLogin() throws Exception {
        RapidShareServiceImpl service = (RapidShareServiceImpl) getPluginService();
        PremiumAccount pa = service.getConfig();
        if (!pa.isSet() || badConfig) {
            synchronized (RapidShareRunner.class) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No RS Premium account login information!");
                }
                badConfig = false;
            }
        }

        String cookie = login(pa.getUsername(), pa.getPassword());
        client.getHTTPClient().getState().addCookie(new Cookie("rapidshare.com", "enc", cookie, "/", 86400, false));
    }

    private String login(String login, String password) throws IOException, BadLoginException, ServiceConnectionProblemException {
        if (RapidShareRunner.cookie != null) {
            return RapidShareRunner.cookie;
        }
        final PostMethod pm = getPostMethod("https://api.rapidshare.com/cgi-bin/rsapi.cgi");
        pm.addParameter("sub", "getaccountdetails");
        pm.addParameter("withcookie", "1");
        pm.addParameter("type", "prem");
        pm.addParameter("login", login);
        pm.addParameter("password", password);

        logger.info("Logging to RS...");
        try {
            int status = client.makeRequest(pm, false);

            if (status == HttpStatus.SC_OK) {
                String response = client.getContentAsString();
                pm.releaseConnection();

                if (response.startsWith("ERROR:")) {
                    throw new BadLoginException(response.replace("ERROR: ", ""));
                }

                Map<String, String> params = RapidShareSupport.parseRapidShareResponse(response);
                RapidShareRunner.cookie = params.get("cookie");
                return RapidShareRunner.cookie;
            } else {
                throw new ServiceConnectionProblemException("Server return status " + status);
            }
        } finally {
            pm.abort();
            pm.releaseConnection();
        }
    }

    private void setBadConfig() {
        badConfig = true;
    }

    private boolean badConfig = false;
    private static String cookie = null;
}
