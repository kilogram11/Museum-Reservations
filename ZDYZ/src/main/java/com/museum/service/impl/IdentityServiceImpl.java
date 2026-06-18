package com.museum.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdcardUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.museum.common.constant.BookingConstant;
import com.museum.common.enums.IdentityStatus;
import com.museum.common.exception.BusinessException;
import com.museum.common.exception.ErrorCode;
import com.museum.common.utils.IdGenerator;
import com.museum.entity.Identity;
import com.museum.mapper.IdentityMapper;
import com.museum.service.IdentityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class IdentityServiceImpl extends ServiceImpl<IdentityMapper, Identity> implements IdentityService {

    @Override
    public List<Identity> listMyIdentity(String userId) {
        if (StrUtil.isBlank(userId)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<Identity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Identity::getUserId, userId);
        wrapper.orderByDesc(Identity::getId);

        List<Identity> allMatches = this.list(wrapper);
        if (CollUtil.isEmpty(allMatches)) {
            return Collections.emptyList();
        }

        return filterByExactUserIdMatch(allMatches, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveIdentity(String userId, Identity identity) {
        validateIdentityInput(identity);

        Identity exist = findByCard(identity.getIdentityCard());

        if (exist == null) {
            createNewIdentityWithUser(userId, identity);
        } else {
            updateExistingIdentityAssociation(userId, identity, exist);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeIdentity(String userId, String identityId) {
        Identity identity = this.lambdaQuery()
                .eq(Identity::getIdentityId, identityId).one();
        if (identity == null) {
            return;
        }

        removeUserFromIdentity(userId, identity);
    }

    // ==================== 私有方法 ====================

    private void validateIdentityInput(Identity identity) {
        if (StrUtil.hasBlank(identity.getIdentityName(), identity.getIdentityCard())) {
            throw BusinessException.of("姓名和证件号不能为空");
        }
        if (!IdcardUtil.isValidCard(identity.getIdentityCard())) {
            throw BusinessException.of("身份证格式不正确");
        }
    }

    private Identity findByCard(String card) {
        return this.lambdaQuery().eq(Identity::getIdentityCard, card).one();
    }

    private List<Identity> filterByExactUserIdMatch(List<Identity> candidates, String userId) {
        List<Identity> result = new ArrayList<>();
        for (Identity identity : candidates) {
            if (isUserAssociated(identity.getUserId(), userId)) {
                result.add(identity);
            }
        }
        return result;
    }

    private boolean isUserAssociated(String userIdsJson, String targetUserId) {
        if (StrUtil.isBlank(userIdsJson)) {
            return false;
        }
        try {
            JSONArray array = JSONUtil.parseArray(userIdsJson);
            return array.contains(targetUserId);
        } catch (Exception e) {
            return false;
        }
    }

    private void createNewIdentityWithUser(String userId, Identity identity) {
        Identity newIdentity = new Identity();
        newIdentity.setId(IdGenerator.generateId());
        newIdentity.setIdentityId(IdGenerator.generateIdentityId());
        newIdentity.setIdentityName(identity.getIdentityName());
        newIdentity.setIdentityCard(identity.getIdentityCard());
        newIdentity.setIdentityMobile(identity.getIdentityMobile());
        newIdentity.setIdentityStatus(IdentityStatus.NORMAL.getCode());
        newIdentity.setUserBanNum(0);
        newIdentity.setPid(BookingConstant.DEFAULT_PID);

        JSONArray array = new JSONArray();
        array.add(userId);
        newIdentity.setUserId(array.toString());

        this.save(newIdentity);
    }

    private void updateExistingIdentityAssociation(String userId, Identity newData,
                                                     Identity existing) {
        JSONArray array = parseUserIdArray(existing.getUserId());

        if (!array.contains(userId)) {
            array.add(userId);
        }

        existing.setUserId(array.toString());
        existing.setIdentityName(newData.getIdentityName());
        if (StrUtil.isNotBlank(newData.getIdentityMobile())) {
            existing.setIdentityMobile(newData.getIdentityMobile());
        }

        this.updateById(existing);
    }

    private void removeUserFromIdentity(String userId, Identity identity) {
        JSONArray array = parseUserIdArray(identity.getUserId());

        if (array.contains(userId)) {
            array.remove(userId);
            identity.setUserId(array.toString());
            this.updateById(identity);
        }
    }

    private JSONArray parseUserIdArray(String jsonStr) {
        return JSONUtil.parseArray(StrUtil.isBlank(jsonStr)
                ? BookingConstant.EMPTY_JSON_ARRAY : jsonStr);
    }
}
