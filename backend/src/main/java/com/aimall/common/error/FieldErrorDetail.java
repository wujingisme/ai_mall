package com.aimall.common.error;

/** 单个请求字段的校验失败信息。 */
public record FieldErrorDetail(String field, String message) {}
