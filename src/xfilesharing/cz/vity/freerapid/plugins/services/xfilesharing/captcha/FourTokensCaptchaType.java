package cz.vity.freerapid.plugins.services.xfilesharing.captcha;

import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

/**
 * @author tong2shot
 * @author ntoskrnl
 */
public class FourTokensCaptchaType implements CaptchaType {

    protected String getFourTokensCaptchaRegex() {
        return "<span style='position:absolute;padding\\-left:(\\d+)px;padding\\-top:\\d+px;'>(.+?)</span>";
    }

    @Override
    public boolean canHandle(final String content) {
        return PlugUtils.find(getFourTokensCaptchaRegex(), content);
    }

    @Override
    public void handleCaptcha(final MethodBuilder methodBuilder, final HttpDownloadClient client, final CaptchaSupport captchaSupport) throws Exception {
        final Matcher captchaMatcher = PlugUtils.matcher(getFourTokensCaptchaRegex(), client.getContentAsString());
        final StringBuilder sb = new StringBuilder(4);
        final List<Token> tokens = new ArrayList<Token>(4);
        while (captchaMatcher.find()) {
            tokens.add(new Token(PlugUtils.unescapeHtml(captchaMatcher.group(2)), Integer.parseInt(captchaMatcher.group(1))));
        }
        Collections.sort(tokens);
        for (final Token token : tokens) {
            sb.append(token.getValue());
        }
        final String captcha = Long.toString(Long.parseLong(sb.toString())); //omit leading '0'
        methodBuilder.setParameter("code", captcha);
    }

    private static class Token implements Comparable<Token> {
        private final String value;
        private final int position;

        public Token(final String value, final int position) {
            this.value = value;
            this.position = position;
        }

        public String getValue() {
            return value;
        }

        @Override
        public int compareTo(final Token that) {
            return Integer.valueOf(this.position).compareTo(that.position);
        }
    }

}
