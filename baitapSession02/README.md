# Bài tập — Kiến trúc 3 tầng, bộ lọc và API tạo công việc

Dự án Spring Boot dùng Java 21, Gradle, Spring Web và Lombok. Dữ liệu mẫu lưu trong bộ nhớ: 3 user và 10 task.

## Cấu trúc

```text
src/main/java/com/example/baitapsession03/
├── BaitapSession03Application.java
├── controller/
│   ├── UserController.java
│   └── TaskController.java
├── service/
│   ├── UserService.java
│   └── TaskService.java
├── repository/
│   ├── UserRepository.java
│   └── TaskRepository.java
└── model/
    ├── User.java
    └── Task.java
```

## Luồng xử lý

```text
GET /users → UserController → UserService → UserRepository → List<User>
GET /tasks → TaskController → TaskService → TaskRepository → List<Task>
POST /tasks → TaskController → TaskService → UserService → UserRepository (kiểm tra user)
                                          → TaskRepository.save() (khi hợp lệ)
```

- `model`: mô tả dữ liệu. `User` có `id`, `username`, `email`, `role`; `Task` có `id`, `title`, `description`, `priority`, `assignedTo`.
- `@Repository`: đánh dấu lớp truy cập dữ liệu. Trong bài này, dữ liệu là `ArrayList` được khởi tạo sẵn. `TaskRepository.findAll()` trả bản sao danh sách để việc đọc không bị ảnh hưởng khi một request khác thêm task; `save()` lưu task mới.
- `@Service`: đánh dấu lớp xử lý nghiệp vụ. Các phương thức lấy danh sách gọi repository; `TaskService.createTask()` kiểm tra dữ liệu và user trước khi lưu.
- `@RestController`: kết hợp vai trò của `@Controller` và `@ResponseBody`, nên các phương thức trả dữ liệu JSON mà không cần ghi thêm `@ResponseBody`.
- `@RequestMapping("/users")` hoặc `@RequestMapping("/tasks")`: đặt đường dẫn chung cho controller. `@GetMapping` trên phương thức nhận HTTP GET tại đường dẫn đó.
- `ResponseEntity<List<User>>` / `ResponseEntity<List<Task>>`: cho phép trả danh sách cùng HTTP status và header. `ResponseEntity.ok(danhSach)` trả HTTP **200 OK** với danh sách làm response body.

Các package nằm bên dưới `com.example.baitapsession03`, cùng cây package với lớp có `@SpringBootApplication`, để Spring tìm thấy các bean.

## Constructor Dependency Injection

Ví dụ trong `UserService`:

```java
private final UserRepository userRepository;

@Autowired
public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
}
```

Spring tạo bean `UserRepository`, rồi truyền bean đó vào constructor của `UserService`. Tương tự, Spring truyền `UserService` vào constructor của `UserController`. Hai lớp của nhánh Task cũng dùng cách này.

Khi chỉ có một constructor, Spring có thể tự tiêm mà không cần ghi `@Autowired`; bài này vẫn ghi rõ annotation theo yêu cầu đề.

Trong bài tạo mới, `TaskService` nhận cả `TaskRepository` và `UserService` qua constructor có `@Autowired`. `TaskRepository` khởi tạo dữ liệu mẫu bằng ID user và không cần phụ thuộc vào `UserRepository` nữa.

`Task.assignedTo` đã đổi từ kiểu `User` sang `Integer` để khớp JSON mẫu `"assignedTo": 1`. Đây là ID của user, không phải toàn bộ đối tượng user. Cả GET và POST hiện trả trường này dưới dạng số. `UserRepository.findById(int id)` tìm user theo ID; `UserService.findUserById(int id)` gọi phương thức đó. Không tìm thấy thì trả `null`.

Ví dụ task có `assignedTo = 2` nghĩa là task thuộc user có `id = 2`. Nhiều task có thể cùng được giao cho một user. Đây là liên kết theo ID được kiểm tra bằng Java; chưa có cơ sở dữ liệu hay ràng buộc khóa ngoại trong database.

Model dùng Lombok để tạo getter, setter, constructor không tham số và constructor đủ tham số. Không cần viết thêm constructor trùng chức năng.

## Chạy và xem kết quả

Trong IntelliJ, chọn JDK 21 cho project và Gradle JVM, tải lại Gradle nếu vừa mở project, rồi chạy `BaitapSession03Application.main()`.

Hoặc mở PowerShell tại thư mục dự án và chạy:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
.\gradlew.bat bootRun
```

Mở trình duyệt hoặc gửi GET bằng Postman:

| URL | Kết quả |
| --- | --- |
| http://localhost:8080/users | Mảng JSON gồm 3 user |
| http://localhost:8080/tasks | Danh sách task (ban đầu có 10 task); `assignedTo` là ID của user |
| http://localhost:8080/users?search=nguyen | User có `username` chứa `nguyen` |
| http://localhost:8080/tasks?search=model | Task có `title` chứa `model` |
| http://localhost:8080/tasks?search=abc | Mảng JSON rỗng `[]` nếu không có task phù hợp; vẫn HTTP 200 |

## Cách hoạt động của bộ lọc

Hai controller nhận query param như sau:

```java
@RequestParam(name = "search", defaultValue = "") String search
```

`search` là tên tham số sau dấu `?` trong URL. `defaultValue = ""` làm tham số này không bắt buộc: không truyền hoặc để trống thì lấy toàn bộ danh sách. Chuỗi tìm kiếm được bỏ khoảng trắng đầu/cuối và chuyển về chữ thường với `Locale.ROOT`; chuỗi chỉ chứa khoảng trắng cũng lấy toàn bộ.

Ví dụ phần xử lý trong `TaskController`:

```java
List<Task> tasks = taskService.findAllTasks();
String keyword = search.trim().toLowerCase(Locale.ROOT);

