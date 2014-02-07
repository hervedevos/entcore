package org.entcore.directory.services.impl;

import edu.one.core.infra.Either;
import edu.one.core.infra.Utils;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.directory.services.UserBookService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Collections;

import static org.entcore.common.neo4j.Neo4jResult.fullNodeMergeHandler;
import static org.entcore.common.neo4j.Neo4jUtils.nodeSetPropertiesFromJson;

public class DefaultUserBookService implements UserBookService {

	private final Neo neo;

	public DefaultUserBookService(Neo neo) {
		this.neo = neo;
	}

	@Override
	public void update(String userId, JsonObject userBook, final Handler<Either<String, JsonObject>> result) {
		JsonObject u = Utils.validAndGet(userBook, UPDATE_USERBOOK_FIELDS, Collections.<String>emptyList());
		if (Utils.defaultValidationError(u, result, userId)) return;
		StatementsBuilder b = new StatementsBuilder();
		String query =
				"MATCH (u:`User` { id : {id}})-[:USERBOOK]->(ub:UserBook) " +
				"SET " + nodeSetPropertiesFromJson("ub", u);
		if (u.size() > 0) {
			b.add(query, u.putString("id", userId));
		}
		String q2 =
				"MATCH (u:`User` { id : {id}})-[:USERBOOK]->(ub:UserBook)" +
				"-[:PUBLIC|PRIVE]->(h:`Hobby` { category : {category}}) " +
				"SET h.values = {values} ";
		JsonArray hobbies = userBook.getArray("hobbies");
		if (hobbies != null) {
			for (Object o : hobbies) {
				if (!(o instanceof JsonObject)) continue;
				JsonObject j = (JsonObject) o;
				b.add(q2, j.putString("id", userId));
			}
		}
		neo.executeTransaction(b.build(), null, true, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> r) {
				if ("ok".equals(r.body().getString("status"))) {
					result.handle(new Either.Right<String, JsonObject>(new JsonObject()));
				} else {
					result.handle(new Either.Left<String, JsonObject>(
							r.body().getString("message", "update.error")));
				}
			}
		});
	}

	@Override
	public void get(String userId, Handler<Either<String, JsonObject>> result) {
		String query =
				"MATCH (u:`User` { id : {id}})-[:USERBOOK]->(ub: UserBook)" +
				"OPTIONAL MATCH ub-[:PUBLIC|PRIVE]->(h:Hobby) " +
				"RETURN ub, COLLECT(h) as hobbies ";
		neo.execute(query, new JsonObject().putString("id", userId),
				fullNodeMergeHandler("ub", result, "hobbies"));
	}

}
