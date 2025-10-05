package com.library.ireaderbackend.service;

import com.library.ireaderbackend.entity.Book;
import com.library.ireaderbackend.entity.UserBookShelf;
import com.library.ireaderbackend.mapper.BookMapper;
import com.library.ireaderbackend.mapper.UserBookShelfMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserBookShelfService {

    @Autowired
    private UserBookShelfMapper userBookShelfMapper;
    @Autowired
    private BookMapper bookMapper;

    public List<Book> getBooksByUser(Long userId, String keyword) {
        List<UserBookShelf> shelf = userBookShelfMapper.findByUserId(userId);
        if (shelf.isEmpty()) return new ArrayList<>();
        List<Long> bookIds = shelf.stream().map(UserBookShelf::getBookId).toList();

        if (keyword != null && !keyword.isEmpty()) {
            return bookMapper.findByIdsAndKeyword(bookIds, keyword);
        } else {
            return bookMapper.findByIds(bookIds);
        }
    }

    public void addBook(Long userId, Long bookId) {
        userBookShelfMapper.addBook(userId, bookId);
    }

    public void removeBook(Long userId, Long bookId) {
        userBookShelfMapper.removeBook(userId, bookId);
    }

    public boolean existsByUserIdAndBookId(Long userId, Long bookId) {
        return userBookShelfMapper.existsByUserIdAndBookId(userId, bookId)>0 ;
    }
}
