package com.minimall.minimall.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.minimall.minimall.common.exception.BusinessException;
import com.minimall.minimall.common.result.Result;
import com.minimall.minimall.common.util.UserContext;
import com.minimall.minimall.entity.Address;
import com.minimall.minimall.service.IAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 收货地址表 前端控制器
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
    @RestController
    @RequestMapping("/api/addresses")
    public class AddressController {

        @Autowired
        private IAddressService addressService;

        // ========== 1. 我的地址列表 ==========
        @GetMapping
        public Result<List<Address>> list() {
            Long userId = UserContext.getUserId();         // ① 取当前用户

            QueryWrapper<Address> w = new QueryWrapper<>();
            w.eq("user_id", userId);                   // ② 强制按 userId 过滤
            w.orderByDesc("is_default")                   // 默认地址排前面
                    .orderByDesc("create_time");

            return Result.success(addressService.list(w));
        }

        // ========== 2. 地址详情 ==========
        @GetMapping("/{id}")
        public Result<Address> detail(@PathVariable Long id) {
            Address addr = getAndCheckOwn(id);            // ③ 调下面写的越权校验方法
            return Result.success(addr);
        }

        // ========== 3. 新增地址 ==========
        @PostMapping
        public Result<Address> create(Address address) {
            // ④ 关键：强制覆盖 userId，不信前端传的
            address.setUserId(UserContext.getUserId());

            addressService.save(address);
            return Result.success(address);
        }

        // ========== 4. 修改地址 ==========
        @PutMapping("/{id}")
        public Result<Address> update(@PathVariable Long id, Address address) {
            getAndCheckOwn(id);                           // ⑤ 先校验"这条 id 真是你的"

            address.setId(id);
            address.setUserId(UserContext.getUserId());   // ⑥ 防恶意改成别人的

            addressService.updateById(address);
            return Result.success(address);
        }

        // ========== 5. 删除地址 ==========
        @DeleteMapping("/{id}")
        public Result<Void> delete(@PathVariable Long id) {
            getAndCheckOwn(id);                           // ⑦ 先校验

            addressService.removeById(id);                 // ⑧ 逻辑删除
            return Result.success();
        }

        // ========== 私有工具方法：查 + 越权校验 ==========
        /**
         * 按 id 查地址，并校验是当前用户的。
         * 不存在 → 抛"地址不存在"
         * 存在但不是你的 → 抛"无权访问"
         */
        private Address getAndCheckOwn(Long id) {
            Address addr = addressService.getById(id);
            if (addr == null) {
                throw new BusinessException(404, "地址不存在");
            }
            if (!addr.getUserId().equals(UserContext.getUserId())) {   // ⑨
                throw new BusinessException(403, "无权访问该地址");
            }
            return addr;
        }
    }
