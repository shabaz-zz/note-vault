package com.explorer.vaultcourier;

// ============================================================
//  VaultCourier.java
//  Backend API for the Explorer-01 Notes Terminal
//
//  ROUTES:
//    GET    /api/health        → check if backend is alive
//    GET    /api/notes         → get all notes
//    GET    /api/notes/{id}    → get one note by id
//    POST   /api/notes         → create a new note
//    PUT    /api/notes/{id}    → update a note
//    DELETE /api/notes/{id}    → delete a note
// ============================================================

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;

// ── 1. ENTRY POINT ──────────────────────────────────────────
// This is where Java starts running your app.
// @SpringBootApplication wires everything together automatically.

@SpringBootApplication
public class VaultCourier {
    public static void main(String[] args) {
        SpringApplication.run(VaultCourier.class, args);
    }
}


// ── 2. NOTE MODEL ────────────────────────────────────────────
// This is the blueprint for what a "note" looks like.
// @Entity tells Java this maps to a table in PostgreSQL.
// Each field below becomes a column in the database.

@Entity
@Table(name = "notes")
class Note {

    @Id
    @Column(name = "id")
    private String id;          // unique ID like "abc123"

    @Column(name = "title", nullable = false)
    private String title;       // note title

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;     // the actual note text

    @Column(name = "tags")
    private String tags;        // stored as comma-separated string e.g. "java,docker"

    @Column(name = "created_at")
    private LocalDateTime createdAt;    // when the note was made

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;    // when it was last saved

    // ── GETTERS & SETTERS ──
    // Java needs these to read and write each field.

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}


// ── 3. REPOSITORY ────────────────────────────────────────────
// This is the layer that talks to PostgreSQL.
// JpaRepository gives you free database methods like:
//   .findAll()       → SELECT * FROM notes
//   .findById(id)    → SELECT * FROM notes WHERE id = ?
//   .save(note)      → INSERT or UPDATE
//   .deleteById(id)  → DELETE FROM notes WHERE id = ?
// You don't write any SQL — Spring handles it.

@Repository
interface NoteRepository extends JpaRepository<Note, String> {
    // Nothing needed here for basic CRUD.
    // Spring generates all the SQL automatically.
}


// ── 4. CONTROLLER ────────────────────────────────────────────
// This is where your API routes live.
// Each method here responds to a different HTTP request
// from your frontend.

@RestController                         // marks this as an API controller
@RequestMapping("/api")                 // all routes start with /api
@CrossOrigin(origins = "*")            // allows your frontend to call this
class NoteController {

    @Autowired
    private NoteRepository repo;        // injects the database layer


    // ── HEALTH CHECK ──────────────────────────────────────
    // Frontend pings this every 10s to check if backend is alive.
    // GET /api/health
    // Returns: { "status": "ONLINE", "system": "VAULT COURIER" }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "ONLINE",
            "system", "VAULT COURIER",
            "timestamp", LocalDateTime.now().toString()
        ));
    }


    // ── GET ALL NOTES ─────────────────────────────────────
    // Returns every note in the database as a JSON list.
    // GET /api/notes
    // Returns: [ { id, title, content, tags, ... }, ... ]

    @GetMapping("/notes")
    public ResponseEntity<List<Note>> getAllNotes() {
        List<Note> notes = repo.findAll();
        return ResponseEntity.ok(notes);
    }


    // ── GET ONE NOTE ──────────────────────────────────────
    // Returns one note by its ID.
    // GET /api/notes/{id}
    // Returns: { id, title, content, tags, ... }
    // Returns 404 if not found.

    @GetMapping("/notes/{id}")
    public ResponseEntity<Note> getNote(@PathVariable String id) {
        Optional<Note> note = repo.findById(id);

        if (note.isPresent()) {
            return ResponseEntity.ok(note.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    // ── CREATE NOTE ───────────────────────────────────────
    // Saves a new note to the database.
    // POST /api/notes
    // Body: { title, content, tags }
    // Returns: the saved note with its new ID

    @PostMapping("/notes")
    public ResponseEntity<Note> createNote(@RequestBody Note incoming) {

        // Generate a unique ID using UUID (random string)
        incoming.setId(UUID.randomUUID().toString());

        // Set timestamps to right now
        LocalDateTime now = LocalDateTime.now();
        incoming.setCreatedAt(now);
        incoming.setUpdatedAt(now);

        // Save to PostgreSQL
        Note saved = repo.save(incoming);

        // Return 201 Created with the saved note
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }


    // ── UPDATE NOTE ───────────────────────────────────────
    // Updates an existing note by ID.
    // PUT /api/notes/{id}
    // Body: { title, content, tags }
    // Returns: the updated note
    // Returns 404 if not found.

    @PutMapping("/notes/{id}")
    public ResponseEntity<Note> updateNote(
        @PathVariable String id,
        @RequestBody Note incoming
    ) {
        Optional<Note> existing = repo.findById(id);

        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Note note = existing.get();

        // Only update fields that were sent
        if (incoming.getTitle() != null) note.setTitle(incoming.getTitle());
        if (incoming.getContent() != null) note.setContent(incoming.getContent());
        if (incoming.getTags() != null) note.setTags(incoming.getTags());

        // Update the timestamp
        note.setUpdatedAt(LocalDateTime.now());

        // Save back to database
        Note updated = repo.save(note);

        return ResponseEntity.ok(updated);
    }


    // ── DELETE NOTE ───────────────────────────────────────
    // Deletes a note by ID.
    // DELETE /api/notes/{id}
    // Returns: { "message": "TRANSMISSION DELETED" }
    // Returns 404 if not found.

    @DeleteMapping("/notes/{id}")
    public ResponseEntity<Map<String, String>> deleteNote(@PathVariable String id) {

        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        repo.deleteById(id);

        return ResponseEntity.ok(Map.of(
            "message", "TRANSMISSION DELETED",
            "id", id
        ));
    }
}
