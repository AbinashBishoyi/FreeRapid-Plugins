package cz.vity.freerapid.plugins.services.hulkfile;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.CaptchaType;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.FourTokensCaptchaType;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class HulkFileFileRunner extends XFileSharingRunner {

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("Preparing download link ...");
        return downloadPageMarkers;
    }

    //we have to override this method, because the commented captcha is picked up by regex
    @Override
    protected List<CaptchaType> getCaptchaTypes() {
        final List<CaptchaType> captchaTypes = super.getCaptchaTypes();
        captchaTypes.add(0, new FourTokensCaptchaType() {  //put it at the top
            @Override
            public void handleCaptcha(MethodBuilder methodBuilder, HttpDownloadClient client, CaptchaSupport captchaSupport) throws Exception {
                super.handleCaptcha(methodBuilder, client, captchaSupport);
                final String captcha = methodBuilder.getParameters().get("code");
                if (captcha.length() > 4) { //captcha length = 8 OR 12
                    int div = captcha.length() / 4;
                    final StringBuilder sb = new StringBuilder(4);
                    for (int i = 0; i < captcha.length(); i++) {
                        if ((i % div) == 1) sb.append(captcha.charAt(i)); //remove redundant commented captcha
                    }
                    methodBuilder.setParameter("code", sb.toString()); //captcha length = 4
                }
            }
        });
        return captchaTypes;
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("This file is not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        super.checkFileProblems();
    }
}