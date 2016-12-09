package com.github.czgov.isds.types;

import org.apache.camel.Converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

import cz.abclinuxu.datoveschranky.common.entities.MessageState;

/**
 * Created by jludvice on 8.10.16.
 */
@Converter
public class SupportConverters {

	public static final Logger log = LoggerFactory.getLogger(SupportConverters.class);

	/**
	 * Support converter to convert String with file path to {@link Path} instance.
	 * @param path string with filesystem path
	 * @return corresponding Path instance
	 */
	@Converter
	public static Path toPath(String path) {
		log.trace("Converting string {} to path.", path);
		return Paths.get(path);
	}

	/**
	 * Convert comma separated list of values into Java ISDS filter object.
	 * Allowed input is {@link MessageState} values, comma and exclamation mark.
	 * <br>
	 * Example:
	 * <ul>
	 *     <li>{@code !read} will filter all unread messages</li>
	 *     <li>{@code delivered, delivered_by_login} will filter messages which were already downloaded or were considered as delivered because user logged in</li>
	 * </ul>
	 * @param query case insensitive expression to be converted
	 * @return Java ISDS specific filter - set of message states we wan't to fetch or {@code null} if query is null or empty
	 */
	@Converter
	public static EnumSet<MessageState> parse(String query) {
		if (query == null || query.isEmpty() || query.toUpperCase().equals("NULL")) {
			return EnumSet.allOf(MessageState.class);
		}
		log.trace("Filtering query '{}'", query);
		EnumSet<MessageState> filter = EnumSet.noneOf(MessageState.class);
		EnumSet<MessageState> exclude = EnumSet.noneOf(MessageState.class);

		for (String item : query.toUpperCase().split(", *")) {
			EnumSet<MessageState> list = item.startsWith("!") ? exclude : filter;
			try {
				list.add(MessageState.valueOf(item.replace("!", "")));
			} catch (Exception e) {
				log.warn("Failed to match value '{}' into MessageState entry.", item, e);
			}
		}
		if (exclude.size() != 0) {
			filter.addAll(EnumSet.complementOf(exclude));
		}
		log.trace("Query parsed as '{}'", filter);
		return filter;
	}

}
