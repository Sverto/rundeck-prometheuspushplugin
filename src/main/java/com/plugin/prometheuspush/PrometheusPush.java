package com.plugin.prometheuspush;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;

import org.apache.commons.lang.exception.ExceptionUtils;

import java.net.URL;
import java.util.*;


@Plugin(service="Notification", name="prometheuspush")
@PluginDescription(title="Prometheus Push Plugin", description="- Push Rundeck job execution results to Prometheus using the notification handler")
public class PrometheusPush implements NotificationPlugin
{
    // Properties
    @PluginProperty(name = "pushgateway_endpoints", title = "Prometheus Pushgateway endpoints", description = "Example: pushgateway1:9091,pushgateway2:9091", defaultValue = "${globals.prometheus_pushgateway_endpoints}", required = true)
    private String pushgatewayEndpoints;
    
    @PluginProperty(name = "metric_prefix", title = "Prometheus Metric Prefix", description = "Example: app", defaultValue = "${globals.prometheus_metric_prefix}", required = true)
    private String metricPrefix;
    
    private static final String LOG_PREFIX = "PrometheusPush Notification: ";
    
    
    /**
     * Called by Rundeck after job execution
     */
    @SuppressWarnings({"rawtypes", "unchecked"})  // Rundeck uses rawtypes...
    public boolean postNotification(String trigger, Map executionData, Map configuration)
    {
        // Only run with supported trigger types
        if (trigger != "success" && trigger != "failure")
        {
            System.out.printf("%sUnsupported trigger type: %s\n", LOG_PREFIX, trigger);
            return false;
        }
        // Properties not properly loaded
        if (pushgatewayEndpoints.startsWith("${") || metricPrefix.startsWith("${"))
        {
            System.out.printf("%sAttempted to run plugin while required properties were not loaded (on startup) or not set. Push failed.", LOG_PREFIX);
            return false;
        }
        
        // Push metrics to given Pushgateway endpoints
        for (String endpoint : pushgatewayEndpoints.toLowerCase().split(","))
        {
            try
            {
                // Parse label and metric values
                PrometheusMetric metric = new PrometheusMetric(trigger, executionData, configuration, metricPrefix);
                // Get command to be executed containing all labels and metrics
                String cmd = metric.toCmd(new URL("http://" + endpoint));
                // Push metrics using command
                Shell.command(cmd, "/tmp");
            }
            catch (Exception ex)
            {
                System.out.printf("%sException -> \n %s\n\n", LOG_PREFIX, ExceptionUtils.getStackTrace(ex));
                return false;
            }
        }
        return true;
    }
}