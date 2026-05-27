package com.museum.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.common.dto.NewsAddDTO;
import com.museum.common.exception.BusinessException;
import com.museum.entity.Museum;
import com.museum.entity.News;
import com.museum.mapper.MuseumMapper;
import com.museum.mapper.NewsMapper;
import com.museum.service.NewsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class NewsServiceImpl extends ServiceImpl<NewsMapper, News> implements NewsService {

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private MuseumMapper museumMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addNews(NewsAddDTO dto) {
        if (StrUtil.isBlank(dto.getNewsTitle())) {
            throw new BusinessException(500, "标题不能为空");
        }
        if (StrUtil.isBlank(dto.getNewsDesc())) {
            throw new BusinessException(500, "内容不能为空");
        }

        long now = System.currentTimeMillis();
        News news = new News();
        news.setId(IdUtil.fastSimpleUUID());
        news.setNewsId("news_" + IdUtil.fastSimpleUUID());
        news.setNewsTitle(dto.getNewsTitle());
        news.setNewsDesc(dto.getNewsDesc());
        // 若有更多扩展字段，这里进行映射
        if (StrUtil.isNotBlank(dto.getNewsContent())) {
            // 假设我们暂存在这里，实际项目中可能需要更复杂的字段，或拼接到Desc
            // 此处仅作示例，如果 News 实体没有 extra 字段，可以暂不处理或拼接到 Desc
        }

        // 默认状态 1
        news.setNewsStatus(dto.getNewsStatus() != null ? dto.getNewsStatus() : 1);
        news.setNewsViewCnt(0);
        news.setNewsAddTime(now);
        news.setNewsEditTime(now);
        news.setPid("1");

        // 特殊需求：NEWS_ADD_IP 自动读取 MUSEUM_ADD_TIME 最大的场馆 MUSEUM_TITLE
        QueryWrapper<Museum> museumWrapper = new QueryWrapper<>();
        museumWrapper.orderByDesc("MUSEUM_ADD_TIME");
        museumWrapper.last("LIMIT 1");
        Museum latestMuseum = museumMapper.selectOne(museumWrapper);

        String ipSource = (latestMuseum != null) ? latestMuseum.getMuseumTitle() : "默认来源";
        news.setNewsAddIp(ipSource);
        news.setNewsEditIp(ipSource); // 同样填充到编辑 IP

        newsMapper.insert(news);
        log.info("发布公告成功: {}, 来源: {}", news.getNewsTitle(), ipSource);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delNews(String id) {
        if (StrUtil.isBlank(id))
            return;
        newsMapper.deleteById(id);
        log.info("删除公告: {}", id);
    }

    @Override
    public Page<News> dataList(String keyword, Integer page, Integer limit) {
        Page<News> pageParam = new Page<>(page, limit);
        QueryWrapper<News> wrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like("NEWS_TITLE", keyword);
        }
        wrapper.orderByDesc("NEWS_ADD_TIME");
        return newsMapper.selectPage(pageParam, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public News viewNews(String id) {
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new BusinessException(500, "公告不存在");
        }

        // 浏览量 +1
        // 注意：由于我们之前为了兼容 XML 移除了 updateById 的 @Param
        // 这里直接修改实体并更新是安全的
        news.setNewsViewCnt(news.getNewsViewCnt() + 1);
        newsMapper.updateById(news);

        return news;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editNews(com.museum.common.dto.NewsEditDTO dto) {
        if (StrUtil.isBlank(dto.getId())) {
            throw new BusinessException(500, "公告ID不能为空");
        }
        News news = newsMapper.selectById(dto.getId());
        if (news == null) {
            throw new BusinessException(500, "公告不存在");
        }

        if (StrUtil.isNotBlank(dto.getNewsTitle())) {
            news.setNewsTitle(dto.getNewsTitle());
        }
        if (StrUtil.isNotBlank(dto.getNewsDesc())) {
            news.setNewsDesc(dto.getNewsDesc());
        }
        if (dto.getNewsStatus() != null) {
            news.setNewsStatus(dto.getNewsStatus());
        }

        news.setNewsEditTime(System.currentTimeMillis());
        // Edit IP 更新逻辑（可选，若需要可跟随 addNews 逻辑或通过上下文获取）
        // 这里暂时不修改 EditIP，或设为最新场馆 Title 以表示 "Manager Action"
        QueryWrapper<Museum> museumWrapper = new QueryWrapper<>();
        museumWrapper.orderByDesc("MUSEUM_ADD_TIME");
        museumWrapper.last("LIMIT 1");
        Museum latestMuseum = museumMapper.selectOne(museumWrapper);
        if (latestMuseum != null) {
            news.setNewsEditIp(latestMuseum.getMuseumTitle());
        }

        newsMapper.updateById(news);
        log.info("修改公告成功: {}", dto.getId());
    }
}
