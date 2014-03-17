package cz.vity.freerapid.plugins.services.youtube.srt;

import java.math.BigDecimal;

/**
 * @author Vity
 */
public class SrtItem {

    private final int counter;
    private final String start;
    private final String end;
    private String text = "";

    public SrtItem(int counter, String start, String dur) {
        this.counter = counter;
        this.start = start;
        this.end = new BigDecimal(start).add(new BigDecimal(dur)).toPlainString();
    }

    private static String toTimeFormat(String value) {
        final String[] split = value.split("\\.");
        final int sec = Integer.parseInt(split[0]);
        final int h, m, s;
        h = sec / 3600;
        m = (sec / 60) % 60;
        s = sec % 60;
        return String.format("%02d:%02d:%02d,%s", h, m, s, (split.length == 1) ? "0" : split[1]);
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append(counter)
                .append("\r\n")
                .append(toTimeFormat(start))
                .append(" --> ")
                .append(toTimeFormat(end))
                .append("\r\n")
                .append(text)
                .append("\r\n\r\n")
                .toString();
    }

}
