package fachri.order.vo;



import fachri.order.model.Order;
import lombok.Data;

@Data
public class ResponseTemplate {

    Order order;
    Produk produk;
    
}
