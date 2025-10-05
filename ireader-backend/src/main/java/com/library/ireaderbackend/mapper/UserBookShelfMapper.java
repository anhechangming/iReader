package com.library.ireaderbackend.mapper;

import com.library.ireaderbackend.entity.Book;
import com.library.ireaderbackend.entity.UserBookShelf;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserBookShelfMapper {

    @Select("SELECT * FROM user_shelf WHERE user_id=#{userId}")
    List<UserBookShelf> findByUserId(Long userId);

    @Insert("INSERT INTO user_shelf(user_id, book_id, added_time) VALUES(#{userId}, #{bookId}, NOW())")
    void addBook(Long userId, Long bookId);

    @Delete("DELETE FROM user_shelf WHERE user_id=#{userId} AND book_id=#{bookId}")
    void removeBook(Long userId, Long bookId);

    @Select("SELECT COUNT(*) FROM user_shelf WHERE user_id=#{userId} AND book_id=#{bookId}")
    int existsByUserIdAndBookId(Long userId, Long bookId);
}

