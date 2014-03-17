package cz.vity.freerapid.plugins.services.youtube.srt;

import java.math.BigDecimal;

/**
 * @author Vity
 */
public class SrtItem {

    private int counter;
    private String start;
    private String text = "";
    private final double end;

    public SrtItem(int counter, String start, String dur) {
        this.counter = counter;
        this.start = start;
        end = new BigDecimal(start).add(new BigDecimal(dur)).doubleValue();
    }

    private String toTimeFormat(String value) {
        final String[] split = value.split("\\.");
        final Long sec = Long.valueOf(split[0]);
        long h, m, s;
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
        final String lineSeparator = System.getProperty("line.separator");
        final StringBuilder builder = new StringBuilder();
        builder.append(counter).append(lineSeparator).append(toTimeFormat(start)).append(" --> ").append(toTimeFormat(String.valueOf(end))).append(lineSeparator).append(text).append(lineSeparator).append(lineSeparator);
        return builder.toString();
    }

}
