package com.museum.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdcardUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.common.exception.BusinessException;
import com.museum.common.utils.IdGenerator;
import com.museum.entity.Identity;
import com.museum.mapper.IdentityMapper;
import com.museum.service.IdentityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 身份业务实现类
 */
@Service
public class IdentityServiceImpl extends ServiceImpl<IdentityMapper, Identity> implements IdentityService {

    @Override
    public List<Identity> listMyIdentity(String userId) {
        if (StrUtil.isBlank(userId))
            return new ArrayList<>();

        // 查询 user_id 字段包含当前 userId 的记录
        // 注意：由于是 JSON 字符串，模糊查询可能存在误判（比如 userId "1" 会匹配到 "12"）
        // 更严谨的做法是 db_find 出来后在内存过滤，或者使用 MySQL JSON 函数（如果数据库支持）
        // 考虑到兼容性，这里先用 like 查出来再过滤
        // userId 在 json 数组中，格式 ["id1", "id2"]

        LambdaQueryWrapper<Identity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Identity::getUserId, userId);
        wrapper.orderByDesc(Identity::getId);

        List<Identity> allMatches = this.list(wrapper);
        List<Identity> result = new ArrayList<>();

        for (Identity identity : allMatches) {
            String jsonStr = identity.getUserId();
            if (StrUtil.isNotBlank(jsonStr)) {
                JSONArray array = JSONUtil.parseArray(jsonStr);
                if (array.contains(userId)) {
                    result.add(identity);
                }
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveIdentity(String userId, Identity identity) {
        String name = identity.getIdentityName();
        String card = identity.getIdentityCard();

        if (StrUtil.hasBlank(name, card)) {
            throw BusinessException.of("姓名和证件号不能为空");
        }
        if (!IdcardUtil.isValidCard(card)) {
            // 这里可以开启校验，测试阶段如需放宽可注释
            // throw BusinessException.of("身份证格式不正确");
        }

        // 1. 根据身份证号查询是否存在该人
        Identity exist = this.lambdaQuery().eq(Identity::getIdentityCard, card).one();

        if (exist == null) {
            // 不存在 -> 新建
            exist = new Identity();
            exist.setId(IdGenerator.generateId());
            exist.setIdentityId(IdGenerator.generateActivityId()); // 借用一下生成逻辑
            exist.setIdentityName(name);
            exist.setIdentityCard(card);
            exist.setIdentityMobile(identity.getIdentityMobile());
            exist.setIdentityStatus(1);
            exist.setUserBanNum(0);
            exist.setPid("1");

            // 初始化 userId 数组
            JSONArray array = new JSONArray();
            array.add(userId);
            exist.setUserId(array.toString());

            this.save(exist);
        } else {
            // 存在 -> 检查关联
            String jsonStr = exist.getUserId();
            JSONArray array = JSONUtil.parseArray(StrUtil.isBlank(jsonStr) ? "[]" : jsonStr);

            if (!array.contains(userId)) {
                array.add(userId);
                exist.setUserId(array.toString());
                // 更新名字和手机号（以最新的为准? 或者保留原样? 这里策略是以最新提交的为准）
                exist.setIdentityName(name);
                if (StrUtil.isNotBlank(identity.getIdentityMobile())) {
                    exist.setIdentityMobile(identity.getIdentityMobile());
                }
                this.updateById(exist);
            } else {
                // 已关联，仅更新信息
                exist.setIdentityName(name);
                if (StrUtil.isNotBlank(identity.getIdentityMobile())) {
                    exist.setIdentityMobile(identity.getIdentityMobile());
                }
                this.updateById(exist);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeIdentity(String userId, String identityId) {
        // identityId 这里指业务ID还是主键ID? 建议用业务ID查询
        Identity identity = this.lambdaQuery().eq(Identity::getIdentityId, identityId).one();
        if (identity == null)
            return;

        String jsonStr = identity.getUserId();
        if (StrUtil.isBlank(jsonStr))
            return;

        JSONArray array = JSONUtil.parseArray(jsonStr);
        if (array.contains(userId)) {
            array.remove(userId);

            // 如果解除关联后数组为空，是否物理删除该条目？
            // 策略：不物理删除，因为可能有历史预约记录关联它（虽然预约表主要看 join 表，但保留着无妨，或者保留以防黑名单数据丢失）
            // 但如果这是一个"我的游客"功能，删除了就应该看不到了。

            identity.setUserId(array.toString());
            this.updateById(identity);
        }
    }
}