if (!keyword.isEmpty()) {
    tasks = tasks.stream()
            .filter(task -> task.getTitle().toLowerCase(Locale.ROOT).contains(keyword))
            .toList();
}

return ResponseEntity.ok(tasks);
```

`stream()` duyệt danh sách, `filter(...)` giữ các task có tiêu đề chứa từ khóa, `toList()` thu kết quả thành danh sách. Việc lọc nằm ngay trong controller trên kết quả của service theo yêu cầu đề; không xóa phần tử trong repository. Kết quả là danh sách rỗng khi không có mục phù hợp.

`UserController` làm tương tự với `getUsername()`: model của bài trước có trường `username`, không có trường `name`. Bộ lọc không phân biệt hoa/thường, ví dụ `search=MODEL` và `search=model` cho cùng kết quả.

Trong URL dùng `?search=abc`, **không đặt dấu ngoặc kép** quanh `abc`. Khi nhập trong trình duyệt/Postman, khoảng trắng có thể được mã hóa thành `%20`.

Đường dẫn `/` chưa có controller; hãy mở đúng `/users` hoặc `/tasks`. Nếu cổng 8080 đang có ứng dụng khác sử dụng, dừng ứng dụng đó hoặc đặt `server.port=8081` trong `application.properties`, rồi đổi cổng trong URL tương ứng.

## Tạo task bằng POST và RequestBody

Chạy lại ứng dụng sau khi sửa code. Trong Postman:

1. Chọn **POST**, URL `http://localhost:8080/tasks`.
2. Chọn **Body → raw → JSON**. Header là `Content-Type: application/json`.
3. Gửi JSON sau:

```json
{
  "title": "Hoc Spring Boot",
  "description": "Thuc hanh PostMapping va RequestBody",
  "priority": "HIGH",
  "assignedTo": 1
}
```

Kết quả: **201 Created**, body là task vừa lưu. Nếu ứng dụng vừa khởi động với 10 task mẫu, ID mới là `11`. Gọi GET `/tasks` sẽ thấy task mới trong danh sách; bộ lọc `?search=Spring` cũng tìm được task này.

Đổi `assignedTo` thành `999` rồi gửi lại sẽ nhận **400 Bad Request**:

```json
{
  "message": "Người được giao việc không tồn tại."
}
```

`@PostMapping` nhận request POST tại `/tasks`; `@RequestBody Task newTask` yêu cầu Spring chuyển JSON trong body thành đối tượng `Task`. Trong service, `userService.findUserById(newTask.getAssignedTo())` kiểm tra người được giao việc. Chỉ khi dữ liệu hợp lệ mới gọi `taskRepository.save(newTask)`.

Controller dùng `ResponseEntity.status(HttpStatus.CREATED).body(savedTask)` khi thành công. Khi service/repository ném `IllegalArgumentException` do dữ liệu không hợp lệ, controller bắt lỗi và trả `ResponseEntity.badRequest()` kèm thông báo JSON. Kiểu `ResponseEntity<?>` cho phép body thành công là `Task`, còn body lỗi là đối tượng chứa `message`.

Quy tắc kiểm tra:

| Trường hợp | Kết quả |
| --- | --- |
| Tiêu đề hợp lệ, `assignedTo` là ID user tồn tại | 201, lưu task |
| `assignedTo` thiếu, null hoặc không tồn tại | 400, không lưu |
| `title` thiếu, null hoặc chỉ có khoảng trắng | 400, không lưu |
| Không gửi `id` hoặc gửi `id: null` | Tự cấp ID lớn hơn ID hiện có |
| Gửi `id` là số dương chưa có | Dùng ID được gửi |
| Gửi `id` đã tồn tại, bằng 0 hoặc âm | 400, không ghi đè task cũ |
| Body rỗng, JSON hỏng hoặc `assignedTo` là đối tượng `{ "id": 1 }` | Spring trả 400 |

`description` và `priority` không bắt buộc trong bài này. Không cần gửi toàn bộ thông tin user trong body. ID mẫu ban đầu từ 1 đến 10 đã được dùng; khi thử nên bỏ `id` để ứng dụng tự cấp, thay vì sao chép `id: 10` từ ảnh đề bài.

## Kiểm tra tự động

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
.\gradlew.bat test bootJar
```

Test khởi động ứng dụng trên một cổng trống, gọi HTTP thật tới cả hai API và kiểm tra mã 200, JSON, số lượng tối thiểu, các trường dữ liệu, ID không trùng và user được giao việc tồn tại. Các test bộ lọc kiểm tra tìm chuỗi con, hoa/thường, khoảng trắng, từ khóa rỗng, không có kết quả và dữ liệu gốc không bị mất sau khi lọc. Nếu Spring không khởi tạo được bean, test cũng thất bại.

`TaskCreationTests` kiểm tra POST 201, ID tự cấp không trùng, ID do client gửi, task xuất hiện trong GET/bộ lọc, các trường hợp 400 và việc request lỗi không làm thay đổi danh sách. Dữ liệu được tạo trong test không lưu vào ứng dụng bạn chạy bằng IntelliJ.

## Tự thêm dữ liệu

Thêm `new User(...)` hoặc `new Task(...)` trong repository tương ứng. Dấu phẩy ngăn cách các phần tử; phần tử cuối trong `List.of(...)` không có dấu phẩy phía sau. Khi thay đổi dữ liệu mẫu khớp từ khóa `GuY` hoặc `uSeR`, cập nhật kết quả mong đợi của test tìm kiếm tương ứng.

Dữ liệu chỉ tồn tại trong bộ nhớ và được tạo lại khi ứng dụng khởi động. Bài tập này chưa dùng cơ sở dữ liệu.
