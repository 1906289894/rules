package com.wb.rules.controller;

import com.wb.rules.common.result.R;
import com.wb.rules.entity.Order;
import com.wb.rules.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/test")
    public R<Order> test(@RequestBody Order order) {
        return R.success(orderService.test(order));
    }
}
