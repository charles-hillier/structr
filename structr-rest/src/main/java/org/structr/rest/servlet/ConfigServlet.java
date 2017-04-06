/**
 * Copyright (C) 2010-2017 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Setting;
import org.structr.core.Services;
import org.structr.api.config.Settings;
import org.structr.api.config.SettingsGroup;
import org.structr.api.service.Service;
import org.structr.api.util.html.Attr;
import org.structr.api.util.html.Document;
import org.structr.api.util.html.Tag;
import org.structr.api.util.html.attr.Href;
import org.structr.api.util.html.attr.Rel;

/**
 *
 * @author Christian Morgner
 */
public class ConfigServlet extends HttpServlet {

	private static final Logger logger                     = LoggerFactory.getLogger(ConfigServlet.class);
	private static final Set<String> authenticatedSessions = new HashSet<>();
	private static final String ConfigUrl                  = "/structr/config";
	private static final String ConfigName                 = "structr.conf";

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		if (request.getParameter("reload") != null) {

			// reload data
			Settings.loadConfiguration(ConfigName);

			// redirect
			response.sendRedirect(ConfigUrl);

		} else if (request.getParameter("reset") != null) {

			final String key      = request.getParameter("reset");
			final Setting setting = Settings.getSetting(key);

			if (setting != null) {

				if (setting.isDynamic()) {

					// remove
					setting.unregister();

				} else {

					// reset to default
					setting.setValue(setting.getDefaultValue());
				}
			}

			// serialize settings
			Settings.storeConfiguration(ConfigName);

			// redirect
			response.sendRedirect(ConfigUrl);

		} else if (request.getParameter("start") != null) {

			final String serviceName = request.getParameter("start");
			if (serviceName != null && isAuthenticated(request)) {

				Services.getInstance().startService(serviceName);
			}

			// redirect
			response.sendRedirect(ConfigUrl + "#services");

		} else if (request.getParameter("stop") != null) {

			final String serviceName = request.getParameter("stop");
			if (serviceName != null && isAuthenticated(request)) {

				Services.getInstance().shutdownService(serviceName);
			}

			// redirect
			response.sendRedirect(ConfigUrl + "#services");

		} else {

			// no trailing semicolon so we dont trip MimeTypes.getContentTypeWithoutCharset
			response.setContentType("text/html; charset=utf-8");

			try (final PrintWriter writer = new PrintWriter(response.getWriter())) {

				if (isAuthenticated(request)) {

					final Document doc = createConfigDocument(request, writer);
					doc.render();

				} else {

					final Document doc = createLoginDocument(request, writer);
					doc.render();
				}

				writer.append("\n");    // useful newline
				writer.flush();

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}
		}
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		final String action = request.getParameter("action");
		if (action != null) {

			switch (action) {

				case "login":
					final String username = request.getParameter("username");
					final String password = request.getParameter("password");

					if ("superadmin".equals(username) && "sehrgeheim".equals(password)) {

						authenticateSession(request);
					}
					break;

				case "logout":
					invalidateSession(request);
					break;

			}

		} else if (isAuthenticated(request)) {

			// a configuration form was submitted
			for (final Entry<String, String[]> entry : request.getParameterMap().entrySet()) {

				final String value   = getFirstElement(entry.getValue());
				final String key     = entry.getKey();
				SettingsGroup parent = null;

				// skip internal group configuration parameter
				if (key.endsWith("._settings_group")) {
					continue;
				}

				Setting<?> setting = Settings.getSetting(key);
				if (setting == null) {

					// group specified?
					final String group = request.getParameter(key + "._settings_group");
					if (group != null) {

						parent = Settings.getGroup(group);
						if (parent == null) {

							// default to misc group
							parent = Settings.miscGroup;
						}

					} else {

						// fallback to misc group
						parent = Settings.miscGroup;
					}

					setting = Settings.createSettingForValue(parent, key, value);
				}

				// store new value
				setting.fromString(value);
			}

			// serialize settings
			Settings.storeConfiguration(ConfigName);
		}

