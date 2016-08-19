package com.gentics.mesh.core.verticle.microschema;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Verticle for /api/v1/PROJECTNAME/microschemas
 */
@Component
@Scope("singleton")
@SpringVerticle
public class ProjectMicroschemaVerticle extends AbstractProjectRestVerticle {

	@Autowired
	private MicroschemaCrudHandler crudHandler;

	public ProjectMicroschemaVerticle() {
		super("microschemas");
	}

	@Override
	public String getDescription() {
		return "Contains endpoints which allow microschemas to be assigned to projects.";
	}

	@Override
	public void registerEndPoints() throws Exception {
		secureAll();
		addReadHandlers();
		addUpdateHandlers();
		addDeleteHandlers();
	}

	private void addReadHandlers() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/");
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Read all microschemas which are assigned to the project.");
		endpoint.exampleResponse(OK, microschemaExamples.getMicroschemaListResponse(), "List of assigned microschemas.");
		endpoint.handler(rc -> {
			crudHandler.handleReadMicroschemaList(InternalActionContext.create(rc));
		});
	}

	private void addUpdateHandlers() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:microschemaUuid");
		endpoint.addUriParameter("microschemaUuid", "Uuid of the microschema.", UUIDUtil.randomUUID());
		endpoint.method(POST);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Add the microschema to the project.");
		endpoint.exampleResponse(OK, microschemaExamples.getGeolocationMicroschema(), "Microschema was added to the project.");
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("microschemaUuid");
			crudHandler.handleAddMicroschemaToProject(ac, uuid);
		});
	}

	private void addDeleteHandlers() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:microschemaUuid");
		endpoint.addUriParameter("microschemaUuid", "Uuid of the microschema.", UUIDUtil.randomUUID());
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Remove the microschema from the project.");
		endpoint.exampleResponse(NO_CONTENT, "Microschema was removed from project.");
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("microschemaUuid");
			crudHandler.handleRemoveMicroschemaFromProject(ac, uuid);
		});
	}
}