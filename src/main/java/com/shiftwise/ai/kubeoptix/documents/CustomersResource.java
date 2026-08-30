package com.shiftwise.ai.kubeoptix.documents;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/costumers-list")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CustomersResource {

    @GET
    public List<PersonResponse> list() {
        return Customer.<Customer>listAll().stream().map(PersonResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    public PersonResponse get(UUID id) {
        return PersonResponse.from(requiredCustomer(id));
    }

    @POST
    @Transactional
    public Response create(PersonRequest request) {
        Customer customer = new Customer();
        apply(request, customer);
        customer.persist();
        return Response.created(URI.create("/costumers-list/" + customer.id)).entity(PersonResponse.from(customer)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public PersonResponse update(UUID id, PersonRequest request) {
        Customer customer = requiredCustomer(id);
        apply(request, customer);
        return PersonResponse.from(customer);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(UUID id) {
        requiredCustomer(id).delete();
        return Response.noContent().build();
    }

    private static Customer requiredCustomer(UUID id) {
        Customer customer = Customer.findById(id);
        if (customer == null) {
            throw new NotFoundException("Customer not found: " + id);
        }
        return customer;
    }

    private static void apply(PersonRequest request, Customer customer) {
        validate(request);
        customer.name = request.name();
        customer.position = request.position();
        customer.email = request.email();
    }

    private static void validate(PersonRequest request) {
        if (request == null || isBlank(request.name()) || isBlank(request.position()) || isBlank(request.email())) {
            throw new BadRequestException("name, position and email are required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}