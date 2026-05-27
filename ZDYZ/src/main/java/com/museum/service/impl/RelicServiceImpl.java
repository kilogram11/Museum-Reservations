package com.museum.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.entity.Relic;
import com.museum.mapper.RelicMapper;
import com.museum.service.RelicService;
import org.springframework.stereotype.Service;

@Service
public class RelicServiceImpl extends ServiceImpl<RelicMapper, Relic> implements RelicService {
}
