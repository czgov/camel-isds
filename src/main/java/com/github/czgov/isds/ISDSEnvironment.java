package com.github.czgov.isds;

import cz.abclinuxu.datoveschranky.common.Config;
import cz.abclinuxu.datoveschranky.common.DataBoxEnvironment;

/**
 * Created by jludvice on 7/29/16.
 */
public enum ISDSEnvironment {

    PRODUCTION("production"),

    TEST("test");

    private final Config config;

    ISDSEnvironment(String env) {
        switch (env) {
            case "production":
                config = new Config(DataBoxEnvironment.PRODUCTION);
                break;
            case "test":
                config = new Config(DataBoxEnvironment.TEST);
                break;
            default:
                throw new RuntimeException("Supported environments are 'production' or 'test'. Not '" + env + '.');
        }
    }

    /**
     * Wrap possible IllegalArgumentException with better explanation.
     *
     * @param env ISDS environmnent. Supported are {@code production} or {@code test}. Case insensitive.
     * @return ISDSEnvironment or throw RuntimeException.
     */
    public static ISDSEnvironment fromString(String env) {
        try {
            return ISDSEnvironment.valueOf(String.valueOf(env).trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Supported environments are 'production' or 'test'. Not '" + env + '.');
        }
    }

    public Config getConfig() {
        return config;
    }
}
