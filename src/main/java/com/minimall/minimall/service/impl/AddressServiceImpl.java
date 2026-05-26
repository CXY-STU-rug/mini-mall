package com.minimall.minimall.service.impl;

import com.minimall.minimall.entity.Address;
import com.minimall.minimall.mapper.AddressMapper;
import com.minimall.minimall.service.IAddressService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 收货地址表 服务实现类
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements IAddressService {

}
