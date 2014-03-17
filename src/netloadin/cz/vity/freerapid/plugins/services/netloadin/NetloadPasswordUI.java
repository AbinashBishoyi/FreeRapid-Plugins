package cz.vity.freerapid.plugins.services.netloadin;

/**
 * @author Ludek Zika
 */
class NetloadPasswordUI extends javax.swing.JPanel {

    /**
     * Creates new form NetloadPass
     */
    public NetloadPasswordUI() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        passField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel1.setText("password:");
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap(46, Short.MAX_VALUE)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(passField, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(49, 49, 49))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(passField, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel1))
                        .addContainerGap(24, Short.MAX_VALUE))
        );
    }

    // Variables declaration - do not modify
    private javax.swing.JLabel jLabel1;
    private javax.swing.JTextField passField;
    // End of variables declaration

    public String getPassword() {
        return passField.getText();

    }

}
