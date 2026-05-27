package com.museum.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.common.exception.BusinessException;
import com.museum.entity.Identity;
import com.museum.mapper.IdentityMapper;
import com.museum.service.BlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 黑名单业务实现
 */
@Service
public class BlacklistServiceImpl implements BlacklistService {

    @Autowired
    private IdentityMapper identityMapper;

    @Override
    public Page<Identity> list(String keyword, Integer page, Integer limit, Integer status) {
        Page<Identity> p = new Page<>(page, limit);
        QueryWrapper<Identity> wrapper = new QueryWrapper<>();
        // 如果未指定 status，默认查询黑名单 (0)；如果指定了 (如 1=正常)，则按指定查询
        wrapper.eq("IDENTITY_STATUS", status != null ? status : 0);

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like("IDENTITY_NAME", keyword)
                    .or().like("IDENTITY_MOBILE", keyword)
                    .or().like("IDENTITY_CARD", keyword));
        }

        // 排序规则：查黑名单按拉黑时间倒序，查正常用户按创建时间或ID排序
        if (status == null || status == 0) {
            wrapper.orderByDesc("BLACK_START_TIME");
        } else {
            wrapper.orderByDesc("_id");
        }
        return identityMapper.selectPage(p, wrapper);
    }

    @Override
    public void add(String identityId, String reason, Long endTime) {
        Identity identity = identityMapper.selectOne(new QueryWrapper<Identity>().eq("IDENTITY_ID", identityId));
        if (identity == null) {
            throw new BusinessException(500, "用户不存在");
        }
        if (endTime == null) {
            throw new BusinessException(500, "请设置拉黑结束时间");
        }

        identity.setIdentityStatus(0); // 0=黑名单
        identity.setBlackStartTime(System.currentTimeMillis());
        identity.setBlackEndTime(endTime);
        identity.setUserCheckType(0); // 0=手动拉黑

        // 记录拉黑原因 (存入 identityObj 扩展字段)
        try {
            Map<String, Object> obj = new HashMap<>();
            if (StrUtil.isNotBlank(identity.getIdentityObj())) {
                obj = JSONUtil.toBean(identity.getIdentityObj(), Map.class);
            }
            obj.put("blackReason", reason);
            identity.setIdentityObj(JSONUtil.toJsonStr(obj));
        } catch (Exception e) {
            // ignore
        }

        identityMapper.updateById(identity);
    }

    @Override
    public void updateEndTime(String identityId, Long endTime) {
        Identity identity = identityMapper.selectOne(new QueryWrapper<Identity>().eq("IDENTITY_ID", identityId));
        if (identity == null) {
            throw new BusinessException(500, "用户不存在");
        }
        if (endTime == null) {
            throw new BusinessException(500, "时间不能为空");
        }

        identity.setBlackEndTime(endTime);
        // 如果当前是正常状态，是否要改为黑名单？通常 updateTime 是针对已黑名单用户的调整
        // 这里仅更新时间，不强制改状态，除非需求明确

        identityMapper.updateById(identity);
    }

    @Override
    public void remove(String identityId) {
        Identity identity = identityMapper.selectOne(new QueryWrapper<Identity>().eq("IDENTITY_ID", identityId));
        if (identity == null)
            return;

        identity.setIdentityStatus(1); // 恢复正常
        identity.setBlackStartTime(null);
        identity.setBlackEndTime(null);
        identityMapper.updateById(identity);
    }

    @Override
    public java.util.List<Identity> getAllBlacklistForExport() {
        QueryWrapper<Identity> wrapper = new QueryWrapper<>();
        wrapper.eq("IDENTITY_STATUS", 0);
        wrapper.orderByDesc("BLACK_START_TIME");
        return identityMapper.selectList(wrapper);
    }
}
