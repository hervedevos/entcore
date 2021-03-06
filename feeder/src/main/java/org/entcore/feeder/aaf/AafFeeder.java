/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.feeder.aaf;

import org.entcore.feeder.Feed;
import org.entcore.feeder.dictionary.structures.Importer;
import org.entcore.feeder.utils.ResultMessage;
import org.entcore.feeder.utils.TransactionManager;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class AafFeeder implements Feed {

	private static final Logger log = LoggerFactory.getLogger(AafFeeder.class);
	private final Vertx vertx;
	private final String path;
	private final boolean aafPlugin;

	public AafFeeder(Vertx vertx, String path, boolean aafPlugin) {
		this.vertx = vertx;
		this.path = path;
		this.aafPlugin = aafPlugin;
	}

	@Override
	public void launch(final Importer importer, final Handler<Message<JsonObject>> handler) throws Exception {
		if (importer.isFirstImport() && aafPlugin) {
			TransactionManager.getInstance().getNeo4j().unmanagedExtension("post", "/aaf/import",
					new JsonObject().putString("path", path).encode(), new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if ("ok".equals(event.body().getString("status"))) {
								log.info("Indexing...");
								importer.reinitTransaction();
								importer.profileConstraints();
								importer.functionConstraints();
								importer.structureConstraints();
								importer.fieldOfStudyConstraints();
								importer.moduleConstraints();
								importer.userConstraints();
								importer.classConstraints();
								importer.groupConstraints();
								importer.persist(new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> message) {
										if (handler != null) {
											handler.handle(message);
										}
									}
								});
							} else {
								if (handler != null) {
									handler.handle(new ResultMessage().error(event.body().getString("message")));
								}
							}
						}
					});
		} else {
			new StructureImportProcessing(path,vertx).start(handler);
		}
	}

	@Override
	public String getSource() {
		return "AAF";
	}

}
