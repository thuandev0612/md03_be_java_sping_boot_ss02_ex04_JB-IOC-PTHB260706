package com.example.baitapsession03;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Du lieu POST cua lop test nay khong anh huong den cac lop test khac.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TaskCreationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void validAssignmentCreatesTasksWithUniqueGeneratedIdsAndPersistsThem() throws Exception {
        JsonNode before = getTasks();
        String body = """
                {"title":"Practice POST API","description":"Learn request body",
                 "priority":"HIGH","assignedTo":1}
                """;

        JsonNode first = postCreated(body);
        JsonNode second = postCreated(body);
        assertTrue(first.get("id").longValue() > 0);
        assertNotEquals(first.get("id"), second.get("id"));
        assertEquals(1, first.get("assignedTo").intValue());
        assertEquals("Practice POST API", first.get("title").stringValue());

        JsonNode after = getTasks();
        assertEquals(before.size() + 2, after.size());
        assertTrue(containsTask(after, first));
        assertTrue(containsTask(after, second));

        HttpResponse<String> search = request("GET", "/tasks?search=Practice%20POST", null);
        assertEquals(200, search.statusCode());
        assertTrue(containsTask(jsonMapper.readTree(search.body()), first));
    }

    @Test
    void uniqueProvidedIdAndOptionalDescriptionAreAccepted() throws Exception {
        JsonNode before = getTasks();
        long maxId = 0;
        for (JsonNode task : before) {
            maxId = Math.max(maxId, task.get("id").longValue());
        }
        long providedId = maxId + 100;
        JsonNode created = postCreated("""
                {"id":%d,"title":"Task12","priority":"high","assignedTo":2}
                """.formatted(providedId));

        assertEquals(providedId, created.get("id").longValue());
        assertEquals(2, created.get("assignedTo").intValue());
        assertEquals(before.size() + 1, getTasks().size());
    }

    @Test
    void nonexistentAssigneeReturns400WithoutSaving() throws Exception {
        JsonNode before = getTasks();
        for (int id : new int[]{999, 0, -1}) {
            assertValidationError("""
                    {"title":"Invalid assignment","assignedTo":%d}
                    """.formatted(id));
            assertEquals(before, getTasks());
        }
    }

    @Test
    void missingOrNullAssigneeReturns400WithoutSaving() throws Exception {
        JsonNode before = getTasks();
        assertValidationError("""
                {"title":"No assignee"}
                """);
        assertValidationError("""
                {"title":"Null assignee","assignedTo":null}
                """);
        assertEquals(before, getTasks());
    }

    @Test
    void missingNullOrBlankTitleReturns400WithoutSaving() throws Exception {
        JsonNode before = getTasks();
        for (String body : new String[]{
                "{\"assignedTo\":1}",
                "{\"title\":null,\"assignedTo\":1}",
                "{\"title\":\"   \",\"assignedTo\":1}"}) {
            assertValidationError(body);
        }
        assertEquals(before, getTasks());
    }

    @Test
    void duplicateIdReturns400AndPreservesExistingTask() throws Exception {
        JsonNode before = getTasks();
        assertValidationError("""
                {"id":1,"title":"Attempt duplicate","assignedTo":1}
                """);
        assertEquals(before, getTasks());
    }

    @Test
    void nonPositiveTaskIdReturns400WithoutSaving() throws Exception {
        JsonNode before = getTasks();
        for (int id : new int[]{0, -1}) {
            assertValidationError("""
                    {"id":%d,"title":"Invalid task ID","assignedTo":1}
                    """.formatted(id));
        }
        assertEquals(before, getTasks());
    }

    @Test
    void malformedBodyAndWrongAssigneeTypeReturn400WithoutSaving() throws Exception {
        JsonNode before = getTasks();
        for (String body : new String[]{
                "", "null", "{",
                "{\"title\":\"Wrong type\",\"assignedTo\":\"abc\"}",
                "{\"title\":\"Wrong type\",\"assignedTo\":{\"id\":1}}"}) {
            HttpResponse<String> response = request("POST", "/tasks", body);
            assertEquals(400, response.statusCode(), response.body());
        }
        assertEquals(before, getTasks());
    }

    private JsonNode postCreated(String body) throws Exception {
        HttpResponse<String> response = request("POST", "/tasks", body);
        assertEquals(201, response.statusCode(), response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElse("")
                .startsWith("application/json"));
        return jsonMapper.readTree(response.body());
    }

    private void assertValidationError(String body) throws Exception {
        HttpResponse<String> response = request("POST", "/tasks", body);
        assertEquals(400, response.statusCode(), response.body());
        JsonNode error = jsonMapper.readTree(response.body());
        assertNotNull(error.get("message"));
        assertFalse(error.get("message").stringValue().isBlank());
    }

    private JsonNode getTasks() throws Exception {
        HttpResponse<String> response = request("GET", "/tasks", null);
        assertEquals(200, response.statusCode(), response.body());
        return jsonMapper.readTree(response.body());
    }

    private boolean containsTask(JsonNode tasks, JsonNode expected) {
        for (JsonNode task : tasks) {
            if (task.equals(expected)) {
                return true;
            }
        }
        return false;
    }

    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + path))
                    .timeout(Duration.ofSeconds(10));
            if (method.equals("POST")) {
                builder.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body));
            } else {
                builder.GET();
            }
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
