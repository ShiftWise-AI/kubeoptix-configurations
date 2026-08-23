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

@Path("/document-versions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DocumentVersionsResource {

    @GET
    @Transactional
    public List<DocumentVersionResponse> list() {
        return DocumentVersion.<DocumentVersion>listAll().stream().map(DocumentVersionResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    @Transactional
    public DocumentVersionResponse get(UUID id) {
        return DocumentVersionResponse.from(requiredDocumentVersion(id));
    }

    @POST
    @Transactional
    public Response create(DocumentVersionRequest request) {
        DocumentVersion documentVersion = new DocumentVersion();
        apply(request, documentVersion);
        documentVersion.persist();
        return Response.created(URI.create("/document-versions/" + documentVersion.id))
                .entity(DocumentVersionResponse.from(documentVersion))
                .build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public DocumentVersionResponse update(UUID id, DocumentVersionRequest request) {
        DocumentVersion documentVersion = requiredDocumentVersion(id);
        apply(request, documentVersion);
        return DocumentVersionResponse.from(documentVersion);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(UUID id) {
        requiredDocumentVersion(id).delete();
        return Response.noContent().build();
    }

    private static DocumentVersion requiredDocumentVersion(UUID id) {
        DocumentVersion documentVersion = DocumentVersion.findById(id);
        if (documentVersion == null) {
            throw new NotFoundException("Document version not found: " + id);
        }
        return documentVersion;
    }

    private static void apply(DocumentVersionRequest request, DocumentVersion documentVersion) {
        if (request == null || isBlank(request.title()) || isBlank(request.projectManager())
                || request.authorId() == null || request.customerId() == null || request.versionId() == null) {
            throw new BadRequestException("title, projectManager, authorId, customerId and versionId are required");
        }

        documentVersion.title = request.title();
        documentVersion.projectManager = request.projectManager();
        documentVersion.author = requiredAuthor(request.authorId());
        documentVersion.customer = requiredCustomer(request.customerId());
        documentVersion.version = requiredVersion(request.versionId());
    }

    private static Author requiredAuthor(UUID id) {
        Author author = Author.findById(id);
        if (author == null) {
            throw new NotFoundException("Author not found: " + id);
        }
        return author;
    }

    private static Customer requiredCustomer(UUID id) {
        Customer customer = Customer.findById(id);
        if (customer == null) {
            throw new NotFoundException("Customer not found: " + id);
        }
        return customer;
    }

    private static Version requiredVersion(UUID id) {
        Version version = Version.findById(id);
        if (version == null) {
            throw new NotFoundException("Version not found: " + id);
        }
        return version;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}