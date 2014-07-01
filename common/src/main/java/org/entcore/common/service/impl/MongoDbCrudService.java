/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.common.service.impl;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.CrudService;
import org.entcore.common.service.VisibilityFilter;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static org.entcore.common.mongodb.MongoDbResult.*;


public class MongoDbCrudService implements CrudService {

	private MongoDb mongo;
	private String collection;

	public MongoDbCrudService(String collection) {
		this.collection = collection;
		this.mongo = MongoDb.getInstance();
	}

	@Override
	public void create(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler) {
		JsonObject now = MongoDb.now();
		data.putObject("owner", new JsonObject()
				.putString("userId", user.getUserId())
				.putString("displayName", user.getUsername())
		).putObject("created", now).putObject("modified", now);
		mongo.save(collection, data, validActionResultHandler(handler));
	}

	@Override
	public void retrieve(String id, UserInfos user, Handler<Either<String, JsonObject>> handler) {
		QueryBuilder builder = QueryBuilder.start("_id").is(id);
		if (user == null) {
			builder.put("visibility").is(VisibilityFilter.PUBLIC.name());
		}
		mongo.findOne(collection,  MongoQueryBuilder.build(builder), validResultHandler(handler));
	}

	@Override
	public void update(String id, JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler) {
		QueryBuilder query = QueryBuilder.start("_id").is(id);
		MongoUpdateBuilder modifier = new MongoUpdateBuilder();
		for (String attr: data.getFieldNames()) {
			modifier.set(attr, data.getValue(attr));
		}
		modifier.set("modified", MongoDb.now());
		mongo.update(collection, MongoQueryBuilder.build(query),
				modifier.build(), validActionResultHandler(handler));
	}

	@Override
	public void delete(String id, UserInfos user, Handler<Either<String, JsonObject>> handler) {
		QueryBuilder q = QueryBuilder.start("_id").is(id);
		mongo.delete(collection, MongoQueryBuilder.build(q), validActionResultHandler(handler));
	}

	@Override
	public void list(VisibilityFilter filter, UserInfos user, Handler<Either<String, JsonArray>> handler) {
		QueryBuilder query;
		if (user != null) {
			List<DBObject> groups = new ArrayList<>();
			groups.add(QueryBuilder.start("userId").is(user.getUserId()).get());
			for (String gpId: user.getProfilGroupsIds()) {
				groups.add(QueryBuilder.start("groupId").is(gpId).get());
			}
			switch (filter) {
				case OWNER:
					query = QueryBuilder.start("owner.userId").is(user.getUserId());
					break;
				case OWNER_AND_SHARED:
					query = new QueryBuilder().or(
							QueryBuilder.start("owner.userId").is(user.getUserId()).get(),
							QueryBuilder.start("shared").elemMatch(
									new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()
							).get());
					break;
				case SHARED:
					query = QueryBuilder.start("shared").elemMatch(
									new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get());
					break;
				case PROTECTED:
					query = QueryBuilder.start("visibility").is(VisibilityFilter.PROTECTED.name());
					break;
				case PUBLIC:
					query = QueryBuilder.start("visibility").is(VisibilityFilter.PUBLIC.name());
					break;
				default:
					query = new QueryBuilder().or(
							QueryBuilder.start("visibility").is(VisibilityFilter.PUBLIC.name()).get(),
							QueryBuilder.start("visibility").is(VisibilityFilter.PROTECTED.name()).get(),
							QueryBuilder.start("owner.userId").is(user.getUserId()).get(),
							QueryBuilder.start("shared").elemMatch(
									new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()
							).get());
					break;
			}
		} else {
			query = QueryBuilder.start("visibility").is(VisibilityFilter.PUBLIC.name());
		}
		JsonObject sort = new JsonObject().putNumber("modified", -1);
		mongo.find(collection, MongoQueryBuilder.build(query), sort, null, validResultsHandler(handler));
	}

}