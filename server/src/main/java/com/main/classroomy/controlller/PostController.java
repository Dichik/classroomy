package com.main.classroomy.controlller;

import com.main.classroomy.entity.Post;
import com.main.classroomy.entity.dto.PostDto;
import com.main.classroomy.exception.DeadlineUpdateException;
import com.main.classroomy.service.PostService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private static final Logger logger = LogManager.getLogger(PostController.class);

    private final PostService postService;
    private final ModelMapper modelMapper;

    @Autowired
    public PostController(PostService postService, ModelMapper modelMapper) {
        this.postService = postService;
        this.modelMapper = modelMapper;
    }

    @PreAuthorize("hasRole('TEACHER')")
    @CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*")
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> create(@Valid @RequestBody PostDto postDto) {
        Post post = this.modelMapper.map(postDto, Post.class);
        return new ResponseEntity<>(this.postService.create(post), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    @RequestMapping(value = "/{id:\\d+}", method = RequestMethod.GET)
    public ResponseEntity<?> findById(@PathVariable Long id) {
        Post post = this.postService.getById(id)
                .orElseThrow(() -> new EntityNotFoundException("Post with id=" + id + " was not found!"));
        return new ResponseEntity<>(post, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    @RequestMapping(value = "/{id:\\d+}", method = RequestMethod.GET, params = {"courseId"})
    public ResponseEntity<?> findByIdAndCourseId(@PathVariable Long id, @RequestParam Long courseId) {
        Post post = this.postService.getByIdAndCourseId(id, courseId)
                .orElseThrow(() -> new EntityNotFoundException("Post with id=" + id + " was not found!"));
        return new ResponseEntity<>(post, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @RequestMapping(value = "/{id:\\d+}/deadline", method = RequestMethod.PUT)
    public ResponseEntity<?> updateDeadline(@PathVariable Long id, @Valid @RequestBody PostDto postDto) {
        try {
            this.postService.updateById(id, postDto);
            return new ResponseEntity<>("Deadline was updated!", HttpStatus.OK);
        } catch (DeadlineUpdateException e) {
            logger.warn("Error while update deadline, please see: " + e);
            return new ResponseEntity<>("Deadline was not updated.", HttpStatus.NOT_FOUND);
        }
    }

    @PreAuthorize("hasRole('TEACHER')")
    @CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*")
    @RequestMapping(value = "/{id:\\d+}", method = RequestMethod.PATCH)
    public ResponseEntity<Post> updatePost(@PathVariable Long id, @Valid @RequestBody PostDto postDto) {
        return new ResponseEntity<>(this.postService.update(id, postDto), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    @RequestMapping(value = "/deadlines", method = RequestMethod.GET)
    public ResponseEntity<?> getDeadlines() {
        List<Post> posts = this.postService.getAssignmentsForNextWeek();
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    @RequestMapping(value = "/deadlines", method = RequestMethod.GET, params = {"courseId"})
    public ResponseEntity<?> getDeadlinesForCourseId(@RequestParam Long courseId, @RequestParam(required = false, defaultValue = "false") boolean urgent) {
        List<PostDto> posts;
        if (!urgent) {
            posts = this.postService.getAssignmentsWithDeadlines(courseId).stream()
                    .map(post -> modelMapper.map(post, PostDto.class))
                    .toList();
        } else {
            posts = this.postService.getAssignmentsForNextWeek(courseId).stream()
                    .map(post -> modelMapper.map(post, PostDto.class))
                    .toList();
        }
        if (posts.isEmpty()) {
            return new ResponseEntity<>("List of posts is empty!", HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    @RequestMapping(method = RequestMethod.GET, params = {"courseId"})
    public ResponseEntity<List<Post>> getPostsByCourseId(@RequestParam Long courseId) {
        List<Post> posts = this.postService.getByCourseId(courseId);
        if (posts.isEmpty()) {
            logger.info("No posts found for course with id=" + courseId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }

}
