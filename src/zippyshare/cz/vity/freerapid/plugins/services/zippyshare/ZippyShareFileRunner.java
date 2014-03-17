package cz.vity.freerapid.plugins.services.zippyshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.plugins.webclient.utils.ScriptUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Vity+ntoskrnl+tonyk
 */
class ZippyShareFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(ZippyShareFileRunner.class.getName());
    private static Integer variant = 0;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
            String url;
            Matcher matcher = getMatcherAgainstContent("download\\?");
            if (matcher.find()) {
                matcher = getMatcherAgainstContent("url: '(.+?)'");

                if (!matcher.find()) {
                    throw new PluginImplementationException("Download link not found");
                }
                Long time = (long) PlugUtils.getNumberBetween(getContentAsString(), "seed: ", "}");
                Integer version = (int) (Float.parseFloat(PlugUtils.getStringBetween(getContentAsString(), "DownloadButton_v", "s.swf")) * 100);
                switch (version) {
                    case 100:
                        url = matcher.group(1) + "&time=" + ((time * 13 % 2139) + 6);
                        break;
                    case 105:
                        url = matcher.group(1) + "&time=" + ((((time * time + time) % 234903) * (long) (time / 8)) % 317562356);
                        break;
                    case 114:
                        switch (variant) {

                            case 4:
                                url = matcher.group(1) + "&time=" + (3 * time % 1424574);
                                break;
                            case 1:
                                url = matcher.group(1) + "&time=" + (6 * time % 78678623);
                                break;
                            case 2:
                                url = matcher.group(1) + "&time=" + (9 * time % 2374755);
                                break;
                            case 3:
                                url = matcher.group(1) + "&time=" + (24 * time % 6743256);
                                break;
                            case 5:
                                url = matcher.group(1) + "&time=" + (11 * time % 9809328);
                                break;
                            case 6:
                                url = matcher.group(1) + "&time=" + (15 * time % 12860893);
                                break;
                            case 7:
                                url = matcher.group(1) + "&time=" + (7 * time % 31241678);
                                break;
                            case 8:
                                url = matcher.group(1) + "&time=" + (16 * time % 8977777);
                                break;
                            case 9:
                                url = matcher.group(1) + "&time=" + (2 * time % 1265673);
                                break;
                            case 10:
                                url = matcher.group(1) + "&time=" + (8 * time % 46784661);
                                break;
                            case 11:
                                url = matcher.group(1) + "&time=" + (5 * time % 71678963);
                                break;
                            case 0:
                                url = matcher.group(1) + "&time=" + (4 * time % 12376764);
                                break;


                            default:
                                checkProblems();
                                variant = 0;
                                throw new ServiceConnectionProblemException("Error starting download");

                        }
                        break;
                    default:
                        throw new PluginImplementationException("New version of download button detected, please report this to freerapid plugins section");

                }


            } else {
                matcher = getMatcherAgainstContent("<script[^<>]*?>\\s*?(var [^\r\n]+)\\s*?var [a-zA-Z\\d]+? ?= ?([^\r\n]+)");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Download link not found");
                }
                final String script = matcher.group(1) + "\n" + matcher.group(2);
                logger.info(script);
                url = ScriptUtils.evaluateJavaScriptToString(script);
                System.out.print(getContentAsString());
            }

            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {

                variant++;
                throw new YouHaveToWaitException("", 1);

            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }


    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("The requsted file does not exist on this server")
                || contentAsString.contains("File has expired")
                || contentAsString.contains("<h1>HTTP Status")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher name = getMatcherAgainstContent("Name:\\s*?<.+?>\\s*?<.+?>(.+?)<.+?>");
        if (!name.find()) throw new PluginImplementationException("File name not found");
        httpFile.setFileName(name.group(1));

        final Matcher size = getMatcherAgainstContent("Size:\\s*?<.+?>\\s*?<.+?>(.+?)<.+?>");
        if (!size.find()) throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(size.group(1)));

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

}