package com.library.ireaderbackend.controller;

import com.library.ireaderbackend.entity.Book;
import com.library.ireaderbackend.entity.BookContent;
import com.library.ireaderbackend.service.BookContentService;
import com.library.ireaderbackend.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/book")
public class BookController {
    @Autowired
    private BookService bookService;
    @Autowired
    private BookContentService bookContentService;

    @GetMapping("/list")
    public List<Book> getBookList(@RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String category) {
        return bookService.getBookList(keyword, category);
    }

    @GetMapping("/detail/{id}")
    public Book getBookDetail(@PathVariable Long id) {
        return bookService.getBookDetail(id);
    }


    // 获取章节列表（若 DB 无则解析 epub 并写入）
    @GetMapping("/content/{bookId}")
    public List<BookContent> getChapters(@PathVariable Long bookId) throws Exception {
        return bookContentService.getChapters(bookId);
    }

    // 获取某一章内容（返回 HTML 字符串）
    @GetMapping("/content/{bookId}/chapter/{order}")
    public BookContent getChapter(@PathVariable Long bookId, @PathVariable Integer order) {
        return bookContentService.getChapter(bookId, order);
    }
}