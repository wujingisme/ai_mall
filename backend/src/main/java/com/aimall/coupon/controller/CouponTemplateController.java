package com.aimall.coupon.controller;

import com.aimall.coupon.dto.CouponTemplateWriteRequest;
import com.aimall.coupon.service.CouponTemplateService;
import com.aimall.coupon.vo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/admin/coupon-templates")
/** 后台优惠券模板配置接口；只处理 HTTP 参数，规则由 CouponTemplateService 负责。 */
public class CouponTemplateController {
    private final CouponTemplateService service;

    public CouponTemplateController(CouponTemplateService service) { this.service = service; }

    @GetMapping
    /** 分页查询模板，可按名称和 DRAFT/ACTIVE/DISABLED 状态过滤。 */
    CouponTemplatePageResponse list(@RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return service.list(page, pageSize, keyword, status);
    }

    @GetMapping("/{id}")
    /** 查询单个模板详情。 */
    CouponTemplateResponse get(@PathVariable @Min(1) Long id) { return service.get(id); }

    @PostMapping
    /** 创建模板；新模板固定从 DRAFT 开始。 */
    ResponseEntity<CouponTemplateResponse> create(@Valid @RequestBody CouponTemplateWriteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    /** 完整替换草稿模板；启用后的模板不允许修改核心规则。 */
    CouponTemplateResponse update(@PathVariable @Min(1) Long id,
            @Valid @RequestBody CouponTemplateWriteRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/activation")
    /** 启用模板，启用前会再次检查有效期和剩余发行量。 */
    CouponTemplateResponse activate(@PathVariable @Min(1) Long id) { return service.activate(id); }

    @PostMapping("/{id}/deactivation")
    /** 停用模板；重复停用设计为幂等。 */
    CouponTemplateResponse deactivate(@PathVariable @Min(1) Long id) { return service.deactivate(id); }
}
