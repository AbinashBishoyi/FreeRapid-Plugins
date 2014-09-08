package cz.vity.freerapid.plugins.services.bbc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author tong2shot
 */
class SettingsPanel extends JPanel {

    private SettingsConfig config;

    public SettingsPanel(BbcServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final JLabel lblQuality = new JLabel("Preferred quality level:");
        final JComboBox<VideoQuality> cbbVideoQuality = new JComboBox<VideoQuality>(VideoQuality.getItems());
        final JLabel lblRtmpPort = new JLabel("Preferred RTMP port:");
        final JComboBox<RtmpPort> cbbRtmpPort = new JComboBox<RtmpPort>(RtmpPort.values());
        final JLabel lblCdn = new JLabel("<html>Preferred CDN: <small><sup>*)</sup></small></html>");
        final JComboBox<Cdn> cbbCdn = new JComboBox<Cdn>(Cdn.values());
        final JCheckBox checkTor = new JCheckBox("<html>Enable Tor <small><sup>**)</sup></small></html>", config.isEnableTor());
        final JCheckBox checkSubtitles = new JCheckBox("Download subtitles", config.isDownloadSubtitles());
        final JLabel lblCdnNote = new JLabel("<html><small>*) Akamai is only downloadble in the UK.</small></html>");
        final JLabel lblTorNote = new JLabel("<html><small>**) Doesn't affect UK and proxy users," +
                "<br>UK and proxy users won't use Tor.</small></html>");

        lblQuality.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbbVideoQuality.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblRtmpPort.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbbRtmpPort.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblCdn.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbbCdn.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkTor.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkSubtitles.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblCdnNote.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblTorNote.setAlignmentX(Component.LEFT_ALIGNMENT);

        cbbVideoQuality.setSelectedItem(config.getVideoQuality());
        cbbVideoQuality.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setVideoQuality((VideoQuality) cbbVideoQuality.getSelectedItem());
            }
        });
        cbbRtmpPort.setSelectedItem(config.getRtmpPort());
        cbbRtmpPort.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setRtmpPort((RtmpPort) cbbRtmpPort.getSelectedItem());
            }
        });
        cbbCdn.setSelectedItem(config.getCdn());
        cbbCdn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setCdn((Cdn) cbbCdn.getSelectedItem());
            }
        });
        checkTor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setEnableTor(checkTor.isSelected());
            }
        });
        checkSubtitles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                config.setDownloadSubtitles(checkSubtitles.isSelected());
            }
        });

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(lblQuality);
        add(cbbVideoQuality);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(lblRtmpPort);
        add(cbbRtmpPort);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(lblCdn);
        add(cbbCdn);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(checkTor);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(checkSubtitles);
        add(Box.createRigidArea(new Dimension(0, 15)));
        add(lblCdnNote);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(lblTorNote);
        add(Box.createRigidArea(new Dimension(0, 15)));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

}
