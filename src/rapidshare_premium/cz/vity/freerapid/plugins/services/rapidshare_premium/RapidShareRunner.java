/*
 * $Id: RapidShareRunner.java 1401 2009-02-21 01:21:18Z Vity $
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
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitásek &amp; Tomáš Procházka &lt;<a href="mailto:tomas.prochazka@atomsoft.cz">tomas.prochazka@atomsoft.cz</a>&gt;
 */
class RapidShareRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(RapidShareRunner.class.getName());

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
                logger.log(Level.WARNING, "Bad password or login!");
                if (i > 4) {
                    throw new BadLoginException("No RS Premium account login information!");
                }
            }
        } while (true);
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            chechFile();
        } else {
            throw new PluginImplementationException();
        }
    }

    private void tryDownloadAndSaveFile(HttpFileDownloadTask downloadTask) throws Exception {
        final GetMethod getMethod = getGetMethod(fileURL);
        checkLogin();

        client.makeRequest(getMethod, false);

        // Redirect directly to download file.
        if (getMethod.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
            logger.info("Direct download mode");
            Header header = getMethod.getResponseHeader("location");
            getMethod.releaseConnection();
            String newUri = null;
            if (header != null) {
                newUri = header.getValue();
            }
            if (newUri != null) {
                finalDownload(newUri, downloadTask);
            }
        } else if (getMethod.getStatusCode() == HttpStatus.SC_OK) {

            chechFile();

            if (client.getContentAsString().contains("Your Cookie has not been recognized")) {
                throw new BadLoginException("<b>RapidShare known error:</b><br> Bad login or password");
            }

            Matcher matcher = getMatcherAgainstContent("form id=\"ff\" action=\"([^\"]*)\"");
            boolean ok = false;
            if (matcher.find()) {
                String s = matcher.group(1);
                //| 5277 KB</font>
                matcher = getMatcherAgainstContent("\\| (.*?) KB</font>");
                if (matcher.find()) {
                    httpFile.setFileSize(new Integer(matcher.group(1).replaceAll(" ", "")) * 1024);
                }
                logger.info("Found File URL - " + s);
                client.setReferer(fileURL);
                final PostMethod postMethod = client.getPostMethod(s);
                postMethod.addParameter("dl.start", "PREMIUM");
                if (makeRequest(postMethod)) {
                    if (client.getContentAsString().contains("Your Cookie has not been recognized")) {
                        throw new BadLoginException("<b>RapidShare known error:</b><br> Bad login or password");
                    }
                    matcher = getMatcherAgainstContent("(http://.*?\\.rapidshare\\.com/files/.*?)\"");
                    if (matcher.find()) {
                        s = matcher.group(1);
                        ok = true;
                        finalDownload(s, downloadTask);
                    } else {
                        checkProblems();
                        logger.info(client.getContentAsString());
                    }
                }
            }

            if (!ok) {
                failed();
            }

        } else {
            failed();
        }
    }

    private void failed() throws PluginImplementationException {
        throw new PluginImplementationException();
    }

    private void chechFile() throws URLNotAvailableAnymoreException, InvalidURLOrServiceProblemException, BadLoginException, YouHaveToWaitException {
        String code = client.getContentAsString().toLowerCase();
        // Fast detec known error messages
        if (code.contains("illegal content")) {
            throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br> Illegal content. File was removed.");
        }
        if (code.contains("could not be found")) {
            throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br> The file could not be found. Please check the download link.");
        }
        if (code.contains("the uploader has removed this file from the server")) {
            throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br> The uploader has removed this file from the server.");
        }
        if (code.contains("violation of our terms of use") || code.contains("file has been removed")) {
            throw new URLNotAvailableAnymoreException("<b>RapidShare error:</b><br> Due to a violation of our terms of use, the file has been removed from the server.");
        }
        if (code.contains("your premium account has not been found")) {
            setBadConfig();
            logger.log(Level.WARNING, "Account expired. Maybe.");
            throw new BadLoginException("<b>RapidShare error:</b><br> Your premium account has not been found.");
        }
        if (code.contains("you have exceeded the download limit")) {
            int pause = 20 * 60;
            int toMidnight = RapidShareSupport.getSecondToMidnight();
            if (toMidnight > 18 * 3600) pause = toMidnight + 5 * 60;
            throw new YouHaveToWaitException("<b>RapidShare known error:</b><br> You have exceeded the download limit.", pause);
        }

        // Match another error messages from standard error box
        Matcher matcher = Pattern.compile("<h1>error.*?class=\"klappbox\">(.*?)</div>", Pattern.DOTALL).matcher(code);
        if (matcher.find()) {
            final String error = matcher.group(1);
            logger.warning("RapidShare unknown error:" + error);
            throw new InvalidURLOrServiceProblemException("<b>RapidShare unknown error:</b><br> " + error);
        }

        // Unknown error message
        if (code.contains("error")) {
            logger.warning(client.getContentAsString());
            throw new InvalidURLOrServiceProblemException("Unknown RapidShare error");
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

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException {
        Matcher matcher;
        final String contentAsString = client.getContentAsString();
        matcher = Pattern.compile("IP address (.*?) is already", Pattern.MULTILINE).matcher(contentAsString);
        if (matcher.find()) {
            final String ip = matcher.group(1);
            throw new ServiceConnectionProblemException(String.format("<b>RapidShare error:</b><br>Your IP address %s is already downloading a file. <br>Please wait until the download is completed.", ip));
        }
        if (contentAsString.indexOf("Currently a lot of users") >= 0) {
            matcher = Pattern.compile("Please try again in ([0-9]+) minute", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(contentAsString);
            if (matcher.find()) {
                throw new YouHaveToWaitException("Currently a lot of users are downloading files.", Integer.parseInt(matcher.group(1)) * 60 + 20);
            }
            throw new ServiceConnectionProblemException(String.format("<b>RapidShare error:</b><br>Currently a lot of users are downloading files."));
        }
    }

    private void finalDownload(String url, HttpFileDownloadTask downloadTask) throws Exception {
        logger.info("Download URL: " + url);
        downloadTask.sleep(0);
        if (downloadTask.isTerminated()) {
            throw new InterruptedException();
        }
        httpFile.setState(DownloadState.GETTING);
        final PostMethod method = client.getPostMethod(url);
        method.addParameter("mirror", "on");
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
        synchronized (RapidShareRunner.class) {
            RapidShareServiceImpl service = (RapidShareServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet() || badConfig) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No RS Premium account login information!");
                }
                badConfig = false;
            }

            String cookie = RapidShareSupport.buildCookie(pa.getUsername(), pa.getPassword());
            logger.info("Builded RS cookie: " + cookie);
            client.getHTTPClient().getState().addCookie(new Cookie("rapidshare.com", "user", cookie, "/", 86400, false));
        }
    }

    private void setBadConfig() {
        badConfig = true;
    }

    private boolean badConfig = false;

}

