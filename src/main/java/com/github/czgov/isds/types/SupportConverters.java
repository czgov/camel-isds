package com.github.czgov.isds.types;

import org.apache.camel.Converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by jludvice on 8.10.16.
 */
@Converter
public class SupportConverters {

	public static final Logger log = LoggerFactory.getLogger(SupportConverters.class);

	/**
	 * Support converter to convert String with file path to {@link Path} instance.
	 * @param path string with filesystem path
	 * @return coresponding Path instance
	 */
	@Converter
	public static Path toPath(String path) {
		log.trace("Converting string {} to path.", path);
		return Paths.get(path);
	}
}
