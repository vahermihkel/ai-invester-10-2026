package com.aiinvestor.api;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aiinvestor.execution.OrderRecordRepository;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderRecordRepository orders;
    private final AccountParams accounts;

    public OrderController(OrderRecordRepository orders, AccountParams accounts) {
        this.orders = orders;
        this.accounts = accounts;
    }

    @GetMapping("/orders")
    public List<OrderDto> orders(@RequestParam(required = false) String accountId,
                                 @RequestParam(defaultValue = "50") int limit) {
        return accounts.resolve(accountId)
                .map(account -> orders
                        .findByAccountIdOrderByCreatedAtDesc(account.getId(), PageRequest.of(0, Math.clamp(limit, 1, 500)))
                        .stream()
                        .map(order -> OrderDto.of(order, account.getBroker()))
                        .toList())
                .orElse(List.of());
    }

    @GetMapping("/orders/{id}/events")
    public void orderEvents(@PathVariable long id) {
        throw NotBuiltYet.error();
    }

    @GetMapping("/orders/{id}/fills")
    public void orderFills(@PathVariable long id) {
        throw NotBuiltYet.error();
    }

    @GetMapping("/trades")
    public void trades() {
        throw NotBuiltYet.error();
    }
}
