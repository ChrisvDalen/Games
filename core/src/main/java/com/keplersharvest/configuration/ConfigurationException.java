package com.keplersharvest.configuration;

/** Thrown when shipped content is missing, malformed or internally inconsistent. */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
