package com.library.ireaderbackend.mapper;

import com.library.ireaderbackend.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookMapper {
    List<Book> findAll();
    Book findById(@Param("id") Long id);
    List<Book> findByCategory(@Param("category") String category);
    List<Book> searchByKeyword(@Param("keyword") String keyword);
    List<Book> findByCategoryAndKeyword(@Param("category") String category, @Param("keyword") String keyword);
    void insert(Book book);
    void update(Book book);
    void delete(@Param("id") Long id);

    List<Book> findByIdsAndKeyword(List<Long> bookIds, String keyword);

    List<Book> findByIds(List<Long> bookIds);
}
