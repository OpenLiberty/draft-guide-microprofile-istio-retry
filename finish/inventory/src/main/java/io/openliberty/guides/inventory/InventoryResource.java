// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
// end::copyright[]
package io.openliberty.guides.inventory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.openliberty.guides.inventory.client.SystemClient;
import io.openliberty.guides.inventory.client.UnknownUrlException;
import io.openliberty.guides.inventory.client.UnknownUrlExceptionMapper;
import io.openliberty.guides.inventory.model.InventoryList;

@RequestScoped
@Path("/systems")
public class InventoryResource {

  @Inject
  @ConfigProperty(name = "system.http.port")
  String SYS_HTTP_PORT;

  @Inject
  InventoryManager manager;

  @GET
  @Path("/{hostname}")
  @Produces(MediaType.APPLICATION_JSON)
  // tag::fallback[]
  @Fallback(fallbackMethod = "getPropertiesFallback")
  // end::fallback[]
  // tag::mpRetry[]
  @Retry(maxRetries = 3, retryOn = WebApplicationException.class)
  // end::mpRetry[]
  // tag::getPropertiesForHost[]
  public Response getPropertiesForHost(@PathParam("hostname") String hostname)
         // tag::webApplicationException[]
         throws WebApplicationException, ProcessingException, UnknownUrlException {
         // end::webApplicationException[]
  // end::getPropertiesForHost[]

    String customURLString = "http://" + hostname + ":" + SYS_HTTP_PORT + "/system";
    URL customURL = null;
    Properties props = null;
    try {
        customURL = new URI(customURLString).toURL();
        SystemClient systemClient = RestClientBuilder.newBuilder()
                .baseUrl(customURL)
                .register(UnknownUrlExceptionMapper.class)
                .build(SystemClient.class);
        // tag::getProperties[]
        props = systemClient.getProperties();
        // end::getProperties[]
    } catch (URISyntaxException e) {
        System.err.println("The given URL string is not formatted correctly: "
                           + customURLString);
    } catch (MalformedURLException e) {
        System.err.println("The given URL string is not formatted correctly: "
                           + customURLString);
    }

    if (props == null) {
        return Response.status(Response.Status.NOT_FOUND)
                       .entity("{ \"error\" : \"Unknown hostname" + hostname
                               + " or the resource may not be running on the"
                               + " host machine\" }")
                       .build();
    }

    manager.add(hostname, props);
    return Response.ok(props).build();
  }

  // tag::fallbackMethod[]
  @Produces(MediaType.APPLICATION_JSON)
  public Response getPropertiesFallback(@PathParam("hostname") String hostname) {
      Properties props = new Properties();
      props.put("error", "Unknown hostname or the system service may not be running.");
      return Response.ok(props)
                     .header("X-From-Fallback", "yes")
                     .build();
  }
  // end::fallbackMethod[]

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public InventoryList listContents() {
      return manager.list();
  }

  @POST
  @Path("/reset")
  public void reset() {
      manager.reset();
  }

}
