package com.shiftwise.ai.kubeoptix.documents;

import java.net.URI;
import java.util.List;

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

@Path("/documents")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DocumentsResource {

    @GET
    @Transactional
    public List<DocumentResponse> list() {
        return Document.<Document>listAll().stream().map(DocumentResponse::from).toList();
    }

    @GET
    @Path("/{documentName}")
    @Transactional
    public DocumentResponse get(String documentName) {
        return DocumentResponse.from(requiredDocument(documentName));
    }

    @POST
    @Transactional
    public Response create(DocumentRequest request) {
        validate(request, true);
        if (Document.findById(request.documentName()) != null) {
            throw new BadRequestException("documentName already exists: " + request.documentName());
        }
        Document document = new Document();
        document.documentName = request.documentName();
        apply(request, document);
        document.persist();
        return Response.created(URI.create("/documents/" + document.documentName))
                .entity(DocumentResponse.from(document))
                .build();
    }

    @PUT
    @Path("/{documentName}")
    @Transactional
    public DocumentResponse update(String documentName, DocumentRequest request) {
        validate(request, false);
        Document document = requiredDocument(documentName);
        apply(request, document);
        return DocumentResponse.from(document);
    }

    @DELETE
    @Path("/{documentName}")
    @Transactional
    public Response delete(String documentName) {
        requiredDocument(documentName).delete();
        return Response.noContent().build();
    }

    private static Document requiredDocument(String documentName) {
        Document document = Document.findById(documentName);
        if (document == null) {
            throw new NotFoundException("Document not found: " + documentName);
        }
        return document;
    }

    private static void validate(DocumentRequest request, boolean requireName) {
        if (request == null || (requireName && isBlank(request.documentName())) || isBlank(request.title())
                || isBlank(request.projectManager()) || isBlank(request.costumer()) || request.authorId() == null
                || request.costumersListId() == null) {
            throw new BadRequestException("documentName, title, projectManager, costumer, authorId and costumersListId are required");
        }
    }

    private static void apply(DocumentRequest request, Document document) {
        document.title = request.title();
        document.projectManager = request.projectManager();
        document.costumer = request.costumer();
        document.markdownContent = request.markdownContent();
        document.author = requiredAuthor(request.authorId());
        document.customer = requiredCustomer(request.costumersListId());
    }

    private static Author requiredAuthor(java.util.UUID id) {
        Author author = Author.findById(id);
        if (author == null) {
            throw new NotFoundException("Author not found: " + id);
        }
        return author;
    }

    private static Customer requiredCustomer(java.util.UUID id) {
        Customer customer = Customer.findById(id);
        if (customer == null) {
            throw new NotFoundException("Customer not found: " + id);
        }
        return customer;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}