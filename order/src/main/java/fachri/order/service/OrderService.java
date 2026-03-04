package fachri.order.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fachri.order.model.Order;
import fachri.order.repository.OrderRepository;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    public List<Order> getAllOrders(){
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id){
        return orderRepository.findById(id).orElse(null);
    }

    public Order createOrder(Order Order){
        return orderRepository.save(Order);
    }

    public void deleteOrder(Long id){
        orderRepository.deleteById(id);
    }
}
