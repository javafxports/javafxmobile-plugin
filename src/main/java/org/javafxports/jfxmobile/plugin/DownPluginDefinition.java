/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2018, Gluon Software
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.javafxports.jfxmobile.plugin;

import org.gradle.api.GradleException;
import org.gradle.api.Named;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DownPluginDefinition implements Named {

    private static final Logger LOGGER = Logging.getLogger(DownConfiguration.class.getName());

    private String name;
    private DownPlugin plugin;

    private String version;

    public DownPluginDefinition(String name) {
        this.name = name;

        try {
            this.plugin = DownPlugin.valueOf(name.replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOGGER.log(LogLevel.ERROR, "Could not determine Charm Down Plugin for name '" + name + "'. The following plugins are available: " + Stream.of(DownPlugin.values()).map(DownPlugin::getPluginName).collect(Collectors.joining(", ")));
            throw new GradleException("Invalid name for Charm Down plugin: " + name, e);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public DownPlugin getPlugin() {
        return plugin;
    }

    public String getVersion() {
        return version;
    }

    public void version(String version) {
        this.version = version;
    }

    public void setVersion(String version) {
        version(version);
    }

    public boolean isConfigurationSupported(Configuration configuration) {
        switch (configuration.getName()) {
            case "androidRuntime":
                return plugin.isAndroidSupported();
            case "iosRuntime":
                return plugin.isIosSupported();
            case "desktopRuntime":
                return plugin.isDesktopSupported();
            case "embeddedRuntime":
                return plugin.isDesktopSupported();
            case "compile":
                return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DownPluginDefinition that = (DownPluginDefinition) o;

        return name.equals(that.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public enum DownPlugin {
        ACCELEROMETER,
        BARCODE_SCAN,
        BATTERY,
        BLE,
        BROWSER,
        CACHE(true /* desktopSupported */),
        COMPASS,
        CONNECTIVITY,
        DEVICE,
        DIALER,
        DISPLAY(true /* desktopSupported */),
        IN_APP_BILLING,
        LIFECYCLE(true /* desktopSupported */),
        LOCAL_NOTIFICATIONS,
        MAGNETOMETER,
        ORIENTATION,
        PICTURES,
        POSITION,
        PUSH_NOTIFICATIONS,
        RUNTIME_ARGS(true /* desktopSupported */),
        SETTINGS(true /* desktopSupported */),
        STATUSBAR,
        STORAGE(true /* desktopSupported */),
        VIBRATION;

        private boolean androidSupported = true;
        private boolean iosSupported = true;
        private boolean desktopSupported = false;

        DownPlugin() {
        }

        DownPlugin(boolean desktopSupported) {
            this.desktopSupported = desktopSupported;
        }

        public String getPluginName() {
            return name().replace('_', '-').toLowerCase(Locale.ROOT);
        }

        public boolean isAndroidSupported() {
            return androidSupported;
        }

        public boolean isIosSupported() {
            return iosSupported;
        }

        public boolean isDesktopSupported() {
            return desktopSupported;
        }
    }
}