		response.sendRedirect(ConfigUrl);
	}

	// ----- private methods -----
	private Document createConfigDocument(final HttpServletRequest request, final PrintWriter writer) {

		final Document doc = new Document(writer);
		final Tag body     = setupDocument(request, doc);
		final Tag form     = body.block("form").css("config-form");
		final Tag main     = form.block("div").id("main");
		final Tag tabs     = main.block("div").id("configTabs");
		final Tag menu     = tabs.block("ul").id("configTabsMenu");

		// configure form
		form.attr(new Attr("action", ConfigUrl), new Attr("method", "post"));

		for (final SettingsGroup group : Settings.getGroups()) {

			final String key  = group.getKey();
			final String name = group.getName();

			menu.block("li").block("a").id(key + "Menu").attr(new Attr("href", "#" + key)).block("span").text(name);

			final Tag container = tabs.block("div").css("tab-content").id(key);

			// let settings group render itself
			group.render(container);

			// stop floating
			container.block("div").attr(new Attr("style", "clear: both;"));
		}

		// add services tab
		menu.block("li").block("a").id("servicesMenu").attr(new Attr("href", "#services")).block("span").text("Services");

		final Services services = Services.getInstance();
		final Tag container     = tabs.block("div").css("tab-content").id("services");
		final Tag table         = container.block("table").id("services-table");
		final Tag header        = table.block("tr");

		header.block("th").text("Service Name");
		header.block("th").attr(new Attr("colspan", "2"));


		for (final String serviceClassName : services.getServices()) {

			final Class<Service> serviceClass = services.getServiceClassForName(serviceClassName);
			final boolean running             = serviceClass != null ? services.isReady(serviceClass) : false;

			final Tag row  = table.block("tr");

			row.block("td").text(serviceClassName);

			if (running) {

				row.block("td").block("button").attr(new Attr("type", "button"), new Attr("onclick", "window.location.href='" + ConfigUrl + "?stop=" + serviceClassName + "';")).text("Stop");
				row.block("td");

			} else {

				row.block("td");
				row.block("td").block("button").attr(new Attr("type", "button"), new Attr("onclick", "window.location.href='" + ConfigUrl + "?start=" + serviceClassName + "';")).text("Start");
			}
		}


		// stop floating
		container.block("div").attr(new Attr("style", "clear: both;"));

		// buttons
		final Tag buttons = form.block("div").css("buttons");

		buttons.block("button").attr(new Attr("type", "button")).id("new-entry-button").text("Add entry");
		buttons.block("button").attr(new Attr("type", "button"), new Attr("onclick", "window.location.href='" + ConfigUrl + "?reload';")).text("Reload configuration");
		buttons.empty("input").attr(new Attr("type", "submit"), new Attr("value", "Save to structr.conf"));

		body.block("script").text("$(function() { $('#configTabs').tabs({}); });");
		body.block("script").text("$('#new-entry-button').on('click', createNewEntry);");

		return doc;
	}

	private Document createLoginDocument(final HttpServletRequest request, final PrintWriter writer) {

		final Document doc = new Document(writer);
		final Tag body     = setupDocument(request, doc);

		final Tag loginBox = body.block("div").id("login").css("dialog").attr(new Attr("style", "display: block; margin: auto; margin-top: 200px;"));

		loginBox.block("i").attr(new Attr("title", "Structr Logo")).css("logo-login sprite sprite-structr_gray_100x27");
		loginBox.block("p").text("Welcome to the Structr Configuration Wizard.");

		final Tag form     = loginBox.block("form").attr(new Attr("action", ConfigUrl), new Attr("method", "post"));
		final Tag table    = form.block("table");

		final Tag row1     = table.block("tr");
		row1.block("td").block("label").attr(new Attr("for", "usernameField")).text("Username:");
		row1.block("td").empty("input").id("usernameField").attr(new Attr("type", "text"), new Attr("name", "username"));

		final Tag row2     = table.block("tr");
		row2.block("td").block("label").attr(new Attr("for", "passwordField")).text("Password:");
		row2.block("td").empty("input").id("passwordField").attr(new Attr("type", "password"), new Attr("name", "password"));

		final Tag row3     = table.block("tr");
		final Tag cell13   = row3.block("td").attr(new Attr("colspan", "2")).css("btn");
		final Tag button   = cell13.block("button").id("loginButton").attr(new Attr("name", "login"));

		button.block("i").css("sprite sprite-key");
		button.block("span").text(" Login");

		cell13.empty("input").attr(new Attr("type", "hidden"), new Attr("name", "action"), new Attr("value", "login"));

		return doc;
	}

	// ----- private methods -----
	private Tag setupDocument(final HttpServletRequest request, final Document doc) {

		final Tag head = doc.block("head");

		head.block("title").text("Welcome to Structr 2.1");
		head.empty("meta").attr(new Attr("http-equiv", "Content-Type"), new Attr("content", "text/html;charset=utf-8"));
		head.empty("meta").attr(new Attr("name", "viewport"), new Attr("content", "width=1024, user-scalable=yes"));
		head.empty("link").attr(new Rel("stylesheet"), new Href("/structr/css/main.css"));
		head.empty("link").attr(new Rel("stylesheet"), new Href("/structr/css/config.css"));
		head.empty("link").attr(new Rel("stylesheet"), new Href("/structr/css/lib/jquery-ui-1.10.3.custom.min.css"));
		head.empty("link").attr(new Rel("icon"), new Href("favicon.ico"), new Attr("type", "image/x-icon"));
		head.block("script").attr(new Attr("src", "/structr/js/lib/jquery-1.11.1.min.js"));
		head.block("script").attr(new Attr("src", "/structr/js/lib/jquery-ui-1.11.0.custom.min.js"));
		head.block("script").attr(new Attr("src", "/structr/js/config.js"));

		final Tag body = doc.block("body");
		final Tag header = body.block("div").id("header");

		header.block("i").attr(new Attr("class", "logo sprite sprite-structr-logo"));
		final Tag links = header.block("div").id("menu").css("menu").block("ul");

		if (isAuthenticated(request) && Services.getInstance().isConfigured()) {

			final Tag form = links.block("li").block("form").attr(new Attr("action", ConfigUrl), new Attr("method", "post"), new Attr("style", "display: none")).id("logout-form");

			form.block("input").attr(new Attr("type", "hidden"), new Attr("name", "action"), new Attr("value", "logout"));
			links.block("a").text("Logout").attr(new Attr("style", "cursor: pointer"), new Attr("onclick", "$('#logout-form').submit();"));
		}

		return body;
	}

	private boolean isAuthenticated(final HttpServletRequest request) {

		// only display login dialog if a configuration exists (i.e. this is NOT the first run of Structr)
		if (!Services.getInstance().isConfigured()) {
			return true;
		}

		final HttpSession session = request.getSession();
		if (session != null) {

			final String sessionId = session.getId();
			if (sessionId != null) {

				return authenticatedSessions.contains(sessionId);

			} else {

				logger.warn("Cannot check HTTP session without session ID, ignoring.");
			}

		} else {

			logger.warn("Cannot check HTTP request, no session.");
		}

		return false;
	}

	private void authenticateSession(final HttpServletRequest request) {

		final HttpSession session = request.getSession();
		if (session != null) {

			final String sessionId = session.getId();
			if (sessionId != null) {

				authenticatedSessions.add(sessionId);

			} else {

				logger.warn("Cannot authenticate HTTP session without session ID, ignoring.");
			}

		} else {

			logger.warn("Cannot authenticate HTTP request, no session.");
		}
	}

	private void invalidateSession(final HttpServletRequest request) {

		final HttpSession session = request.getSession();
		if (session != null) {

			final String sessionId = session.getId();
			if (sessionId != null) {

				authenticatedSessions.remove(sessionId);

			} else {

				logger.warn("Cannot invalidate HTTP session without session ID, ignoring.");
			}

		} else {

			logger.warn("Cannot invalidate HTTP request, no session.");
		}
	}

	private String getFirstElement(final String[] values) {

		if (values != null && values.length == 1) {

			return values[0];
		}

		return null;
	}
}