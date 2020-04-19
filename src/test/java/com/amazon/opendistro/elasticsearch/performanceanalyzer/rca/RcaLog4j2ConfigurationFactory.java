package com.amazon.opendistro.elasticsearch.performanceanalyzer.rca;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.plugins.Plugin;


@Plugin(name = "RcaLog4j2ConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(10)
public class RcaLog4j2ConfigurationFactory extends ConfigurationFactory {
    static Configuration createConfiguration(final String name, ConfigurationBuilder<BuiltConfiguration> builder) {
        builder.setConfigurationName(name);
        builder.setStatusLevel(Level.INFO);
        AppenderComponentBuilder appenderBuilder = builder.newAppender("STDOUT", "Console");
        appenderBuilder.add(builder.newLayout("PatternLayout")
                .addAttribute("pattern", "%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"));
        builder.add(appenderBuilder);
        String suffix = "/logs";
        String prefix = System.getenv("GITHUB_WORKSPACE");
        if (prefix == null) {
            prefix = "/tmp";
        }
        Path logFileDir = Paths.get(prefix, suffix);
        appenderBuilder = builder.newAppender("PerformanceAnalyzerLog", "File")
                .addAttribute("fileName", Paths.get(logFileDir.toString(), "PerformanceAnalyzer.log").toString())
                .addAttribute("immediateFlush", true)
                .addAttribute("append", true)
                .addAttribute("filePermissions", "rwxrwxrwx");
        builder.add(appenderBuilder);
        appenderBuilder = builder.newAppender("StatsLog", "File")
                .addAttribute("fileName", Paths.get(logFileDir.toString(), "performance_analyzer_agent_stats.log").toString())
                .addAttribute("immediateFlush", true)
                .addAttribute("append", true)
                .addAttribute("filePermissions", "rwxrwxrwx");
        builder.add(appenderBuilder);
        builder.add(builder.newRootLogger(Level.ALL).add(builder.newAppenderRef("STDOUT"))
                .add(builder.newAppenderRef("PerformanceAnalyzerLog")));
        builder.add(builder.newLogger("stats_log", Level.DEBUG)
                .add(builder.newAppenderRef("StatsLog"))
                .addAttribute("additivity", false));
        return builder.build();
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
        return getConfiguration(loggerContext, source.toString(), null);
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final String name, final URI configLocation) {
        ConfigurationBuilder<BuiltConfiguration> builder = newConfigurationBuilder();
        return createConfiguration(name, builder);
    }

    @Override
    protected String[] getSupportedTypes() {
        return new String[] {"*"};
    }
}

