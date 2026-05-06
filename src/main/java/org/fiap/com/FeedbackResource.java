package org.fiap.com;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.fiap.com.models.Feedback;
import org.fiap.com.service.FeedbackServiceImpl;

import java.util.List;

@Path("/feedback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FeedbackResource {

    @Inject
    FeedbackServiceImpl feedbackService;

    @GET
    public List<Feedback> findAll() {
        return feedbackService.findAll();
    }

    @GET
    @Path("/{id}")
    public Feedback findById(@PathParam("id") Long id) {
        return feedbackService.findById(id);
    }

    @POST
    public List<Feedback> add(Feedback feedback) {
        feedbackService.add(feedback);
        return feedbackService.findAll();
    }
}
