package com.yeoboya.lunch.api.v1.order.service;

import com.yeoboya.lunch.api.v1.Item.domain.Item;
import com.yeoboya.lunch.api.v1.Item.repository.ItemRepository;
import com.yeoboya.lunch.api.v1.common.exception.EntityNotFoundException;
import com.yeoboya.lunch.api.v1.member.domain.Member;
import com.yeoboya.lunch.api.v1.member.repository.MemberRepository;
import com.yeoboya.lunch.api.v1.member.response.MemberResponse;
import com.yeoboya.lunch.api.v1.order.constants.OrderStatus;
import com.yeoboya.lunch.api.v1.order.domain.GroupOrder;
import com.yeoboya.lunch.api.v1.order.domain.Order;
import com.yeoboya.lunch.api.v1.order.domain.OrderItem;
import com.yeoboya.lunch.api.v1.order.repository.GroupOrderRepository;
import com.yeoboya.lunch.api.v1.order.repository.OrderRepository;
import com.yeoboya.lunch.api.v1.order.request.GroupOrderJoin;
import com.yeoboya.lunch.api.v1.order.request.OrderItemCreate;
import com.yeoboya.lunch.api.v1.order.request.OrderRecruitmentCreate;
import com.yeoboya.lunch.api.v1.order.request.OrderSearch;
import com.yeoboya.lunch.api.v1.order.response.GroupOrderResponse;
import com.yeoboya.lunch.api.v1.order.response.OrderDetailResponse;
import com.yeoboya.lunch.api.v1.order.response.OrderRecruitmentResponse;
import com.yeoboya.lunch.api.v1.shop.domain.Shop;
import com.yeoboya.lunch.api.v1.shop.repository.ShopRepository;
import com.yeoboya.lunch.api.v1.shop.response.ShopResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;
    private final MemberRepository memberRepository;
    private final GroupOrderRepository groupOrderRepository;
    private final ItemRepository itemRepository;

    public OrderService(OrderRepository orderRepository, ShopRepository shopRepository, MemberRepository memberRepository, GroupOrderRepository groupOrderRepository, ItemRepository itemRepository) {
        this.orderRepository = orderRepository;
        this.shopRepository = shopRepository;
        this.memberRepository = memberRepository;
        this.groupOrderRepository = groupOrderRepository;
        this.itemRepository = itemRepository;
    }


    public OrderDetailResponse lunchOrderRecruitWrite(OrderRecruitmentCreate orderRecruitmentCreate) {
        Member member = memberRepository.findByEmail(orderRecruitmentCreate.getEmail()).
                orElseThrow(() -> new EntityNotFoundException("Member not found - " + orderRecruitmentCreate.getEmail()));

        Shop shop = shopRepository.findByName(orderRecruitmentCreate.getShopName()).
                orElseThrow(() -> new EntityNotFoundException("Shop not fount - " + orderRecruitmentCreate.getShopName()));

        Order order = Order.recruit(member, shop, orderRecruitmentCreate);
        Order save = orderRepository.save(order);

        return null;
    }

//    public OrderResponse order(OrderRecruitmentCreate orderRecruitmentCreate) {
//
//        Member member = memberRepository.findByEmail(orderRecruitmentCreate.getEmail()).
//                orElseThrow(() -> new EntityNotFoundException("Member not found - " + orderRecruitmentCreate.getEmail()));
//
//        List<OrderItemCreate> orderItemCreates = orderCreate.getOrderItems();
//
//        Item item;
//        List<OrderItem> orderItems = new ArrayList<>();
//
//        for (OrderItemCreate orderItemCreate : orderItemCreates) {
//            item = itemRepository.getItemByShopNameAndName(orderRecruitmentCreate.getShopName(), orderItemCreate.getItemName())
//                    .orElseThrow(() -> new EntityNotFoundException("Item not found - " + orderItemCreate.getItemName()));
//            orderItems.add(OrderItem.createOrderItem(item, item.getPrice(), orderItemCreate.getOrderQuantity()));
//        }
//
//        Order order = Order.createOrder(member, orderItems);
//        Order save = orderRepository.save(order);
//
//        return OrderResponse.builder()
//                .orderStatus(save.getStatus())
//                .orderName(save.getMember().getName())
//                .totalPrice(save.getTotalPrice())
//                .orderItems(order)
//                .build();
//    }


    public Map<String, Object> recruits(OrderSearch search, Pageable pageable) {
        Slice<Order> orders = orderRepository.orderRecruits(search, pageable);
        List<OrderRecruitmentResponse> orderRecruitmentResponses = orders.getContent().stream()
                .map(OrderRecruitmentResponse::from)
                .collect(Collectors.toList());

        return Map.of(
                "list", orderRecruitmentResponses,
                "isFirst", orders.isFirst(),
                "isLast", orders.isLast(),
                "hasNext", orders.hasNext(),
                "hasPrevious", orders.hasPrevious(),
                "pageNo", orders.getNumber() + 1
        );
    }


    public Map<String, Object> lunchRecruitByOrderId(Long orderNo) {
        Order order = orderRepository.findById(orderNo).orElseThrow(() -> new EntityNotFoundException("Order not found - " + orderNo));

        //주문모집정보
        OrderDetailResponse orderDetailResponse = OrderDetailResponse.orderInfo(order);

        //주문참가자정보
        List<GroupOrderResponse> groupOrderResponse = order.getGroupOrders().stream()
                .map((GroupOrder member) -> GroupOrderResponse.from(member.getMember(), member.getOrderItems()))
                .collect(Collectors.toList());

        //식당정보
        ShopResponse shopResponse = ShopResponse.from(order.getShop());

        //주문장 정보
        MemberResponse memberResponse = MemberResponse.from(order.getMember());

        return Map.of("order", orderDetailResponse,
                "shop", shopResponse,
                "orderMember", memberResponse,
                "group", groupOrderResponse
        );
    }


    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found - " + orderId));
        order.setStatus(OrderStatus.CANCEL);
    }

    public void lunchRecruitsJoin(GroupOrderJoin groupOrderJoin) {
        Order order = orderRepository.findById(groupOrderJoin.getOrderNo()).orElseThrow(() -> new EntityNotFoundException("Order not found - " + groupOrderJoin.getOrderNo()));

        Member member = memberRepository.findByEmail(groupOrderJoin.getEmail()).
                orElseThrow(() -> new EntityNotFoundException("Member not found - " + groupOrderJoin.getEmail()));

        List<OrderItemCreate> orderItemCreates = groupOrderJoin.getOrderItems();

        Item item;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemCreate orderItemCreate : orderItemCreates) {
            item = itemRepository.getItemByShopNameAndName(order.getShop().getName(), orderItemCreate.getItemName())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found - " + orderItemCreate.getItemName()));
            orderItems.add(OrderItem.createOrderItem(item, item.getPrice(), orderItemCreate.getOrderQuantity()));
        }

        GroupOrder groupOrder = GroupOrder.createGroupOrder(order, member, orderItems);
        groupOrderRepository.save(groupOrder);
    }

    @Transactional
    public void lunchRecruitStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found - " + orderId));
        order.setStatus(status);
    }
}