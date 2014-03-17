package cz.vity.freerapid.plugins.services.raagfm;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RaagFmSettingsPanel extends JPanel {
    private RaagFmSettingsConfig config;

    public RaagFmSettingsPanel(RaagFmServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final String[] qualityStrings = {"128 kbps", "320 kbps"};
        final int[] qualityIndexMap = {0, 1};

        final JLabel qualityLabel = new JLabel("Preferred quality level:");
        final JComboBox qualityList = new JComboBox(qualityStrings);

        qualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setAlignmentX(Component.LEFT_ALIGNMENT);

        int qs = config.getQualitySetting();
        for (int i = 0; i < qualityIndexMap.length; i++) {
            if (qualityIndexMap[i] == qs) {
                qualityList.setSelectedIndex(i);
                break;
            }
        }

        qualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setQualitySetting(qualityIndexMap[qualityList.getSelectedIndex()]);
            }
        });

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(qualityLabel);
        add(qualityList);
        add(Box.createRigidArea(new Dimension(0, 15)));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }
}
