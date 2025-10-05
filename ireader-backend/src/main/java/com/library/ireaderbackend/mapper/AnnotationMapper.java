package com.library.ireaderbackend.mapper;

import com.library.ireaderbackend.entity.Annotation;
import org.apache.ibatis.annotations.*;


import java.util.List;

@Mapper
public interface AnnotationMapper {
    int insert(Annotation annotation);
    List<Annotation> findByBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
    List<Annotation> findByChapter(@Param("userId") Long userId, @Param("bookContentId") Long bookContentId);
    int delete(Long id);
    int update(Annotation annotation);
}
