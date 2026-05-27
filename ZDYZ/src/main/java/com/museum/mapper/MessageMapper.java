package com.museum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.museum.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
