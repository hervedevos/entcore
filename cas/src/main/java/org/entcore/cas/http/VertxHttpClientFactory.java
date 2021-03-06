/*
 * Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.cas.http;

import fr.wseduc.cas.http.HttpClient;
import fr.wseduc.cas.http.HttpClientFactory;
import org.vertx.java.core.Vertx;

public class VertxHttpClientFactory implements HttpClientFactory {

	private final Vertx vertx;

	public VertxHttpClientFactory(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public HttpClient create(String host, int port, boolean ssl) {
		org.vertx.java.core.http.HttpClient httpClient = vertx.createHttpClient()
				.setHost(host)
				.setPort(port)
				.setSSL(ssl)
				.setKeepAlive(false);
		return new WrappedVertxHttpClient(httpClient);

	}
}
