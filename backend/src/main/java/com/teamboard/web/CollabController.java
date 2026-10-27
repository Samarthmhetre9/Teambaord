package com.teamboard.web;

import static com.teamboard.dto.Dtos.*;

import com.teamboard.domain.*;
import com.teamboard.repo.*;
import com.teamboard.service.StorageService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Comments (REST + STOMP) and file attachments. */
@RestController
public class CollabController {
    private final CommentRepository comments;
    private final AttachmentRepository attachments;
    private final TaskRepository tasks;
    private final StorageService storage;
    private final SimpMessagingTemplate ws;

    public CollabController(CommentRepository comments, AttachmentRepository attachments, TaskRepository tasks,
                            StorageService storage, SimpMessagingTemplate ws) {
        this.comments = comments; this.attachments = attachments; this.tasks = tasks; this.storage = storage; this.ws = ws;
    }

    // ---- comments ----
    @GetMapping("/api/tasks/{id}/comments")
    public List<CommentDto> comments(@PathVariable Long id) {
        return comments.findByTaskIdOrderByCreatedAtAsc(id).stream().map(c -> CommentDto.of(c, id)).toList();
    }

    @PostMapping("/api/tasks/{id}/comments") @ResponseStatus(HttpStatus.CREATED)
    public CommentDto addViaRest(@PathVariable Long id, @Valid @RequestBody CommentRequest r) { return save(id, r); }

    /** Client publishes to /app/tasks/{id}/comment; every subscriber of /topic/tasks/{id}/comments receives it. */
    @MessageMapping("/tasks/{id}/comment")
    public void addViaStomp(@DestinationVariable Long id, CommentRequest r) {
        if (r == null || r.text() == null || r.text().isBlank() || r.author() == null || r.author().isBlank()) return;
        save(id, r);
    }

    private CommentDto save(Long taskId, CommentRequest r) {
        if (!tasks.existsById(taskId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        Comment c = new Comment();
        c.setTask(tasks.getReferenceById(taskId));
        c.setAuthor(r.author());
        c.setText(r.text());
        CommentDto dto = CommentDto.of(comments.save(c), taskId);
        ws.convertAndSend("/topic/tasks/" + taskId + "/comments", dto);
        return dto;
    }

    // ---- attachments ----
    @GetMapping("/api/tasks/{id}/attachments")
    public List<AttachmentDto> attachments(@PathVariable Long id) {
        return attachments.findByTaskId(id).stream().map(a -> AttachmentDto.of(a, id)).toList();
    }

    @PostMapping("/api/tasks/{id}/attachments") @ResponseStatus(HttpStatus.CREATED)
    public AttachmentDto upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || !tasks.existsById(id)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad upload");
        Attachment a = new Attachment();
        a.setTask(tasks.getReferenceById(id));
        a.setFileName(file.getOriginalFilename() == null ? "file" : Path.of(file.getOriginalFilename()).getFileName().toString());
        a.setContentType(file.getContentType());
        a.setSize(file.getSize());
        a.setStorageKey(storage.store(file));
        AttachmentDto dto = AttachmentDto.of(attachments.save(a), id);
        ws.convertAndSend("/topic/tasks/" + id + "/attachments", dto);
        return dto;
    }

    /** S3 mode: short-lived presigned URL. Local mode: URL of the /file endpoint below. */
    @GetMapping("/api/attachments/{id}/url")
    public UrlDto url(@PathVariable Long id) {
        Attachment a = attachments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new UrlDto(storage.downloadUrl(a.getId(), a.getStorageKey(), a.getFileName()));
    }

    @GetMapping("/api/attachments/{id}/file")
    public ResponseEntity<Resource> localFile(@PathVariable Long id) {
        Attachment a = attachments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Path p = storage.localPath(a.getStorageKey());
        if (storage.useS3() || !Files.exists(p)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        MediaType type = a.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(a.getContentType());
        return ResponseEntity.ok().contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(a.getFileName()).build().toString())
                .body(new FileSystemResource(p));
    }
}
