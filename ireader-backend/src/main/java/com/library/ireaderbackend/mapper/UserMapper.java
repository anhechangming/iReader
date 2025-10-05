package com.library.ireaderbackend.mapper;

import com.library.ireaderbackend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM user WHERE phone = #{phone}")
    User selectByPhone(String phone);

    @Insert("INSERT INTO user(phone, password, nickname, create_time) " +
            "VALUES(#{phone}, #{password}, #{nickname}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT * FROM user WHERE id = #{id}")
    User selectById(Long id);
}
