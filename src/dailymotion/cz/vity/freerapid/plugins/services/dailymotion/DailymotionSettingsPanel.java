package cz.vity.freerapid.plugins.services.dailymotion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author tong2shot
 */
class DailymotionSettingsPanel extends JPanel {
    private DailymotionSettingsConfig config;

    public DailymotionSettingsPanel(DailymotionServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final JLabel qualityLabel = new JLabel("Preferred quality level:");
        final JComboBox<VideoQuality> qualityList = new JComboBox<VideoQuality>(VideoQuality.getItems());
        final JCheckBox subtitleCheck = new JCheckBox("Download subtitle");

        qualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setSelectedItem(config.getVideoQuality());
        subtitleCheck.setSelected(config.isSubtitleDownload());

        qualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setVideoQuality((VideoQuality) qualityList.getSelectedItem());
            }
        });

        subtitleCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setSubtitleDownload(subtitleCheck.isSelected());
            }
        });
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(qualityLabel);
        add(qualityList);
        add(subtitleCheck);
        add(Box.createRigidArea(new Dimension(0, 15)));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

}
