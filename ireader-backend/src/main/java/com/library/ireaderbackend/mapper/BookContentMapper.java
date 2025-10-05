package com.library.ireaderbackend.mapper;

import com.library.ireaderbackend.entity.BookContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface BookContentMapper {
    List<BookContent> findByBookId(@Param("bookId") Long bookId);
    BookContent findByBookIdAndOrder(@Param("bookId") Long bookId, @Param("order") Integer order);
    void insert(BookContent bc);
    void insertBatch(List<BookContent> list);
    void deleteByBookId(@Param("bookId") Long bookId);
}
