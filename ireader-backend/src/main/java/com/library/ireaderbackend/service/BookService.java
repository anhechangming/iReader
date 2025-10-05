package com.library.ireaderbackend.service;

import com.library.ireaderbackend.entity.Book;
import com.library.ireaderbackend.entity.BookContent;
import com.library.ireaderbackend.mapper.BookMapper;
import com.library.ireaderbackend.mapper.BookContentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BookContentMapper bookContentMapper;

    /**
     * 获取书籍列表，可以按关键词和分类过滤
     */
    public List<Book> getBookList(String keyword, String category) {
        if (keyword != null && !keyword.isEmpty() && category != null && !category.isEmpty()) {
            // 同时按关键词和分类查询
            return bookMapper.findByCategoryAndKeyword(category, keyword);
        } else if (keyword != null && !keyword.isEmpty()) {
            // 按关键词查询
            return bookMapper.searchByKeyword(keyword);
        } else if (category != null && !category.isEmpty()) {
            // 按分类查询
            return bookMapper.findByCategory(category);
        } else {
            // 查询所有
            return bookMapper.findAll();
        }
    }

    public void save(Book book) {
        bookMapper.insert(book);
    }
    /**
     * 根据 ID 获取书籍详情
     */
    public Book getBookDetail(Long id) {
        return bookMapper.findById(id);
    }

    /**
     * 获取书籍内容（按章节顺序）
     */
    public List<BookContent> getBookContent(Long bookId) {
        return bookContentMapper.findByBookId(bookId);
    }

    /**
     * 新增书籍
     */
    public void addBook(Book book) {
        bookMapper.insert(book);
    }

    /**
     * 更新书籍
     */
    public void updateBook(Book book) {
        bookMapper.update(book);
    }

    /**
     * 删除书籍
     */
    public void deleteBook(Long id) {
        bookMapper.delete(id);
    }
}
