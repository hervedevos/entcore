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

package org.entcore.directory.services.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.collections.Joiner;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.entcore.directory.services.GroupService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;
import static org.entcore.common.user.DefaultFunctions.ADMIN_LOCAL;
import static org.entcore.common.user.DefaultFunctions.SUPER_ADMIN;

public class DefaultGroupService implements GroupService {

	private final Neo4j neo = Neo4j.getInstance();

	@Override
	public void listAdmin(String structureId, UserInfos userInfos, JsonArray expectedTypes,
			Handler<Either<String, JsonArray>> results) {
		if (userInfos == null) {
			results.handle(new Either.Left<String, JsonArray>("invalid.user"));
			return;
		}
		String condition;
		if (expectedTypes != null && expectedTypes.size() > 0) {
			condition = "WHERE (g:" + Joiner.on(" OR g:").join(expectedTypes) + ") ";
		} else {
			condition = "WHERE g:Group ";
		}

		JsonObject params = new JsonObject();
		if (!userInfos.getFunctions().containsKey(SUPER_ADMIN) &&
				!userInfos.getFunctions().containsKey(ADMIN_LOCAL)) {
			results.handle(new Either.Left<String, JsonArray>("forbidden"));
			return;
		} else if (userInfos.getFunctions().containsKey(ADMIN_LOCAL)) {
			UserInfos.Function f = userInfos.getFunctions().get(ADMIN_LOCAL);
			List<String> structuresIds = f.getStructures();
			if (structuresIds != null && !structuresIds.isEmpty()) {
				condition += "AND s.id IN {structures} ";
				params.putArray("structures", new JsonArray(structuresIds.toArray()));
			}
		}

		if (structureId != null && !structureId.trim().isEmpty()) {
			condition += " AND s.id = {structure} ";
			params.putString("structure", structureId);
		}
		String query =
				"MATCH (s:Structure)<-[:DEPENDS*1..2]-(g) " + condition +
				"RETURN g.id as id, g.name as name, g.displayName as displayName, " +
				"HEAD(filter(x IN labels(g) WHERE x <> 'Visible' AND x <> 'Group')) as type ";
		neo.execute(query, params, validResultHandler(results));
	}

}