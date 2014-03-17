package cz.vity.freerapid.plugins.services.fileparadox;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.CaptchaType;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.regex.Matcher;

/**
 * @author birchie
 */
public class SolveMediaCaptchaType implements CaptchaType {

    protected String getReCaptchaKeyRegex() {
        return "src=\"(.+?solvemedia\\.com/)papi/challenge\\.noscript\\?k=(.+?)\"";
    }

    @Override
    public boolean canHandle(final String content) {
        return PlugUtils.find(getReCaptchaKeyRegex(), content);
    }

    @Override
    public void handleCaptcha(final MethodBuilder methodBuilder, final HttpDownloadClient client, final CaptchaSupport captchaSupport) throws Exception {
        final Matcher captchaKeyMatcher = PlugUtils.matcher(getReCaptchaKeyRegex(), client.getContentAsString());
        if (!captchaKeyMatcher.find()) {
            throw new PluginImplementationException("Captcha key not found");
        }
        final String baseUrl = captchaKeyMatcher.group(1);
        final String captchaKey = captchaKeyMatcher.group(2);

        String mediaType;
        do {
            final HttpMethod httpMethodKey = new MethodBuilder(client)
                    .setAction(baseUrl + "papi/challenge.script")
                    .setParameter("k", captchaKey)
                    .toGetMethod();
            client.makeRequest(httpMethodKey, true);
            final Matcher magicMatcher = PlugUtils.matcher("magic\\s*:\\s*'(.+?)',", client.getContentAsString());
            if (!magicMatcher.find()) {
                throw new PluginImplementationException("Magic key not found");
            }
            final String magic = magicMatcher.group(1);
            final HttpMethod httpMethodPuzzle = new MethodBuilder(client)
                    .setAction(baseUrl + "papi/_challenge.js")
                    .setParameter("k", captchaKey + ";f=_ACPuzzleUtil.callbacks%5B0%5D;l=en;t=img;s=standard;c=js,swf11,swf11.2,swf,h5c,h5ct,svg,h5v,v/h264,v/ogg,v/webm,h5a,a/mp3,a/ogg,ua/chrome,ua/chrome18,os/nt,os/nt6.0,fwv/NNlHHA.miih79,jslib/jquery,jslib/jqueryui;am=" + magic + ";ca=script;ts=1353464281;th=custom;r=" + Math.random())
                    .toGetMethod();
            client.makeRequest(httpMethodPuzzle, true);
            final Matcher mediaMatcher = PlugUtils.matcher("\"mediatype\"\\s*:\\s*\"(.+?)\",", client.getContentAsString());
            if (!mediaMatcher.find()) {
                throw new PluginImplementationException("Captcha media type not found");
            }
            mediaType = mediaMatcher.group(1);
        } while (!mediaType.contains("img"));

        final Matcher captchaIdMatcher = PlugUtils.matcher("\"chid\"\\s*:\\s*\"(.+?)\",", client.getContentAsString());
        if (!captchaIdMatcher.find()) throw new PluginImplementationException("Captcha ID not found");
        final String captchaChID = captchaIdMatcher.group(1);
        final String captchaImg = baseUrl + "papi/media?c=" + captchaChID + ";w=300;h=150;fg=333333;bg=ffffff";

        final String captchaTxt = captchaSupport.getCaptcha(captchaImg);
        methodBuilder.setParameter("adcopy_challenge", captchaChID);
        methodBuilder.setParameter("solvemedia_response", captchaTxt);
        methodBuilder.setParameter("adcopy_response", captchaTxt);
    }

}
