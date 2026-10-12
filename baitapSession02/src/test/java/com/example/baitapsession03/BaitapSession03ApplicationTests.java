package com.example.baitapsession03;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// Khoi dong server tren cong ngau nhien de kiem tra toan bo 3 tang qua HTTP.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BaitapSession03ApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void usersEndpointReturnsAtLeastThreeCompleteUsers() throws Exception {
        JsonNode users = getJson("/users");

        assertTrue(users.isArray());
        assertTrue(users.size() >= 3);
        Set<Long> ids = new HashSet<>();
        for (JsonNode user : users) {
            assertTrue(user.get("id").longValue() > 0);
            assertTrue(ids.add(user.get("id").longValue()), "User IDs must be unique");
            assertNonBlankFields(user, "username", "email", "role");
        }
    }

    @Test
    void tasksEndpointReturnsAtLeastTenTasksAssignedToExistingUsers() throws Exception {
        Map<Long, JsonNode> usersById = new HashMap<>();
        for (JsonNode user : getJson("/users")) {
            usersById.put(user.get("id").longValue(), user);
        }

        JsonNode tasks = getJson("/tasks");
        assertTrue(tasks.isArray());
        assertTrue(tasks.size() >= 10);
        Set<Long> ids = new HashSet<>();
        for (JsonNode task : tasks) {
            assertTrue(task.get("id").longValue() > 0);
            assertTrue(ids.add(task.get("id").longValue()), "Task IDs must be unique");
            assertNonBlankFields(task, "title", "description", "priority");
            JsonNode assignedTo = task.get("assignedTo");
            assertNotNull(assignedTo);
            assertTrue(assignedTo.isIntegralNumber());
            assertTrue(usersById.containsKey(assignedTo.longValue()));
        }
    }

    @Test
    void usersSearchMatchesPartOfUsernameIgnoringCaseAndSurroundingSpaces() throws Exception {
        JsonNode users = getJson("/users?search=%20GuY%20");

        assertTrue(users.isArray());
        assertEquals(1, users.size());
        assertEquals("nguyenvana", users.get(0).get("username").stringValue());
    }

    @Test
    void tasksSearchMatchesPartOfTitleIgnoringCaseAndSurroundingSpaces() throws Exception {
        JsonNode tasks = getJson("/tasks?search=%20uSeR%20");

        assertTrue(tasks.isArray());
        assertEquals(3, tasks.size());
        assertEquals("Create User model", tasks.get(0).get("title").stringValue());
        assertEquals("Create User repository", tasks.get(1).get("title").stringValue());
        assertEquals("Create User service", tasks.get(2).get("title").stringValue());
    }

    @Test
    void emptyOrWhitespaceSearchReturnsAllData() throws Exception {
        for (String path : new String[]{"/users", "/tasks"}) {
            JsonNode allData = getJson(path);
            assertEquals(allData, getJson(path + "?search="));
            assertEquals(allData, getJson(path + "?search=%20%20"));
        }
    }

    @Test
    void unmatchedSearchReturnsEmptyArrayWithStatus200() throws Exception {
        for (String path : new String[]{"/users", "/tasks"}) {
            JsonNode results = getJson(path + "?search=no-match-987654321");
            assertTrue(results.isArray());
            assertEquals(0, results.size());
        }
    }

    @Test
    void filteringDoesNotRemoveDataFromRepositories() throws Exception {
        JsonNode allUsers = getJson("/users");
        JsonNode allTasks = getJson("/tasks");

        getJson("/users?search=nguyen");
        getJson("/tasks?search=model");

        assertEquals(allUsers, getJson("/users"));
        assertEquals(allTasks, getJson("/tasks"));
    }

    private JsonNode getJson(String path) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + path))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body());
            assertTrue(response.headers().firstValue("Content-Type").orElse("")
                    .startsWith("application/json"));
            return jsonMapper.readTree(response.body());
        }
    }

    private void assertNonBlankFields(JsonNode item, String... fields) {
        for (String field : fields) {
            JsonNode value = item.get(field);
            assertNotNull(value, "Missing field: " + field);
            assertTrue(value.isString(), "Expected text field: " + field);
            assertFalse(value.stringValue().isBlank(), "Empty field: " + field);
        }
    }
}
