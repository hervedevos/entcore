package edu.one.core.directory;

import edu.one.core.datadictionary.dictionary.DefaultDictionary;
import edu.one.core.datadictionary.dictionary.Dictionary;
import edu.one.core.infra.Controller;
import edu.one.core.infra.Neo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;


public class Directory extends Controller {
	
	JsonObject admin;
	Neo neo;
	Dictionary d;

	public Directory() {
	}
	@Override
	public void start() {
		super.start();
		neo = new Neo(vertx.eventBus(),log);
		d = new DefaultDictionary(vertx, container, "../edu.one.core~dataDictionary~0.1.0-SNAPSHOT/aaf-dictionary.json");
		admin = new JsonObject(vertx.fileSystem().readFileSync("super-admin.json").toString());

		neo.send("START n=node(*) "
			+ "CREATE (m {id:'" + admin.getString("id") + "', "
			+ "type:'" + admin.getString("type") + "',"
			+ "ENTPersonNom:'"+ admin.getString("firstname") +"', "
			+ "ENTPersonPrenom:'"+ admin.getString("lastname") +"', "
			+ "ENTPersonMotDePasse:'"+ admin.getString("password") +"'})");

		rm.get("/admin", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request, new JsonObject());
			}
		});

		rm.get("/api/ecole", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				neo.send("START n=node(*) WHERE has(n.type) "
						+ "AND n.type='ETABEDUCNAT' "
						+ "RETURN distinct n.ENTStructureNomCourant, n.id", request.response());
			}
		});

		rm.get("/api/groupes", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				neo.send("START n=node(*) WHERE has(n.type) "
						+ "AND n.type='GROUPE' "
						+ "RETURN distinct n.ENTGroupeNom, n.id, n.ENTPeople", request.response());
			}
		});

		rm.get("/api/classes", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String schoolId = request.params().get("id");
				neo.send("START n=node(*) MATCH n<--m WHERE has(n.id) "
						+ "AND n.id='" + schoolId + "' "
						+ "AND has(m.type) AND m.type='CLASSE' "
						+ "RETURN distinct m.ENTGroupeNom, m.id, n.id", request.response());
			}
		});

		rm.get("/api/personnes", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String classId = request.params().get("id").replaceAll("-", " ").replaceAll("_", "\\$");
				neo.send("START n=node(*) MATCH n<--m WHERE has(n.id) "
						+ "AND n.id='" + classId + "' "
						+ "AND has(m.type) AND (m.type='ELEVE' OR m.type='PERSRELELEVE' OR m.type='PERSEDUCNAT') "
						+ "RETURN distinct m.id,m.ENTPersonNom,m.ENTPersonPrenom, n.id", request.response());
			}
		});

		rm.get("/api/membres", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String[] people = request.params().get("data").split("-");
				String neoRequest = "START n=node(*) where has(n.id) and (";
				for (String id : people) {
					neoRequest += "n.id='" + id + "' or ";
				}
				neoRequest += "n.id='') return distinct n.id, n.ENTPersonNom, n.ENTPersonPrenom";
				neo.send(neoRequest, request.response());
			}
		});

		rm.get("/api/details", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String personId = request.params().get("id");
				neo.send("START n=node(*) WHERE has(n.id) "
						+ "AND n.id='" + personId + "' "
						+ "RETURN distinct n.ENTPersonNom, n.ENTPersonPrenom, n.ENTPersonAdresse", request.response());
			}
		});

		rm.get("/api/enseignants", new Handler<HttpServerRequest>(){
			@Override
			public void handle(HttpServerRequest request) {
				String classId = request.params().get("id").replaceAll("-", " ").replaceAll("_", "\\$");
				neo.send("START n=node(*), m=node(*) WHERE has(m.type) "
						+ "AND m.type='PERSEDUCNAT' AND has(n.id) "
						+ "AND n.id='" + classId + "' "
						+ "RETURN distinct m.id, m.ENTPersonNom, m.ENTPersonPrenom, n.id", request.response());
			}
		});

		rm.get("/api/link", new Handler<HttpServerRequest>(){
			@Override
			public void handle(HttpServerRequest request) {
				String classId = request.params().get("class").replaceAll("-", " ").replaceAll("_", "\\$");
				String userId = request.params().get("id");
				neo.send("START n=node(*), m=node(*) WHERE has(m.id) AND has(n.id)"
						+ "AND m.id='" + userId + "' AND n.id='" + classId + "' "
						+ "CREATE m-[:APPARTIENT]->n", request.response());
			}
		});

		rm.get("/api/create-user", new Handler<HttpServerRequest>(){
			@Override
			public void handle(HttpServerRequest request) {
				JsonObject obj = new JsonObject();
				Map<String,Boolean> params = d.validateFields(request.params());
				if (!params.values().contains(false)){
					trace.info("Creating new User : " + request.params().get("ENTPersonNom") + " " + request.params().get("ENTPersonPrenom"));
					obj.putString("id", request.params().get("ENTPersonIdentifiant"))
							.putString("nom", request.params().get("ENTPersonNom"))
							.putString("prenom", request.params().get("ENTPersonPrenom"))
							.putString("login", request.params().get("ENTPersonNom") + "." + request.params().get("ENTPersonPrenom"))
							.putString("classe", "4400000002_ORDINAIRE_CM2deMmeRousseau")
							.putString("type", request.params().get("ENTPersonProfils"))
							.putString("password", "dummypass");
					vertx.eventBus().send(config.getString("wp-connector.address"), obj, new Handler<Message>() {
						public void handle(Message event) {
							container.logger().info("MESSAGE : " + event.body());
						}
					});
					neo.send("START n=node(*) WHERE has(n.ENTGroupeNom) "
							+ "AND n.ENTGroupeNom='" + request.params().get("ENTPersonStructRattach").replaceAll("-", " ") + "' "
							+ "CREATE (m {id:'" + request.params().get("ENTPersonIdentifiant") + "', " 
							+ "type:'" + request.params().get("ENTPersonProfils") + "',"
							+ "ENTPersonNom:'"+request.params().get("ENTPersonNom") +"', "
							+ "ENTPersonPrenom:'"+request.params().get("ENTPersonPrenom") +"', "
							+ "ENTPersonDateNaissance:'"+request.params().get("ENTPersonDateNaissance") +"'}), "
							+ "m-[:APPARTIENT]->n ", request.response());
				} else {
					obj.putString("result", "error");
					for (Map.Entry<String, Boolean> entry : params.entrySet()) {
						if (!entry.getValue()){
							obj.putBoolean(entry.getKey(), entry.getValue());
						}
					}
					renderJson(request, obj);
				}
			}
		});

		rm.get("/api/create-admin", new Handler<HttpServerRequest>(){
			@Override
			public void handle(HttpServerRequest request) {
				String start = "";
				String conditions= "";
				String creation = "";
				for (Map.Entry<String, String> entry : request.params()) {
					if (!entry.getKey().startsWith("ENT")){
						start += entry.getKey()+"=node(*), ";
						conditions += "has("+entry.getKey()+".ENTStructureNomCourant) AND "
								+entry.getKey()+".ENTStructureNomCourant='"+entry.getValue() + "' AND ";
						creation += "m-[:ADMINISTRE]->" + entry.getKey() + ", ";
					}
				}
				if (request.params().get("ENTPerson").equals("none")){
					JsonObject obj = new JsonObject();
					//TODO : Send new user to WP (with multi groups attribute)
					neo.send("START " + start.substring(0,start.length()-2) + " WHERE "+ conditions.substring(0, conditions.length()-4)
						+ "CREATE (m {id:'" + request.params().get("ENTAdminId") + "', "
						+ "type:'CORRESPONDANT',"
						+ "ENTPersonNom:'"+request.params().get("ENTAdminNom") +"', "
						+ "ENTPersonPrenom:'"+request.params().get("ENTAdminPrenom") +"', "
						+ "ENTPersonDateNaissance:'"+request.params().get("ENTAdminBirthdate") +"'}), "
						+ creation.substring(0, creation.length()-2), request.response());
				} else {
					JsonObject obj = new JsonObject();
					//TODO : Send link user to groups to WP Connector
					neo.send("START m=node(*), "+ start.substring(0,start.length()-2) + " WHERE " +conditions
						+ "has(m.ENTPersonIdentifiant) AND m.ENTPersonIdentifiant='"
						+ request.params().get("ENTPerson") + "' CREATE "
						+ creation.substring(0, creation.length()-2), request.response());
				}
			}
		});

		rm.get("/api/create-group", new Handler<HttpServerRequest>(){
			@Override
			public void handle(HttpServerRequest request) {
				List users = new ArrayList<String>();
				for (Map.Entry<String, String> entry : request.params()) {
					if (!entry.getKey().equals("ENTGroupeNom")){
						users.add(entry.getValue());
					}
				}
				JsonObject obj = new JsonObject().putString("id", request.params().get("ENTGroupId"))
						.putString("nom", request.params().get("ENTGroupName"))
						.putString("parent", "4400000002_ORDINAIRE_CM2deMmeRousseau")
						.putString("type", request.params().get("type"));
				vertx.eventBus().send(config.getString("wp-connector.address"), obj, new Handler<Message>() {
					public void handle(Message event) {
						container.logger().info("MESSAGE : " + event.body());
					}
				});
				neo.send("START n=node(*) WHERE has(n.id) AND n.id='4400000002'"
						+ "CREATE (m {id:'"+request.params().get("ENTGroupId")+"',"
						+ "type:'"+request.params().get("type")+"',"
						+ "ENTGroupeNom:'"+request.params().get("ENTGroupName")
						+"', ENTPeople:'" + users.toString() + "'}), "
						+ "m-[:APPARTIENT]->n ", request.response());
				trace.info("Creating new Group : " + request.params().get("ENTGroupName"));
			}
		});
		
		rm.get("/api/create-school", new Handler<HttpServerRequest>(){
			@Override
			public void handle(HttpServerRequest request) {
				JsonObject obj = new JsonObject().putString("id", request.params().get("ENTSchoolId"))
						.putString("nom", request.params().get("ENTSchoolName"))
						.putString("type", "ETABEDUCNAT");
				vertx.eventBus().send(config.getString("wp-connector.address"), obj, new Handler<Message>() {
					public void handle(Message event) {
						container.logger().info("MESSAGE : " + event.body());
					}
				});
				neo.send("START n=node(*) "
						+ "CREATE (m {id:'" + request.params().get("ENTSchoolId")
						+ "', type:'ETABEDUCNAT',"
						+ "ENTStructureNomCourant:'" + request.params().get("ENTSchoolName")
						+ "'})", request.response());
				trace.info("Creating new School : " + request.params().get("ENTSchoolName"));
			}
		});

		rm.get("/api/export", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String neoRequest = createExportRequest(request.params());
				trace.info("Exporting auth data for " + request.params().get("id"));
				neo.send(neoRequest, request.response());
			}
		});

	}


	private String createExportRequest(MultiMap params){
		if (params.get("id").equals("all")){
			return "START m=node(*) WHERE has(m.type) AND has(m.ENTPersonLogin)"
					+ "AND (m.type='ELEVE' OR m.type='PERSEDUCNAT' OR m.type='PERSRELELEVE') "
					+ "RETURN distinct m.id,m.ENTPersonNom, m.ENTPersonPrenom, m.ENTPersonLogin, m.ENTPersonMotDePasse";
		} else {
			return "START n=node(*) MATCH n<--m "
					+ "WHERE has(n.id)  AND has(m.ENTPersonLogin) AND n.id='" + params.get("id") + "' "
					+ "AND has(m.type) AND (m.type='ELEVE' OR m.type='PERSEDUCNAT' OR m.type='PERSRELELEVE') "
					+ "RETURN distinct m.id,m.ENTPersonNom,m.ENTPersonPrenom, m.ENTPersonLogin, m.ENTPersonMotDePasse";
		}
	}

}