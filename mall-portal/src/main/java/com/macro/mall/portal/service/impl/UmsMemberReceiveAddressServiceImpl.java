package com.macro.mall.portal.service.impl;

import com.macro.mall.mapper.UmsMemberReceiveAddressMapper;
import com.macro.mall.model.UmsMember;
import com.macro.mall.model.UmsMemberReceiveAddress;
import com.macro.mall.model.UmsMemberReceiveAddressExample;
import com.macro.mall.portal.service.UmsMemberReceiveAddressService;
import com.macro.mall.portal.service.UmsMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 用户地址管理Service实现类
 * https://gitee.com/zscat-platform/mall on 2018/8/28.
 */
@Service
public class UmsMemberReceiveAddressServiceImpl implements UmsMemberReceiveAddressService {
    @Autowired
    private UmsMemberService memberService;
    @Autowired
    private UmsMemberReceiveAddressMapper addressMapper;

    @Override
    public int add(UmsMemberReceiveAddress address) {
        UmsMember currentMember = memberService.getCurrentMember();
        address.setMemberId(currentMember.getId());
        if (this.list()!=null && this.list().size()>0){
            address.setDefaultStatus(0);
        }else {
            address.setDefaultStatus(1);
        }
        return addressMapper.insert(address);
    }

    @Override
    public int delete(Long id) {
        UmsMember currentMember = memberService.getCurrentMember();
        UmsMemberReceiveAddressExample example = new UmsMemberReceiveAddressExample();
        example.createCriteria().andMemberIdEqualTo(currentMember.getId()).andIdEqualTo(id);
        return addressMapper.deleteByExample(example);
    }

    @Override
    public int update(Long id, UmsMemberReceiveAddress address) {
        address.setId(null);
        UmsMember currentMember = memberService.getCurrentMember();
        UmsMemberReceiveAddressExample example = new UmsMemberReceiveAddressExample();
        example.createCriteria().andMemberIdEqualTo(currentMember.getId()).andIdEqualTo(id);
        return addressMapper.updateByExampleSelective(address, example);
    }

    @Override
    public List<UmsMemberReceiveAddress> list() {
        UmsMember currentMember = memberService.getCurrentMember();
        UmsMemberReceiveAddressExample example = new UmsMemberReceiveAddressExample();
        example.createCriteria().andMemberIdEqualTo(currentMember.getId());
        return addressMapper.selectByExample(example);
    }

    @Override
    public UmsMemberReceiveAddress getItem(Long id) {
        UmsMember currentMember = memberService.getCurrentMember();
        UmsMemberReceiveAddressExample example = new UmsMemberReceiveAddressExample();
        example.createCriteria().andMemberIdEqualTo(currentMember.getId()).andIdEqualTo(id);
        List<UmsMemberReceiveAddress> addressList = addressMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(addressList)) {
            return addressList.get(0);
        }
        return null;
    }

    @Override
    public UmsMemberReceiveAddress getDefaultItem() {
        UmsMember currentMember = memberService.getCurrentMember();
        UmsMemberReceiveAddressExample example = new UmsMemberReceiveAddressExample();
        example.createCriteria().andMemberIdEqualTo(currentMember.getId()).andDefaultStatusEqualTo(1);
        List<UmsMemberReceiveAddress> addressList = addressMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(addressList)) {
            return addressList.get(0);
        }
        return null;
    }

    @Transactional
    @Override
    public int setDefault(Long id) {
        UmsMember currentMember = memberService.getCurrentMember();
         addressMapper.updateStatusByMember(currentMember.getId());

        UmsMemberReceiveAddress def = new UmsMemberReceiveAddress();
        def.setId(id);
        def.setDefaultStatus(1);
        addressMapper.updateByPrimaryKeySelective(def);
        return 1;
    }
}
