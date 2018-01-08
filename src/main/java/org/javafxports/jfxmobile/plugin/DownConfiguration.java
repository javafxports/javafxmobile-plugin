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

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class DownConfiguration {

    private static final String DEPENDENCY_GROUP = "com.gluonhq";
    private static final String DEPENDENCY_NAME_PREFIX = "charm-down-plugin-";
    private static final Map<String, String> DEPENDENCY_NAME_SUFFIXES = new HashMap<>();
    static {
        DEPENDENCY_NAME_SUFFIXES.put("compile", "");
        DEPENDENCY_NAME_SUFFIXES.put("androidRuntime", "-android");
        DEPENDENCY_NAME_SUFFIXES.put("iosRuntime", "-ios");
        DEPENDENCY_NAME_SUFFIXES.put("desktopRuntime", "-desktop");
        DEPENDENCY_NAME_SUFFIXES.put("embeddedRuntime", "-desktop");
    }

    private Project project;

    private String version = "3.3.0";
    private NamedDomainObjectContainer<DownPluginDefinition> plugins;

    @Inject
    public DownConfiguration(Project project) {
        this.project = project;
        this.plugins = project.container(DownPluginDefinition.class);
    }

    public void version(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Collection<DownPluginDefinition> getPlugins() {
        return plugins;
    }

    public void plugins(String... plugins) {
        if (plugins != null) {
            for (String plugin : plugins) {
                this.plugins.create(plugin);
            }
        }
    }

    /**
     * Configures down plugins.
     */
    public void plugins(Action<? super NamedDomainObjectContainer<DownPluginDefinition>> action) {
        action.execute(plugins);
    }

    /**
     * Add dependencies to the specified configuration. Only dependencies to plugins that support the provided
     * configuration will be included.
     *
     * @param configuration the configuration where the plugin dependencies are added to
     */
    public void applyConfiguration(Configuration configuration) {
        if (plugins != null) {
            plugins.stream()
                    .filter(pluginDefinition -> pluginDefinition.isConfigurationSupported(configuration))
                    .forEach(pluginDefinition -> project.getDependencies().add(configuration.getName(), generateDependencyNotation(configuration, pluginDefinition)));
        }
    }

    private Object generateDependencyNotation(Configuration configuration, DownPluginDefinition pluginDefinition) {
        Map<String, String> dependencyNotationMap = new HashMap<>();
        dependencyNotationMap.put("group", DEPENDENCY_GROUP);
        dependencyNotationMap.put("name", getDependencyName(configuration, pluginDefinition));
        dependencyNotationMap.put("version", getDependencyVersion(pluginDefinition));

        project.getLogger().info("Adding dependency for {} in configuration {}: {}", pluginDefinition.getPlugin().getPluginName(), configuration.getName(), dependencyNotationMap);
        return dependencyNotationMap;
    }

    private String getDependencyName(Configuration configuration, DownPluginDefinition pluginDefinition) {
        return DEPENDENCY_NAME_PREFIX + pluginDefinition.getName() + DEPENDENCY_NAME_SUFFIXES.get(configuration.getName());
    }

    private String getDependencyVersion(DownPluginDefinition pluginDefinition) {
        return pluginDefinition.getVersion() == null ? version : pluginDefinition.getVersion();
    }
}
