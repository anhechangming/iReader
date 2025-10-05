package com.library.ireaderbackend.controller;

import com.library.ireaderbackend.Utils.EpubParser;
import com.library.ireaderbackend.Utils.PdfParser;
import com.library.ireaderbackend.Utils.TxtParser;
import com.library.ireaderbackend.entity.Book;
import com.library.ireaderbackend.entity.BookContent;
import com.library.ireaderbackend.service.BookContentService;
import com.library.ireaderbackend.service.BookService;
import com.library.ireaderbackend.service.UserBookShelfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("api/bookshelf")
public class BookShelfController {

    @Autowired
    private UserBookShelfService userBookShelfService;
    @Autowired
    private BookService bookService;
    @Autowired
    private BookContentService bookContentService;
    @Autowired
    private UserBookShelfService userShelfService;
    // 获取用户书架列表
    @GetMapping("/list")
    public ResponseEntity<List<Book>> getUserBooks(
            @RequestParam Long userId,
            @RequestParam(required = false) String keyword) {
        List<Book> books = userBookShelfService.getBooksByUser(userId, keyword);
        return ResponseEntity.ok(books);
    }

//    @PostMapping("/add")
//    public ResponseEntity<String> addBookToShelf(@RequestParam Long userId, @RequestParam Long bookId) {
//        userBookShelfService.addBook(userId, bookId);
//        return ResponseEntity.ok("添加成功");
//    }
@PostMapping("/add")
public ResponseEntity<String> addBookToShelf(@RequestParam Long userId, @RequestParam Long bookId) {
    try {
        if (userId == null || bookId == null) {
            return ResponseEntity.badRequest().body("userId 或 bookId 不能为空");
        }
        // 先检查是否已存在
        boolean exists = userBookShelfService.existsByUserIdAndBookId(userId, bookId);
        if (exists) {
            // 409 表示冲突（资源已存在）
            return ResponseEntity.status(409).body("该书已在您的书架中");
        }
        userBookShelfService.addBook(userId, bookId);
        return ResponseEntity.ok("添加成功");
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(500).body("服务器错误: " + e.getMessage());
    }
}


    @DeleteMapping("/remove")
    public ResponseEntity<String> removeBookFromShelf(@RequestParam Long userId, @RequestParam Long bookId) {
        userBookShelfService.removeBook(userId, bookId);
        return ResponseEntity.ok("移除成功");
    }

    @PostMapping("/uploadAndParse")
    @Transactional
    public ResponseEntity<String> uploadAndParse(@RequestParam("file") MultipartFile file,
                                                 @RequestParam("userId") Long userId) {
        try {
            String filename = file.getOriginalFilename();
            if (filename == null) return ResponseEntity.badRequest().body("文件名为空");
            String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            if (!List.of("txt", "epub", "pdf").contains(ext)) {
                return ResponseEntity.badRequest().body("仅支持 txt / epub / pdf");
            }

            // 替换原来的 Path baseDir
            Path baseDir = Paths.get(System.getProperty("user.dir"), "uploads", "books");
            if (!Files.exists(baseDir)) Files.createDirectories(baseDir);
            Path savePath = baseDir.resolve(System.currentTimeMillis() + "_" + filename);
            file.transferTo(savePath.toFile());


            Book book = new Book();
            book.setTitle(stripExtension(filename));
            book.setAuthor("未知");      // 必须非空
            book.setCover("");           // 可选
            book.setCategory("");        // 可选
            book.setIntro("");           // 可选
            book.setFilePath(savePath.toAbsolutePath().toString());
            book.setFileType(ext);
            book.setIsFree(1);
            book.setPublisher("");
            book.setPublishTime(null);
            book.setIsbn("");
            book.setBrand("");
            book.setUploader_id(userId);
            book.setVisibility("private"); // 或者 "pending"，区分后台审核
            bookService.save(book);

            // 2. 解析成章节
            List<BookContent> contents;
            if ("txt".equals(ext)) {
                contents = TxtParser.parseTxt(savePath, book.getId());
            } else if ("pdf".equals(ext)) {
              //  contents = PdfParser.parsePdf(savePath.toFile(), book.getId());
                contents = PdfParser.parseEbook(savePath.toFile(), book.getId());
            } else { // epub
                // 这里调用 EpubParser。若你的 EpubParser 只接受 classpath resource, 我建议把它改为 File/InputStream 版本。
                // 假设我们有 parseEpubFile(File, bookId, staticImgDir)
                String staticImgDir = "ireader/uploads/static/book/" + book.getId();
                contents = EpubParser.parseEpubFile(savePath.toFile(), book.getId(), staticImgDir);
            }

            // 3. 批量插入 BookContent
            bookContentService.saveAll(contents);

            // 4. 自动加入用户书架
            userShelfService.addBook(userId, book.getId());

            return ResponseEntity.ok("上传并解析成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("上传解析出错：" + e.getMessage());
        }
    }

    private String stripExtension(String name) {
        if (name == null) return "";
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(0, i) : name;
    }

}