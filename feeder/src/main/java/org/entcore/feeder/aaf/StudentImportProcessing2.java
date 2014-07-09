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

import org.entcore.feeder.dictionary.structures.DefaultProfiles;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class StudentImportProcessing2 extends StudentImportProcessing {

	protected StudentImportProcessing2(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	public void start(final Handler<Message<JsonObject>> handler) {
		parse(handler, null);
	}

	@Override
	protected void preCommit() {
		importer.linkRelativeToStructure(DefaultProfiles.RELATIVE_PROFILE_EXTERNAL_ID);
		importer.linkRelativeToClass(DefaultProfiles.RELATIVE_PROFILE_EXTERNAL_ID);
	}

	@Override
	public void process(JsonObject object) {
		String[][] classes = createClasses(object.getArray("classes"));
		String[][] groups = createGroups(object.getArray("groups"));
		JsonArray relative = parseRelativeField(object.getArray("relative"));
		importer.createOrUpdateStudent(object, DefaultProfiles.STUDENT_PROFILE_EXTERNAL_ID,
				null, null, classes, groups, relative, false, true);
	}

}
