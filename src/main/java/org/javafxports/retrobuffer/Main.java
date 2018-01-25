package org.javafxports.retrobuffer;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        SystemPropertiesConfig config = new SystemPropertiesConfig(System.getProperties());

        try {
            Retrobuffer.run(config);
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Failed to run Retrobuffer", t);
            System.exit(1);
        }

    }
}
