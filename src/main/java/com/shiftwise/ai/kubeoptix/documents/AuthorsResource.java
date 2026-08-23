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

@Path("/authors")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthorsResource {

    @GET
    public List<PersonResponse> list() {
        return Author.<Author>listAll().stream().map(PersonResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    public PersonResponse get(UUID id) {
        return PersonResponse.from(requiredAuthor(id));
    }

    @POST
    @Transactional
    public Response create(PersonRequest request) {
        Author author = new Author();
        apply(request, author);
        author.persist();
        return Response.created(URI.create("/authors/" + author.id)).entity(PersonResponse.from(author)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public PersonResponse update(UUID id, PersonRequest request) {
        Author author = requiredAuthor(id);
        apply(request, author);
        return PersonResponse.from(author);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(UUID id) {
        requiredAuthor(id).delete();
        return Response.noContent().build();
    }

    private static Author requiredAuthor(UUID id) {
        Author author = Author.findById(id);
        if (author == null) {
            throw new NotFoundException("Author not found: " + id);
        }
        return author;
    }

    private static void apply(PersonRequest request, Author author) {
        validate(request);
        author.name = request.name();
        author.position = request.position();
        author.email = request.email();
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